package in.sellzon.identity.user.service.impl;

import in.sellzon.identity.common.enums.AuthProvider;
import in.sellzon.identity.common.enums.Status;
import in.sellzon.identity.common.enums.UserRole;
import in.sellzon.identity.common.exception.DuplicateResourceException;
import in.sellzon.identity.common.exception.ResourceNotFoundException;
import in.sellzon.identity.user.Entity.User;
import in.sellzon.identity.user.dto.request.CreateUserRequestDTO;
import in.sellzon.identity.user.dto.response.UserResponseDTO;
import in.sellzon.identity.user.repository.UserRepository;
import in.sellzon.identity.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponseDTO createUser(CreateUserRequestDTO request) {

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String phoneNumber = request.getPhoneNumber().trim();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }

        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicateResourceException("Phone number already exists");
        }

        User user = new User();

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(phoneNumber);
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(Status.ACTIVE);

        user.setProvider(AuthProvider.LOCAL);
        user.setProviderId(null);

        User savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id
                ));

        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {

        String email2 = email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email2)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email2
                ));

        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByPhoneNumber(String phoneNumber) {

        String normalizedPhoneNumber = phoneNumber.trim();

        User user = userRepository.findByPhoneNumber(normalizedPhoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with phone number: " + normalizedPhoneNumber
                ));

        return mapToResponse(user);
    }

    @Override
    public void disableUser(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id
                ));

        user.setStatus(Status.INACTIVE);
    }

    private UserResponseDTO mapToResponse(User user) {

        UserResponseDTO response = new UserResponseDTO();

        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());

        return response;
    }
}