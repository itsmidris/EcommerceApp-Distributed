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
import in.sellzon.inventory.catalog.client.CatalogClient;
import in.sellzon.inventory.catalog.response.CatalogProductResponse;
import in.sellzon.inventory.inventory.service.InventorySystemService;
import in.sellzon.inventory.kafka.event.InventoryStockChangedEvent;
import in.sellzon.inventory.kafka.producer.InventoryStockEventProducer;
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
    private final CatalogClient catalogClient;
    private final InventoryStockEventProducer inventoryStockEventProducer;

    @Override
    @Transactional
    public InventoryReservationResponseDTO reserve(InventoryReservationRequestDTO request) {

        InventoryReservation existing = reservationRepository.findByOrderIdAndProductId(request.orderId(), request.productId()).orElse(null);

        if (existing != null) {
            LocalDateTime now = LocalDateTime.now();

            if (existing.getStatus() == InventoryReservationStatus.ACTIVE
                    && existing.getExpiresAt().isAfter(now)) {
                return map(existing);
            }

            if (existing.getStatus() == InventoryReservationStatus.COMMITTED) {
                return map(existing);
            }

            // A released/expired reservation can be reused when the customer retries payment.
            if (existing.getStatus() == InventoryReservationStatus.ACTIVE
                    && !existing.getExpiresAt().isAfter(now)) {
                existing.setStatus(InventoryReservationStatus.EXPIRED);
            }
        }

        InventoryItem item = itemRepository
                .findByProductIdForUpdate(request.productId())
                .orElseGet(() -> {
                    CatalogProductResponse catalog = catalogClient.getProductById(
                            request.productId()
                    );
                    return createInventoryFromCatalog(catalog);
                });

        if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) throw new BusinessException("Product inventory is not active");

        int reserved = getReservedQuantity(item.getProductId());
        int available = item.getStockQuantity() - reserved;
        if (request.quantity() > available) throw new BusinessException("Only " + Math.max(available, 0) + " units are available");

        InventoryReservation reservation = existing != null ? existing : new InventoryReservation();

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

        inventoryStockEventProducer.publish(
                new InventoryStockChangedEvent(
                        UUID.randomUUID(),
                        item.getProductId(),
                        item.getMarketplaceProductId(),
                        item.getStockQuantity()
                )
        );

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
    @Transactional
    public void commitOrder(UUID orderId) {

        var reservations = reservationRepository
                .findByOrderIdOrderByProductIdAsc(orderId);

        if (reservations.isEmpty()) {
            return;
        }

        boolean hasReleasedOrExpired = reservations.stream()
                .anyMatch(r -> r.getStatus() == InventoryReservationStatus.RELEASED
                        || r.getStatus() == InventoryReservationStatus.EXPIRED);

        if (hasReleasedOrExpired) {
            throw new BusinessException("Inventory reservation is no longer available for order: " + orderId);
        }

        if (reservations.stream().allMatch(r -> r.getStatus() == InventoryReservationStatus.COMMITTED)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (InventoryReservation reservation : reservations) {
            if (reservation.getStatus() == InventoryReservationStatus.COMMITTED) {
                continue;
            }
            if (!reservation.getExpiresAt().isAfter(now)) {
                reservation.setStatus(InventoryReservationStatus.EXPIRED);
                throw new BusinessException("Inventory reservation has expired for product: " + reservation.getMarketplaceProductId());
            }

            InventoryItem item = itemRepository
                    .findByProductIdForUpdate(reservation.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + reservation.getProductId()));

            if (reservation.getQuantity() > item.getStockQuantity()) {
                throw new BusinessException("Insufficient stock while committing order: " + orderId);
            }

            int previousStock = item.getStockQuantity();
            item.setStockQuantity(previousStock - reservation.getQuantity());
            itemRepository.save(item);

            inventoryStockEventProducer.publish(
                    new InventoryStockChangedEvent(
                            UUID.randomUUID(),
                            item.getProductId(),
                            item.getMarketplaceProductId(),
                            item.getStockQuantity()
                    )
            );

            inventorySystemService.recordMovement(
                    item,
                    previousStock,
                    item.getStockQuantity(),
                    InventoryAdjustmentType.ORDER,
                    "Order inventory committed: " + orderId
            );

            reservation.setStatus(InventoryReservationStatus.COMMITTED);
        }
    }

    @Override
    @Transactional
    public void releaseOrder(UUID orderId) {

        reservationRepository
                .findByOrderIdAndStatusOrderByProductIdAsc(orderId, InventoryReservationStatus.ACTIVE)
                .forEach(reservation -> reservation.setStatus(InventoryReservationStatus.RELEASED));
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


    private InventoryItem createInventoryFromCatalog(CatalogProductResponse p) {
        InventoryItem item = new InventoryItem();
        item.setProductId(p.id());
        item.setSellerId(p.sellerId());
        item.setMarketplaceProductId(p.marketplaceProductId());
        item.setSku(p.sku());
        item.setTitle(p.title());
        item.setBrand(p.brand());
        item.setStockQuantity(p.stockQuantity() == null ? 0 : p.stockQuantity());
        item.setLowStockThreshold(5);
        item.setStatus(p.status() == null ? "ACTIVE" : p.status());
        return itemRepository.save(item);
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
