package in.sellzon.inventory.inventory.dto.response;

import in.sellzon.inventory.inventory.enums.InventoryAdjustmentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryAdjustmentResponseDTO(
        UUID id,
        String marketplaceProductId,
        String sku,
        InventoryAdjustmentType type,
        Integer quantityChange,
        Integer previousStock,
        Integer newStock,
        String reason,
        LocalDateTime createdAt
) {
}
