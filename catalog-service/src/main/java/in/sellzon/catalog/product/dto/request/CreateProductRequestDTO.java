package in.sellzon.catalog.product.dto.request;

import in.sellzon.catalog.productattribute.dto.request.SaveProductAttributeValueRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProductRequestDTO(

        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU cannot exceed 100 characters")
        String sku,

        @NotBlank(message = "Product title is required")
        @Size(max = 250, message = "Product title cannot exceed 250 characters")
        String title,

        @Size(max = 5000, message = "Product description cannot exceed 5000 characters")
        String description,

        @NotBlank(message = "Brand is required")
        @Size(max = 150, message = "Brand cannot exceed 150 characters")
        String brand,

        @NotNull(message = "MRP is required")
        @DecimalMin(value = "0.01", message = "MRP must be greater than 0")
        BigDecimal mrp,

        @NotNull(message = "Selling price is required")
        @DecimalMin(value = "0.01", message = "Selling price must be greater than 0")
        BigDecimal sellingPrice,

        @NotNull(message = "Category attributes are required")
        @Valid
        List<@Valid SaveProductAttributeValueRequestDTO> attributes
) {
}
