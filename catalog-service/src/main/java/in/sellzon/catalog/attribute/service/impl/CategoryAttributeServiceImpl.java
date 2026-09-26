package in.sellzon.catalog.attribute.service.impl;

import in.sellzon.catalog.attribute.dto.request.CreateCategoryAttributeRequestDTO;
import in.sellzon.catalog.attribute.dto.request.UpdateCategoryAttributeRequestDTO;
import in.sellzon.catalog.attribute.dto.response.AttributeOptionResponseDTO;
import in.sellzon.catalog.attribute.dto.response.CategoryAttributeResponseDTO;
import in.sellzon.catalog.attribute.dto.response.SellerCategoryAttributeResponseDTO;
import in.sellzon.catalog.attribute.entity.Attribute;
import in.sellzon.catalog.attribute.entity.AttributeOption;
import in.sellzon.catalog.attribute.entity.CategoryAttribute;
import in.sellzon.catalog.attribute.enums.AttributeType;
import in.sellzon.catalog.attribute.repository.AttributeOptionRepository;
import in.sellzon.catalog.attribute.repository.AttributeRepository;
import in.sellzon.catalog.attribute.repository.CategoryAttributeRepository;
import in.sellzon.catalog.attribute.service.CategoryAttributeService;
import in.sellzon.catalog.category.entity.Category;
import in.sellzon.catalog.category.enums.CategoryLevel;
import in.sellzon.catalog.category.repository.CategoryRepository;
import in.sellzon.catalog.common.enums.Status;
import in.sellzon.catalog.common.exception.DuplicateResourceException;
import in.sellzon.catalog.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryAttributeServiceImpl implements CategoryAttributeService {

    private final CategoryAttributeRepository categoryAttributeRepository;
    private final CategoryRepository categoryRepository;
    private final AttributeRepository attributeRepository;
    private final AttributeOptionRepository attributeOptionRepository;

    @Override
    public CategoryAttributeResponseDTO createCategoryAttribute(UUID categoryId, CreateCategoryAttributeRequestDTO request) {
        Category category = getActiveCategory(categoryId);

        validateLeafCategory(category);

        Attribute attribute = getActiveAttribute(request.attributeId());

        validateDuplicateMapping(categoryId, request.attributeId());

        validateConfiguration(attribute, request.variantAttribute());

        CategoryAttribute categoryAttribute = new CategoryAttribute();

        categoryAttribute.setCategory(category);
        categoryAttribute.setAttribute(attribute);
        categoryAttribute.setRequired(request.required());
        categoryAttribute.setFilterable(request.filterable());
        categoryAttribute.setSearchable(request.searchable());
        categoryAttribute.setVariantAttribute(request.variantAttribute());
        categoryAttribute.setDisplayOrder(getNextDisplayOrder(categoryId));
        categoryAttribute.setStatus(Status.ACTIVE);

        CategoryAttribute saved = categoryAttributeRepository.save(categoryAttribute);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryAttributeResponseDTO getCategoryAttribute(UUID categoryAttributeId) {
        CategoryAttribute categoryAttribute = getActiveCategoryAttribute(categoryAttributeId);
        return mapToResponse(categoryAttribute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryAttributeResponseDTO> getAttributesByCategory(UUID categoryId) {
        Category category = getActiveCategory(categoryId);

        validateLeafCategory(category);
        return categoryAttributeRepository
                .findByCategoryIdAndStatusOrderByDisplayOrderAsc(categoryId, Status.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerCategoryAttributeResponseDTO> getSellerAttributesByCategory(UUID categoryId) {
        Category category = getActiveCategory(categoryId);

        validateLeafCategory(category);

        List<CategoryAttribute> categoryAttributes = categoryAttributeRepository.findByCategoryIdAndStatusOrderByDisplayOrderAsc(categoryId, Status.ACTIVE);

        if (categoryAttributes.isEmpty()) {
            return List.of();
        }

        List<UUID> attributeIds = categoryAttributes
                .stream()
                .map(categoryAttribute -> categoryAttribute.getAttribute().getId())
                .distinct()
                .toList();

        Map<UUID, List<AttributeOption>> optionsByAttributeId = attributeOptionRepository
                .findByAttributeIdInAndStatusOrderByAttributeIdAscDisplayOrderAsc(attributeIds, Status.ACTIVE)
                .stream()
                .collect(Collectors.groupingBy(option -> option.getAttribute().getId()));

        return categoryAttributes
                .stream()
                .map(categoryAttribute -> {
                    var attribute = categoryAttribute.getAttribute();

                    List<AttributeOptionResponseDTO> options = optionsByAttributeId
                            .getOrDefault(attribute.getId(), List.of())
                            .stream()
                            .map(this::mapOptionToResponse)
                            .toList();

                    return new SellerCategoryAttributeResponseDTO(
                            categoryAttribute.getId(),
                            attribute.getId(),
                            attribute.getName(),
                            attribute.getCode(),
                            attribute.getType(),
                            attribute.getUnit(),
                            categoryAttribute.isRequired(),
                            categoryAttribute.isFilterable(),
                            categoryAttribute.isSearchable(),
                            categoryAttribute.isVariantAttribute(),
                            categoryAttribute.getDisplayOrder(),
                            options);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryAttributeResponseDTO> getCategoriesByAttribute(UUID attributeId) {
        getActiveAttribute(attributeId);

        return categoryAttributeRepository
                .findByAttributeIdAndStatusOrderByDisplayOrderAsc(attributeId, Status.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public CategoryAttributeResponseDTO updateCategoryAttribute(UUID categoryAttributeId, UpdateCategoryAttributeRequestDTO request) {
        CategoryAttribute categoryAttribute = getActiveCategoryAttribute(categoryAttributeId);

        Attribute attribute = categoryAttribute.getAttribute();

        validateConfiguration(attribute, request.variantAttribute());

        categoryAttribute.setRequired(request.required());
        categoryAttribute.setFilterable(request.filterable());
        categoryAttribute.setSearchable(request.searchable());
        categoryAttribute.setVariantAttribute(request.variantAttribute());

        CategoryAttribute saved = categoryAttributeRepository.save(categoryAttribute);

        return mapToResponse(saved);
    }

    @Override
    public void deactivateCategoryAttribute(UUID categoryAttributeId) {
        CategoryAttribute categoryAttribute = getActiveCategoryAttribute(categoryAttributeId);

        categoryAttribute.setStatus(Status.INACTIVE);

        categoryAttributeRepository.save(categoryAttribute);
    }

    private Category getActiveCategory(UUID categoryId) {
        return categoryRepository
                .findByIdAndStatus(categoryId, Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("category not found: " + categoryId));
    }

    private Attribute getActiveAttribute(UUID attributeId) {
        return attributeRepository
                .findByIdAndStatus(attributeId, Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found: " + attributeId));
    }

    private void validateLeafCategory(Category category) {
        if (category.getLevel() != CategoryLevel.LEVEL_4) {
            throw new IllegalArgumentException("Attributes can only be assigned to LEVEL_4 categories");
        }
    }

    private void validateDuplicateMapping(UUID categoryId, UUID attributeId) {
        if (categoryAttributeRepository.existsByCategoryIdAndAttributeId(categoryId, attributeId)) {
            throw new DuplicateResourceException("Attribute is already assigned to this category");
        }
    }

    private void validateConfiguration(Attribute attribute, boolean variantAttribute) {
        if (!variantAttribute) {
            return;
        }

        AttributeType type = attribute.getType();

        if (type != AttributeType.DROPDOWN && type != AttributeType.MULTI_SELECT) {
            throw new IllegalArgumentException("Only DROPDOWN or MULTI_SELECT attributes can be variant attributes");
        }
    }

    private int getNextDisplayOrder(UUID categoryId) {
        return categoryAttributeRepository
                .findTopByCategoryIdAndStatusOrderByDisplayOrderDesc(categoryId, Status.ACTIVE)
                .map(categoryAttribute -> categoryAttribute.getDisplayOrder() + 1)
                .orElse(1);
    }

    private CategoryAttribute getActiveCategoryAttribute(UUID categoryAttributeId) {
        return categoryAttributeRepository
                .findByIdAndStatus(categoryAttributeId, Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Category attribute not found: " + categoryAttributeId));
    }

    private CategoryAttributeResponseDTO mapToResponse(CategoryAttribute categoryAttribute) {
        Attribute attribute = categoryAttribute.getAttribute();
        return new CategoryAttributeResponseDTO(
                categoryAttribute.getId(),
                categoryAttribute.getCategory().getId(),
                attribute.getId(),
                attribute.getName(),
                attribute.getCode(),
                attribute.getType(),
                attribute.getUnit(),
                categoryAttribute.isRequired(),
                categoryAttribute.isFilterable(),
                categoryAttribute.isSearchable(),
                categoryAttribute.isVariantAttribute(),
                categoryAttribute.getDisplayOrder(),
                categoryAttribute.getStatus()
        );
    }

    private AttributeOptionResponseDTO mapOptionToResponse(AttributeOption option) {
        return new AttributeOptionResponseDTO(
                option.getId(),
                option.getAttribute().getId(),
                option.getValue(),
                option.getCode(),
                option.getDisplayOrder(),
                option.getStatus()
        );
    }
}
