package in.sellzon.inventory.inventory.repository;

import in.sellzon.inventory.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    List<InventoryItem> findBySellerIdOrderByUpdatedAtDesc(UUID sellerId);
    Optional<InventoryItem> findByProductId(UUID productId);
    Optional<InventoryItem> findByMarketplaceProductId(String marketplaceProductId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.productId = :productId")
    Optional<InventoryItem> findByProductIdForUpdate(@Param("productId") UUID productId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.marketplaceProductId = :marketplaceProductId")
    Optional<InventoryItem> findByMarketplaceProductIdForUpdate(@Param("marketplaceProductId") String marketplaceProductId);
}
