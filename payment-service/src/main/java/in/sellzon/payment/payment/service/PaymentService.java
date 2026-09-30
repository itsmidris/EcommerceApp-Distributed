package in.sellzon.payment.payment.service;

import in.sellzon.payment.payment.dto.request.CreateRazorpayOrderRequest;
import in.sellzon.payment.payment.dto.request.VerifyPaymentRequest;
import in.sellzon.payment.payment.dto.response.CreateRazorpayOrderResponse;
import in.sellzon.payment.payment.dto.response.VerifyPaymentResponse;

public interface PaymentService {
    CreateRazorpayOrderResponse createRazorpayOrder(CreateRazorpayOrderRequest request);
    VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request);
    void markPaymentFailed(String razorpayOrderId, String reason);
}
