package in.sellzon.inventory.inventory.repository;

import in.sellzon.inventory.inventory.entity.InventoryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, UUID> {
    List<InventoryAdjustment> findTop50ByProductIdOrderByCreatedAtDesc(UUID productId);
}
