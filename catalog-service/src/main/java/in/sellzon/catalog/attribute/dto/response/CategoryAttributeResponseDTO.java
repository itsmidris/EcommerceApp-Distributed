package in.sellzon.catalog.attribute.dto.response;

import in.sellzon.catalog.attribute.enums.AttributeType;
import in.sellzon.catalog.common.enums.Status;

import java.util.UUID;

public record CategoryAttributeResponseDTO(

        UUID id,
        UUID categoryId,
        UUID attributeId,
        String attributeName,
        String attributeCode,
        AttributeType attributeType,
        String unit,
        boolean required,
        boolean filterable,
        boolean searchable,
        boolean variantAttribute,
        Integer displayOrder,
        Status status
) {
}
