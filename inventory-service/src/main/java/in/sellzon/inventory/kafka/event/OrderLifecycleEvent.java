package in.sellzon.inventory.kafka.event;

import java.util.List;
import java.util.UUID;

public record OrderLifecycleEvent(
        UUID eventId,
        EventType eventType,
        UUID orderId,
        List<OrderItemReservation> items,
        UUID sellerOrderId
) {
    public enum EventType {
        ORDER_PLACED,
        PAYMENT_FAILED,
        ORDER_CANCELLED,
        ORDER_SHIPPED
    }
}
