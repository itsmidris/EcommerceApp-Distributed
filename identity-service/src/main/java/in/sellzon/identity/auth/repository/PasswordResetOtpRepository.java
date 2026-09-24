package in.sellzon.identity.auth.repository;

import in.sellzon.identity.auth.entity.PasswordResetOtp;
import in.sellzon.identity.user.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, UUID> {

    Optional<PasswordResetOtp> findTopByUserOrderByCreatedAtDesc(User user);

    Optional<PasswordResetOtp> findByResetTokenHash(String resetTokenHash);
}
