package in.sellzon.catalog.product.service.impl;

import in.sellzon.catalog.attribute.entity.CategoryAttribute;
import in.sellzon.catalog.attribute.repository.CategoryAttributeRepository;
import in.sellzon.catalog.category.entity.Category;
import in.sellzon.catalog.category.enums.CategoryLevel;
import in.sellzon.catalog.category.repository.CategoryRepository;
import in.sellzon.catalog.common.enums.Status;
import in.sellzon.catalog.common.exception.DuplicateResourceException;
import in.sellzon.catalog.common.exception.ResourceNotFoundException;
import in.sellzon.catalog.identity.response.IdentityUserResponse;
import in.sellzon.catalog.product.dto.request.CreateProductRequestDTO;
import in.sellzon.catalog.product.dto.request.UpdateProductRequestDTO;
import in.sellzon.catalog.product.dto.response.ProductResponseDTO;
import in.sellzon.catalog.product.entity.Product;
import in.sellzon.catalog.product.repository.ProductRepository;
import in.sellzon.catalog.product.service.ProductService;
import in.sellzon.catalog.product.util.MarketplaceProductIdGenerator;
import in.sellzon.catalog.productattribute.dto.request.SaveProductAttributeValueRequestDTO;
import in.sellzon.catalog.productattribute.service.ProductAttributeValueService;
import in.sellzon.catalog.productimage.entity.ProductImage;
import in.sellzon.catalog.productimage.repository.ProductImageRepository;
import in.sellzon.catalog.security.CurrentUserService;
import in.sellzon.catalog.identity.client.IdentityUserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MarketplaceProductIdGenerator marketplaceProductIdGenerator;
    private final CurrentUserService currentUserService;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductAttributeValueService productAttributeValueService;
    private final IdentityUserClient identityUserClient;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional
    public ProductResponseDTO createProduct(CreateProductRequestDTO request) {
        UUID sellerId = currentUserService.getCurrentUserId();

        String sku = normalizeSku(request.sku());

        validateSkuUniqueness(sellerId, sku);

        Category category = getValidListingCategory(request.categoryId());

        validatePricing(request.mrp(), request.sellingPrice());

        List<CategoryAttribute> categoryAttributes = categoryAttributeRepository
                .findByCategoryIdAndStatusOrderByDisplayOrderAsc(category.getId(), Status.ACTIVE);

        validateProductAttributes(categoryAttributes, request.attributes());

        Product product = new Product();

        product.setMarketplaceProductId(marketplaceProductIdGenerator.generate());
        product.setSellerId(sellerId);
        product.setCategory(category);
        product.setSku(sku);
        product.setTitle(normalizeRequiredText(request.title()));
        product.setDescription(normalizeOptionalText(request.description()));
        product.setBrand(normalizeRequiredText(request.brand()));
        product.setMrp(request.mrp());
        product.setSellingPrice(request.sellingPrice());
        product.setStockQuantity(request.stockQuantity());
        product.setStatus(Status.ACTIVE);

        Product savedProduct = productRepository.save(product);

        for (SaveProductAttributeValueRequestDTO attributeRequest
                : request.attributes()) {

            if (isEmptyAttributeValue(attributeRequest)) {
                continue;
            }

            productAttributeValueService.createValue(
                    savedProduct.getMarketplaceProductId(),
                    attributeRequest
            );
        }

        return mapToResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProduct(String marketplaceProductId) {
        Product product = getProductEntity(marketplaceProductId);
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getMyProducts() {
        UUID sellerId = currentUserService.getCurrentUserId();
        return productRepository
                .findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductResponseDTO updateProduct(String marketplaceProductId, UpdateProductRequestDTO request) {
        Product product = getProductEntity(marketplaceProductId);

        UUID sellerId = currentUserService.getCurrentUserId();

        verifyOwnership(product, sellerId);

        validatePricing(product.getMrp(), request.sellingPrice());

        product.setTitle(normalizeRequiredText(request.title()));
        product.setDescription(normalizeOptionalText(request.description()));
        product.setSellingPrice(request.sellingPrice());
        product.setStockQuantity(request.stockQuantity());

        Product updatedProduct = productRepository.save(product);

        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deactivateProduct(String marketplaceProductId) {

        Product product = getProductEntity(marketplaceProductId);

        UUID sellerId = currentUserService.getCurrentUserId();

        verifyOwnership(product, sellerId);

        if (product.getStatus() == Status.INACTIVE) {
            return;
        }

        product.setStatus(Status.INACTIVE);

        productRepository.save(product);
    }

    @Override
    @Transactional
    public void activateProduct(String marketplaceProductId) {
        Product product = getProductEntity(marketplaceProductId);

        UUID sellerId = currentUserService.getCurrentUserId();

        verifyOwnership(product, sellerId);

        getValidListingCategory(product.getCategory().getId());

        product.setStatus(Status.ACTIVE);

        productRepository.save(product);
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required");
        }

        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private void validateSkuUniqueness(UUID sellerId, String sku) {
        if (productRepository.existsBySellerIdAndSku(sellerId, sku)) {
            throw new DuplicateResourceException("Product already exists with SKU: " + sku);
        }
    }

    private Category getValidListingCategory(UUID categoryId) {
        Category category = categoryRepository
                .findByIdAndStatus(categoryId, Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active category not found with ID: " + categoryId));

        if (category.getLevel() != CategoryLevel.LEVEL_4) {
            throw new IllegalArgumentException("Products can only be listed under level 4 categories");
        }

        return category;
    }

    private void validatePricing(BigDecimal mrp, BigDecimal sellingPrice) {
        if (mrp == null || mrp.signum() <= 0) {
            throw new IllegalArgumentException("MRP must be greater than 0");
        }

        if (sellingPrice == null || sellingPrice.signum() <= 0) {
            throw new IllegalArgumentException("Selling price must be greater than 0");
        }

        if (sellingPrice.compareTo(mrp) > 0) {
            throw new IllegalArgumentException("Selling price cannot be greater than MRP");
        }
    }

    private String normalizeRequiredText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required product field cannot be blank");
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private Product getProductEntity(String marketplaceProductId) {
        if (marketplaceProductId == null || marketplaceProductId.isBlank()) {
            throw new IllegalArgumentException("Marketplace product ID is required");
        }

        return productRepository
                .findByMarketplaceProductId(marketplaceProductId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with marketplace ID: " + marketplaceProductId));
    }

    private void verifyOwnership(Product product, UUID sellerId) {
        if (!product.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("You are not allowed to modify this product");
        }
    }

    private boolean isEmptyAttributeValue(SaveProductAttributeValueRequestDTO request) {

        if (request == null) {
            return true;
        }

        boolean hasTextValue = request.value() != null && !request.value().isBlank();

        boolean hasOptions = request.optionIds() != null && !request.optionIds().isEmpty();

        return !hasTextValue && !hasOptions;
    }

    private void validateProductAttributes(
            List<CategoryAttribute> categoryAttributes,
            List<SaveProductAttributeValueRequestDTO> submittedAttributes) {

        if (submittedAttributes == null) {
            throw new IllegalArgumentException("Category attributes are required");
        }


        Map<UUID, SaveProductAttributeValueRequestDTO> submittedById = new HashMap<>();

        for (SaveProductAttributeValueRequestDTO request : submittedAttributes) {

            if (request == null || request.categoryAttributeId() == null) {
                throw new IllegalArgumentException("Category attribute ID is required");
            }

            if (submittedById.put(request.categoryAttributeId(), request) != null) {
                throw new IllegalArgumentException("Duplicate category attribute submitted: " + request.categoryAttributeId()
                );
            }
        }

        Set<UUID> configuredAttributeIds = categoryAttributes
                .stream()
                .map(CategoryAttribute::getId)
                .collect(Collectors.toSet());

        for (UUID submittedId : submittedById.keySet()) {

            if (!configuredAttributeIds.contains(submittedId)) {
                throw new IllegalArgumentException("Attribute does not belong to the selected category: " + submittedId);
            }
        }

        for (CategoryAttribute categoryAttribute : categoryAttributes) {

            SaveProductAttributeValueRequestDTO request = submittedById.get(categoryAttribute.getId());

            if (!categoryAttribute.isRequired()) {
                continue;
            }

            if (request == null || isEmptyAttributeValue(request)) {
                throw new IllegalArgumentException("Required attribute is missing: " + categoryAttribute.getAttribute().getName());
            }
        }
    }

    private ProductResponseDTO mapToResponse(Product product) {
        UUID sellerId = product.getSellerId();

        Category category = product.getCategory();

        String accessToken = currentUserService.getCurrentAccessToken();

        IdentityUserResponse seller = identityUserClient.getUserById(sellerId, accessToken);

        String sellerName = (seller.firstName() + " " + seller.lastName().trim());

        String imageUrl = productImageRepository
                .findByProductIdAndPrimaryImageTrue(product.getId())
                .map(ProductImage::getImageUrl)
                .orElseGet(() ->
                        productImageRepository
                                .findByProductIdOrderByDisplayOrderAsc(product.getId())
                                .stream()
                                .findFirst()
                                .map(ProductImage::getImageUrl)
                                .orElse(null)
                );

        return new ProductResponseDTO(
                product.getId(),
                product.getMarketplaceProductId(),
                seller.id(),
                sellerName,
                category.getId(),
                category.getName(),
                product.getSku(),
                product.getTitle(),
                product.getDescription(),
                product.getBrand(),
                product.getMrp(),
                product.getSellingPrice(),
                product.getStockQuantity(),
                product.getStatus(),
                imageUrl
        );
    }
}
