package in.sellzon.cart.catalog.client;

import in.sellzon.cart.catalog.response.CatalogProductResponse;
import in.sellzon.cart.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class CatalogClient {

    private final RestClient catalogRestClient;

    public CatalogProductResponse getProduct(String marketplaceProductId, String accessToken) {
        try {
            CatalogProductResponse response = catalogRestClient.get()
                    .uri("/api/internal/products/{marketplaceProductId}", marketplaceProductId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(CatalogProductResponse.class);

            if (response == null) {
                throw new ResourceNotFoundException("Product not found or unavailable");
            }

            return response;

        } catch (RestClientException ex) {
            throw new ResourceNotFoundException("Product not found or unavailable");
        }
    }
}
