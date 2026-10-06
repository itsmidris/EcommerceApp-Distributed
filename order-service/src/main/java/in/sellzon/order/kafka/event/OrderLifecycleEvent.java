package in.sellzon.order.kafka.event;

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

    public static OrderLifecycleEvent orderPlaced(UUID orderId, List<OrderItemReservation> items) {
        return new OrderLifecycleEvent(UUID.randomUUID(), EventType.ORDER_PLACED, orderId, items, null);
    }

    public static OrderLifecycleEvent paymentFailed(UUID orderId) {
        return new OrderLifecycleEvent(UUID.randomUUID(), EventType.PAYMENT_FAILED, orderId, null, null);
    }

    public static OrderLifecycleEvent orderCancelled(UUID orderId) {
        return new OrderLifecycleEvent(UUID.randomUUID(), EventType.ORDER_CANCELLED, orderId, null, null);
    }

    public static OrderLifecycleEvent orderShipped(UUID orderId, UUID sellerOrderId, List<OrderItemReservation> items) {
        return new OrderLifecycleEvent(UUID.randomUUID(), EventType.ORDER_SHIPPED, orderId, items, sellerOrderId);
    }
}
