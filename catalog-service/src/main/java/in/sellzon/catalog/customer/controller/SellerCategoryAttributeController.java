package in.sellzon.catalog.customer.controller;

import in.sellzon.catalog.attribute.dto.response.SellerCategoryAttributeResponseDTO;
import in.sellzon.catalog.attribute.service.CategoryAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/categories")
@RequiredArgsConstructor
public class SellerCategoryAttributeController {

    private final CategoryAttributeService categoryAttributeService;

    @GetMapping("/{categoryId}/attributes")
    public ResponseEntity<List<SellerCategoryAttributeResponseDTO>> getCategoryAttributes(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(categoryAttributeService.getSellerAttributesByCategory(categoryId));
    }
}
