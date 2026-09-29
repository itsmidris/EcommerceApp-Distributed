package in.sellzon.inventory.inventory.service.impl;

import in.sellzon.inventory.common.exception.BusinessException;
import in.sellzon.inventory.common.exception.ResourceNotFoundException;
import in.sellzon.inventory.inventory.dto.request.InventoryReservationRequestDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryAvailabilityResponseDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryReservationResponseDTO;
import in.sellzon.inventory.inventory.entity.InventoryItem;
import in.sellzon.inventory.inventory.entity.InventoryReservation;
import in.sellzon.inventory.inventory.enums.InventoryAdjustmentType;
import in.sellzon.inventory.inventory.enums.InventoryReservationStatus;
import in.sellzon.inventory.inventory.repository.InventoryItemRepository;
import in.sellzon.inventory.inventory.repository.InventoryReservationRepository;
import in.sellzon.inventory.inventory.service.InventoryReservationService;
import in.sellzon.inventory.inventory.service.InventorySystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryReservationServiceImpl implements InventoryReservationService {

    private final InventoryItemRepository itemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventorySystemService inventorySystemService;

    @Override
    @Transactional
    public InventoryReservationResponseDTO reserve(InventoryReservationRequestDTO request) {

        InventoryReservation existing = reservationRepository.findByOrderIdAndProductId(request.orderId(), request.productId()).orElse(null);

        if (existing != null) {
            if (existing.getStatus() == InventoryReservationStatus.ACTIVE && existing.getExpiresAt().isAfter(LocalDateTime.now())) return map(existing);

            if (existing.getStatus() == InventoryReservationStatus.COMMITTED) return map(existing);

            throw new BusinessException("A reservation already exists for this order and product");
        }

        InventoryItem item = itemRepository
                .findByProductIdForUpdate(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + request.productId()));

        if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) throw new BusinessException("Product inventory is not active");

        int reserved = getReservedQuantity(item.getProductId());
        int available = item.getStockQuantity() - reserved;
        if (request.quantity() > available) throw new BusinessException("Only " + Math.max(available, 0) + " units are available");

        InventoryReservation reservation = new InventoryReservation();

        reservation.setOrderId(request.orderId());
        reservation.setProductId(item.getProductId());
        reservation.setSellerId(item.getSellerId());
        reservation.setMarketplaceProductId(item.getMarketplaceProductId());
        reservation.setQuantity(request.quantity());
        reservation.setStatus(InventoryReservationStatus.ACTIVE);
        reservation.setExpiresAt(LocalDateTime.now().plusSeconds(request.effectiveTtlSeconds()));

        return map(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public InventoryReservationResponseDTO release(UUID reservationId) {

        InventoryReservation reservation = reservationRepository
                .findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory reservation not found"));

        if (reservation.getStatus() == InventoryReservationStatus.ACTIVE) reservation.setStatus(InventoryReservationStatus.RELEASED);

        return map(reservation);
    }

    @Override
    @Transactional
    public InventoryReservationResponseDTO commit(UUID reservationId) {

        InventoryReservation reservation = reservationRepository
                .findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory reservation not found"));

        if (reservation.getStatus() == InventoryReservationStatus.COMMITTED) return map(reservation);

        if (reservation.getStatus() == InventoryReservationStatus.ACTIVE && !reservation.getExpiresAt().isAfter(LocalDateTime.now())) {
            reservation.setStatus(InventoryReservationStatus.EXPIRED);
            throw new BusinessException("Inventory reservation has expired");
        }

        if (reservation.getStatus() != InventoryReservationStatus.ACTIVE) throw new BusinessException("Inventory reservation is no longer active");

        InventoryItem item = itemRepository
                .findByProductIdForUpdate(reservation.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (reservation.getQuantity() > item.getStockQuantity()) throw new BusinessException("Insufficient stock while committing reservation");

        int previousStock = item.getStockQuantity();

        item.setStockQuantity(previousStock - reservation.getQuantity());

        itemRepository.save(item);

        inventorySystemService.recordMovement(
                item,
                previousStock,
                item.getStockQuantity(),
                InventoryAdjustmentType.ORDER,
                "Order inventory committed: " + reservation.getOrderId()
        );

        reservation.setStatus(InventoryReservationStatus.COMMITTED);

        return map(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryAvailabilityResponseDTO getAvailability(UUID productId) {

        InventoryItem item = itemRepository
                .findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));

        int reserved = getReservedQuantity(productId);

        return new InventoryAvailabilityResponseDTO(
                productId,
                item.getMarketplaceProductId(),
                item.getStockQuantity(),
                reserved,
                Math.max(0, item.getStockQuantity() - reserved)
        );
    }

    private int getReservedQuantity(UUID productId) {

        Long value = reservationRepository.sumQuantityByProductIdAndStatus(productId, InventoryReservationStatus.ACTIVE);

        return value == null ? 0 : Math.toIntExact(value);
    }

    private InventoryReservationResponseDTO map(InventoryReservation r) {

        return new InventoryReservationResponseDTO(
                r.getId(),
                r.getOrderId(),
                r.getProductId(),
                r.getMarketplaceProductId(),
                r.getQuantity(),
                r.getStatus(),
                r.getExpiresAt()
        );
    }
}
