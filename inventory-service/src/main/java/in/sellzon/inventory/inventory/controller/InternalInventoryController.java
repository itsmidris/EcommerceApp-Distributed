package in.sellzon.inventory.inventory.controller;

import in.sellzon.inventory.inventory.dto.request.InventoryReservationRequestDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryAvailabilityResponseDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryReservationResponseDTO;
import in.sellzon.inventory.inventory.service.InventoryReservationService;
import in.sellzon.inventory.security.InternalServiceTokenValidator;
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
    private final InternalServiceTokenValidator serviceTokenValidator;

    private void validateServiceToken(String token) {
        serviceTokenValidator.validate(token);
    }

    @PostMapping("/reservations")
    public ResponseEntity<InventoryReservationResponseDTO> reserve(
            @RequestHeader("X-Service-Token") String serviceToken,
            @Valid @RequestBody InventoryReservationRequestDTO request) {
        validateServiceToken(serviceToken);
        return ResponseEntity.ok(reservationService.reserve(request));
    }

    @PostMapping("/reservations/{reservationId}/commit")
    public ResponseEntity<InventoryReservationResponseDTO> commit(
            @RequestHeader("X-Service-Token") String serviceToken,
            @PathVariable UUID reservationId) {
        validateServiceToken(serviceToken);
        return ResponseEntity.ok(reservationService.commit(reservationId));
    }

    @PostMapping("/reservations/{reservationId}/release")
    public ResponseEntity<InventoryReservationResponseDTO> release(
            @RequestHeader("X-Service-Token") String serviceToken,
            @PathVariable UUID reservationId) {
        validateServiceToken(serviceToken);
        return ResponseEntity.ok(reservationService.release(reservationId));
    }

    @PostMapping("/orders/{orderId}/commit")
    public ResponseEntity<Void> commitOrder(
            @RequestHeader("X-Service-Token") String serviceToken,
            @PathVariable UUID orderId) {
        validateServiceToken(serviceToken);
        reservationService.commitOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{orderId}/release")
    public ResponseEntity<Void> releaseOrder(
            @RequestHeader("X-Service-Token") String serviceToken,
            @PathVariable UUID orderId) {
        validateServiceToken(serviceToken);
        reservationService.releaseOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products/{productId}/availability")
    public ResponseEntity<InventoryAvailabilityResponseDTO> availability(
            @RequestHeader("X-Service-Token") String serviceToken,
            @PathVariable UUID productId) {
        validateServiceToken(serviceToken);
        return ResponseEntity.ok(reservationService.getAvailability(productId));
    }
}
