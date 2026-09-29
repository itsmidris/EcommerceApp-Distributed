package in.sellzon.inventory.inventory.service.impl;

import in.sellzon.inventory.inventory.enums.InventoryReservationStatus;
import in.sellzon.inventory.inventory.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class InventoryReservationExpiryJob {

    private final InventoryReservationRepository repository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expireReservations() {
        repository
                .findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(InventoryReservationStatus.ACTIVE, LocalDateTime.now())
                .forEach(r -> r.setStatus(InventoryReservationStatus.EXPIRED));
    }
}
