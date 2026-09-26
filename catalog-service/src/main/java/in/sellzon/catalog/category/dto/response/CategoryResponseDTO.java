package in.sellzon.catalog.category.dto.response;

import in.sellzon.catalog.category.enums.CategoryLevel;
import in.sellzon.catalog.common.enums.Status;

import java.util.List;
import java.util.UUID;

public record CategoryResponseDTO(
        UUID id,
        String name,
        String slug,
        String description,
        CategoryLevel level,
        UUID parentId,
        String parentName,
        Integer displayOrder,
        Status status,
        List<CategoryResponseDTO> children
) {
}
