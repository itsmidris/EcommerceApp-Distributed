package in.sellzon.catalog.attribute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAttributeOptionRequestDTO(
        @NotBlank(message = "Option value is required")
        @Size(max = 150, message = "Option value cannot exceed 150 characters")
        String value
) {
}
