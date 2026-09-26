package in.sellzon.catalog.product.entity;

import in.sellzon.catalog.category.entity.Category;
import in.sellzon.catalog.common.entity.BaseEntity;
import in.sellzon.catalog.common.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_seller_id", columnList = "seller_id"),
                @Index(name = "idx_products_category_id", columnList = "category_id"),
                @Index(name = "idx_products_status", columnList = "status"),
                @Index(name = "idx_products_seller_status", columnList = "seller_id, status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_products_marketplace_product_id", columnNames = "marketplace_product_id"),
                @UniqueConstraint(name = "uk_products_seller_sku", columnNames = {"seller_id", "sku"})
        }
)
public class Product extends BaseEntity {

    @Column(name = "marketplace_product_id", nullable = false, length = 32, updatable = false)
    private String marketplaceProductId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_category"))
    private Category category;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 5000)
    private String description;

    @Column(nullable = false, length = 150)
    private String brand;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal mrp;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;
}
