package in.sellzon.catalog.identity.response;

import java.util.UUID;

public record IdentityUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String role,
        String status
) {
}
