package in.sellzon.catalog.category.dto.response;

import java.util.UUID;

public record CategoryImportChangeDTO(
        int rowNumber,
        String action,
        UUID categoryId,
        String path,
        String message
) {
}
