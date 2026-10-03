package in.sellzon.inventory.catalog.client;

import in.sellzon.inventory.catalog.response.CatalogProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;
import static org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver.principal;

@Component
@RequiredArgsConstructor
public class CatalogClient {

    private static final String INVENTORY_CLIENT = "inventory-service";

    private final RestClient catalogRestClient;

    public List<CatalogProductResponse> getMyProducts(String accessToken) {

        List<CatalogProductResponse> response = catalogRestClient.get()
                .uri("/api/seller/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return response == null ? List.of() : response;
    }

    public void updateStock(String marketplaceProductId, int stockQuantity) {
        catalogRestClient.patch()
                .uri("/api/internal/products/{marketplaceProductId}/stock", marketplaceProductId)
                .attributes(clientRegistrationId(INVENTORY_CLIENT))
                .attributes(principal(INVENTORY_CLIENT))
                .body(new StockUpdateRequest(stockQuantity))
                .retrieve()
                .toBodilessEntity();
    }

    private record StockUpdateRequest(int stockQuantity) {}

    public CatalogProductResponse getProductById(UUID productId) {
        CatalogProductResponse response = catalogRestClient.get()
                .uri("/api/internal/products/by-id/{productId}", productId)
                .attributes(clientRegistrationId(INVENTORY_CLIENT))
                .attributes(principal(INVENTORY_CLIENT))
                .retrieve()
                .body(CatalogProductResponse.class);

        if (response == null) throw new IllegalStateException("Catalog service returned no product");

        return response;
    }

    public CatalogProductResponse getProduct(String marketplaceProductId, String accessToken) {
        CatalogProductResponse response = catalogRestClient.get()
                .uri("/api/internal/products/{marketplaceProductId}", marketplaceProductId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(CatalogProductResponse.class);

        if (response == null) throw new IllegalStateException("Catalog service returned no product");

        return response;
    }

    public List<CatalogProductResponse> getMyProductsBySellerId(UUID sellerId) {

        List<CatalogProductResponse> response = catalogRestClient.get()
                .uri("/api/internal/products/seller/{sellerId}", sellerId)
                .attributes(clientRegistrationId(INVENTORY_CLIENT))
                .attributes(principal(INVENTORY_CLIENT))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return response == null ? List.of() : response;
    }
}
