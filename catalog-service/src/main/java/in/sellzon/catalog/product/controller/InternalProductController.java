package in.sellzon.catalog.product.controller;

import in.sellzon.catalog.product.dto.response.ProductResponseDTO;
import in.sellzon.catalog.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/{marketplaceProductId}")
    public ResponseEntity<ProductResponseDTO> getProduct(@PathVariable String marketplaceProductId) {
        return ResponseEntity.ok(productService.getProduct(marketplaceProductId));
    }
}