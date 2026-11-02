package in.sellzon.inventory.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryReservationRequestDTO(
        @NotNull
        UUID orderId,
        @NotNull
        UUID productId,
        @NotNull
        @Min(1)
        Integer quantity
) {
}
