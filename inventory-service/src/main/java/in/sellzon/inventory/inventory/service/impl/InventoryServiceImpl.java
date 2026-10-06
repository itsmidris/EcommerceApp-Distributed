package in.sellzon.inventory.inventory.service.impl;

import in.sellzon.inventory.catalog.client.CatalogClient;
import in.sellzon.inventory.catalog.response.CatalogProductResponse;
import in.sellzon.inventory.common.exception.BusinessException;
import in.sellzon.inventory.common.exception.ResourceNotFoundException;
import in.sellzon.inventory.inventory.dto.request.InventoryAdjustmentRequestDTO;
import in.sellzon.inventory.inventory.dto.request.InventoryThresholdRequestDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryAdjustmentResponseDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryProductResponseDTO;
import in.sellzon.inventory.inventory.dto.response.InventoryThresholdResponseDTO;
import in.sellzon.inventory.inventory.entity.InventoryAdjustment;
import in.sellzon.inventory.inventory.entity.InventoryItem;
import in.sellzon.inventory.inventory.entity.InventorySettings;
import in.sellzon.inventory.inventory.enums.InventoryAdjustmentType;
import in.sellzon.inventory.inventory.enums.InventoryReservationStatus;
import in.sellzon.inventory.inventory.repository.InventoryAdjustmentRepository;
import in.sellzon.inventory.inventory.repository.InventoryItemRepository;
import in.sellzon.inventory.inventory.repository.InventoryReservationRepository;
import in.sellzon.inventory.inventory.repository.InventorySettingsRepository;
import in.sellzon.inventory.inventory.service.InventoryService;
import in.sellzon.inventory.kafka.event.InventoryStockChangedEvent;
import in.sellzon.inventory.kafka.producer.InventoryStockEventProducer;
import in.sellzon.inventory.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final InventoryItemRepository itemRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final InventorySettingsRepository settingsRepository;
    private final CurrentUserService currentUserService;
    private final CatalogClient catalogClient;
    private final InventoryStockEventProducer inventoryStockEventProducer;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryProductResponseDTO> getMyInventory() {

        UUID sellerId = currentUserService.getCurrentUserId();

        return itemRepository
                .findBySellerIdOrderByUpdatedAtDesc(sellerId)
                .stream()
                .map(this::mapItem)
                .toList();
    }

    @Override
    @Transactional
    public InventoryProductResponseDTO adjustStock(String marketplaceProductId, InventoryAdjustmentRequestDTO request) {

        UUID sellerId = currentUserService.getCurrentUserId();

        String id = normalizeId(marketplaceProductId);

        CatalogProductResponse catalog = catalogClient.getProduct(id, currentUserService.getAccessToken());

        if (!sellerId.equals(catalog.sellerId())) throw new BusinessException("You do not own this product");

        InventoryItem item = itemRepository.findByMarketplaceProductIdForUpdate(id).orElseGet(() -> createFromCatalog(catalog));

        if (!sellerId.equals(item.getSellerId())) throw new BusinessException("You do not own this inventory");

        if (request.type() == InventoryAdjustmentType.ORDER || request.type() == InventoryAdjustmentType.CANCELLATION) {
            throw new BusinessException("ORDER and CANCELLATION adjustments are system controlled");
        }

        int previousAvailable = item.getAvailableQuantity();

        int nextAvailable =
                calculateNextStock(previousAvailable, request.operation(), request.quantity());

        int change = nextAvailable - previousAvailable;

        if (change == 0) return mapItem(item);

        validateAdjustmentType(request.type(), change);

        item.setAvailableQuantity(nextAvailable);

        itemRepository.save(item);

        inventoryStockEventProducer.publish(
                new InventoryStockChangedEvent(
                        UUID.randomUUID(),
                        item.getProductId(),
                        item.getMarketplaceProductId(),
                        item.getAvailableQuantity()
                )
        );

        saveAdjustment(item, change, previousAvailable, nextAvailable, request.type(), normalizeReason(request.reason()));

        return mapItem(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAdjustmentResponseDTO> getHistory(String marketplaceProductId) {

        UUID sellerId = currentUserService.getCurrentUserId();

        InventoryItem item = itemRepository
                .findByMarketplaceProductId(normalizeId(marketplaceProductId))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (!sellerId.equals(item.getSellerId())) throw new BusinessException("You do not own this inventory");

        return adjustmentRepository
                .findTop50ByProductIdOrderByCreatedAtDesc(item.getProductId())
                .stream()
                .map(a -> new InventoryAdjustmentResponseDTO(a.getId(), a.getMarketplaceProductId(), a.getSku(),
                        a.getType(), a.getQuantityChange(), a.getPreviousStock(), a.getNewStock(), a.getReason(), a.getCreatedAt())).toList();
    }

    @Override
    @Transactional
    public InventoryThresholdResponseDTO updateThreshold(String marketplaceProductId, InventoryThresholdRequestDTO request) {

        UUID sellerId = currentUserService.getCurrentUserId();

        CatalogProductResponse catalog = catalogClient.getProduct(normalizeId(marketplaceProductId), currentUserService.getAccessToken());

        if (!sellerId.equals(catalog.sellerId())) throw new BusinessException("You do not own this product");

        InventoryItem item = itemRepository.findByProductId(catalog.id()).orElseGet(() -> createFromCatalog(catalog));

        item.setLowStockThreshold(request.lowStockThreshold());

        itemRepository.save(item);

        InventorySettings settings = settingsRepository.findByProductId(item.getProductId()).orElseGet(() -> {
            InventorySettings s = new InventorySettings(); s.setProductId(item.getProductId()); return s;
        });

        settings.setLowStockThreshold(request.lowStockThreshold());

        settingsRepository.save(settings);

        return new InventoryThresholdResponseDTO(request.lowStockThreshold());
    }

    public InventoryItem createFromCatalog(CatalogProductResponse p) {

        InventoryItem item = new InventoryItem();

        item.setProductId(p.id());
        item.setSellerId(p.sellerId());
        item.setMarketplaceProductId(p.marketplaceProductId());
        item.setSku(p.sku());
        item.setTitle(p.title());
        item.setBrand(p.brand());
        item.setAvailableQuantity(p.stockQuantity() == null ? 0 : p.stockQuantity());
        item.setBlockedQuantity(0);
        item.setStatus(p.status() == null ? "ACTIVE" : p.status());
        item.setLowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD);

        return itemRepository.save(item);
    }

    private void refreshCatalogSnapshot(InventoryItem item, CatalogProductResponse p) {

        item.setSellerId(p.sellerId());
        item.setMarketplaceProductId(p.marketplaceProductId());
        item.setSku(p.sku());
        item.setTitle(p.title());
        item.setBrand(p.brand());
        item.setStatus(p.status() == null ? "ACTIVE" : p.status());

        itemRepository.save(item);
    }

    private int calculateNextStock(int current, InventoryAdjustmentRequestDTO.Operation operation, int quantity) {

        if (operation == null) throw new BusinessException("Operation is required");

        return switch (operation) {
            case ADD -> {
                if (quantity <= 0) throw new BusinessException("Add quantity must be greater than 0");
                if (quantity > Integer.MAX_VALUE - current) throw new BusinessException("Stock quantity is too large");
                yield current + quantity;
            }
            case REMOVE -> {
                if (quantity <= 0) throw new BusinessException("Remove quantity must be greater than 0");
                if (quantity > current) throw new BusinessException("Cannot remove more stock than currently available");
                yield current - quantity;
            }
            case SET -> quantity;
        };
    }

    private void validateAdjustmentType(InventoryAdjustmentType type, int change) {
        if (change > 0 && type == InventoryAdjustmentType.DAMAGED) throw new BusinessException("Damaged stock cannot increase inventory");
        if (change < 0 && (type == InventoryAdjustmentType.RESTOCK || type == InventoryAdjustmentType.RETURN)) throw new BusinessException(type + " cannot decrease inventory");
    }

    private void saveAdjustment(InventoryItem item, int change, int previous, int next, InventoryAdjustmentType type, String reason) {

        InventoryAdjustment a = new InventoryAdjustment();

        a.setProductId(item.getProductId());
        a.setSellerId(item.getSellerId());
        a.setMarketplaceProductId(item.getMarketplaceProductId());
        a.setSku(item.getSku());
        a.setType(type);
        a.setQuantityChange(change);
        a.setPreviousStock(previous);
        a.setNewStock(next);
        a.setReason(reason);

        adjustmentRepository.save(a);
    }

    private InventoryProductResponseDTO mapItem(InventoryItem item) {

        int threshold = settingsRepository
                .findByProductId(item.getProductId())
                .map(InventorySettings::getLowStockThreshold)
                .orElse(item.getLowStockThreshold());

        int available = item.getAvailableQuantity();

        int blocked = item.getBlockedQuantity();

        return new InventoryProductResponseDTO(
                item.getProductId(),
                item.getMarketplaceProductId(),
                item.getSku(),
                item.getTitle(),
                item.getBrand(),
                available,
                blocked,
                threshold,
                available > 0 && available <= threshold,
                available == 0,
                item.getStatus(),
                item.getUpdatedAt()
        );
    }

    private String normalizeId(String id) {
        if (id == null || id.isBlank()) throw new BusinessException("Marketplace product ID is required");
        return id.trim();
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim().replaceAll("\\s+", " ");
    }
}
