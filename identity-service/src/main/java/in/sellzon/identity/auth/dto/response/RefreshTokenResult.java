package in.sellzon.identity.auth.dto.response;

import in.sellzon.identity.auth.entity.RefreshToken;

public record RefreshTokenResult (String rawToken, RefreshToken entity){
}
