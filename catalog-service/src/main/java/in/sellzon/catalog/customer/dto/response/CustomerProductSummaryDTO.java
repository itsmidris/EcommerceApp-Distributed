package in.sellzon.catalog.customer.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerProductSummaryDTO(
        String marketplaceProductId,
        String title,
        String brand,
        BigDecimal mrp,
        BigDecimal sellingPrice,
        Integer stockQuantity,
        UUID categoryId,
        String categoryName,
        String primaryImageUrl
) {}
