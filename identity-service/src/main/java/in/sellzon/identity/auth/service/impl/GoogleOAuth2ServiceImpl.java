package in.sellzon.identity.auth.service.impl;

import in.sellzon.identity.auth.service.GoogleOAuth2Service;
import in.sellzon.identity.common.enums.AuthProvider;
import in.sellzon.identity.common.enums.Status;
import in.sellzon.identity.common.enums.UserRole;
import in.sellzon.identity.user.Entity.User;
import in.sellzon.identity.user.repository.UserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class GoogleOAuth2ServiceImpl implements GoogleOAuth2Service {

    private final UserRepository userRepository;

    public GoogleOAuth2ServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User processGoogleUser(OAuth2User oAuth2User) {

        String email = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getAttribute("sub");

        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google account does not provide an email address");
        }

        if (googleId == null || googleId.isBlank()) {
            throw new IllegalArgumentException("Google account identifier is missing");
        }

        email = email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user != null) {
            if (user.getProvider() == AuthProvider.LOCAL) {
//                user.setProvider(AuthProvider.GOOGLE);
//                User should have both option of Google and password
                user.setProviderId(googleId);
            } else if (user.getProvider() == AuthProvider.GOOGLE) {
                if (user.getProviderId() == null) {
                    user.setProviderId(googleId);
                }
            }
            return user;
        }

        User newUser = new User();

        newUser.setFirstName(firstName != null && !firstName.isBlank() ? firstName : "Google");
        newUser.setLastName(lastName != null && !lastName.isBlank() ? lastName : "Customer");
        newUser.setEmail(email);
        newUser.setPassword(null);
        newUser.setPhoneNumber(null);
        newUser.setRole(UserRole.CUSTOMER);
        newUser.setStatus(Status.ACTIVE);
        newUser.setProvider(AuthProvider.GOOGLE);
        newUser.setProviderId(googleId);

        return userRepository.save(newUser);
    }
}
