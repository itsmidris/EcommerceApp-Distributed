package in.sellzon.inventory.inventory.dto.response;

import java.util.UUID;

public record InventoryAvailabilityResponseDTO(
        UUID productId,
        String marketplaceProductId,
        Integer stockQuantity,
        Integer reservedQuantity,
        Integer availableQuantity
) {
}
