package in.sellzon.inventory.inventory.repository;

import in.sellzon.inventory.inventory.entity.InventoryReservation;
import in.sellzon.inventory.inventory.enums.InventoryReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    @Query("select coalesce(sum(r.quantity),0) from InventoryReservation r where r.productId=:productId and r.status=:status")
    Long sumQuantityByProductIdAndStatus(@Param("productId") UUID productId, @Param("status") InventoryReservationStatus status);
    Optional<InventoryReservation> findByOrderIdAndProductId(UUID orderId, UUID productId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from InventoryReservation r where r.id=:id")
    Optional<InventoryReservation> findByIdForUpdate(@Param("id") UUID id);
    List<InventoryReservation> findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(InventoryReservationStatus status, LocalDateTime now);
}
