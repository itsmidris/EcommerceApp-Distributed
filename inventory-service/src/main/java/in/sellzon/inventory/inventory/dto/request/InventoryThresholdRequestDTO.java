package in.sellzon.inventory.inventory.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryThresholdRequestDTO(
        @NotNull
        @Min(0)
        @Max(1000000)
        Integer lowStockThreshold
) {
}
