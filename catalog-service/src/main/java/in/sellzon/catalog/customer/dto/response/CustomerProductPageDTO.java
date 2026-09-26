package in.sellzon.catalog.customer.dto.response;

import java.util.List;

public record CustomerProductPageDTO(
        List<CustomerProductSummaryDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {}
