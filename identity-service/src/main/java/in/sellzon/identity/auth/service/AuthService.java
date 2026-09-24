package in.sellzon.identity.auth.service;

import in.sellzon.identity.auth.dto.request.LoginRequestDTO;
import in.sellzon.identity.auth.dto.request.RefreshTokenRequestDTO;
import in.sellzon.identity.auth.dto.response.LoginResponseDTO;

public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO request);
    LoginResponseDTO refreshAccessToken(RefreshTokenRequestDTO request);
    void logout(String refreshToken);
}
