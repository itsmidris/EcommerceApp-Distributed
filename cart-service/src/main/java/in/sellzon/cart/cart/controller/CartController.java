package in.sellzon.cart.cart.controller;

import in.sellzon.cart.cart.dto.request.AddCartItemRequestDTO;
import in.sellzon.cart.cart.dto.request.UpdateCartItemRequestDTO;
import in.sellzon.cart.cart.dto.response.CartResponseDTO;
import in.sellzon.cart.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponseDTO> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDTO> addItem(@Valid @RequestBody AddCartItemRequestDTO request) {
        return ResponseEntity.ok(cartService.addItem(request));
    }

    @PutMapping("/items/{marketplaceProductId}")
    public ResponseEntity<CartResponseDTO> updateItem(
            @PathVariable String marketplaceProductId,
            @Valid @RequestBody UpdateCartItemRequestDTO request) {
        return ResponseEntity.ok(cartService.updateItem(marketplaceProductId, request));
    }

    @DeleteMapping("/items/{marketplaceProductId}")
    public ResponseEntity<CartResponseDTO> removeItem(@PathVariable String marketplaceProductId) {
        return ResponseEntity.ok(cartService.removeItem(marketplaceProductId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }
}
