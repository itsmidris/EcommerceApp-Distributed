package in.sellzon.catalog.product.controller;

import in.sellzon.catalog.product.dto.request.InventoryStockUpdateRequest;
import in.sellzon.catalog.product.dto.response.ProductResponseDTO;
import in.sellzon.catalog.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;


    @GetMapping("/by-id/{productId}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable UUID productId) {
        return ResponseEntity.ok(productService.getProductByIdInternal(productId));
    }

    @PatchMapping("/{marketplaceProductId}/stock")
    public ResponseEntity<Void> updateStock(
            @PathVariable String marketplaceProductId,
            @Valid @RequestBody InventoryStockUpdateRequest request) {
        productService.updateStockFromInventory(marketplaceProductId, request.stockQuantity());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{marketplaceProductId}")
    public ResponseEntity<ProductResponseDTO> getProduct(@PathVariable String marketplaceProductId) {
        return ResponseEntity.ok(productService.getProduct(marketplaceProductId));
    }


    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ProductResponseDTO>> getProductsBySeller(@PathVariable UUID sellerId) {
        return ResponseEntity.ok(productService.getProductsBySellerIdInternal(sellerId));
    }
}