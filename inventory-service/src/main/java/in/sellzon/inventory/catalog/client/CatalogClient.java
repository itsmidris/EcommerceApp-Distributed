package in.sellzon.inventory.catalog.client;

import in.sellzon.inventory.catalog.response.CatalogProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogClient {

    private final RestClient catalogRestClient;

    public List<CatalogProductResponse> getMyProducts(String accessToken) {

        List<CatalogProductResponse> response = catalogRestClient.get()
                .uri("/api/seller/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return response == null ? List.of() : response;
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
}
