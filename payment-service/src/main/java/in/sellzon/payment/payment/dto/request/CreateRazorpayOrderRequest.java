package in.sellzon.payment.payment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateRazorpayOrderRequest(
        @NotNull UUID orderId
) {
}
