package in.sellzon.catalog.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCategoryRequestDTO(
        @NotBlank(message = "Category name is required")
        @Size(max = 150, message = "Category name cannot exceed 150 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        UUID parentId
) {
}
