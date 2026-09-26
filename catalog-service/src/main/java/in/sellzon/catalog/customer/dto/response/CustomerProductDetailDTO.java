package in.sellzon.catalog.customer.dto.response;

import in.sellzon.catalog.productimage.dto.response.ProductImageResponseDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CustomerProductDetailDTO(
        String marketplaceProductId,
        String title,
        String description,
        String brand,
        BigDecimal mrp,
        BigDecimal sellingPrice,
        Integer stockQuantity,
        UUID categoryId,
        String categoryName,
        String categoryPath,
        String sellerName,
        List<ProductImageResponseDTO> images,
        List<CustomerProductAttributeDTO> attributes
) {}
