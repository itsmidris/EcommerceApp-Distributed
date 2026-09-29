package in.sellzon.cart.cart.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddCartItemRequestDTO(
        @NotBlank(message = "Marketplace product ID is required")
        String marketplaceProductId,
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 99, message = "Quantity cannot exceed 99")
        int quantity
) {
}
