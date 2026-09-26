package in.sellzon.catalog.productattribute.repository;

import in.sellzon.catalog.productattribute.entity.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, UUID> {
    Optional<ProductAttributeValue> findByIdAndProductId(UUID id, UUID productId);
    List<ProductAttributeValue> findByProductId(UUID productId);
    Optional<ProductAttributeValue> findByProductIdAndCategoryAttributeId(UUID productId, UUID categoryAttributeId);
    boolean existsByProductIdAndCategoryAttributeId(UUID productId, UUID categoryAttributeId);
    void deleteByProductId(UUID productId);
    List<ProductAttributeValue> findByProductIdOrderByCategoryAttributeDisplayOrderAsc(UUID productId);
}
