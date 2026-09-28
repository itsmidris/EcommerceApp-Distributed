package in.sellzon.order.order.repository;

import in.sellzon.order.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByCustomerIdAndClientRequestId(UUID customerId, String clientRequestId);
    Optional<Order> findByIdAndCustomerId(UUID orderId, UUID customerId);
    List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    boolean existsByOrderNumber(String orderNumber);
}
