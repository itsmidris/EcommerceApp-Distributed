package in.sellzon.catalog.productimage.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import in.sellzon.catalog.productimage.service.ImageFileValidator;
import in.sellzon.catalog.productimage.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sellzon.storage.provider", havingValue = "cloudinary")
public class CloudinaryImageStorageService implements ImageStorageService {

    private final Cloudinary cloudinary;
    private final ImageFileValidator imageFileValidator;

    @Override
    public StoredImage store(MultipartFile file, String marketplaceProductId) {

        imageFileValidator.validate(file);

        String publicId = "products/" + marketplaceProductId + "/" + UUID.randomUUID();

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "image",
                            "overwrite", false
                    )
            );

            String secureUrl = (String) result.get("secure_url");

            if (secureUrl == null || secureUrl.isBlank()) {
                throw new IllegalStateException("Cloudinary did not return an image URL");
            }

            return new StoredImage(publicId, secureUrl);

        } catch (IOException e) {
            throw new IllegalStateException("Unable to upload product image to Cloudinary", e);
        }
    }

    @Override
    public void delete(String storedFileName) {

        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(
                    storedFileName,
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "type", "upload",
                            "invalidate", true
                    )
            );
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete product image from Cloudinary", e);
        }
    }
}
