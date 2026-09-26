package in.sellzon.catalog.category.dto.response;

import java.util.List;

public record CategoryImportPreviewDTO(
        int totalRows,
        int createCount,
        int updateCount,
        int unchangedCount,
        List<CategoryImportIssueDTO> errors,
        List<CategoryImportChangeDTO> changes
) {
}
