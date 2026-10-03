package in.sellzon.catalog.kafka.producer;

import in.sellzon.catalog.kafka.event.ProductCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductEventProducer {

    public static final String PRODUCT_EVENTS_TOPIC = "product-events";

    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    public ProductEventProducer(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(ProductCreatedEvent event) {

        kafkaTemplate.send(
                PRODUCT_EVENTS_TOPIC,
                event.marketplaceProductId(),
                event
        );
    }
}