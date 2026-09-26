package in.sellzon.catalog.attribute.dto.response;

import in.sellzon.catalog.attribute.enums.AttributeType;

import java.util.List;
import java.util.UUID;

public record SellerCategoryAttributeResponseDTO(
        UUID categoryAttributeId,
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
        List<AttributeOptionResponseDTO> options
) {
}
