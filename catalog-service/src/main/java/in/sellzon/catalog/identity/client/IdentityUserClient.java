package in.sellzon.catalog.identity.client;

import in.sellzon.catalog.identity.response.IdentityUserResponse;
import in.sellzon.catalog.identity.response.PublicUserProfileResponse;
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

        return identityRestClient
                .get()
                .uri("/api/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(IdentityUserResponse.class);
    }

    public PublicUserProfileResponse getPublicUserProfile(UUID userId) {

        return identityRestClient
                .get()
                .uri("/api/users/public-profile/{id}", userId)
                .retrieve()
                .body(PublicUserProfileResponse.class);
    }
}
