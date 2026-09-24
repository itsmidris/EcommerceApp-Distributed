package in.sellzon.identity.auth.service.impl;

import in.sellzon.identity.auth.service.PassResetEmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PassResetEmailServiceImpl implements PassResetEmailService {

    private final JavaMailSender mailSender;

    public PassResetEmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetOtp(String email, String otp) {

        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setTo(email);
        mailMessage.setSubject("SellZon Password Reset Otp");
        mailMessage.setText("""
                Hello,

                We received a request to reset your Sellzon password.

                Your OTP is:

                %s

                This OTP will expire in 5 minutes.

                If you did not request a password reset, you can safely ignore this email.

                Regards,
                Sellzon Team
                """.formatted(otp));

        mailSender.send(mailMessage);
    }
}
