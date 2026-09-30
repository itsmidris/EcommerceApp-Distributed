package in.sellzon.payment.order.response;

import in.sellzon.payment.order.enums.PaymentMethod;
import in.sellzon.payment.order.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPaymentContext(
        UUID id,
        String orderNumber,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus
) {
}
