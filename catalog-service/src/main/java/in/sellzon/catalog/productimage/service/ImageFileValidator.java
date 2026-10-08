package in.sellzon.catalog.productimage.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Component
public class ImageFileValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Image size cannot exceed 5 MB");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPG, PNG and WebP images are allowed");
        }

        validateImageDimensions(file);
    }

    private void validateImageDimensions(MultipartFile file) {

        try (InputStream inputStream = file.getInputStream()) {

            BufferedImage image = ImageIO.read(inputStream);

            if (image == null) {
                throw new IllegalArgumentException(
                        "Unable to read image. Please upload a valid JPG, PNG or WebP image"
                );
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if (width != height || width < 400 || width > 1600) {
                throw new IllegalArgumentException(
                        "Image must be square with dimensions between 400x400 and 1600x1600 pixels"
                );
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read image dimensions", e);
        }
    }
}
