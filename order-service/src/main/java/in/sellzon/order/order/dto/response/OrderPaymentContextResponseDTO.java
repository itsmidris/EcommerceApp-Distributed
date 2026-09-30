package in.sellzon.order.order.dto.response;

import in.sellzon.order.order.enums.PaymentMethod;
import in.sellzon.order.order.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPaymentContextResponseDTO(
        UUID id,
        String orderNumber,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus
) {
}
