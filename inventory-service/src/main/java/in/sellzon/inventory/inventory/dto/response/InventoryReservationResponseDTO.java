package in.sellzon.inventory.inventory.dto.response;

import in.sellzon.inventory.inventory.enums.InventoryReservationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryReservationResponseDTO(
        UUID reservationId,
        UUID orderId,
        UUID productId,
        String marketplaceProductId,
        Integer quantity,
        InventoryReservationStatus status,
        LocalDateTime expiresAt
) {
}
