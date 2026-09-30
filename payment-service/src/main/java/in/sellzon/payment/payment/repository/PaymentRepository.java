package in.sellzon.payment.payment.repository;

import in.sellzon.payment.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderIdAndCustomerId(UUID orderId, UUID customerId);
    Optional<Payment> findByRazorpayOrderIdAndCustomerId(String razorpayOrderId, UUID customerId);
}
