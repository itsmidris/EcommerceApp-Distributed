package in.sellzon.catalog.customer.service.impl;

import in.sellzon.catalog.attribute.entity.AttributeOption;
import in.sellzon.catalog.attribute.entity.CategoryAttribute;
import in.sellzon.catalog.category.repository.CategoryRepository;
import in.sellzon.catalog.common.enums.Status;
import in.sellzon.catalog.common.exception.ResourceNotFoundException;
import in.sellzon.catalog.customer.dto.response.CustomerProductAttributeDTO;
import in.sellzon.catalog.customer.dto.response.CustomerProductDetailDTO;
import in.sellzon.catalog.customer.dto.response.CustomerProductPageDTO;
import in.sellzon.catalog.customer.dto.response.CustomerProductSummaryDTO;
import in.sellzon.catalog.customer.service.CustomerCatalogService;
import in.sellzon.catalog.identity.client.IdentityUserClient;
import in.sellzon.catalog.identity.response.PublicUserProfileResponse;
import in.sellzon.catalog.product.entity.Product;
import in.sellzon.catalog.product.repository.ProductRepository;
import in.sellzon.catalog.productattribute.entity.ProductAttributeValue;
import in.sellzon.catalog.productattribute.repository.ProductAttributeValueRepository;
import in.sellzon.catalog.productimage.dto.response.ProductImageResponseDTO;
import in.sellzon.catalog.productimage.entity.ProductImage;
import in.sellzon.catalog.productimage.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerCatalogServiceImpl implements CustomerCatalogService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final CategoryRepository categoryRepository;
    private final IdentityUserClient identityUserClient;

    @Override
    @Transactional(readOnly = true)
    public CustomerProductPageDTO searchProducts(String query, UUID categoryId, int page, int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 48);

        String normalizedQuery = query == null ? "" : query.trim();

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> products;

        if (categoryId == null) {
            products = productRepository.searchPublic(normalizedQuery, Status.ACTIVE, pageable);
        } else {

            List<UUID> categoryIds = categoryRepository.findActiveCategoryAndDescendantIds(categoryId );

            if (categoryIds.isEmpty()) {
                throw new ResourceNotFoundException("Category not found: " + categoryId);
            }

            products = productRepository.searchPublicByCategories(
                    normalizedQuery,
                    categoryIds,
                    Status.ACTIVE,
                    pageable
            );
        }

        List<CustomerProductSummaryDTO> content =
                products
                        .getContent()
                        .stream()
                        .map(this::mapSummary)
                        .toList();

        return new CustomerProductPageDTO(
                content,
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages(),
                products.isFirst(),
                products.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProductDetailDTO getProduct(String marketplaceProductId) {

        if (marketplaceProductId == null || marketplaceProductId.isBlank()) {
            throw new IllegalArgumentException("Marketplace product ID is required");
        }

        Product product = productRepository
                .findByMarketplaceProductIdAndStatus(marketplaceProductId.trim(), Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<ProductImageResponseDTO> images = productImageRepository
                .findByProductIdOrderByDisplayOrderAsc(product.getId())
                .stream()
                .map(this::mapImage)
                .toList();

        List<CustomerProductAttributeDTO> attributes = productAttributeValueRepository
                .findByProductIdOrderByCategoryAttributeDisplayOrderAsc(product.getId())
                .stream()
                .map(this::mapAttribute)
                .toList();

        String categoryPath = buildCategoryPath(product.getCategory());

        PublicUserProfileResponse seller = identityUserClient.getPublicUserProfile(product.getSellerId());

        String sellerName = (seller.firstName() + " " + seller.lastName()).trim();

        return new CustomerProductDetailDTO(
                product.getMarketplaceProductId(),
                product.getTitle(),
                product.getDescription(),
                product.getBrand(),
                product.getMrp(),
                product.getSellingPrice(),
                product.getStockQuantity(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                categoryPath,
                sellerName,
                images,
                attributes
        );
    }

    private CustomerProductSummaryDTO mapSummary(Product product) {
        String primaryImageUrl = productImageRepository
                .findByProductIdAndPrimaryImageTrue(product.getId())
                .map(ProductImage::getImageUrl)
                .orElseGet(() -> productImageRepository
                        .findByProductIdOrderByDisplayOrderAsc(product.getId())
                        .stream()
                        .findFirst()
                        .map(ProductImage::getImageUrl)
                        .orElse(null));

        return new CustomerProductSummaryDTO(
                product.getMarketplaceProductId(),
                product.getTitle(),
                product.getBrand(),
                product.getMrp(),
                product.getSellingPrice(),
                product.getStockQuantity(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                primaryImageUrl
        );
    }

    private ProductImageResponseDTO mapImage(ProductImage image) {
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

    private CustomerProductAttributeDTO mapAttribute(ProductAttributeValue value) {
        CategoryAttribute categoryAttribute = value.getCategoryAttribute();
        var attribute = categoryAttribute.getAttribute();

        String displayValue = value.getValueText();
        if (displayValue == null && value.getValueNumber() != null) displayValue = value.getValueNumber().toString();
        if (displayValue == null && value.getValueDecimal() != null) displayValue = value.getValueDecimal().stripTrailingZeros().toPlainString();
        if (displayValue == null && value.getValueBoolean() != null) displayValue = value.getValueBoolean().toString();
        if (displayValue == null && value.getSelectedOption() != null) displayValue = value.getSelectedOption().getValue();

        List<String> selectedOptions =
                value.getSelectedOptions()
                        == null ? List.of() : value.getSelectedOptions().stream().map(AttributeOption::getValue).toList();

        return new CustomerProductAttributeDTO(
                categoryAttribute.getId(),
                attribute.getName(),
                attribute.getCode(),
                attribute.getType(),
                attribute.getUnit(),
                displayValue,
                selectedOptions
        );
    }

    private String buildCategoryPath(in.sellzon.catalog.category.entity.Category category) {

        LinkedList<String> parts = new LinkedList<>();
        var current = category;
        while (current != null) {
            parts.addFirst(current.getName());
            current = current.getParent();
        }
        return String.join(" › ", parts);
    }
}
