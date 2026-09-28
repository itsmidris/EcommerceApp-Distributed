package in.sellzon.order.order.repository;

import in.sellzon.order.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findBySellerIdAndSellerOrderIsNull(UUID sellerId);
    List<OrderItem> findByOrderId(UUID orderId);
}
