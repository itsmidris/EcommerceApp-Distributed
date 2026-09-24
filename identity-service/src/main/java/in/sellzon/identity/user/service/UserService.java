package in.sellzon.identity.user.service;


import in.sellzon.identity.user.dto.request.CreateUserRequestDTO;
import in.sellzon.identity.user.dto.response.UserResponseDTO;

import java.util.UUID;

public interface UserService {

    UserResponseDTO createUser(CreateUserRequestDTO request);
    UserResponseDTO getUserById(UUID id);
    UserResponseDTO getUserByPhoneNumber(String phoneNumber);
    UserResponseDTO getUserByEmail(String email);
    void disableUser(UUID id);
}
