package in.sellzon.inventory.kafka.event;

import java.util.UUID;

public record OrderItemReservation(
        UUID productId,
        int quantity
) {
}
