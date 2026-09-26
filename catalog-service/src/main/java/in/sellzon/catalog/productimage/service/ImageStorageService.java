package in.sellzon.catalog.productimage.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    StoredImage store(MultipartFile file, String marketplaceProductId);
    void delete(String storedFileName);
    record StoredImage(String storedFileName, String imageUrl
    ) {
    }
}
