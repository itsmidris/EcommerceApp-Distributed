package in.sellzon.inventory.inventory.service;

import in.sellzon.inventory.inventory.dto.request.InventoryAdjustmentRequestDTO;
import in.sellzon.inventory.inventory.dto.request.InventoryThresholdRequestDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryAdjustmentResponseDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryProductResponseDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryThresholdResponseDTO;

import java.util.List;

public interface InventoryService {
    List<InventoryProductResponseDTO> getMyInventory();
    InventoryProductResponseDTO adjustStock(String marketplaceProductId, InventoryAdjustmentRequestDTO request);
    List<InventoryAdjustmentResponseDTO> getHistory(String marketplaceProductId);
    InventoryThresholdResponseDTO updateThreshold(String marketplaceProductId, InventoryThresholdRequestDTO request);
}
