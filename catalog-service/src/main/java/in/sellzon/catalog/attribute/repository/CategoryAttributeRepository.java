package in.sellzon.catalog.attribute.repository;

import in.sellzon.catalog.attribute.entity.CategoryAttribute;
import in.sellzon.catalog.common.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, UUID> {

    Optional<CategoryAttribute> findByIdAndStatus(UUID id, Status status);
    List<CategoryAttribute> findByCategoryIdAndStatusOrderByDisplayOrderAsc(UUID categoryId, Status status);
    List<CategoryAttribute> findByAttributeIdAndStatusOrderByDisplayOrderAsc(UUID attributeId, Status status);
    boolean existsByCategoryIdAndAttributeId(UUID categoryId, UUID attributeId);
    boolean existsByCategoryIdAndAttributeIdAndIdNot(UUID categoryId, UUID attributeId, UUID id);
    boolean existsByCategoryId(UUID categoryId);
    Optional<CategoryAttribute> findTopByCategoryIdAndStatusOrderByDisplayOrderDesc(UUID categoryId, Status status);
}
