package in.sellzon.catalog.customer.dto.response;

import in.sellzon.catalog.attribute.enums.AttributeType;

import java.util.List;
import java.util.UUID;

public record CustomerProductAttributeDTO(
        UUID categoryAttributeId,
        String name,
        String code,
        AttributeType type,
        String unit,
        String value,
        List<String> selectedOptions
) {}
