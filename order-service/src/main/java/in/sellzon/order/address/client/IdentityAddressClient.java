package in.sellzon.order.address.client;

import in.sellzon.order.address.response.AddressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IdentityAddressClient {

    private final RestClient identityRestClient;

    public AddressResponse getAddress(UUID addressId, UUID customerId, String accessToken) {

        AddressResponse address = identityRestClient
                .get()
                .uri("/api/customer/addresses/{addressId}", addressId)
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

    public List<AddressResponse> getMyAddresses(String accessToken) {

        List<AddressResponse> addresses =
                identityRestClient
                        .get()
                        .uri("/api/customer/addresses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(new org.springframework.core.ParameterizedTypeReference<List<AddressResponse>>() {});

        return addresses == null ? List.of() : addresses;
    }
}