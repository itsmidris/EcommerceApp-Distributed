package in.sellzon.order.order.service.impl;

import in.sellzon.order.address.client.IdentityAddressClient;
import in.sellzon.order.address.response.AddressResponse;
import in.sellzon.order.cart.client.CartClient;
import in.sellzon.order.cart.response.CartItemResponse;
import in.sellzon.order.cart.response.CartResponse;
import in.sellzon.order.catalog.client.CatalogClient;
import in.sellzon.order.catalog.response.CatalogProductResponse;
import in.sellzon.order.order.dto.request.PlaceOrderRequestDTO;
import in.sellzon.order.order.dto.response.OrderAddressResponseDTO;
import in.sellzon.order.order.dto.response.OrderItemResponseDTO;
import in.sellzon.order.order.dto.response.OrderResponseDTO;
import in.sellzon.order.order.entity.Order;
import in.sellzon.order.order.entity.OrderAddressSnapshot;
import in.sellzon.order.order.entity.OrderItem;
import in.sellzon.order.order.entity.SellerOrder;
import in.sellzon.order.order.enums.OrderStatus;
import in.sellzon.order.order.enums.PaymentStatus;
import in.sellzon.order.order.enums.SellerOrderStatus;
import in.sellzon.order.order.repository.OrderRepository;
import in.sellzon.order.order.repository.SellerOrderRepository;
import in.sellzon.order.order.service.OrderService;
import in.sellzon.order.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final SellerOrderRepository sellerOrderRepository;
    private final CurrentUserService currentUserService;
    private final IdentityAddressClient identityAddressClient;
    private final CatalogClient catalogClient;
    private final CartClient cartClient;

    @Override
    @Transactional
    public OrderResponseDTO placeOrder(PlaceOrderRequestDTO request) {

        UUID customerId = currentUserService.getCurrentUserId();
        String accessToken = currentUserService.getCurrentAccessToken();
        String clientRequestId = request.clientRequestId().trim();

        if (clientRequestId.isBlank()) {
            throw new IllegalArgumentException("Client request Id is required");
        }

        Order existing = orderRepository
                .findByCustomerIdAndClientRequestId(customerId, clientRequestId)
                .orElse(null);

        if (existing != null) {
            return map(existing);
        }

        AddressResponse address = identityAddressClient.getAddress(
                request.addressId(),
                customerId,
                accessToken
        );

        CartResponse cart = cartClient.getCart(accessToken);

        if (cart.items() == null || cart.items().isEmpty()) {
            throw new IllegalStateException("Your cart is Empty");
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerId(customerId);
        order.setClientRequestId(clientRequestId);
        order.setSubtotal(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setShippingAddress(snapshot(address));

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItemResponse cartItem : cart.items()) {

            CatalogProductResponse product = catalogClient.getProduct(
                    cartItem.marketplaceProductId(),
                    accessToken
            );

            if (!"ACTIVE".equals(product.status())) {
                throw new IllegalStateException(
                        "Product is no longer available: " + cartItem.title()
                );
            }

            if (cartItem.quantity() > product.stockQuantity()) {
                throw new IllegalStateException(
                        "Only " + product.stockQuantity()
                                + " units are available for "
                                + product.title()
                );
            }

            BigDecimal unitPrice = product.sellingPrice();
            BigDecimal itemTotal = unitPrice.multiply(
                    BigDecimal.valueOf(cartItem.quantity())
            );

            OrderItem item = new OrderItem();
            item.setProductId(product.id());
            item.setSellerId(product.sellerId());
            item.setMarketplaceProductId(product.marketplaceProductId());
            item.setSku(product.sku());
            item.setProductTitle(product.title());
            item.setSellerName(product.sellerName());
            item.setQuantity(cartItem.quantity());
            item.setUnitPrice(unitPrice);
            item.setTotalPrice(itemTotal);

            order.addItem(item);
            subtotal = subtotal.add(itemTotal);
        }

        order.setSubtotal(subtotal);

        order.setTotalAmount(subtotal.add(order.getShippingAmount()).subtract(order.getDiscountAmount()));

        createSellerOrders(order);

        Order saved = orderRepository.save(order);

        clearCartAfterCommit(accessToken);

        return map(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getMyOrder() {

        UUID customerId = currentUserService.getCurrentUserId();

        return orderRepository
                .findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getMyOrder(UUID orderId) {
        UUID customerId = currentUserService.getCurrentUserId();

        Order order = orderRepository
                .findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new IllegalStateException("Order not found"));

        return map(order);
    }

    private OrderResponseDTO map(Order order) {

        List<OrderItemResponseDTO> items = order.getItems()
                .stream()
                .map(item -> {
                    SellerOrderStatus status = item.getSellerOrder() == null
                            ? null
                            : item.getSellerOrder().getStatus();

                    return new OrderItemResponseDTO(
                            item.getMarketplaceProductId(),
                            item.getSku(),
                            item.getProductTitle(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getTotalPrice(),
                            item.getSellerName(),
                            status
                    );
                })
                .toList();

        OrderAddressSnapshot address = order.getShippingAddress();

        OrderAddressResponseDTO addressResponse = new OrderAddressResponseDTO(
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

        return new OrderResponseDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getSubtotal(),
                order.getShippingAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getOrderStatus(),
                addressResponse,
                items,
                order.getCreatedAt()
        );
    }

    private String generateOrderNumber() {

        String value;

        do {
            value = "SELLZON-"
                    + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase();
        } while (orderRepository.existsByOrderNumber(value));

        return value;
    }

    private OrderAddressSnapshot snapshot(AddressResponse address) {
        OrderAddressSnapshot snapshot = new OrderAddressSnapshot();
        snapshot.setRecipientName(address.recipientName());
        snapshot.setPhoneNumber(address.phoneNumber());
        snapshot.setAddressLine1(address.addressLine1());
        snapshot.setAddressLine2(address.addressLine2());
        snapshot.setLandmark(address.landmark());
        snapshot.setCity(address.city());
        snapshot.setState(address.state());
        snapshot.setPostalCode(address.postalCode());
        snapshot.setCountry(address.country());
        snapshot.setAddressType(address.addressType());
        return snapshot;
    }

    private void createSellerOrders(Order order) {

        Map<UUID, List<OrderItem>> itemsBySeller = new LinkedHashMap<>();

        for (OrderItem item : order.getItems()) {
            itemsBySeller
                    .computeIfAbsent(item.getSellerId(), ignored -> new ArrayList<>())
                    .add(item);
        }

        for (Map.Entry<UUID, List<OrderItem>> entry : itemsBySeller.entrySet()) {

            UUID sellerId = entry.getKey();
            List<OrderItem> sellerItems = entry.getValue();

            SellerOrder sellerOrder = new SellerOrder();
            sellerOrder.setOrder(order);
            sellerOrder.setSellerId(sellerId);
            sellerOrder.setSellerOrderNumber(generateSellerOrderNumber());
            sellerOrder.setStatus(SellerOrderStatus.PLACED);
            sellerOrder.setShippingAmount(BigDecimal.ZERO);
            sellerOrder.setDiscountAmount(BigDecimal.ZERO);

            BigDecimal sellerSubtotal = sellerItems.stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            sellerOrder.setSubtotal(sellerSubtotal);
            sellerOrder.setTotalAmount(sellerSubtotal);

            order.addSellerOrder(sellerOrder);

            for (OrderItem item : sellerItems) {
                sellerOrder.addItem(item);
            }
        }
    }

    private String generateSellerOrderNumber() {
        String value;
        do {
            value = "SO-"
                    + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 14)
                    .toUpperCase();
        } while (sellerOrderRepository.existsBySellerOrderNumber(value));
        return value;
    }

    private void clearCartAfterCommit(String accessToken) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cartClient.clearCart(accessToken);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        try {
                            cartClient.clearCart(accessToken);
                        } catch (Exception ignored) {
                            // Cart clearing can be retried by the Cart/Order integration later.
                        }
                    }
                }
        );
    }
}
