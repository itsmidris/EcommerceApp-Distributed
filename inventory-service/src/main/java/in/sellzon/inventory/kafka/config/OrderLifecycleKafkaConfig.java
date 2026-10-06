package in.sellzon.inventory.kafka.config;

import in.sellzon.inventory.kafka.event.OrderLifecycleEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class OrderLifecycleKafkaConfig {

    @Bean
    public ConsumerFactory<String, OrderLifecycleEvent> orderLifecycleConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-order-lifecycle");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JacksonJsonDeserializer<OrderLifecycleEvent> deserializer =
                new JacksonJsonDeserializer<>(OrderLifecycleEvent.class)
                        .ignoreTypeHeaders()
                        .trustedPackages("in.sellzon.inventory.kafka.event");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean(name = "orderLifecycleKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrderLifecycleEvent>
    orderLifecycleKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderLifecycleEvent> orderLifecycleConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, OrderLifecycleEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderLifecycleConsumerFactory);
        return factory;
    }
}
