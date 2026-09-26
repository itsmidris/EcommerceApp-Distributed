package in.sellzon.catalog.identity.response;

import java.util.UUID;

public record PublicUserProfileResponse(
        UUID id,
        String firstName,
        String lastName
) {
}
