package in.sellzon.catalog.category.controller;

import in.sellzon.catalog.category.dto.request.CreateCategoryRequestDTO;
import in.sellzon.catalog.category.dto.request.UpdateCategoryRequestDTO;
import in.sellzon.catalog.category.dto.response.CategoryResponseDTO;
import in.sellzon.catalog.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(@Valid @RequestBody CreateCategoryRequestDTO request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> getCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(categoryService.getCategory(categoryId));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(@PathVariable UUID categoryId, @Valid @RequestBody UpdateCategoryRequestDTO request) {
        return ResponseEntity.ok(categoryService.updateCategory(categoryId, request));
    }

    @GetMapping("/roots")
    public ResponseEntity<List<CategoryResponseDTO>> getRootCategories() {
        return ResponseEntity.ok(categoryService.getRootCategories());
    }

    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<CategoryResponseDTO>> getChildCategories(@PathVariable UUID parentId) {
        return ResponseEntity.ok(categoryService.getChildCategories(parentId));
    }

    @GetMapping("/tree")
    public ResponseEntity<List<CategoryResponseDTO>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }

    @GetMapping("/level-4")
    public ResponseEntity<List<CategoryResponseDTO>> getLevelFourCategories() {
        return ResponseEntity.ok(categoryService.getLevelFourCategories());
    }

    @PatchMapping("/{categoryId}/deactivate")
    public ResponseEntity<Void> deactivateCategory(@PathVariable UUID categoryId) {
        categoryService.deactivateCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
