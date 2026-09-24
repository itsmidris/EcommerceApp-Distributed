package in.sellzon.identity.auth.service;

import in.sellzon.identity.auth.dto.response.RefreshTokenResult;
import in.sellzon.identity.auth.entity.RefreshToken;
import in.sellzon.identity.user.Entity.User;

public interface RefreshTokenService {

    RefreshTokenResult createRefreshToken(User user);
    RefreshToken validateRefreshToken(String rawToken);
    void revokeToken(RefreshToken refreshToken);
    void revokeAllUserTokens(User user);
}
