package in.sellzon.catalog.attribute.dto.response;

import in.sellzon.catalog.common.enums.Status;

import java.util.UUID;

public record AttributeOptionResponseDTO(
        UUID id,
        UUID attributeId,
        String value,
        String code,
        Integer displayOrder,
        Status status
) {
}
