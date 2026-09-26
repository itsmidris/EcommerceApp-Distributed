package in.sellzon.catalog.productimage.dto.response;

import java.util.UUID;

public record ProductImageResponseDTO(
        UUID id,
        UUID productId,
        String imageUrl,
        String originalFileName,
        String contentType,
        Long fileSize,
        Integer displayOrder,
        boolean primaryImage
) {
}
