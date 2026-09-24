package in.sellzon.identity.security;

import in.sellzon.identity.auth.service.GoogleOAuth2Service;
import in.sellzon.identity.auth.service.OAuthLoginCodeService;
import in.sellzon.identity.user.Entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleOAuth2Service googleOAuth2Service;
    private final OAuthLoginCodeService oauthLoginCodeService;

    public GoogleOAuth2SuccessHandler(
            GoogleOAuth2Service googleOAuth2Service,
            OAuthLoginCodeService oauthLoginCodeService) {
        this.googleOAuth2Service = googleOAuth2Service;
        this.oauthLoginCodeService = oauthLoginCodeService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid oAuth2 Authentication");
            return;
        }

        if (!"google".equalsIgnoreCase(oauthToken.getAuthorizedClientRegistrationId())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unsupported oAuth provider");
            return;
        }

        User user = googleOAuth2Service.processGoogleUser(oauthToken.getPrincipal());

        String code = oauthLoginCodeService.createLoginCode(user);

        String host = request.getServerName().toLowerCase(Locale.ROOT);

        String portal = switch (host) {
            case "admin.localhost" -> "admin";
            case "partner.localhost" -> "partner";
            default -> "customer";
        };

        String scheme = request.getScheme();
        int port = request.getServerPort();

        String baseUrl = scheme + "://" + host;

        if (!(("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443))) {
            baseUrl += ":" + port;
        }

        String redirectUrl = baseUrl + "/" + portal + "/oauth2/callback.html?code=" + code;

        response.sendRedirect(redirectUrl);
    }
}
