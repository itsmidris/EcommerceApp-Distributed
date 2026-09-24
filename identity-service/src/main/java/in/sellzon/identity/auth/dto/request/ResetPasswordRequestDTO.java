package in.sellzon.identity.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO (
        @NotBlank String resetToken,
        @NotBlank @Size(min = 8, message = "Password must contain at least 8 characters") String newPassword){
}
