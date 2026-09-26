package in.sellzon.catalog.productimage.controller;

import in.sellzon.catalog.productimage.dto.response.ProductImageResponseDTO;
import in.sellzon.catalog.productimage.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/products/{marketplaceProductId}/images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImageResponseDTO> uploadImage(
            @PathVariable String marketplaceProductId,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productImageService.uploadImage(marketplaceProductId, file));
    }

    @GetMapping
    public ResponseEntity<List<ProductImageResponseDTO>> getProductImages(@PathVariable String marketplaceProductId) {

        return ResponseEntity
                .ok(productImageService.getProductImages(marketplaceProductId));
    }

    @PatchMapping("/{imageId}/primary")
    public ResponseEntity<ProductImageResponseDTO> setPrimaryImage(
            @PathVariable String marketplaceProductId,
            @PathVariable UUID imageId) {

        return ResponseEntity
                .ok(productImageService.setPrimaryImage(marketplaceProductId, imageId));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable String marketplaceProductId,
            @PathVariable UUID imageId) {

        productImageService.deleteImage(marketplaceProductId, imageId);

        return ResponseEntity.noContent().build();
    }
}
