package in.sellzon.inventory.inventory.repository;

import in.sellzon.inventory.inventory.entity.InventoryProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryProcessedEventRepository extends JpaRepository<InventoryProcessedEvent, UUID> {
    boolean existsByEventId(UUID eventId);
}
