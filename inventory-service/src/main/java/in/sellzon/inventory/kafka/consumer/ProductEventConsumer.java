package in.sellzon.inventory.kafka.consumer;

import in.sellzon.inventory.inventory.entity.InventoryItem;
import in.sellzon.inventory.inventory.repository.InventoryItemRepository;
import in.sellzon.inventory.kafka.event.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventConsumer {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final InventoryItemRepository inventoryItemRepository;

    @KafkaListener(topics = "product-events", groupId = "inventory-service")
    @Transactional
    public void handleProductCreated(ProductCreatedEvent event) {

        log.info(
                "Received PRODUCT_CREATED event: marketplaceProductId={}, productId={}",
                event.marketplaceProductId(),
                event.productId()
        );

        /*
         * Idempotency:
         * Kafka can deliver the same message more than once.
         */
        if (inventoryItemRepository.findByProductId(event.productId()).isPresent()) {
            log.info("Inventory already exists for productId={}, ignoring duplicate event", event.productId());
            return;
        }

        InventoryItem item = new InventoryItem();

        item.setProductId(event.productId());
        item.setSellerId(event.sellerId());
        item.setMarketplaceProductId(event.marketplaceProductId());
        item.setSku(event.sku());
        item.setTitle(event.title());
        item.setBrand(event.brand());
        item.setStockQuantity(event.initialStock() == null ? 0 : event.initialStock());
        item.setStatus(event.status() == null ? "ACTIVE" : event.status());
        item.setLowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD);

        inventoryItemRepository.save(item);

        log.info(
                "Inventory created for product {} with stock {}",
                event.marketplaceProductId(),
                item.getStockQuantity()
        );
    }
}