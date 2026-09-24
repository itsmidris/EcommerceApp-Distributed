package in.sellzon.identity.user.dto.response;

import in.sellzon.identity.common.enums.Status;
import in.sellzon.identity.common.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private UserRole role;
    private Status status;
}
