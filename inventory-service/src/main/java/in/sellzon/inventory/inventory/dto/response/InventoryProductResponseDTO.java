package in.sellzon.inventory.inventory.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryProductResponseDTO(
        UUID productId,
        String marketplaceProductId,
        String sku,
        String title,
        String brand,
        Integer availableQuantity,
        Integer blockedQuantity,
        Integer lowStockThreshold,
        boolean lowStock,
        boolean outOfStock,
        String status,
        LocalDateTime updatedAt
) {
}
