package in.sellzon.order.order.entity;

import in.sellzon.order.common.BaseEntity;
import in.sellzon.order.order.enums.SellerOrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "seller_orders",
        indexes = {
                @Index(name = "idx_seller_orders_seller_created", columnList = "seller_id,created_at"),
                @Index(name = "idx_seller_orders_order", columnList = "order_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_seller_order_number", columnNames = "seller_order_number"),
                @UniqueConstraint(name = "uk_order_seller", columnNames = {"order_id","seller_id"})
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SellerOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "seller_order_number", nullable = false, length = 30, updatable = false)
    private String sellerOrderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellerOrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "sellerOrder")
    @OrderBy("createdAt ASC")
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setSellerOrder(this);
    }
}
