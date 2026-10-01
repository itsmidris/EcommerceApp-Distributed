package in.sellzon.cart.cart.service.impl;

import in.sellzon.cart.cart.dto.request.AddCartItemRequestDTO;
import in.sellzon.cart.cart.dto.request.UpdateCartItemRequestDTO;
import in.sellzon.cart.cart.dto.response.CartItemResponseDTO;
import in.sellzon.cart.cart.dto.response.CartResponseDTO;
import in.sellzon.cart.cart.service.CartService;
import in.sellzon.cart.catalog.client.CatalogClient;
import in.sellzon.cart.catalog.response.CatalogProductResponse;
import in.sellzon.cart.common.exception.ResourceNotFoundException;
import in.sellzon.cart.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final String KEY_PREFIX = "sellzon:cart:";
    private static final Duration CART_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;
    private final CatalogClient catalogClient;
    private final CurrentUserService currentUserService;

    @Override
    public CartResponseDTO getCart() {

        String key = cartKey();

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        List<CartItemResponseDTO> items = new ArrayList<>();
        List<String> staleProducts = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String marketplaceProductId = String.valueOf(entry.getKey());
            int quantity = parseQuantity(entry.getValue());
            CatalogProductResponse product;
            try {
                product = getActiveProduct(marketplaceProductId);
            } catch (ResourceNotFoundException | IllegalArgumentException ex) {
                staleProducts.add(marketplaceProductId);
                continue;
            }
            int stock = safeStock(product.stockQuantity());
            if (stock <= 0) {
                staleProducts.add(marketplaceProductId);
                continue;
            }
            int safeQuantity = Math.min(quantity, Math.min(stock, 99));
            if (safeQuantity != quantity) {
                redisTemplate.opsForHash().put(key, marketplaceProductId, String.valueOf(safeQuantity));
            }
            items.add(toResponse(product, safeQuantity));
        }

        if (!staleProducts.isEmpty()) {
            redisTemplate.opsForHash().delete(key, staleProducts.toArray());
        }

        refreshTtl(key);

        return buildCart(items);
    }

    @Override
    public CartResponseDTO addItem(AddCartItemRequestDTO request) {

        CatalogProductResponse product = getActiveProduct(request.marketplaceProductId());

        validateQuantity(product, request.quantity());

        String key = cartKey();

        String marketplaceProductId = product.marketplaceProductId();

        Object existing = redisTemplate.opsForHash().get(key, marketplaceProductId);

        int existingQuantity = existing == null ? 0 : parseQuantity(existing);
        int newQuantity = existingQuantity + request.quantity();
        int stock = safeStock(product.stockQuantity());

        if (newQuantity > stock) {
            throw new IllegalArgumentException("Only " + stock + " units are available");
        }

        if (newQuantity > 99) {
            throw new IllegalArgumentException("Maximum quantity per product is 99");
        }

        redisTemplate.opsForHash().put(key, marketplaceProductId, String.valueOf(newQuantity));

        refreshTtl(key);

        return getCart();
    }

    @Override
    public CartResponseDTO updateItem(String marketplaceProductId, UpdateCartItemRequestDTO request) {

        CatalogProductResponse product = getActiveProduct(marketplaceProductId);
        validateQuantity(product, request.quantity());
        String key = cartKey();

        if (!redisTemplate.opsForHash().hasKey(key, product.marketplaceProductId())) {
            throw new ResourceNotFoundException("Product is not in your cart");
        }

        redisTemplate.opsForHash().put(key, product.marketplaceProductId(), String.valueOf(request.quantity()));
        refreshTtl(key);

        return getCart();
    }

    @Override
    public CartResponseDTO removeItem(String marketplaceProductId) {

        String key = cartKey();
        redisTemplate.opsForHash().delete(key, normalizeId(marketplaceProductId));
        refreshTtl(key);

        return getCart();
    }

    @Override
    public void clearCart() {
        redisTemplate.delete(cartKey());
    }

    private CatalogProductResponse getActiveProduct(String marketplaceProductId) {

        String id = normalizeId(marketplaceProductId);

        CatalogProductResponse product = catalogClient.getProduct(id, currentUserService.getAccessToken());

        if (!"ACTIVE".equalsIgnoreCase(product.status())) {
            throw new ResourceNotFoundException("Product not found or unavailable");
        }

        if (safeStock(product.stockQuantity()) <= 0) {
            throw new IllegalArgumentException("Product is out of stock");
        }

        return product;
    }

    private void validateQuantity(CatalogProductResponse product, int quantity) {

        if (quantity < 1 || quantity > 99) {
            throw new IllegalArgumentException("Quantity must be between 1 and 99");
        }

        int stock = safeStock(product.stockQuantity());

        if (quantity > stock) {
            throw new IllegalArgumentException("Only " + stock + " units are available");
        }
    }

    private CartItemResponseDTO toResponse(CatalogProductResponse product, int quantity) {

        BigDecimal sellingPrice = product.sellingPrice();

        return new CartItemResponseDTO(
                product.marketplaceProductId(),
                product.title(),
                product.brand(),
                product.mrp(),
                sellingPrice,
                quantity,
                safeStock(product.stockQuantity()),
                product.imageUrl(),
                sellingPrice.multiply(BigDecimal.valueOf(quantity))
        );
    }

    private CartResponseDTO buildCart(List<CartItemResponseDTO> items) {

        int totalItems = items.stream().mapToInt(CartItemResponseDTO::quantity).sum();

        BigDecimal subtotal = items.stream()
                .map(CartItemResponseDTO::itemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponseDTO(List.copyOf(items), totalItems, subtotal);
    }

    private String cartKey() {
        return KEY_PREFIX + currentUserService.getCurrentUserId();
    }

    private void refreshTtl(String key) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.expire(key, CART_TTL);
        }
    }

    private int parseQuantity(Object value) {
        try {
            int quantity = Integer.parseInt(String.valueOf(value));
            return Math.max(quantity, 1);
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private int safeStock(Integer stock) {
        return stock == null ? 0 : Math.max(stock, 0);
    }

    private String normalizeId(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Marketplace product ID is required");
        }

        return value.trim();
    }
}
