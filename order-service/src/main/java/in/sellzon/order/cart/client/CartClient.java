package in.sellzon.order.cart.client;

import in.sellzon.order.cart.response.CartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CartClient {

    private final RestClient cartRestClient;

    public CartResponse getCart(String accessToken) {
        CartResponse cart = cartRestClient
                .get()
                .uri("/api/customer/cart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(CartResponse.class);

        if (cart == null) {
            throw new IllegalStateException("Cart Service returned no cart");
        }

        return cart;
    }

    public void clearCart(String accessToken) {
        cartRestClient
                .method(HttpMethod.DELETE)
                .uri("/api/customer/cart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity();
    }
}
