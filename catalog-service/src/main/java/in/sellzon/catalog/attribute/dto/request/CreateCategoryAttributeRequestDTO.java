package in.sellzon.catalog.attribute.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCategoryAttributeRequestDTO(

        @NotNull(message = "Attribute ID is required")
        UUID attributeId,
        boolean required,
        boolean filterable,
        boolean searchable,
        boolean variantAttribute
) {
}
