package in.sellzon.identity.auth.service;

import in.sellzon.identity.auth.dto.response.LoginResponseDTO;
import in.sellzon.identity.user.Entity.User;

public interface OAuthLoginCodeService {

    String createLoginCode(User user);
    LoginResponseDTO exchangeLoginCode(String rawCode);
}
