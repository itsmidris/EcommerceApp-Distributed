package in.sellzon.catalog.productimage.service.impl;

import in.sellzon.catalog.productimage.service.ImageFileValidator;
import in.sellzon.catalog.productimage.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "sellzon.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalImageStorageService implements ImageStorageService {

    private final Path rootDirectory;
    private final ImageFileValidator imageFileValidator;

    public LocalImageStorageService(
            @Value("${sellzon.storage.image-dir:uploads/products}") String imageDirectory,
            ImageFileValidator imageFileValidator) {
        this.rootDirectory = Paths.get(imageDirectory).toAbsolutePath().normalize();
        this.imageFileValidator = imageFileValidator;
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize image storage", e);
        }
    }

    @Override
    public StoredImage store(MultipartFile file, String marketplaceProductId) {

        imageFileValidator.validate(file);

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

    private String getExtension(String fileName) {

        String extension = StringUtils.getFilenameExtension(fileName);

        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("Image file extension is required");
        }

        return "." + extension.toLowerCase();
    }
}
