package in.sellzon.inventory.kafka.event;

import java.util.UUID;

public record InventoryStockChangedEvent(
        UUID eventId,
        UUID productId,
        String marketplaceProductId,
        Integer stockQuantity
) {
}