package in.sellzon.catalog.category.service.impl;

import in.sellzon.catalog.category.dto.request.CreateCategoryRequestDTO;
import in.sellzon.catalog.category.dto.request.UpdateCategoryRequestDTO;
import in.sellzon.catalog.category.dto.response.CategoryResponseDTO;
import in.sellzon.catalog.category.entity.Category;
import in.sellzon.catalog.category.enums.CategoryLevel;
import in.sellzon.catalog.category.repository.CategoryRepository;
import in.sellzon.catalog.category.service.CategoryService;
import in.sellzon.catalog.common.enums.Status;
import in.sellzon.catalog.common.exception.DuplicateResourceException;
import in.sellzon.catalog.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public CategoryResponseDTO createCategory(CreateCategoryRequestDTO request) {

        String name = normalizeName(request.name());
        String slug = generateSlug(name);

        Category parent = null;
        CategoryLevel level;

        if (request.parentId() == null) {
            level = CategoryLevel.LEVEL_1;
        } else {
            parent = getActiveCategory(request.parentId());
            if (parent.getLevel() == CategoryLevel.LEVEL_4) {
                throw new IllegalArgumentException("Level_4 category cannot have child categories");
            }

            level = CategoryLevel.fromLevelNumber(parent.getLevel().getLevelNumber() + 1);
        }

        validateDuplicateName(parent, name, null);
        validateDuplicateSlug(parent, slug, null);

        Category category = new Category();

        category.setName(name);
        category.setSlug(slug);
        category.setDescription(normalizeDescription(request.description()));
        category.setParent(parent);
        category.setLevel(level);
        category.setDisplayOrder(getNextDisplayOrder(parent));
        category.setStatus(Status.ACTIVE);

        Category savedCategory = categoryRepository.save(category);

        return mapToResponse(savedCategory, false);
    }

    @Override
    public CategoryResponseDTO updateCategory(UUID categoryId, UpdateCategoryRequestDTO request) {
        Category category = getActiveCategory(categoryId);

        String name = normalizeName(request.name());
        String slug = generateSlug(name);

        UUID requestedParentId = request.parentId();

        Category newParent = null;

        if (requestedParentId != null) {
            newParent = getActiveCategory(requestedParentId);

            if (newParent.getId(). equals(category.getId())) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }

            if (newParent.getLevel() == CategoryLevel.LEVEL_4) {
                throw new IllegalArgumentException("LEVEL_4 category cannot have child categories");
            }

            if (isDescendant(newParent, category)) {
                throw new IllegalArgumentException("Category cannot be moved under one of its descendants");
            }
        }

        CategoryLevel newLevel;

        if (newParent == null) {
            newLevel = CategoryLevel.LEVEL_1;
        } else {
            newLevel = CategoryLevel.fromLevelNumber(newParent.getLevel().getLevelNumber() + 1);
        }

        if (category.getChildren() != null
                && !category.getChildren().isEmpty()
                && newLevel.getLevelNumber() + getMaximumDescendantDepth(category) > 4) {
            throw new IllegalArgumentException("Moving this category would create a hierarchy deeper than 4 levels");
        }

        validateDuplicateName(newParent, name, categoryId);
        validateDuplicateSlug(newParent, slug, categoryId);

        boolean parentChanged =
                !java.util.Objects.equals(
                        category.getParent() == null
                                ? null
                                : category.getParent().getId(),
                        newParent == null
                                ? null
                                : newParent.getId()
                );

        category.setName(name);
        category.setSlug(slug);
        category.setDescription(normalizeDescription(request.description()));
        category.setParent(newParent);
        category.setLevel(newLevel);

        if (parentChanged) {
            category.setDisplayOrder(getNextDisplayOrder(newParent));
        }

        Category savedCategory = categoryRepository.save(category);

        updateDescendantLevels(savedCategory);

        return mapToResponse(savedCategory, false);
    }

    @Override
//    @Transactional
    public CategoryResponseDTO getCategory(UUID categoryId) {
        Category category = getActiveCategory(categoryId);

        return mapToResponse(category, true);
    }

    @Override
    public List<CategoryResponseDTO> getRootCategories() {
        return  categoryRepository
                .findByParentIsNullAndStatusOrderByDisplayOrderAscNameAsc(Status.ACTIVE)
                .stream()
                .map(category -> mapToResponse(category, false))
                .toList();
    }

    @Override
    public List<CategoryResponseDTO> getChildCategories(UUID parentId) {
        Category parent = getActiveCategory(parentId);

        return categoryRepository
                .findByParentIdAndStatusOrderByDisplayOrderAscNameAsc(parent.getId(), Status.ACTIVE)
                .stream()
                .map(category -> mapToResponse(category, false))
                .toList();
    }

    @Override
    public List<CategoryResponseDTO> getCategoryTree() {
        return categoryRepository
                .findByParentIsNullAndStatusOrderByDisplayOrderAscNameAsc(Status.ACTIVE)
                .stream()
                .map(category -> mapToResponse(category, true))
                .toList();
    }

    @Override
    public List<CategoryResponseDTO> getLevelFourCategories() {
        return categoryRepository
                .findByLevelAndStatusOrderByDisplayOrderAscNameAsc(CategoryLevel.LEVEL_4, Status.ACTIVE)
                .stream()
                .map(category -> mapToResponse(category, false))
                .toList();
    }

    @Override
    public void deactivateCategory(UUID categoryId) {
        Category category = getActiveCategory(categoryId);

        if (categoryRepository.existsByParentId(category.getId())) {
            throw new IllegalArgumentException("Cannot deactivate a category that has child categories");
        }

        category.setStatus(Status.INACTIVE);

        categoryRepository.save(category);
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    private String generateSlug(String value) {
        String normalized = Normalizer
                .normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private Category getActiveCategory(UUID categoryId) {
        return categoryRepository
                .findByIdAndStatus(categoryId, Status.ACTIVE)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Category not found: " + categoryId
                ));
    }

    private void validateDuplicateName(Category parent, String name, UUID categoryId) {

        boolean exists;

        if (parent == null) {

            if (categoryId == null) {
                exists = categoryRepository.existsByParentIsNullAndNameIgnoreCase(name);
            } else {
                exists = categoryRepository.existsByParentIsNullAndNameIgnoreCaseAndIdNot(name, categoryId);
            }

        } else {

            if (categoryId == null) {
                exists = categoryRepository.existsByParentIdAndNameIgnoreCase(parent.getId(), name);
            } else {
                exists = categoryRepository.existsByParentIdAndNameIgnoreCaseAndIdNot(parent.getId(), name, categoryId);
            }

        }

        if (exists) {
            throw new DuplicateResourceException("Category with name '" + name + "' already exists under this parent");
        }
    }

    private void validateDuplicateSlug(Category parent, String slug, UUID categoryId) {

        boolean exists;

        if (parent == null) {

            if (categoryId == null) {
                exists = categoryRepository.existsByParentIsNullAndSlug(slug);
            } else {
                exists = categoryRepository.existsByParentIsNullAndSlugAndIdNot(slug, categoryId);
            }

        } else {

            if (categoryId == null) {
                exists = categoryRepository.existsByParentIdAndSlug(parent.getId(), slug);
            } else {
                exists = categoryRepository.existsByParentIdAndSlugAndIdNot(parent.getId(), slug, categoryId);
            }

        }

        if (exists) {
            throw new DuplicateResourceException("Category with slug '" + slug + "' already exists under this parent");
        }
    }

    private CategoryResponseDTO mapToResponse(Category category, boolean includeChildren) {
        List<CategoryResponseDTO> children =
                includeChildren
                        ? category.getChildren()
                        .stream()
                        .filter(child -> child.getStatus() == Status.ACTIVE)
                        .map(child -> mapToResponse(child, true))
                        .toList()
                        : List.of();

        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getLevel(),
                category.getParent() == null
                        ? null
                        : category.getParent().getId(),
                category.getParent() == null
                        ? null
                        : category.getParent().getName(),
                category.getDisplayOrder(),
                category.getStatus(),
                children
        );
    }

    private boolean isDescendant(Category possibleDescendant, Category category) {
        Category current = possibleDescendant;

        while (current != null) {
            if (current.getId().equals(category.getId())) {
                return true;
            }

            current = current.getParent();
        }

        return false;
    }

    private int getMaximumDescendantDepth(Category category) {
        if (category.getChildren() == null || category.getChildren().isEmpty()) {
            return 0;
        }

        return category.getChildren()
                .stream()
                .mapToInt(child -> 1 + getMaximumDescendantDepth(child))
                .max()
                .orElse(0);
    }

    private void updateDescendantLevels(Category category) {
        if (category.getChildren() == null) {
            return;
        }

        for (Category child : category.getChildren()) {
            CategoryLevel childLevel = CategoryLevel.fromLevelNumber(category.getLevel().getLevelNumber() + 1);

            child.setLevel(childLevel);

            categoryRepository.save(child);

            updateDescendantLevels(child);
        }
    }
    private int getNextDisplayOrder(Category parent) {

        if (parent == null) {

            return categoryRepository
                    .findTopByParentIsNullAndStatusOrderByDisplayOrderDesc(Status.ACTIVE)
                    .map(category -> category.getDisplayOrder() + 1)
                    .orElse(1);

        }

        return categoryRepository
                .findTopByParentAndStatusOrderByDisplayOrderDesc(parent, Status.ACTIVE)
                .map(category -> category.getDisplayOrder() + 1)
                .orElse(1);
    }
}
