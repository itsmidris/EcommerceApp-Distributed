package in.sellzon.inventory.inventory.repository;

import in.sellzon.inventory.inventory.entity.InventorySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventorySettingsRepository extends JpaRepository<InventorySettings, UUID> {
    Optional<InventorySettings> findByProductId(UUID productId);
}
