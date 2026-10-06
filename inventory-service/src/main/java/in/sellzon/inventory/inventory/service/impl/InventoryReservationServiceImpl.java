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
import java.util.List;
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

                InventoryItem oldItem = itemRepository
                        .findByProductIdForUpdate(existing.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

                oldItem.setBlockedQuantity(oldItem.getBlockedQuantity() - existing.getQuantity());

                oldItem.setAvailableQuantity(oldItem.getAvailableQuantity() + existing.getQuantity());

                existing.setStatus(InventoryReservationStatus.EXPIRED);

                itemRepository.save(oldItem);
                reservationRepository.save(existing);

                publishAvailability(oldItem);
            }
        }

        InventoryItem item = itemRepository
                .findByProductIdForUpdate(request.productId())
                .orElseGet(() -> {
                    CatalogProductResponse catalog = catalogClient.getProductById(request.productId());
                    return createInventoryFromCatalog(catalog);
                });

        if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) throw new BusinessException("Product inventory is not active");

        if (request.quantity() <= 0) {
            throw new BusinessException("Reservation quantity must be greater than 0");
        }

        if (request.quantity() > item.getAvailableQuantity()) {
            throw new BusinessException("Only " + item.getAvailableQuantity() + " units are available");
        }

        InventoryReservation reservation = existing != null ? existing : new InventoryReservation();

        reservation.setOrderId(request.orderId());
        reservation.setProductId(item.getProductId());
        reservation.setSellerId(item.getSellerId());
        reservation.setMarketplaceProductId(item.getMarketplaceProductId());
        reservation.setQuantity(request.quantity());
        reservation.setStatus(InventoryReservationStatus.ACTIVE);
        reservation.setExpiresAt(LocalDateTime.now().plusSeconds(request.effectiveTtlSeconds()));

        item.setAvailableQuantity(item.getAvailableQuantity() - request.quantity());
        item.setBlockedQuantity(item.getBlockedQuantity() + request.quantity());

        itemRepository.save(item);

        InventoryReservation saved = reservationRepository.save(reservation);

        publishAvailability(item);

        return map(saved);
    }

    @Override
    @Transactional
    public InventoryReservationResponseDTO release(UUID reservationId) {

        InventoryReservation reservation = reservationRepository
                .findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory reservation not found"));

        if (reservation.getStatus() == InventoryReservationStatus.ACTIVE) {

            InventoryItem item = itemRepository
                            .findByProductIdForUpdate(reservation.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

            item.setBlockedQuantity(item.getBlockedQuantity() - reservation.getQuantity());

            item.setAvailableQuantity(item.getAvailableQuantity() + reservation.getQuantity());

            reservation.setStatus(InventoryReservationStatus.RELEASED);

            itemRepository.save(item);
            reservationRepository.save(reservation);

            publishAvailability(item);
        }

        return map(reservation);
    }

    @Override
    @Transactional
    public InventoryReservationResponseDTO commit(UUID reservationId) {

        InventoryReservation reservation = reservationRepository
                .findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory reservation not found"));

        if (reservation.getStatus() == InventoryReservationStatus.COMMITTED) return map(reservation);

        if (reservation.getStatus() == InventoryReservationStatus.ACTIVE &&
                !reservation.getExpiresAt().isAfter(LocalDateTime.now())) {

            InventoryItem item = itemRepository
                            .findByProductIdForUpdate(reservation.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

            item.setBlockedQuantity(item.getBlockedQuantity() - reservation.getQuantity());

            item.setAvailableQuantity(item.getAvailableQuantity() + reservation.getQuantity());

            reservation.setStatus(InventoryReservationStatus.EXPIRED);

            itemRepository.save(item);
            reservationRepository.save(reservation);

            publishAvailability(item);

            throw new BusinessException("Inventory reservation has expired");
        }

        if (reservation.getStatus() != InventoryReservationStatus.ACTIVE) throw new BusinessException("Inventory reservation is no longer active");

        InventoryItem item = itemRepository
                .findByProductIdForUpdate(reservation.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (reservation.getQuantity() > item.getBlockedQuantity()) {
            throw new BusinessException("Blocked inventory is insufficient");
        }

        int previousTotal = item.getAvailableQuantity() + item.getBlockedQuantity();

        item.setBlockedQuantity(item.getBlockedQuantity() - reservation.getQuantity());

        reservation.setStatus(InventoryReservationStatus.COMMITTED);

        itemRepository.save(item);
        reservationRepository.save(reservation);

        int newTotal = item.getAvailableQuantity() + item.getBlockedQuantity();

        inventorySystemService.recordMovement(
                item,
                previousTotal,
                newTotal,
                InventoryAdjustmentType.ORDER,
                "Order inventory committed: " + reservation.getOrderId()
        );

        publishAvailability(item);

        return map(reservation);
    }


    @Override
    @Transactional
    public void commitOrder(UUID orderId) {

        List<InventoryReservation> reservations = reservationRepository.findByOrderIdOrderByProductIdAsc(orderId);

        if (reservations.isEmpty()) {return;}

        for (InventoryReservation reservation : reservations) {
            if (reservation.getStatus()
                    == InventoryReservationStatus.COMMITTED) {
                continue;
            }
            commit(reservation.getId());
        }
    }

    @Override
    @Transactional
    public void commitProducts(UUID orderId, List<UUID> productIds) {

        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        for (UUID productId : productIds) {
            InventoryReservation reservation = reservationRepository
                    .findByOrderIdAndProductId(orderId, productId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory reservation not found for order " + orderId + " and product " + productId
                    ));

            if (reservation.getStatus() == InventoryReservationStatus.COMMITTED) {
                continue;
            }

            commit(reservation.getId());
        }
    }

    @Override
    @Transactional
    public void releaseOrder(UUID orderId) {

        List<InventoryReservation> reservations =
                reservationRepository.findByOrderIdAndStatusOrderByProductIdAsc(orderId, InventoryReservationStatus.ACTIVE);

        for (InventoryReservation reservation : reservations) {

            InventoryItem item = itemRepository
                            .findByProductIdForUpdate(reservation.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

            item.setBlockedQuantity(item.getBlockedQuantity() - reservation.getQuantity());

            item.setAvailableQuantity(item.getAvailableQuantity() + reservation.getQuantity());

            reservation.setStatus(InventoryReservationStatus.RELEASED);

            itemRepository.save(item);
            reservationRepository.save(reservation);

            publishAvailability(item);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryAvailabilityResponseDTO getAvailability(UUID productId) {

        InventoryItem item = itemRepository
                .findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));

        return new InventoryAvailabilityResponseDTO(
                productId,
                item.getMarketplaceProductId(),
                item.getAvailableQuantity(),
                item.getBlockedQuantity()
        );
    }

    @Override
    @Transactional
    public void expireReservations() {

        List<InventoryReservation> reservations =
                reservationRepository
                        .findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
                                InventoryReservationStatus.ACTIVE,
                                LocalDateTime.now()
                        );

        for (InventoryReservation reservation : reservations) {

            InventoryItem item =
                    itemRepository
                            .findByProductIdForUpdate(reservation.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

            item.setBlockedQuantity(item.getBlockedQuantity() - reservation.getQuantity());

            item.setAvailableQuantity(item.getAvailableQuantity() + reservation.getQuantity());

            reservation.setStatus(InventoryReservationStatus.EXPIRED);

            itemRepository.save(item);
            reservationRepository.save(reservation);

            publishAvailability(item);
        }
    }


    private InventoryItem createInventoryFromCatalog(CatalogProductResponse p) {
        InventoryItem item = new InventoryItem();
        item.setProductId(p.id());
        item.setSellerId(p.sellerId());
        item.setMarketplaceProductId(p.marketplaceProductId());
        item.setSku(p.sku());
        item.setTitle(p.title());
        item.setBrand(p.brand());
        item.setAvailableQuantity(p.stockQuantity() == null ? 0 : p.stockQuantity());
        item.setBlockedQuantity(0);
        item.setLowStockThreshold(5);
        item.setStatus(p.status() == null ? "ACTIVE" : p.status());
        return itemRepository.save(item);
    }

    private void publishAvailability(InventoryItem item) {

        inventoryStockEventProducer.publish(
                new InventoryStockChangedEvent(
                        UUID.randomUUID(),
                        item.getProductId(),
                        item.getMarketplaceProductId(),
                        item.getAvailableQuantity()
                )
        );
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
