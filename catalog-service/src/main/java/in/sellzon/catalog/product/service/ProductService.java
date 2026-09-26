package in.sellzon.catalog.product.service;

import in.sellzon.catalog.product.dto.request.CreateProductRequestDTO;
import in.sellzon.catalog.product.dto.request.UpdateProductRequestDTO;
import in.sellzon.catalog.product.dto.response.ProductResponseDTO;

import java.util.List;

public interface ProductService {

    ProductResponseDTO createProduct(CreateProductRequestDTO request);

    ProductResponseDTO getProduct(String marketplaceProductId);

    List<ProductResponseDTO> getMyProducts();

    ProductResponseDTO updateProduct(String marketplaceProductId, UpdateProductRequestDTO request);

    void deactivateProduct(String marketplaceProductId);

    void activateProduct(String marketplaceProductId);
}
