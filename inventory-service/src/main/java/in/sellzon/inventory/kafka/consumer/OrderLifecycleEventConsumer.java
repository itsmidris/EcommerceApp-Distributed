package in.sellzon.inventory.kafka.consumer;

import in.sellzon.inventory.inventory.entity.InventoryProcessedEvent;
import in.sellzon.inventory.inventory.dto.request.InventoryReservationRequestDTO;
import in.sellzon.inventory.inventory.repository.InventoryProcessedEventRepository;
import in.sellzon.inventory.inventory.service.InventoryReservationService;
import in.sellzon.inventory.kafka.event.OrderLifecycleEvent;
import in.sellzon.inventory.kafka.event.OrderItemReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderLifecycleEventConsumer {

    private final InventoryReservationService reservationService;
    private final InventoryProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "order-events",
            containerFactory = "orderLifecycleKafkaListenerContainerFactory"
    )
    @Transactional
    public void handle(OrderLifecycleEvent event) {

        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Ignoring duplicate inventory lifecycle event {}", event.eventId());
            return;
        }

        log.info(
                "Received order lifecycle event: type={}, eventId={}, orderId={}",
                event.eventType(), event.eventId(), event.orderId()
        );

        switch (event.eventType()) {
            case ORDER_PLACED -> handleOrderPlaced(event);
            case PAYMENT_FAILED, ORDER_CANCELLED -> reservationService.releaseOrder(event.orderId());
            case ORDER_SHIPPED -> reservationService.commitProducts(event.orderId(), event.items().stream().map(OrderItemReservation::productId).toList());
        }

        processedEventRepository.save(new InventoryProcessedEvent(event.eventId(), event.eventType().name()));
    }

    private void handleOrderPlaced(OrderLifecycleEvent event) {

        if (event.items() == null || event.items().isEmpty()) {
            log.warn("OrderPlacedEvent contains no items: orderId={}", event.orderId());
            return;
        }

        for (var item : event.items()) {
            reservationService.reserve(
                    new InventoryReservationRequestDTO(
                            event.orderId(),
                            item.productId(),
                            item.quantity(),
                            null
                    )
            );
        }
    }
}
