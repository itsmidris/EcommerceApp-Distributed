package in.sellzon.inventory.inventory.service;

import in.sellzon.inventory.inventory.entity.InventoryAdjustment;
import in.sellzon.inventory.inventory.entity.InventoryItem;
import in.sellzon.inventory.inventory.enums.InventoryAdjustmentType;

public interface InventorySystemService {
    InventoryAdjustment recordMovement(InventoryItem item, int previousStock, int newStock, InventoryAdjustmentType type, String reason);
}
