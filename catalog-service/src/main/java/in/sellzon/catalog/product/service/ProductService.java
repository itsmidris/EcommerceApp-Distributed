package in.sellzon.catalog.product.service;

import in.sellzon.catalog.product.dto.request.CreateProductRequestDTO;
import in.sellzon.catalog.product.dto.request.UpdateProductRequestDTO;
import in.sellzon.catalog.product.dto.response.ProductResponseDTO;

import java.util.UUID;

import java.util.List;

public interface ProductService {

    ProductResponseDTO createProduct(CreateProductRequestDTO request);

    ProductResponseDTO getProduct(String marketplaceProductId);

    ProductResponseDTO getProductById(UUID productId);

    ProductResponseDTO getProductByIdInternal(UUID productId);

    List<ProductResponseDTO> getProductsBySellerId(UUID sellerId);

    List<ProductResponseDTO> getProductsBySellerIdInternal(UUID sellerId);

    List<ProductResponseDTO> getMyProducts();

    ProductResponseDTO updateProduct(String marketplaceProductId, UpdateProductRequestDTO request);

    void updateStockFromInventory(String marketplaceProductId, Integer stockQuantity);

    void deactivateProduct(String marketplaceProductId);

    void activateProduct(String marketplaceProductId);
}
