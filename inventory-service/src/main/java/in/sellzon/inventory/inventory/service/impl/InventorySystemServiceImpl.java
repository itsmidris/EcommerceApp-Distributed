package in.sellzon.inventory.inventory.service.impl;

import in.sellzon.inventory.inventory.entity.InventoryAdjustment;
import in.sellzon.inventory.inventory.entity.InventoryItem;
import in.sellzon.inventory.inventory.enums.InventoryAdjustmentType;
import in.sellzon.inventory.inventory.repository.InventoryAdjustmentRepository;
import in.sellzon.inventory.inventory.service.InventorySystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventorySystemServiceImpl implements InventorySystemService {

    private final InventoryAdjustmentRepository repository;

    @Override
    @Transactional
    public InventoryAdjustment recordMovement(
            InventoryItem item, int previousStock,
            int newStock, InventoryAdjustmentType type,
            String reason) {

        InventoryAdjustment adjustment = new InventoryAdjustment();

        adjustment.setProductId(item.getProductId());
        adjustment.setSellerId(item.getSellerId());
        adjustment.setMarketplaceProductId(item.getMarketplaceProductId());
        adjustment.setSku(item.getSku());
        adjustment.setType(type);
        adjustment.setQuantityChange(newStock - previousStock);
        adjustment.setPreviousStock(previousStock);
        adjustment.setNewStock(newStock);
        adjustment.setReason(reason);

        return repository.save(adjustment);
    }
}
