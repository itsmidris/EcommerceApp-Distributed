package in.sellzon.catalog.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    public UUID getCurrentUserId() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new IllegalStateException("Authenticated user is not using JWT authentication");
        }

        String subject = jwtAuthenticationToken.getToken().getSubject();

        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("User ID is missing from JWT");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid user ID in JWT", ex);
        }
    }

    public String getCurrentAccessToken() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new IllegalStateException("Authenticated user is not using JWT authentication");
        }

        return jwtAuthenticationToken.getToken().getTokenValue();
    }
}