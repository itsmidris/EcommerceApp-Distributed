package in.sellzon.catalog.attribute.repository;

import in.sellzon.catalog.attribute.entity.Attribute;
import in.sellzon.catalog.attribute.entity.AttributeOption;
import in.sellzon.catalog.common.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttributeOptionRepository extends JpaRepository<AttributeOption, UUID> {

    Optional<AttributeOption> findByIdAndStatus(UUID id, Status status);
    List<AttributeOption> findByAttributeAndStatusOrderByDisplayOrderAsc(Attribute attribute, Status status);
    List<AttributeOption> findByAttributeIdAndStatusOrderByDisplayOrderAsc(UUID attributeId, Status status);
    boolean existsByAttributeIdAndValueIgnoreCase(UUID attributeId, String value);
    boolean existsByAttributeIdAndCodeIgnoreCase(UUID attributeId, String code);
    boolean existsByAttributeIdAndValueIgnoreCaseAndIdNot(UUID attributeId, String value, UUID id);
    boolean existsByAttributeIdAndCodeIgnoreCaseAndIdNot(UUID attributeId, String code, UUID id);
    Optional<AttributeOption> findTopByAttributeAndStatusOrderByDisplayOrderDesc(Attribute attribute, Status status);
    boolean existsByAttributeId(UUID attributeId);
    List<AttributeOption> findByAttributeIdInAndStatusOrderByAttributeIdAscDisplayOrderAsc(List<UUID> attributeIds, Status status);
}
