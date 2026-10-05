package in.sellzon.order.order.service.impl;

import in.sellzon.order.identity.IdentityUserResponse;
import in.sellzon.order.identity.client.IdentityUserClient;
import in.sellzon.order.order.dto.request.UpdateSellerOrderStatusRequestDTO;
import in.sellzon.order.order.dto.response.SellerOrderAddressResponseDTO;
import in.sellzon.order.order.dto.response.SellerOrderItemResponseDTO;
import in.sellzon.order.order.dto.response.SellerOrderResponseDTO;
import in.sellzon.order.order.entity.Order;
import in.sellzon.order.order.entity.OrderAddressSnapshot;
import in.sellzon.order.order.entity.SellerOrder;
import in.sellzon.order.order.enums.OrderStatus;
import in.sellzon.order.order.enums.PaymentMethod;
import in.sellzon.order.order.enums.PaymentStatus;
import in.sellzon.order.order.enums.SellerOrderStatus;
import in.sellzon.order.order.repository.OrderItemRepository;
import in.sellzon.order.order.repository.OrderRepository;
import in.sellzon.order.order.repository.SellerOrderRepository;
import in.sellzon.order.order.service.SellerOrderService;
import in.sellzon.order.kafka.event.OrderLifecycleEvent;
import in.sellzon.order.kafka.producer.OrderEventProducer;
import in.sellzon.order.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerOrderServiceImpl implements SellerOrderService {

    private final SellerOrderRepository sellerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;
    private final IdentityUserClient identityUserClient;
    private final OrderEventProducer orderEventProducer;

    @Override
    @Transactional(readOnly = true)
    public List<SellerOrderResponseDTO> getMyOrders(SellerOrderStatus status) {

        UUID sellerId = currentUserService.getCurrentUserId();

        List<SellerOrder> orders = status == null
                ? sellerOrderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                : sellerOrderRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(sellerId, status);

        String accessToken = currentUserService.getCurrentAccessToken();

        return orders.stream()
                .map(order -> map(order, accessToken))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SellerOrderResponseDTO getMyOrder(UUID sellerOrderId) {

        UUID sellerId = currentUserService.getCurrentUserId();

        SellerOrder sellerOrder = sellerOrderRepository
                .findByIdAndSellerId(sellerOrderId, sellerId)
                .orElseThrow(() -> new IllegalStateException("Seller order not found"));

        return map(sellerOrder, currentUserService.getCurrentAccessToken());
    }

    @Override
    @Transactional
    public SellerOrderResponseDTO updateStatus(UUID sellerOrderId, UpdateSellerOrderStatusRequestDTO request) {

        UUID sellerId = currentUserService.getCurrentUserId();

        SellerOrder sellerOrder = sellerOrderRepository
                .findByIdAndSellerId(sellerOrderId, sellerId)
                .orElseThrow(() -> new IllegalStateException("Seller order not found"));

        SellerOrderStatus currentStatus = sellerOrder.getStatus();
        SellerOrderStatus newStatus = request.status();

        validateTransition(currentStatus, newStatus);

        if (newStatus == SellerOrderStatus.CONFIRMED
                && sellerOrder.getOrder().getPaymentMethod() == PaymentMethod.ONLINE
                && sellerOrder.getOrder().getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Online payment must be successful before the seller can confirm this order");
        }

        if (currentStatus == newStatus) {
            return map(sellerOrder, currentUserService.getCurrentAccessToken());
        }

        sellerOrder.setStatus(newStatus);
        sellerOrderRepository.save(sellerOrder);

        Order order = sellerOrder.getOrder();
        OrderStatus previousOrderStatus = order.getOrderStatus();
        recalculateCustomerOrderStatus(order);
        orderRepository.save(order);

        if (previousOrderStatus != OrderStatus.CANCELLED
                && order.getOrderStatus() == OrderStatus.CANCELLED) {
            orderEventProducer.orderCancelled(
                    OrderLifecycleEvent.orderCancelled(order.getId())
            );
        }

        // Inventory is finalized per seller shipment. This must be emitted for
        // every seller order that reaches SHIPPED, not only the first one that
        // causes the aggregate customer order to become SHIPPED.
        if (newStatus == SellerOrderStatus.SHIPPED) {
            orderEventProducer.orderShipped(
                    OrderLifecycleEvent.orderShipped(
                            order.getId(),
                            sellerOrder.getId(),
                            sellerOrder.getItems().stream()
                                    .map(item -> item.getProductId())
                                    .toList()
                    )
            );
        }

        return map(sellerOrder, currentUserService.getCurrentAccessToken());
    }

    private SellerOrderResponseDTO map(SellerOrder sellerOrder, String accessToken) {

        Order order = sellerOrder.getOrder();
        OrderAddressSnapshot address = order.getShippingAddress();

        IdentityUserResponse customer = identityUserClient.getUserById(order.getCustomerId(), accessToken);

        String customerName = (safe(customer.firstName()) + " " + safe(customer.lastName())).trim();

        SellerOrderAddressResponseDTO addressResponse =
                new SellerOrderAddressResponseDTO(
                        address.getRecipientName(),
                        address.getPhoneNumber(),
                        address.getAddressLine1(),
                        address.getAddressLine2(),
                        address.getLandmark(),
                        address.getCity(),
                        address.getState(),
                        address.getPostalCode(),
                        address.getCountry(),
                        address.getAddressType()
                );

        List<SellerOrderItemResponseDTO> items = sellerOrder.getItems()
                .stream()
                .map(item -> new SellerOrderItemResponseDTO(
                        item.getMarketplaceProductId(),
                        item.getSku(),
                        item.getProductTitle(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotalPrice()
                ))
                .toList();

        return new SellerOrderResponseDTO(
                sellerOrder.getId(),
                sellerOrder.getSellerOrderNumber(),
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                customerName,
                customer.phoneNumber(),
                sellerOrder.getSubtotal(),
                sellerOrder.getShippingAmount(),
                sellerOrder.getDiscountAmount(),
                sellerOrder.getTotalAmount(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                sellerOrder.getStatus(),
                addressResponse,
                items,
                sellerOrder.getCreatedAt(),
                sellerOrder.getUpdatedAt()
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void validateTransition(SellerOrderStatus current, SellerOrderStatus target) {

        if (target == null) {
            throw new IllegalArgumentException("Order status is required");
        }

        if (current == SellerOrderStatus.CANCELLED || current == SellerOrderStatus.DELIVERED) {
            throw new IllegalStateException("Order cannot be changed after it reaches " + current);
        }

        boolean allowed = switch (current) {
            case PENDING, PLACED ->
                    EnumSet.of(
                            SellerOrderStatus.CONFIRMED,
                            SellerOrderStatus.CANCELLED
                    ).contains(target);

            case CONFIRMED ->
                    EnumSet.of(
                            SellerOrderStatus.PROCESSING,
                            SellerOrderStatus.CANCELLED
                    ).contains(target);

            case PROCESSING ->
                    EnumSet.of(
                            SellerOrderStatus.SHIPPED,
                            SellerOrderStatus.CANCELLED
                    ).contains(target);

            case SHIPPED -> target == SellerOrderStatus.DELIVERED;

            case DELIVERED, CANCELLED -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Invalid order status transition: "
                            + current + " -> " + target
            );
        }
    }

    private void recalculateCustomerOrderStatus(Order order) {

        List<SellerOrder> sellerOrders = sellerOrderRepository
                .findByOrderIdOrderByCreatedAtAsc(order.getId());

        if (sellerOrders.isEmpty()) {
            return;
        }

        List<SellerOrder> activeOrders = sellerOrders.stream()
                .filter(orderPart -> orderPart.getStatus() != SellerOrderStatus.CANCELLED)
                .toList();

        if (activeOrders.isEmpty()) {
            order.setOrderStatus(OrderStatus.CANCELLED);
            return;
        }

        boolean allDelivered = activeOrders.stream()
                .allMatch(orderPart -> orderPart.getStatus() == SellerOrderStatus.DELIVERED);

        if (allDelivered) {
            order.setOrderStatus(OrderStatus.DELIVERED);
            return;
        }

        boolean anyShippedOrDelivered = activeOrders.stream()
                .anyMatch(orderPart ->
                        orderPart.getStatus() == SellerOrderStatus.SHIPPED
                                || orderPart.getStatus() == SellerOrderStatus.DELIVERED
                );

        if (anyShippedOrDelivered) {
            order.setOrderStatus(OrderStatus.SHIPPED);
            return;
        }

        boolean anyProcessing = activeOrders.stream()
                .anyMatch(orderPart -> orderPart.getStatus() == SellerOrderStatus.PROCESSING);

        if (anyProcessing) {
            order.setOrderStatus(OrderStatus.PROCESSING);
            return;
        }

        boolean allConfirmed = activeOrders.stream()
                .allMatch(orderPart -> orderPart.getStatus() == SellerOrderStatus.CONFIRMED);

        order.setOrderStatus(
                allConfirmed ? OrderStatus.CONFIRMED : OrderStatus.PLACED
        );
    }
}
