package in.sellzon.catalog.productattribute.dto.response;

import in.sellzon.catalog.attribute.enums.AttributeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductAttributeValueResponseDTO(
        UUID id,
        UUID productId,
        UUID categoryAttributeId,
        String attributeName,
        String attributeCode,
        AttributeType attributeType,
        String unit,
        String valueText,
        Long valueNumber,
        BigDecimal valueDecimal,
        Boolean valueBoolean,
        UUID selectedOptionId,
        String selectedOptionValue,
        List<UUID> selectedOptionIds,
        List<String> selectedOptionValues
) {
}
