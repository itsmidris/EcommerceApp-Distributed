package in.sellzon.inventory.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_product", columnNames = "product_id"),
        indexes = {
                @Index(name = "idx_inventory_seller", columnList = "seller_id"),
                @Index(name = "idx_inventory_marketplace_id", columnList = "marketplace_product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "marketplace_product_id", nullable = false, length = 20)
    private String marketplaceProductId;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 200)
    private String brand;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 5;

    @Column(nullable = false, length = 30)
    private String status;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
