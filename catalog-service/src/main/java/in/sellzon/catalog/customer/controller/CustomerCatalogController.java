package in.sellzon.catalog.customer.controller;

import in.sellzon.catalog.category.dto.response.CategoryResponseDTO;
import in.sellzon.catalog.category.service.CategoryService;
import in.sellzon.catalog.customer.dto.response.CustomerProductDetailDTO;
import in.sellzon.catalog.customer.dto.response.CustomerProductPageDTO;
import in.sellzon.catalog.customer.service.CustomerCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerCatalogController {

    private final CustomerCatalogService customerCatalogService;
    private final CategoryService categoryService;

    @GetMapping("/products")
    public ResponseEntity<CustomerProductPageDTO> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return ResponseEntity.ok(customerCatalogService.searchProducts(q, categoryId, page, size));
    }

    @GetMapping("/products/{marketplaceProductId}")
    public ResponseEntity<CustomerProductDetailDTO> getProduct(
            @PathVariable String marketplaceProductId) {
        return ResponseEntity.ok(customerCatalogService.getProduct(marketplaceProductId));
    }

    @GetMapping("/categories/tree")
    public ResponseEntity<List<CategoryResponseDTO>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }
}
