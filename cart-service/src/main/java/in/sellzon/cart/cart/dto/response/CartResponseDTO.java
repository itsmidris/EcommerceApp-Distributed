package in.sellzon.cart.cart.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponseDTO(
        List<CartItemResponseDTO> items,
        int totalItems,
        BigDecimal subtotal
) {
}
