package in.sellzon.inventory.inventory.service;

import in.sellzon.inventory.inventory.dto.request.InventoryReservationRequestDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryAvailabilityResponseDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryReservationResponseDTO;

import java.util.UUID;

public interface InventoryReservationService {
    InventoryReservationResponseDTO reserve(InventoryReservationRequestDTO request);
    InventoryReservationResponseDTO release(UUID reservationId);
    InventoryReservationResponseDTO commit(UUID reservationId);
    InventoryAvailabilityResponseDTO getAvailability(UUID productId);
}
