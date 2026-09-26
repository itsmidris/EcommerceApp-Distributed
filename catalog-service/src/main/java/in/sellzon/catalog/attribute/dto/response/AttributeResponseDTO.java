package in.sellzon.catalog.attribute.dto.response;

import in.sellzon.catalog.attribute.enums.AttributeType;
import in.sellzon.catalog.common.enums.Status;

import java.util.UUID;

public record AttributeResponseDTO(
        UUID id,
        String name,
        String code,
        AttributeType type,
        String description,
        String unit,
        Status status
) {
}
