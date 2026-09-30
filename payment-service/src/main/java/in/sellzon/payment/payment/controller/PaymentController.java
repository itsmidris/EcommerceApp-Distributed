package in.sellzon.payment.payment.controller;

import in.sellzon.payment.payment.dto.request.CreateRazorpayOrderRequest;
import in.sellzon.payment.payment.dto.request.VerifyPaymentRequest;
import in.sellzon.payment.payment.dto.response.CreateRazorpayOrderResponse;
import in.sellzon.payment.payment.dto.response.VerifyPaymentResponse;
import in.sellzon.payment.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/razorpay/order")
    public ResponseEntity<CreateRazorpayOrderResponse> createRazorpayOrder(
            @Valid @RequestBody CreateRazorpayOrderRequest request) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(request));
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<VerifyPaymentResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }

    @PostMapping("/razorpay/failed")
    public ResponseEntity<Void> markFailed(
            @RequestParam String razorpayOrderId,
            @RequestParam(defaultValue = "Payment failed in Razorpay Checkout") String reason) {
        paymentService.markPaymentFailed(razorpayOrderId, reason);
        return ResponseEntity.noContent().build();
    }
}
