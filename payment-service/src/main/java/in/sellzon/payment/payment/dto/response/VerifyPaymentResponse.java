package in.sellzon.payment.payment.dto.response;

import in.sellzon.payment.payment.enums.PaymentRecordStatus;

import java.util.UUID;

public record VerifyPaymentResponse(
        UUID paymentId,
        UUID orderId,
        String razorpayPaymentId,
        PaymentRecordStatus paymentStatus,
        String message
) {
}
