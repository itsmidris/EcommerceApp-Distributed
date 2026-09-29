package in.sellzon.cart.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    public UUID getCurrentUserId() {

        JwtAuthenticationToken jwt = getJwtAuthentication();

        try {
            return UUID.fromString(jwt.getToken().getSubject());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid user ID in JWT subject", ex);
        }
    }

    public String getAccessToken() {
        return getJwtAuthentication().getToken().getTokenValue();
    }

    private JwtAuthenticationToken getJwtAuthentication() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new IllegalStateException("Authenticated JWT is required");
        }

        return jwt;
    }
}
