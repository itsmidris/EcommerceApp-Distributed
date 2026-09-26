package in.sellzon.catalog.productimage.service.impl;

import in.sellzon.catalog.productimage.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalImageStorageService implements ImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path rootDirectory;

    public LocalImageStorageService(@Value("${sellzon.storage.image-dir:uploads/products}") String imageDirectory) {
        this.rootDirectory = Paths.get(imageDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize image storage", e);
        }
    }

    @Override
    public StoredImage store(MultipartFile file, String marketplaceProductId) {

        validateFile(file);

        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "image" : file.getOriginalFilename()
        );

        String extension = getExtension(originalFileName);

        String storedFileName = UUID.randomUUID() + extension;

        Path productDirectory = rootDirectory.resolve(marketplaceProductId).normalize();

        try {
            Files.createDirectories(productDirectory);

            Path target = productDirectory.resolve(storedFileName).normalize();

            if (!target.startsWith(productDirectory)) {
                throw new IllegalArgumentException("Invalid image file path");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String imageUrl = "/uploads/products/" + marketplaceProductId + "/" +storedFileName;

            return new StoredImage(storedFileName, imageUrl);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store product image", e);
        }
    }

    @Override
    public void delete(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }

        try {
            Files.walk(rootDirectory)
                    .filter(path -> path.getFileName().toString().equals(storedFileName))
                    .findFirst()
                    .ifPresent(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new IllegalStateException("Unable to delete product image", e);
                        }
                    });
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete product image", e);
        }
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        if (file.getSize() > 5 * 1024 *1024) {
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
                throw new IllegalArgumentException("Unable to read image. Please upload a valid JPG, PNG or WebP image");
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

    private String getExtension(String fileName) {

        String extension = StringUtils.getFilenameExtension(fileName);

        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("Image file extension is required");
        }

        return "." + extension.toLowerCase();
    }
}
