package in.sellzon.catalog.productattribute.service.impl;

import in.sellzon.catalog.attribute.entity.Attribute;
import in.sellzon.catalog.attribute.entity.AttributeOption;
import in.sellzon.catalog.attribute.entity.CategoryAttribute;
import in.sellzon.catalog.attribute.enums.AttributeType;
import in.sellzon.catalog.attribute.repository.AttributeOptionRepository;
import in.sellzon.catalog.attribute.repository.CategoryAttributeRepository;
import in.sellzon.catalog.category.entity.Category;
import in.sellzon.catalog.common.enums.Status;
import in.sellzon.catalog.common.exception.DuplicateResourceException;
import in.sellzon.catalog.common.exception.ResourceNotFoundException;
import in.sellzon.catalog.product.entity.Product;
import in.sellzon.catalog.product.repository.ProductRepository;
import in.sellzon.catalog.productattribute.dto.request.SaveProductAttributeValueRequestDTO;
import in.sellzon.catalog.productattribute.dto.response.ProductAttributeValueResponseDTO;
import in.sellzon.catalog.productattribute.entity.ProductAttributeValue;
import in.sellzon.catalog.productattribute.repository.ProductAttributeValueRepository;
import in.sellzon.catalog.productattribute.service.ProductAttributeValueService;
import in.sellzon.catalog.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductAttributeValueServiceImpl implements ProductAttributeValueService {

    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final ProductRepository productRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final AttributeOptionRepository attributeOptionRepository;
    private final CurrentUserService currentUserService;

    @Override
    public ProductAttributeValueResponseDTO createValue(String marketplaceProductId, SaveProductAttributeValueRequestDTO request) {
        Product product = getOwnedProduct(marketplaceProductId);
        CategoryAttribute categoryAttribute = getActiveCategoryAttribute(request.categoryAttributeId());

        validateCategoryAttributeForProduct(product, categoryAttribute);

        if (productAttributeValueRepository.existsByProductIdAndCategoryAttributeId(product.getId(), categoryAttribute.getId())) {
            throw new DuplicateResourceException("Attribute value already exists for this product");
        }

        ProductAttributeValue entity = new ProductAttributeValue();

        entity.setProduct(product);
        entity.setCategoryAttribute(categoryAttribute);

        populateValue(entity, categoryAttribute, request);

        ProductAttributeValue saved = productAttributeValueRepository.save(entity);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeValueResponseDTO> getProductAttributes(String marketplaceProductId) {
        Product product = getOwnedProduct(marketplaceProductId);

        return productAttributeValueRepository
                .findByProductIdOrderByCategoryAttributeDisplayOrderAsc(product.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeValueResponseDTO getValue(String marketplaceProductId, UUID valueId) {
        Product product = getOwnedProduct(marketplaceProductId);
        ProductAttributeValue entity = productAttributeValueRepository
                .findByIdAndProductId(valueId, product.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product attribute value not found: " + valueId));

        return mapToResponse(entity);
    }

    @Override
    public ProductAttributeValueResponseDTO updateValue(String marketplaceProductId, UUID valueId, SaveProductAttributeValueRequestDTO request) {
        Product product = getOwnedProduct(marketplaceProductId);

        ProductAttributeValue entity = productAttributeValueRepository
                .findByIdAndProductId(valueId, product.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product attribute value not found: " + valueId));

        CategoryAttribute categoryAttribute = getActiveCategoryAttribute(request.categoryAttributeId());

        validateCategoryAttributeForProduct(product, categoryAttribute);

        UUID currentCategoryAttributeId = entity.getCategoryAttribute().getId();

        if (!currentCategoryAttributeId.equals(categoryAttribute.getId())) {
            boolean alreadyExists = productAttributeValueRepository.existsByProductIdAndCategoryAttributeId(product.getId(), categoryAttribute.getId());

            if (alreadyExists) {
                throw new DuplicateResourceException("Attribute value already exists for this product");
            }
        }

        entity.setCategoryAttribute(categoryAttribute);

        populateValue(entity, categoryAttribute, request);

        ProductAttributeValue updated = productAttributeValueRepository.save(entity);

        return mapToResponse(updated);
    }

    @Override
    public void deleteValue(String marketplaceProductId, UUID valueId) {
        Product product = getOwnedProduct(marketplaceProductId);

        ProductAttributeValue entity = productAttributeValueRepository
                .findByIdAndProductId(valueId, product.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product attribute value not found: " + valueId));

        productAttributeValueRepository.delete(entity);
    }


    @Override
    public void createValuesForNewProduct(Product product, List<CategoryAttribute> categoryAttributes, List<SaveProductAttributeValueRequestDTO> requests) {

        Map<UUID, CategoryAttribute> attributesById =
                categoryAttributes
                        .stream()
                        .collect(Collectors.toMap(
                                CategoryAttribute::getId,
                                Function.identity()
                        ));

        List<ProductAttributeValue> values = new ArrayList<>();

        for (SaveProductAttributeValueRequestDTO request : requests) {
            if (request == null) {
                continue;
            }

            boolean hasValue = request.value() != null && !request.value().isBlank();

            boolean hasOptions = request.optionIds() != null && !request.optionIds().isEmpty();

            if (!hasValue && !hasOptions) {
                continue;
            }

            CategoryAttribute categoryAttribute = attributesById.get(request.categoryAttributeId());

            if (categoryAttribute == null) {
                throw new IllegalArgumentException("Attribute does not belong to the selected category");
            }

            ProductAttributeValue entity = new ProductAttributeValue();
            entity.setProduct(product);
            entity.setCategoryAttribute(categoryAttribute);

            // Preserve existing type-specific validation.
            populateValue(entity, categoryAttribute, request);

            values.add(entity);
        }

        if (!values.isEmpty()) {
            productAttributeValueRepository.saveAll(values);
        }
    }


    private Product getOwnedProduct(String marketplaceProductId) {

        if (marketplaceProductId == null) {
            throw new IllegalArgumentException("Marketplace product ID is required");
        }

        UUID sellerId = currentUserService.getCurrentUserId();

        Product product = productRepository
                .findByMarketplaceProductIdAndStatus(marketplaceProductId.trim(), Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active product not found with marketplace ID: " + marketplaceProductId));

        if (!sellerId.equals(product.getSellerId())) {
            throw new IllegalArgumentException("You are not authorized to modify this product");
        }

        return product;
    }

    private CategoryAttribute getActiveCategoryAttribute(UUID categoryAttributeId) {

        return categoryAttributeRepository
                .findByIdAndStatus(categoryAttributeId, Status.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Category attribute not found or inactive"));
    }

    private void validateCategoryAttributeForProduct(Product product, CategoryAttribute categoryAttribute) {

        Category productCategory = product.getCategory();

        Category attributeCategory = categoryAttribute.getCategory();

        if (!productCategory.getId().equals(attributeCategory.getId())) {
            throw new IllegalArgumentException("Category attribute does not belong to the product category");
        }

        if (productCategory.getStatus() != Status.ACTIVE) {
            throw new IllegalArgumentException("Product category is inactive");
        }

        if (categoryAttribute.getStatus() != Status.ACTIVE) {
            throw new IllegalArgumentException("Category attribute is inactive");
        }

        Attribute attribute = categoryAttribute.getAttribute();

        if (attribute.getStatus() != Status.ACTIVE) {
            throw new IllegalArgumentException("Attribute is inactive");
        }
    }



    private void populateValue(
            ProductAttributeValue entity,
            CategoryAttribute categoryAttribute,
            SaveProductAttributeValueRequestDTO request
    ) {

        clearExistingValues(entity);

        Attribute attribute = categoryAttribute.getAttribute();

        AttributeType type = attribute.getType();

        switch (type) {
            case TEXT ->
                    setTextValue(entity, request.value());
            case NUMBER ->
                    setNumberValue(entity, request.value());
            case DECIMAL ->
                    setDecimalValue(entity, request.value());
            case BOOLEAN ->
                    setBooleanValue(entity, request.value());
            case DROPDOWN ->
                    setDropdownValue(entity, categoryAttribute, request.optionIds());
            case MULTI_SELECT ->
                    setMultiSelectValue(entity, categoryAttribute, request.optionIds());
        }
    }

    private void clearExistingValues(ProductAttributeValue entity) {

        entity.setValueText(null);
        entity.setValueNumber(null);
        entity.setValueDecimal(null);
        entity.setValueBoolean(null);
        entity.setSelectedOption(null);

        if (entity.getSelectedOptions() == null) {
            entity.setSelectedOptions(new ArrayList<>());
        } else {
            entity.getSelectedOptions().clear();
        }
    }


    private void setTextValue(ProductAttributeValue entity, String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Text value is required");
        }
        entity.setValueText(value.trim());
    }


    private void setNumberValue(ProductAttributeValue entity, String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Number value is required");
        }

        try {
            Long number = Long.parseLong(value.trim());
            entity.setValueNumber(number);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid number value: " + value);
        }
    }


    private void setDecimalValue(ProductAttributeValue entity, String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Decimal value is required");
        }

        try {
            BigDecimal decimal = new BigDecimal(value.trim());
            entity.setValueDecimal(decimal);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid decimal value: " + value);
        }
    }


    private void setBooleanValue(ProductAttributeValue entity, String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Boolean value is required");
        }

        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("Boolean value must be true or false");
        }

        entity.setValueBoolean(Boolean.parseBoolean(value));
    }


    private void setDropdownValue(ProductAttributeValue entity, CategoryAttribute categoryAttribute, List<UUID> optionIds) {

        if (optionIds == null || optionIds.size() != 1) {
            throw new IllegalArgumentException("Exactly one option must be selected for a dropdown attribute");
        }

        AttributeOption option = getValidOption(categoryAttribute, optionIds.get(0));

        entity.setSelectedOption(option);
    }


    private void setMultiSelectValue(ProductAttributeValue entity, CategoryAttribute categoryAttribute, List<UUID> optionIds) {

        if (optionIds == null || optionIds.isEmpty()) {
            throw new IllegalArgumentException("At least one option must be selected");
        }

        List<UUID> uniqueOptionIds = optionIds.stream().distinct().toList();

        List<AttributeOption> options = uniqueOptionIds
                .stream()
                .map(optionId -> getValidOption(categoryAttribute, optionId))
                .toList();

        entity.setSelectedOptions(new ArrayList<>(options));
    }

    private AttributeOption getValidOption(CategoryAttribute categoryAttribute, UUID optionId) {

        AttributeOption option = attributeOptionRepository
                        .findByIdAndStatus(optionId, Status.ACTIVE)
                        .orElseThrow(() -> new ResourceNotFoundException("Attribute option not found or inactive: " + optionId));

        UUID attributeId = categoryAttribute.getAttribute().getId();

        if (!option.getAttribute().getId().equals(attributeId)) {
            throw new IllegalArgumentException("Selected option does not belong to this attribute");
        }

        return option;
    }

    private ProductAttributeValueResponseDTO mapToResponse(ProductAttributeValue entity) {

        Attribute attribute = entity.getCategoryAttribute().getAttribute();

        AttributeOption selectedOption = entity.getSelectedOption();

        List<AttributeOption> selectedOptions =
                entity.getSelectedOptions() == null
                        ? List.of()
                        : entity.getSelectedOptions();

        return new ProductAttributeValueResponseDTO(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getCategoryAttribute().getId(),
                attribute.getName(),
                attribute.getCode(),
                attribute.getType(),
                attribute.getUnit(),
                entity.getValueText(),
                entity.getValueNumber(),
                entity.getValueDecimal(),
                entity.getValueBoolean(),
                selectedOption == null ? null : selectedOption.getId(),
                selectedOption == null ? null : selectedOption.getValue(),
                selectedOptions.stream().map(AttributeOption::getId).toList(),
                selectedOptions.stream().map(AttributeOption::getValue).toList()
        );
    }
}
