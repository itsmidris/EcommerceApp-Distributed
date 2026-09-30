package in.sellzon.payment.order.client;

import in.sellzon.payment.order.response.OrderPaymentContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderClient {

    private final RestClient orderRestClient;

    public OrderPaymentContext getPaymentContext(UUID orderId, String accessToken) {
        return orderRestClient.get()
                .uri("/api/customer/orders/{orderId}/payment-context", orderId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(OrderPaymentContext.class);
    }

    public void markPaid(UUID orderId, String razorpayPaymentId, String accessToken) {
        orderRestClient.patch()
                .uri("/api/customer/orders/{orderId}/payment-status", orderId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .body(new PaymentStatusUpdateBody("PAID", razorpayPaymentId))
                .retrieve()
                .toBodilessEntity();
    }

    public void markFailed(UUID orderId, String accessToken) {
        orderRestClient.patch()
                .uri("/api/customer/orders/{orderId}/payment-status", orderId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .body(new PaymentStatusUpdateBody("FAILED", null))
                .retrieve()
                .toBodilessEntity();
    }

    private record PaymentStatusUpdateBody(String paymentStatus, String razorpayPaymentId) {
    }
}
