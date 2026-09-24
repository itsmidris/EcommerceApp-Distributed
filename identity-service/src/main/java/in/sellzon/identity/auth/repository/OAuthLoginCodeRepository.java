package in.sellzon.identity.auth.repository;

import in.sellzon.identity.auth.entity.OAuthLoginCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthLoginCodeRepository extends JpaRepository<OAuthLoginCode, UUID> {

    Optional<OAuthLoginCode> findByCodeHash(String codeHash);
}
