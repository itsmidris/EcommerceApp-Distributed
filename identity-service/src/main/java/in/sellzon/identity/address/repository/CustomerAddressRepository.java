package in.sellzon.identity.address.repository;

import in.sellzon.identity.address.entity.CustomerAddress;
import in.sellzon.identity.common.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {
    List<CustomerAddress> findByCustomerIdAndStatusOrderByDefaultAddressDescCreatedAtDesc(UUID customerId, Status status);
    Optional<CustomerAddress> findByIdAndCustomerIdAndStatus(UUID id, UUID customerId, Status status);
    boolean existsByCustomerIdAndStatus(UUID customerId, Status status);
}
