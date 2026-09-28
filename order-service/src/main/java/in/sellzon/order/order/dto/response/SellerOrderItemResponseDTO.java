package in.sellzon.order.order.dto.response;

import java.math.BigDecimal;

public record SellerOrderItemResponseDTO(
        String marketplaceProductId,
        String sku,
        String productTitle,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
