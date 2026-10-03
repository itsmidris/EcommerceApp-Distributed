package in.sellzon.catalog.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequestDTO(

        @NotBlank(message = "Product title is required")
        @Size(max = 250, message = "Product title cannot exceed 250 characters")
        String title,

        @Size(max = 5000, message = "Product description cannot exceed 5000 characters")
        String description,

        @NotNull(message = "Selling price is required")
        @DecimalMin(value = "0.01", message = "Selling price must be greater than 0")
        BigDecimal sellingPrice
) {}
