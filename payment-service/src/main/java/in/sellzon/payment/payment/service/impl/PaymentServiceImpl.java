package in.sellzon.payment.payment.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import in.sellzon.payment.order.client.OrderClient;
import in.sellzon.payment.order.response.OrderPaymentContext;
import in.sellzon.payment.security.CurrentUserService;
import in.sellzon.payment.payment.dto.request.CreateRazorpayOrderRequest;
import in.sellzon.payment.payment.dto.request.VerifyPaymentRequest;
import in.sellzon.payment.payment.dto.response.CreateRazorpayOrderResponse;
import in.sellzon.payment.payment.dto.response.VerifyPaymentResponse;
import in.sellzon.payment.payment.entity.Payment;
import in.sellzon.payment.payment.enums.PaymentRecordStatus;
import in.sellzon.payment.payment.repository.PaymentRepository;
import in.sellzon.payment.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final CurrentUserService currentUserService;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Override
    @Transactional
    public CreateRazorpayOrderResponse createRazorpayOrder(CreateRazorpayOrderRequest request) {

        UUID customerId = currentUserService.getCurrentUserId();
        String accessToken = currentUserService.getCurrentAccessToken();

        OrderPaymentContext orderContext = orderClient.getPaymentContext(request.orderId(), accessToken);

        if (orderContext == null) {
            throw new IllegalStateException("Order not found");
        }

        if (orderContext.paymentMethod() != in.sellzon.payment.order.enums.PaymentMethod.ONLINE) {
            throw new IllegalStateException("Razorpay is only available for ONLINE orders");
        }

        if (orderContext.paymentStatus() == in.sellzon.payment.order.enums.PaymentStatus.PAID) {
            throw new IllegalStateException("Order is already paid");
        }

        Payment existing = paymentRepository
                .findByOrderIdAndCustomerId(request.orderId(), customerId)
                .orElse(null);

        if (existing != null && existing.getStatus() == PaymentRecordStatus.CREATED) {
            return response(existing);
        }

        BigDecimal amount = orderContext.totalAmount().setScale(2, RoundingMode.HALF_UP);
        long amountInPaise = amount.movePointRight(2).longValueExact();

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            options.put("receipt", orderContext.orderNumber());

            Order razorpayOrder = client.orders.create(options);

            Payment payment = existing != null ? existing : new Payment();

            payment.setOrderId(request.orderId());
            payment.setCustomerId(customerId);
            payment.setOrderNumber(orderContext.orderNumber());
            payment.setAmount(amount);
            payment.setCurrency("INR");
            payment.setRazorpayOrderId(razorpayOrder.get("id"));
            payment.setRazorpayPaymentId(null);
            payment.setRazorpaySignature(null);
            payment.setStatus(PaymentRecordStatus.CREATED);
            payment.setFailureReason(null);
            payment.setPaidAt(null);

            Payment saved = paymentRepository.save(payment);

            return response(saved);

        } catch (RazorpayException e) {
            throw new IllegalStateException("Unable to create Razorpay order: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {

        UUID customerId = currentUserService.getCurrentUserId();
        String accessToken = currentUserService.getCurrentAccessToken();

        Payment payment = paymentRepository
                .findByOrderIdAndCustomerId(request.orderId(), customerId)
                .orElseThrow(() -> new IllegalStateException("Payment record not found"));

        if (!payment.getRazorpayOrderId().equals(request.razorpayOrderId())) {
            throw new IllegalStateException("Razorpay order does not match Sellzon payment");
        }

        if (payment.getStatus() == PaymentRecordStatus.SUCCESS) {
            return new VerifyPaymentResponse(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getRazorpayPaymentId(),
                    payment.getStatus(),
                    "Payment already verified"
            );
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.razorpayOrderId());
            options.put("razorpay_payment_id", request.razorpayPaymentId());
            options.put("razorpay_signature", request.razorpaySignature());

            boolean valid = Utils.verifyPaymentSignature(options, keySecret);

            if (!valid) {
                payment.setStatus(PaymentRecordStatus.FAILED);
                payment.setFailureReason("Invalid Razorpay signature");
                paymentRepository.save(payment);
                throw new IllegalStateException("Payment signature verification failed");
            }

            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            com.razorpay.Payment razorpayPayment =
                    client.payments.fetch(request.razorpayPaymentId());

            String razorpayStatus = razorpayPayment.get("status");

            if (!"captured".equalsIgnoreCase(razorpayStatus)) {
                payment.setStatus(PaymentRecordStatus.FAILED);
                payment.setFailureReason("Razorpay payment status: " + razorpayStatus);
                paymentRepository.save(payment);
                throw new IllegalStateException(
                        "Payment is not captured yet. Current Razorpay status: " + razorpayStatus
                );
            }

            long razorpayAmount = ((Number) razorpayPayment.get("amount")).longValue();
            long expectedAmount = payment.getAmount()
                    .movePointRight(2)
                    .longValueExact();

            if (razorpayAmount != expectedAmount) {
                payment.setStatus(PaymentRecordStatus.FAILED);
                payment.setFailureReason("Payment amount mismatch");
                paymentRepository.save(payment);
                throw new IllegalStateException("Payment amount does not match the Sellzon order");
            }

            payment.setRazorpayPaymentId(request.razorpayPaymentId());
            payment.setRazorpaySignature(request.razorpaySignature());
            payment.setStatus(PaymentRecordStatus.SUCCESS);
            payment.setFailureReason(null);
            payment.setPaidAt(LocalDateTime.now());

            Payment saved = paymentRepository.save(payment);

            orderClient.markPaid(
                    request.orderId(),
                    request.razorpayPaymentId(),
                    accessToken
            );

            return new VerifyPaymentResponse(
                    saved.getId(),
                    saved.getOrderId(),
                    saved.getRazorpayPaymentId(),
                    saved.getStatus(),
                    "Payment verified successfully"
            );

        } catch (RazorpayException e) {
            payment.setStatus(PaymentRecordStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);
            throw new IllegalStateException("Unable to verify Razorpay payment", e);
        }
    }

    @Override
    @Transactional
    public void markPaymentFailed(String razorpayOrderId, String reason) {
        Payment payment = paymentRepository
                .findByRazorpayOrderIdAndCustomerId(razorpayOrderId, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Payment record not found"));

        if (payment.getStatus() == PaymentRecordStatus.SUCCESS) {
            return;
        }

        payment.setStatus(PaymentRecordStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        orderClient.markFailed(payment.getOrderId(), currentUserService.getCurrentAccessToken());
    }

    private CreateRazorpayOrderResponse response(Payment payment) {
        long paise = payment.getAmount()
                .movePointRight(2)
                .longValueExact();

        return new CreateRazorpayOrderResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getOrderNumber(),
                payment.getRazorpayOrderId(),
                keyId,
                payment.getAmount(),
                paise,
                payment.getCurrency()
        );
    }
}
