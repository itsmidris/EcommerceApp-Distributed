package in.sellzon.identity.auth.service;

import in.sellzon.identity.user.Entity.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface GoogleOAuth2Service {

    User processGoogleUser(OAuth2User oAuth2User);
}
