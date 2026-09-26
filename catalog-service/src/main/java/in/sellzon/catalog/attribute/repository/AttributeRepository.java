package in.sellzon.catalog.attribute.repository;

import in.sellzon.catalog.attribute.entity.Attribute;
import in.sellzon.catalog.attribute.enums.AttributeType;
import in.sellzon.catalog.common.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttributeRepository extends JpaRepository<Attribute, UUID> {

    Optional<Attribute> findByIdAndStatus(UUID id, Status status);
    Optional<Attribute> findByCode(String code);
    Optional<Attribute> findByCodeAndStatus(String code, Status status);
    List<Attribute> findByStatusOrderByNameAsc(Status status);
    List<Attribute> findByTypeAndStatusOrderByNameAsc(AttributeType type, Status status);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
