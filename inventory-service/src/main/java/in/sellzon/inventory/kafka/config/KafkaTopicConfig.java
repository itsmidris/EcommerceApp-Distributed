package in.sellzon.inventory.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String INVENTORY_STOCK_EVENTS_TOPIC = "inventory-stock-events";

    @Bean
    public NewTopic inventoryStockEventsTopic() {
        return TopicBuilder
                .name(INVENTORY_STOCK_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
