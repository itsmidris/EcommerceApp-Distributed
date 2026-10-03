package in.sellzon.catalog.kafka.event;

import java.util.UUID;

public record ProductCreatedEvent(

        UUID eventId,
        UUID productId,
        String marketplaceProductId,
        UUID sellerId,
        String sku,
        String title,
        String brand,
        Integer initialStock,
        String status
) {
}
