package in.sellzon.catalog.productimage.service;

import in.sellzon.catalog.productimage.dto.response.ProductImageResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProductImageService {
    ProductImageResponseDTO uploadImage(String marketplaceProductId, MultipartFile file);
    List<ProductImageResponseDTO> getProductImages(String marketplaceProductId);
    ProductImageResponseDTO setPrimaryImage(String marketplaceProductId, UUID imageId);
    void deleteImage(String marketplaceProductId, UUID imageId);
}
