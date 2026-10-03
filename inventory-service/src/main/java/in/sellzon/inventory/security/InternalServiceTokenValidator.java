package in.sellzon.inventory.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InternalServiceTokenValidator {

    private final String expectedToken;

    public InternalServiceTokenValidator(
            @Value("${sellzon.internal.service-token}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    public void validate(String token) {
        if (token == null || !expectedToken.equals(token)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid internal service token");
        }
    }
}
