package in.sellzon.catalog.attribute.dto.request;

import in.sellzon.catalog.attribute.enums.AttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAttributeRequestDTO(

        @NotBlank(message = "Attribute name is required")
        @Size(max = 150, message = "Attribute name cannot exceed 150 characters")
        String name,

        @NotBlank(message = "Attribute code is required")
        @Size(max = 100, message = "Attribute code cannot exceed 100 characters")
        String code,

        @NotNull(message = "Attribute type is required")
        AttributeType type,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @Size(max = 50, message = "Unit cannot exceed 50 characters")
        String unit
) {
}
