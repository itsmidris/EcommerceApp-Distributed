package in.sellzon.identity.auth.controller;

import in.sellzon.identity.auth.dto.request.*;
import in.sellzon.identity.auth.dto.response.LoginResponseDTO;
import in.sellzon.identity.auth.dto.response.VerifyOtpResponseDTO;
import in.sellzon.identity.auth.service.AuthService;
import in.sellzon.identity.auth.service.OAuthLoginCodeService;
import in.sellzon.identity.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final OAuthLoginCodeService oauthLoginCodeService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService, OAuthLoginCodeService oauthLoginCodeService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.oauthLoginCodeService = oauthLoginCodeService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        LoginResponseDTO response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.sendOtp(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponseDTO> verifyOtp(@Valid @RequestBody VerifyOtpRequestDTO request) {
        return ResponseEntity.ok(passwordResetService.verifyOtp(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth2/exchange")
    public ResponseEntity<LoginResponseDTO> exchangeOauthCode(@RequestParam String code) {
        return ResponseEntity.ok(oauthLoginCodeService.exchangeLoginCode(code));
    }
}
