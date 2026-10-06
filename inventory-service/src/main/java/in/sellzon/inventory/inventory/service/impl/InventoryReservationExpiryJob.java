package in.sellzon.inventory.inventory.service.impl;

import in.sellzon.inventory.inventory.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryReservationExpiryJob {

    private final InventoryReservationService reservationService;

    @Scheduled(fixedDelay = 60000)
    public void expireReservations() {
        reservationService.expireReservations();
    }
}
