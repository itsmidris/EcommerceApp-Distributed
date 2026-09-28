package in.sellzon.order.order.repository;

import in.sellzon.order.order.entity.SellerOrder;
import in.sellzon.order.order.enums.SellerOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerOrderRepository extends JpaRepository<SellerOrder, UUID> {

    boolean existsBySellerOrderNumber(String sellerOrderNumber);
    Optional<SellerOrder> findByOrderIdAndSellerId(UUID orderId, UUID sellerId);
    List<SellerOrder> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
    List<SellerOrder> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);
    List<SellerOrder> findBySellerIdAndStatusOrderByCreatedAtDesc(UUID sellerId, SellerOrderStatus status);
    Optional<SellerOrder> findByIdAndSellerId(UUID id, UUID sellerId);
}
