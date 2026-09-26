package in.sellzon.identity.user.dto.response;

import java.util.UUID;

public record PublicUserProfileDTO(
        UUID id,
        String firstName,
        String lastName
) {
}