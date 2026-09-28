package in.sellzon.order.order.dto.request;

import in.sellzon.order.order.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PlaceOrderRequestDTO(
        @NotNull(message = "Shipping address is required")
        UUID addressId,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotNull(message = "Client request Id is required")
        @Size(max = 100)
        String clientRequestId
) {
}
