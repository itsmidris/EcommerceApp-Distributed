package in.sellzon.order.identity;

import java.util.UUID;

public record IdentityUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String role,
        String status
) {
}
