package in.sellzon.payment.payment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateRazorpayOrderResponse(
        UUID paymentId,
        UUID orderId,
        String orderNumber,
        String razorpayOrderId,
        String keyId,
        BigDecimal amount,
        long amountInPaise,
        String currency
) {
}
