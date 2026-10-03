package in.sellzon.inventory.kafka.producer;

import in.sellzon.inventory.kafka.config.KafkaTopicConfig;
import in.sellzon.inventory.kafka.event.InventoryStockChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryStockEventProducer {

    private final KafkaTemplate<String, InventoryStockChangedEvent> kafkaTemplate;

    public void publish(InventoryStockChangedEvent event) {

        kafkaTemplate.send(
                KafkaTopicConfig.INVENTORY_STOCK_EVENTS_TOPIC,
                event.marketplaceProductId(),
                event
        );
    }
}
