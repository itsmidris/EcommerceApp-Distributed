package in.sellzon.inventory.inventory.dto.request;

import in.sellzon.inventory.inventory.enums.InventoryAdjustmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryAdjustmentRequestDTO(
        @NotNull(message="Operation is required")
        Operation operation,
        @Min(value=0, message="Quantity cannot be negative")
        int quantity,
        @NotNull(message="Adjustment type is required")
        InventoryAdjustmentType type,
        @Size(max=500, message="Reason cannot exceed 500 characters")
        String reason
) {
    public enum Operation {
        ADD, REMOVE, SET
    }
}
