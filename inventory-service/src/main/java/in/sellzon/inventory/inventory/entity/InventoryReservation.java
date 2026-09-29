package in.sellzon.inventory.inventory.entity;

import in.sellzon.inventory.inventory.enums.InventoryReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_reservations",
        uniqueConstraints=@UniqueConstraint(name="uk_inventory_reservation_order_product", columnNames={"order_id","product_id"}),
        indexes={
                @Index(name="idx_inventory_reservation_order", columnList="order_id"),
                @Index(name="idx_inventory_reservation_expiry", columnList="status,expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class InventoryReservation {

    @Id @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    @Column(name="order_id", nullable=false)
    private UUID orderId;

    @Column(name="product_id", nullable=false)
    private UUID productId;

    @Column(name="seller_id", nullable=false)
    private UUID sellerId;

    @Column(name="marketplace_product_id", nullable=false)
    private String marketplaceProductId;

    @Column(nullable=false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private InventoryReservationStatus status= InventoryReservationStatus.ACTIVE;

    @Column(name="expires_at", nullable=false)
    private LocalDateTime expiresAt;

    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable=false)
    private Long version;

    @PrePersist void onCreate() {
        LocalDateTime now=LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
