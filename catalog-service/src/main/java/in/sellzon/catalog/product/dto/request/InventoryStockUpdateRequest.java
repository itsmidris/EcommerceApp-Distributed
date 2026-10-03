package in.sellzon.catalog.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryStockUpdateRequest(
        @NotNull
        @Min(0)
        Integer stockQuantity
) {}
