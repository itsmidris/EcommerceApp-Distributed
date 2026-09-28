package in.sellzon.order.order.dto.response;

import in.sellzon.order.order.enums.PaymentMethod;
import in.sellzon.order.order.enums.PaymentStatus;
import in.sellzon.order.order.enums.SellerOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SellerOrderResponseDTO(
        UUID id,
        String sellerOrderNumber,
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String customerName,
        String customerPhone,
        BigDecimal subtotal,
        BigDecimal shippingAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        SellerOrderStatus status,
        SellerOrderAddressResponseDTO shippingAddress,
        List<SellerOrderItemResponseDTO> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
