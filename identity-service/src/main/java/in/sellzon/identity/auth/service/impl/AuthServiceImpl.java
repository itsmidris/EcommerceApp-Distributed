package in.sellzon.identity.auth.service.impl;

import in.sellzon.identity.auth.dto.request.LoginRequestDTO;
import in.sellzon.identity.auth.dto.request.RefreshTokenRequestDTO;
import in.sellzon.identity.auth.dto.response.LoginResponseDTO;
import in.sellzon.identity.auth.dto.response.RefreshTokenResult;
import in.sellzon.identity.auth.entity.RefreshToken;
import in.sellzon.identity.auth.service.AuthService;
import in.sellzon.identity.auth.service.RefreshTokenService;
import in.sellzon.identity.security.JwtService;
import in.sellzon.identity.user.Entity.User;
import in.sellzon.identity.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        User user = getUser(email);
        String accessToken = jwtService.generateAccessToken(user);
        RefreshTokenResult refreshTokenResult = refreshTokenService.createRefreshToken(user);
        String refreshToken = refreshTokenResult.rawToken();

        return buildLoginResponse(user, accessToken, refreshToken);
    }

    @Override
    public LoginResponseDTO refreshAccessToken(RefreshTokenRequestDTO request) {

        RefreshToken oldRefreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());

        User user = oldRefreshToken.getUser();

        refreshTokenService.revokeToken(oldRefreshToken);

        RefreshTokenResult newRefreshToken = refreshTokenService.createRefreshToken(user);

        oldRefreshToken.setReplacedByTokenId(newRefreshToken.entity().getId());

        String newAccessToken = jwtService.generateAccessToken(user);

        return buildLoginResponse(user, newAccessToken, newRefreshToken.rawToken());
    }

    @Override
    public void logout(String refreshToken) {
        RefreshToken storedToken = refreshTokenService.validateRefreshToken(refreshToken);
        refreshTokenService.revokeToken(storedToken);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(()-> new IllegalStateException(
                        "Authenticated user could not be found"
                ));
    }

    private LoginResponseDTO buildLoginResponse(User user, String  accessToken, String refreshToken) {
        return new LoginResponseDTO(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }
}
