package in.sellzon.catalog.product.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductRequestDTO(

        @NotBlank(message = "Product title is required")
        @Size(max = 250, message = "Product title cannot exceed 250 characters")
        String title,

        @Size(max = 5000, message = "Product description cannot exceed 5000 characters")
        String description,

        @NotNull(message = "Selling price is required")
        @DecimalMin(value = "0.01", message = "Selling price must be greater than 0")
        BigDecimal sellingPrice,

        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity
) {
}
