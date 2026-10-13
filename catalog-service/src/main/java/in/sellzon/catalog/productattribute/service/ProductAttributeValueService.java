package in.sellzon.catalog.productattribute.service;

import in.sellzon.catalog.attribute.entity.CategoryAttribute;
import in.sellzon.catalog.product.entity.Product;
import in.sellzon.catalog.productattribute.dto.request.SaveProductAttributeValueRequestDTO;
import in.sellzon.catalog.productattribute.dto.response.ProductAttributeValueResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ProductAttributeValueService {

    ProductAttributeValueResponseDTO createValue(String marketplaceProductId, SaveProductAttributeValueRequestDTO request);
    List<ProductAttributeValueResponseDTO> getProductAttributes(String marketplaceProductId);
    ProductAttributeValueResponseDTO getValue(String marketplaceProductId, UUID valueId);
    ProductAttributeValueResponseDTO updateValue(String marketplaceProductId, UUID valueId, SaveProductAttributeValueRequestDTO request);
    void deleteValue(String marketplaceProductId, UUID valueId);
    void createValuesForNewProduct(Product product, List<CategoryAttribute> categoryAttributes, List<SaveProductAttributeValueRequestDTO> requests);
}
