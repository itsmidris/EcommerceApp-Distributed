package in.sellzon.inventory.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name="inventory_settings", uniqueConstraints = @UniqueConstraint( name ="uk_inventory_settings_product", columnNames="product_id"))
@Getter
@Setter
@NoArgsConstructor
public class InventorySettings {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 5;
}
