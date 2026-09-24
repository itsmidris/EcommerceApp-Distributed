package in.sellzon.identity.security;

import in.sellzon.identity.common.enums.UserRole;
import in.sellzon.identity.common.exception.ResourceNotFoundException;
import in.sellzon.identity.user.Entity.User;
import in.sellzon.identity.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        String userId = authentication.getName();

        UUID id;

        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid authenticated user ID");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getCurrentSeller() {
        User user = getCurrentUser();
        if (user.getRole() != UserRole.SELLER) {
            throw new IllegalStateException("Authenticated user is not a seller");
        }
        return user;
    }

    public User getCurrentCustomer() {
        User user = getCurrentUser();
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new IllegalStateException("Authenticated user is not a customer");
        }
        return user;
    }

    public User getCurrentAdmin() {
        User user = getCurrentUser();
        if (user.getRole() != UserRole.ADMIN) {
            throw new IllegalStateException("Authenticated user is not an admin");
        }
        return user;
    }
}
