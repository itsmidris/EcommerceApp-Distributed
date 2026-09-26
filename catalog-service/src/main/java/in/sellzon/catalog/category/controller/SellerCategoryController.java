package in.sellzon.catalog.category.controller;

import in.sellzon.catalog.category.dto.response.CategoryResponseDTO;
import in.sellzon.catalog.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller/categories")
@RequiredArgsConstructor
public class SellerCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/level-4")
    public ResponseEntity<List<CategoryResponseDTO>> getLevelFourCategories() {
        return ResponseEntity.ok(categoryService.getLevelFourCategories());
    }
}
