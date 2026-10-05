package in.sellzon.order.kafka.producer;

import in.sellzon.order.kafka.config.KafkaTopicConfig;
import in.sellzon.order.kafka.event.OrderLifecycleEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishAfterCommit(OrderLifecycleEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(event);
            }
        });
    }

    private void publish(OrderLifecycleEvent event) {
        kafkaTemplate.send(
                KafkaTopicConfig.ORDER_EVENTS_TOPIC,
                event.orderId().toString(),
                event
        );
    }

    public void orderPlaced(OrderLifecycleEvent event) {
        publishAfterCommit(event);
    }

    public void paymentFailed(OrderLifecycleEvent event) {
        publishAfterCommit(event);
    }

    public void orderCancelled(OrderLifecycleEvent event) {
        publishAfterCommit(event);
    }

    public void orderShipped(OrderLifecycleEvent event) {
        publishAfterCommit(event);
    }
}
