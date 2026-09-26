package in.sellzon.catalog.productattribute.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SaveProductAttributeValueRequestDTO(
        @NotNull(message = "Category attribute ID is required")
        UUID categoryAttributeId,

        @Size(max = 5000, message = "Attribute value cannot exceed 5000 characters")
        String value,

        List<UUID> optionIds
) {
}
