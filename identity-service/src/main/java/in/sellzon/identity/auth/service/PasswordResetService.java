package in.sellzon.identity.auth.service;

import in.sellzon.identity.auth.dto.request.ForgotPasswordRequestDTO;
import in.sellzon.identity.auth.dto.request.ResetPasswordRequestDTO;
import in.sellzon.identity.auth.dto.request.VerifyOtpRequestDTO;
import in.sellzon.identity.auth.dto.response.VerifyOtpResponseDTO;

public interface PasswordResetService {

    void sendOtp(ForgotPasswordRequestDTO request);
    VerifyOtpResponseDTO verifyOtp(VerifyOtpRequestDTO request);
    void resetPassword(ResetPasswordRequestDTO request);
}
