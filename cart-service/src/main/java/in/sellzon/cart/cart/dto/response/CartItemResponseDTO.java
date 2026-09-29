package in.sellzon.cart.cart.dto.response;

import java.math.BigDecimal;

public record CartItemResponseDTO(
        String marketplaceProductId,
        String title,
        String brand,
        BigDecimal mrp,
        BigDecimal sellingPrice,
        int quantity,
        int availableStock,
        String imageUrl,
        BigDecimal itemTotal
) {
}
