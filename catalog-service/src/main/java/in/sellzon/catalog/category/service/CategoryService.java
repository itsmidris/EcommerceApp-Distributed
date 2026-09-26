package in.sellzon.catalog.category.service;

import in.sellzon.catalog.category.dto.request.CreateCategoryRequestDTO;
import in.sellzon.catalog.category.dto.request.UpdateCategoryRequestDTO;
import in.sellzon.catalog.category.dto.response.CategoryResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponseDTO createCategory(CreateCategoryRequestDTO request);
    CategoryResponseDTO updateCategory(UUID categoryId, UpdateCategoryRequestDTO request);
    CategoryResponseDTO getCategory(UUID categoryId);
    List<CategoryResponseDTO> getRootCategories();
    List<CategoryResponseDTO> getChildCategories(UUID parentId);
    List<CategoryResponseDTO> getCategoryTree();
    List<CategoryResponseDTO> getLevelFourCategories();
    void deactivateCategory(UUID categoryId);
}
