package in.sellzon.order.order.dto.response;


import in.sellzon.order.order.enums.OrderStatus;
import in.sellzon.order.order.enums.PaymentMethod;
import in.sellzon.order.order.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponseDTO(
        UUID id,
        String orderNumber,
        BigDecimal subtotal,
        BigDecimal shippingAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        OrderStatus orderStatus,
        OrderAddressResponseDTO shippingAddress,
        List<OrderItemResponseDTO> items,
        LocalDateTime createdAt
) {
}
