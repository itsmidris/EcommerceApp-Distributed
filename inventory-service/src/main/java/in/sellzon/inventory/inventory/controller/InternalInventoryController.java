package in.sellzon.inventory.inventory.controller;

import in.sellzon.inventory.inventory.dto.request.InventoryReservationRequestDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryAvailabilityResponseDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryReservationResponseDTO;
import in.sellzon.inventory.inventory.service.InventoryReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/inventory")
@RequiredArgsConstructor
public class InternalInventoryController {

    private final InventoryReservationService reservationService;

    @PostMapping("/reservations")
    public ResponseEntity<InventoryReservationResponseDTO> reserve(@Valid @RequestBody InventoryReservationRequestDTO request) {
        return ResponseEntity.ok(reservationService.reserve(request));
    }

    @PostMapping("/reservations/{reservationId}/commit")
    public ResponseEntity<InventoryReservationResponseDTO> commit(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(reservationService.commit(reservationId));
    }

    @PostMapping("/reservations/{reservationId}/release")
    public ResponseEntity<InventoryReservationResponseDTO> release(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(reservationService.release(reservationId));
    }

    @GetMapping("/products/{productId}/availability")
    public ResponseEntity<InventoryAvailabilityResponseDTO> availability(@PathVariable UUID productId) {
        return ResponseEntity.ok(reservationService.getAvailability(productId));
    }
}
