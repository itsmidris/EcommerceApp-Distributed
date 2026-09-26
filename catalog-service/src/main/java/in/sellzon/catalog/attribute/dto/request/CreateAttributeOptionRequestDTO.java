package in.sellzon.catalog.attribute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAttributeOptionRequestDTO(
        @NotBlank(message = "Option value is required")
        @Size(max = 150, message = "Option value cannot exceed 150 characters")
        String value,

        @NotBlank(message = "Option code is required")
        @Size(max = 100, message = "Option code cannot exceed 100 characters")
        String code
) {
}
