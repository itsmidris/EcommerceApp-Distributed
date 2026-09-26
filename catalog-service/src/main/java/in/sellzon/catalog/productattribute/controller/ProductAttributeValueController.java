package in.sellzon.catalog.productattribute.controller;

import in.sellzon.catalog.productattribute.dto.request.SaveProductAttributeValueRequestDTO;
import in.sellzon.catalog.productattribute.dto.response.ProductAttributeValueResponseDTO;
import in.sellzon.catalog.productattribute.service.ProductAttributeValueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/products/{marketplaceProductId}/attributes")
@RequiredArgsConstructor
public class ProductAttributeValueController {

    private final ProductAttributeValueService productAttributeValueService;

    @PostMapping
    public ResponseEntity<ProductAttributeValueResponseDTO> createValue(
            @PathVariable String marketplaceProductId,
            @Valid @RequestBody SaveProductAttributeValueRequestDTO request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productAttributeValueService.createValue(marketplaceProductId, request));
    }

    @GetMapping
    public ResponseEntity<List<ProductAttributeValueResponseDTO>> getProductAttributes(@PathVariable String marketplaceProductId) {

        return ResponseEntity.ok(productAttributeValueService.getProductAttributes(marketplaceProductId));
    }


    @GetMapping("/{valueId}")
    public ResponseEntity<ProductAttributeValueResponseDTO> getValue(
            @PathVariable String marketplaceProductId,
            @PathVariable UUID valueId) {

        return ResponseEntity.ok(productAttributeValueService.getValue(marketplaceProductId, valueId));
    }


    @PutMapping("/{valueId}")
    public ResponseEntity<ProductAttributeValueResponseDTO> updateValue(
            @PathVariable String marketplaceProductId,
            @PathVariable UUID valueId,
            @Valid @RequestBody SaveProductAttributeValueRequestDTO request) {

        return ResponseEntity.ok(productAttributeValueService.updateValue(marketplaceProductId, valueId, request));
    }


    @DeleteMapping("/{valueId}")
    public ResponseEntity<Void> deleteValue(
            @PathVariable String marketplaceProductId,
            @PathVariable UUID valueId) {

        productAttributeValueService.deleteValue(marketplaceProductId, valueId);

        return ResponseEntity.noContent().build();
    }
}
