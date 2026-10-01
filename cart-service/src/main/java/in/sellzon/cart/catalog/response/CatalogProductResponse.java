package in.sellzon.cart.catalog.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogProductResponse(
        UUID id,
        String marketplaceProductId,
        UUID sellerId,
        String sellerName,
        UUID categoryId,
        String categoryName,
        String sku,
        String title,
        String description,
        String brand,
        BigDecimal mrp,
        BigDecimal sellingPrice,
        Integer stockQuantity,
        String status,
        String imageUrl
) {
}
