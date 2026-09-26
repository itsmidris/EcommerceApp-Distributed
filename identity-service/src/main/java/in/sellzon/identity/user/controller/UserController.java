package in.sellzon.identity.user.controller;

import in.sellzon.identity.user.dto.request.CreateUserRequestDTO;
import in.sellzon.identity.user.dto.response.PublicUserProfileDTO;
import in.sellzon.identity.user.dto.response.UserResponseDTO;
import in.sellzon.identity.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody CreateUserRequestDTO request) {
        UserResponseDTO response = userService.createUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/public-profile/{id}")
    public ResponseEntity<PublicUserProfileDTO> getPublicProfile(@PathVariable UUID id) {

        UserResponseDTO user = userService.getUserById(id);

        PublicUserProfileDTO response =
                new PublicUserProfileDTO(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable UUID id) {
        UserResponseDTO response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@RequestParam String email) {
        UserResponseDTO response = userService.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/phone")
    public ResponseEntity<UserResponseDTO> getUserByPhoneNumber(@RequestParam String phoneNumber) {
        UserResponseDTO response = userService.getUserByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable UUID id) {
        userService.disableUser(id);
        return ResponseEntity.noContent().build();
    }
}