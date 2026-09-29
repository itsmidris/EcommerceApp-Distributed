package in.sellzon.inventory.inventory.entity;

import in.sellzon.inventory.inventory.enums.InventoryAdjustmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_adjustments",
        indexes = {
                @Index(name = "idx_inventory_adjustment_product", columnList = "product_id"),
                @Index(name = "idx_inventory_adjustment_seller_created", columnList = "seller_id,created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class InventoryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="product_id", nullable=false)
    private UUID productId;

    @Column(name="seller_id", nullable=false)
    private UUID sellerId;

    @Column(name="marketplace_product_id", nullable=false)
    private String marketplaceProductId;

    @Column(nullable=false)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=30)
    private InventoryAdjustmentType type;

    @Column(name="quantity_change", nullable=false)
    private Integer quantityChange;

    @Column(name="previous_stock", nullable=false)
    private Integer previousStock;

    @Column(name="new_stock", nullable=false)
    private Integer newStock;

    @Column(length=500)
    private String reason;

    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
