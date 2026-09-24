package in.sellzon.identity.auth.service.impl;

import in.sellzon.identity.auth.dto.request.ForgotPasswordRequestDTO;
import in.sellzon.identity.auth.dto.request.ResetPasswordRequestDTO;
import in.sellzon.identity.auth.dto.request.VerifyOtpRequestDTO;
import in.sellzon.identity.auth.dto.response.VerifyOtpResponseDTO;
import in.sellzon.identity.auth.entity.PasswordResetOtp;
import in.sellzon.identity.auth.repository.PasswordResetOtpRepository;
import in.sellzon.identity.auth.service.PassResetEmailService;
import in.sellzon.identity.auth.service.PasswordResetService;
import in.sellzon.identity.auth.service.RefreshTokenService;
import in.sellzon.identity.user.Entity.User;
import in.sellzon.identity.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;

@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int OTP_EXPIRATION_MINUTES = 5;
    private static final int RESET_TOKEN_EXPIRATION_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final PassResetEmailService emailService;
    private final RefreshTokenService refreshTokenService;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetServiceImpl(UserRepository userRepository, PasswordResetOtpRepository otpRepository, PasswordEncoder passwordEncoder, PassResetEmailService emailService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
    }


    @Override
    public void sendOtp(ForgotPasswordRequestDTO request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new IllegalArgumentException(
                        "No account found with this email"
                ));

        otpRepository.findTopByUserOrderByCreatedAtDesc(user)
                .ifPresent(previousOtp -> {
                    previousOtp.setUsed(true);
                });

        String otp = generateOtp();

        PasswordResetOtp resetOtp = new PasswordResetOtp();

        resetOtp.setUser(user);
        resetOtp.setOtpHash(passwordEncoder.encode(otp));
        resetOtp.setExpiresAt(Instant.now().plus(OTP_EXPIRATION_MINUTES, ChronoUnit.MINUTES));

        otpRepository.save(resetOtp);

        emailService.sendPasswordResetOtp(email, otp);
    }

    @Override
    public VerifyOtpResponseDTO verifyOtp(VerifyOtpRequestDTO request) {
        String email = request.email()
                .trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new IllegalArgumentException("Invalid OTP"));

        PasswordResetOtp resetOtp = otpRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(()-> new IllegalArgumentException("Invalid OTP"));

        validateOtpState(resetOtp);

        if (!passwordEncoder.matches(request.otp(), resetOtp.getOtpHash())) {

            resetOtp.incrementAttemptCount();

            if (resetOtp.getAttemptCount() >= MAX_ATTEMPTS) {
                resetOtp.setUsed(true);
            }

            throw new IllegalArgumentException("Invalid OTP");
        }

        resetOtp.setVerifiedAt(Instant.now());

        String rawResetToken = generateSecureToken();

        resetOtp.setResetTokenHash(hashToken(rawResetToken));

        resetOtp.setResetTokenExpiresAt(Instant.now().plus(RESET_TOKEN_EXPIRATION_MINUTES, ChronoUnit.MINUTES));

        return new VerifyOtpResponseDTO("OTP verified successfully", rawResetToken);
    }

    @Override
    public void resetPassword(ResetPasswordRequestDTO request) {
        String resetTokenHash = hashToken(request.resetToken());

        PasswordResetOtp resetOtp = otpRepository.findByResetTokenHash(resetTokenHash)
                .orElseThrow(()-> new IllegalArgumentException("Invalid reset token"));

        if (resetOtp.isUsed()) {
            throw new IllegalArgumentException("Reset token has already been used");
        }

        if (resetOtp.getVerifiedAt() == null) {
            throw new IllegalArgumentException("OTP verification required");
        }

        if (resetOtp.getResetTokenExpiresAt() == null || resetOtp.getResetTokenExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Reset token has expired");
        }

        User user = resetOtp.getUser();

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        resetOtp.setUsed(true);

        refreshTokenService.revokeAllUserTokens(user);
    }

    public void validateOtpState(PasswordResetOtp otp) {
        if (otp.isUsed()) {
            throw new IllegalArgumentException("OTP is no longer valid");
        }

        if (otp.getExpiresAt().isBefore(Instant.now())) {
            otp.setUsed(true);
            throw new IllegalArgumentException("OTP has expired");
        }

        if (otp.getAttemptCount() >= MAX_ATTEMPTS) {
            otp.setUsed(true);
            throw new IllegalArgumentException("Maximum OTP attempts exceeded");
        }
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
