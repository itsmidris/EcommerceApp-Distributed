package in.sellzon.order.kafka.event;

import java.util.UUID;

public record OrderItemReservation(
        UUID productId,
        int quantity
) {
}
