package in.sellzon.order.order.dto.request;

import in.sellzon.order.order.enums.SellerOrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSellerOrderStatusRequestDTO(
        @NotNull(message = "Order status is required")
        SellerOrderStatus status
) {
}
