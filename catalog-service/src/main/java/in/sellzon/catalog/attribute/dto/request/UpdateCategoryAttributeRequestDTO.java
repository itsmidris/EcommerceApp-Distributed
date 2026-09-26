package in.sellzon.catalog.attribute.dto.request;

public record UpdateCategoryAttributeRequestDTO(
        boolean required,
        boolean filterable,
        boolean searchable,
        boolean variantAttribute
) {
}
