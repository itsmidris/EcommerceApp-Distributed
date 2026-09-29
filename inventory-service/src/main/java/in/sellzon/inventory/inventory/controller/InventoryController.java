package in.sellzon.inventory.inventory.controller;

import in.sellzon.inventory.inventory.dto.request.InventoryAdjustmentRequestDTO;
import in.sellzon.inventory.inventory.dto.request.InventoryThresholdRequestDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryAdjustmentResponseDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryProductResponseDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryThresholdResponseDTO;
import in.sellzon.inventory.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryProductResponseDTO>> getMyInventory() {
        return ResponseEntity.ok(inventoryService.getMyInventory());
    }

    @PatchMapping("/{marketplaceProductId}")
    public ResponseEntity<InventoryProductResponseDTO> adjustStock(@PathVariable String marketplaceProductId, @Valid @RequestBody InventoryAdjustmentRequestDTO request) {
        return ResponseEntity.ok(inventoryService.adjustStock(marketplaceProductId, request));
    }

    @GetMapping("/{marketplaceProductId}/history")
    public ResponseEntity<List<InventoryAdjustmentResponseDTO>> getHistory(@PathVariable String marketplaceProductId) {
        return ResponseEntity.ok(inventoryService.getHistory(marketplaceProductId));
    }

    @PatchMapping("/{marketplaceProductId}/threshold")
    public ResponseEntity<InventoryThresholdResponseDTO> updateThreshold(@PathVariable String marketplaceProductId, @Valid @RequestBody InventoryThresholdRequestDTO request) {
        return ResponseEntity.ok(inventoryService.updateThreshold(marketplaceProductId, request));
    }
}
