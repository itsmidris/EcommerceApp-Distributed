package in.sellzon.order.address.client;

import in.sellzon.order.address.response.AddressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IdentityAddressClient {

    private final RestClient identityRestClient;

    public AddressResponse getAddress(UUID addressId, UUID customerId, String accessToken) {

        AddressResponse address = identityRestClient
                .get()
                .uri("/api/addresses/{addressId}", addressId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(AddressResponse.class);

        if (address == null) {
            throw new IllegalStateException("Identity Service returned no address");
        }

        if (!customerId.equals(address.customerId())) {
            throw new IllegalStateException("Shipping address does not belong to current customer");
        }

        if (address.status() != null && !"ACTIVE".equals(address.status())) {
            throw new IllegalStateException("Shipping address is not active");
        }

        return address;
    }
}