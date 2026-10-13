package in.sellzon.catalog.kafka.producer;

import in.sellzon.catalog.kafka.event.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCreatedAfterCommitListener {

    private final ProductEventProducer productEventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductCreatedEvent event) {
        log.info(
                "Publishing PRODUCT_CREATED after catalog transaction commit: marketplaceProductId={}",
                event.marketplaceProductId()
        );

        productEventProducer.publish(event);
    }
}