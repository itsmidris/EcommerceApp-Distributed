package in.sellzon.catalog.productimage.repository;

import in.sellzon.catalog.productimage.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(UUID productId);
    Optional<ProductImage> findByIdAndProductId(UUID imageId, UUID productId);
    Optional<ProductImage> findByProductIdAndPrimaryImageTrue(UUID productId);
    Optional<ProductImage> findTopByProductIdOrderByDisplayOrderDesc(UUID productId);
    Long countByProductId(UUID productId);
    boolean existsByProductIdAndPrimaryImageTrue(UUID productId);
}
