package in.sellzon.identity.auth.service.impl;

import in.sellzon.identity.auth.dto.response.LoginResponseDTO;
import in.sellzon.identity.auth.entity.OAuthLoginCode;
import in.sellzon.identity.auth.repository.OAuthLoginCodeRepository;
import in.sellzon.identity.auth.service.OAuthLoginCodeService;
import in.sellzon.identity.auth.service.RefreshTokenService;
import in.sellzon.identity.security.JwtService;
import in.sellzon.identity.user.Entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional
public class OAuthLoginCodeServiceImpl implements OAuthLoginCodeService {

    private static final long CODE_EXPIRATION_SECONDS = 60;

    private final OAuthLoginCodeRepository repository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthLoginCodeServiceImpl(OAuthLoginCodeRepository repository, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public String createLoginCode(User user) {

        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(randomBytes);

        String rawCode = Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        OAuthLoginCode loginCode = new OAuthLoginCode();

        loginCode.setUser(user);
        loginCode.setCodeHash(hash(rawCode));
        loginCode.setExpiresAt(Instant.now().plus(CODE_EXPIRATION_SECONDS, ChronoUnit.SECONDS));

        repository.save(loginCode);

        return rawCode;
    }

    @Override
    public LoginResponseDTO exchangeLoginCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("OAuth login code is required");
        }

        OAuthLoginCode loginCode = repository.findByCodeHash(hash(rawCode))
                .orElseThrow(()-> new IllegalArgumentException(
                        "Invalid OAuth login code"
                ));

        if (loginCode.isUsed()) {
            throw new IllegalArgumentException("OAuth login code has already been used");
        }

        if (loginCode.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("OAuth login code has expired");
        }

        User user = loginCode.getUser();

        loginCode.setUsed(true);

        String accessToken = jwtService.generateAccessToken(user);

        String refreshToken = refreshTokenService.createRefreshToken(user).rawToken();

        return new LoginResponseDTO(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                user.getId(),
                user.getEmail(),
                user.getRole());
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw  new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
