package in.sellzon.catalog.kafka.consumer;

import in.sellzon.catalog.kafka.event.InventoryStockChangedEvent;
import in.sellzon.catalog.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryStockChangedConsumer {

    private final ProductService productService;

    @KafkaListener(topics = "inventory-stock-events", groupId = "catalog-service")
    @Transactional
    public void handleStockChanged(InventoryStockChangedEvent event) {

        log.info(
                "Received INVENTORY_STOCK_CHANGED event: marketplaceProductId={}, stock={}",
                event.marketplaceProductId(),
                event.stockQuantity()
        );

        productService.updateStockFromInventory(
                event.marketplaceProductId(),
                event.stockQuantity()
        );

        log.info(
                "Catalog stock snapshot updated: marketplaceProductId={}, stock={}",
                event.marketplaceProductId(),
                event.stockQuantity()
        );
    }
}
