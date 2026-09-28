package in.sellzon.order.cart.response;

import java.math.BigDecimal;

public record CartItemResponse(
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
