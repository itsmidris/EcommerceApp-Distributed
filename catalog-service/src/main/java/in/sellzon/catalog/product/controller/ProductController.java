package in.sellzon.catalog.product.controller;

import in.sellzon.catalog.product.dto.request.CreateProductRequestDTO;
import in.sellzon.catalog.product.dto.request.UpdateProductRequestDTO;
import in.sellzon.catalog.product.dto.response.ProductResponseDTO;
import in.sellzon.catalog.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody CreateProductRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getMyProducts() {
        return ResponseEntity.ok(productService.getMyProducts());
    }

    @GetMapping("/{marketplaceProductId}")
    public ResponseEntity<ProductResponseDTO> getProduct(@PathVariable String marketplaceProductId) {
        return ResponseEntity.ok(productService.getProduct(marketplaceProductId));
    }

    @PutMapping("/{marketplaceProductId}")
    public ResponseEntity<ProductResponseDTO> updateProduct(@PathVariable String marketplaceProductId, @Valid @RequestBody UpdateProductRequestDTO request) {
        return ResponseEntity.ok(productService.updateProduct(marketplaceProductId, request));
    }

    @PatchMapping("/{marketplaceProductId}/deactivate")
    public ResponseEntity<Void> deactivateProduct(@PathVariable String marketplaceProductId) {
        productService.deactivateProduct(marketplaceProductId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{marketplaceProductId}/activate")
    public ResponseEntity<Void> activateProduct(@PathVariable String marketplaceProductId) {
        productService.activateProduct(marketplaceProductId);
        return ResponseEntity.noContent().build();
    }
}
