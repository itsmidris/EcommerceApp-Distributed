package in.sellzon.catalog.attribute.controller;

import in.sellzon.catalog.attribute.dto.request.CreateCategoryAttributeRequestDTO;
import in.sellzon.catalog.attribute.dto.request.UpdateCategoryAttributeRequestDTO;
import in.sellzon.catalog.attribute.dto.response.CategoryAttributeResponseDTO;
import in.sellzon.catalog.attribute.service.CategoryAttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/category-attributes")
@RequiredArgsConstructor
public class CategoryAttributeController {

    private final CategoryAttributeService categoryAttributeService;

    @PostMapping("/category/{categoryId}")
    public ResponseEntity<CategoryAttributeResponseDTO> createCategoryAttribute(@PathVariable UUID categoryId, @Valid @RequestBody CreateCategoryAttributeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryAttributeService.createCategoryAttribute(categoryId, request));
    }

    @GetMapping("/{categoryAttributeId}")
    public ResponseEntity<CategoryAttributeResponseDTO> getCategoryAttribute(@PathVariable UUID categoryAttributeId) {
        return ResponseEntity.ok(categoryAttributeService.getCategoryAttribute(categoryAttributeId));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<CategoryAttributeResponseDTO>> getAttributesByCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(categoryAttributeService.getAttributesByCategory(categoryId));
    }

    @GetMapping("/attribute/{attributeId}")
    public ResponseEntity<List<CategoryAttributeResponseDTO>> getCategoriesByAttribute(@PathVariable UUID attributeId) {
        return ResponseEntity.ok(categoryAttributeService.getCategoriesByAttribute(attributeId));
    }

    @PutMapping("/{categoryAttributeId}")
    public ResponseEntity<CategoryAttributeResponseDTO> updateCategoryAttribute(@PathVariable UUID categoryAttributeId, @Valid @RequestBody UpdateCategoryAttributeRequestDTO request) {
        return ResponseEntity.ok(categoryAttributeService.updateCategoryAttribute(categoryAttributeId, request));
    }

    @PatchMapping("/{categoryAttributeId}/deactivate")
    public ResponseEntity<Void> deactivateCategoryAttribute(@PathVariable UUID categoryAttributeId) {
        categoryAttributeService.deactivateCategoryAttribute(categoryAttributeId);
        return ResponseEntity.noContent().build();
    }
}
