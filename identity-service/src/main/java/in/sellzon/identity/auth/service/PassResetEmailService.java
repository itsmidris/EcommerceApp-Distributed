package in.sellzon.identity.auth.service;

public interface PassResetEmailService {

    void sendPasswordResetOtp(String email, String otp);
}
