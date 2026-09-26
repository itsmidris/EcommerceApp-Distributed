package in.sellzon.catalog.attribute.service;

import in.sellzon.catalog.attribute.dto.request.CreateCategoryAttributeRequestDTO;
import in.sellzon.catalog.attribute.dto.request.UpdateCategoryAttributeRequestDTO;
import in.sellzon.catalog.attribute.dto.response.CategoryAttributeResponseDTO;
import in.sellzon.catalog.attribute.dto.response.SellerCategoryAttributeResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CategoryAttributeService {

    CategoryAttributeResponseDTO createCategoryAttribute(UUID categoryId, CreateCategoryAttributeRequestDTO request);
    CategoryAttributeResponseDTO getCategoryAttribute(UUID categoryAttributeId);
    List<CategoryAttributeResponseDTO> getAttributesByCategory(UUID categoryId);
    List<SellerCategoryAttributeResponseDTO> getSellerAttributesByCategory(UUID categoryId);
    List<CategoryAttributeResponseDTO> getCategoriesByAttribute(UUID attributeId);
    CategoryAttributeResponseDTO updateCategoryAttribute(UUID categoryAttributeId, UpdateCategoryAttributeRequestDTO request);
    void deactivateCategoryAttribute(UUID categoryAttributeId);
}
