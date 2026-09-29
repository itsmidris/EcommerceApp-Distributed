package in.sellzon.cart.cart.service;

import in.sellzon.cart.cart.dto.request.AddCartItemRequestDTO;
import in.sellzon.cart.cart.dto.request.UpdateCartItemRequestDTO;
import in.sellzon.cart.cart.dto.response.CartResponseDTO;

public interface CartService {
    CartResponseDTO getCart();
    CartResponseDTO addItem(AddCartItemRequestDTO request);
    CartResponseDTO updateItem(String marketplaceProductId, UpdateCartItemRequestDTO request);
    CartResponseDTO removeItem(String marketplaceProductId);
    void clearCart();
}
