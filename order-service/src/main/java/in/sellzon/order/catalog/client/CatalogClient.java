package in.sellzon.order.catalog.client;

import in.sellzon.order.catalog.response.CatalogProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CatalogClient {

    private final RestClient catalogRestClient;

    public CatalogProductResponse getProduct(String marketplaceProductId, String accessToken) {

        CatalogProductResponse product = catalogRestClient
                .get()
                .uri("/api/internal/products/{marketplaceProductId}", marketplaceProductId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(CatalogProductResponse.class);

        if (product == null) {
            throw new IllegalStateException("Catalog Service returned no product");
        }

        return product;
    }
}
