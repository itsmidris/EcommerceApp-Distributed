package in.sellzon.catalog.customer.service;

import in.sellzon.catalog.customer.dto.response.CustomerProductDetailDTO;
import in.sellzon.catalog.customer.dto.response.CustomerProductPageDTO;

import java.util.UUID;

public interface CustomerCatalogService {
    CustomerProductPageDTO searchProducts(String query, UUID categoryId, int page, int size);
    CustomerProductDetailDTO getProduct(String marketplaceProductId);
}
