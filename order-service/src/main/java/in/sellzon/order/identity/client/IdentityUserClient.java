package in.sellzon.order.identity.client;

import in.sellzon.order.identity.IdentityUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IdentityUserClient {

    private final RestClient identityRestClient;

    public IdentityUserResponse getUserById(UUID userId, String accessToken) {

        IdentityUserResponse user = identityRestClient
                .get()
                .uri("/api/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(IdentityUserResponse.class);

        if (user == null) {
            throw new IllegalStateException("Identity Service returned no user");
        }

        return user;
    }
}
