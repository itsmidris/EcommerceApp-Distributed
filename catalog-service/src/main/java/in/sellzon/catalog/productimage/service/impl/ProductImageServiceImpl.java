package in.sellzon.catalog.productimage.service.impl;

import in.sellzon.catalog.common.exception.ResourceNotFoundException;
import in.sellzon.catalog.product.entity.Product;
import in.sellzon.catalog.product.repository.ProductRepository;
import in.sellzon.catalog.productimage.dto.response.ProductImageResponseDTO;
import in.sellzon.catalog.productimage.entity.ProductImage;
import in.sellzon.catalog.productimage.repository.ProductImageRepository;
import in.sellzon.catalog.productimage.service.ImageStorageService;
import in.sellzon.catalog.productimage.service.ProductImageService;
import in.sellzon.catalog.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {

    private static final int MAX_IMAGES_PER_PRODUCT = 10;

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CurrentUserService currentUserService;
    private final ImageStorageService imageStorageService;

    @Override
    @Transactional
    public ProductImageResponseDTO uploadImage(String marketplaceProductId, MultipartFile file) {
        Product product = getProduct(marketplaceProductId);

        UUID sellerId = currentUserService.getCurrentUserId();

        verifyOwnership(product, sellerId);

        long imageCount = productImageRepository.countByProductId(product.getId());

        if (imageCount >= MAX_IMAGES_PER_PRODUCT) {
            throw new IllegalArgumentException("A product can not have more than " + MAX_IMAGES_PER_PRODUCT + " images");
        }

        ImageStorageService.StoredImage storedImage = imageStorageService.store(file, product.getMarketplaceProductId());

        ProductImage image = new ProductImage();

        image.setProduct(product);
        image.setImageUrl(storedImage.imageUrl());
        image.setOriginalFileName(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        image.setStoredFileName(storedImage.storedFileName());
        image.setContentType(file.getContentType());
        image.setFileSize(file.getSize());

        int nextOrder = productImageRepository
                .findTopByProductIdOrderByDisplayOrderDesc(product.getId())
                .map(existing -> existing.getDisplayOrder() + 1)
                .orElse(1);

        image.setDisplayOrder(nextOrder);

        boolean hasPrimary = productImageRepository.existsByProductIdAndPrimaryImageTrue(product.getId());

        image.setPrimaryImage(!hasPrimary);

        ProductImage saved = productImageRepository.save(image);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponseDTO> getProductImages(String marketplaceProductId) {
        Product product = getProduct(marketplaceProductId);

        UUID sellerId = currentUserService.getCurrentUserId();

        verifyOwnership(product, sellerId);

        return productImageRepository
                .findByProductIdOrderByDisplayOrderAsc(product.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();

    }

    @Override
    @Transactional
    public ProductImageResponseDTO setPrimaryImage(String marketplaceProductId, UUID imageId) {
        Product product = getProduct(marketplaceProductId);

        UUID sellerId = currentUserService.getCurrentUserId();

        verifyOwnership(product, sellerId);

        ProductImage image = productImageRepository
                .findByIdAndProductId(imageId, product.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found"));

        productImageRepository
                .findByProductIdAndPrimaryImageTrue(product.getId())
                .ifPresent(existing -> existing.setPrimaryImage(false));

        image.setPrimaryImage(true);

        return mapToResponse(productImageRepository.save(image));
    }

    @Override
    @Transactional
    public void deleteImage(String marketplaceProductId, UUID imageId) {
        Product product = getProduct(marketplaceProductId);

        UUID sellerId = currentUserService.getCurrentUserId();

        verifyOwnership(product, sellerId);

        ProductImage image = productImageRepository
                .findByIdAndProductId(imageId, product.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found"));

        boolean wasPrimary = image.isPrimaryImage();

        String storedFileName = image.getStoredFileName();

        productImageRepository.delete(image);

        imageStorageService.delete(storedFileName);

        if (wasPrimary) {
            productImageRepository
                    .findTopByProductIdOrderByDisplayOrderDesc(product.getId())
                    .stream()
                    .findFirst()
                    .ifPresent(nextImage -> nextImage.setPrimaryImage(true));
        }
    }

    private Product getProduct(String marketplaceProductId) {

        if (marketplaceProductId == null || marketplaceProductId.isBlank()) {
            throw new IllegalArgumentException("Marketplace product Id is required");
        }

        return productRepository
                .findByMarketplaceProductId(marketplaceProductId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with marketplace ID: " + marketplaceProductId));
    }

    private void verifyOwnership(Product product, UUID sellerId) {
        if (!sellerId.equals(product.getSellerId())) {
            throw new IllegalArgumentException("You are not allowed to manage images for this product");
        }
    }

    private ProductImageResponseDTO mapToResponse(ProductImage image) {
        return new ProductImageResponseDTO(
                image.getId(),
                image.getProduct().getId(),
                image.getImageUrl(),
                image.getOriginalFileName(),
                image.getContentType(),
                image.getFileSize(),
                image.getDisplayOrder(),
                image.isPrimaryImage()
        );
    }
}
