package in.sellzon.order.order.dto.response;


import in.sellzon.order.order.enums.SellerOrderStatus;

import java.math.BigDecimal;

public record OrderItemResponseDTO(
        String marketplaceProductId,
        String sku,
        String productTitle,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String sellerName,
        SellerOrderStatus sellerOrderStatus
) {
}
