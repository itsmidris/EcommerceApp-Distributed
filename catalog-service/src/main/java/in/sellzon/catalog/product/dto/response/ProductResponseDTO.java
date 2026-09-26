package in.sellzon.catalog.product.dto.response;

import in.sellzon.catalog.common.enums.Status;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponseDTO(

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
        Status status
) {
}
