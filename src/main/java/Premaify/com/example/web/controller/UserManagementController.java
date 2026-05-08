package Premaify.com.example.web.controller;

import Premaify.com.example.web.model.UserAccount;
import Premaify.com.example.web.model.UserRole;
import Premaify.com.example.web.repository.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserManagementController {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return userAccountRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        if (userAccountRepository.existsByEmailIgnoreCase(request.email())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("User already exists"));
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setEmail(request.email().toLowerCase());
        userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
        userAccount.setRole(request.role() == null ? UserRole.STAFF : request.role());
        userAccount.setEnabled(true);

        return ResponseEntity.ok(UserResponse.from(userAccountRepository.save(userAccount)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return userAccountRepository.findById(id)
                .map(userAccount -> {
                    if (request.role() != null) {
                        userAccount.setRole(request.role());
                    }
                    if (request.enabled() != null) {
                        userAccount.setEnabled(request.enabled());
                    }
                    if (request.password() != null && !request.password().isBlank()) {
                        userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
                    }
                    return ResponseEntity.ok(UserResponse.from(userAccountRepository.save(userAccount)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record CreateUserRequest(String email, String password, UserRole role) {
    }

    public record UpdateUserRequest(String password, UserRole role, Boolean enabled) {
    }

    public record ErrorResponse(String message) {
    }

    public record UserResponse(Long id, String email, UserRole role, Boolean enabled) {
        static UserResponse from(UserAccount userAccount) {
            return new UserResponse(
                    userAccount.getId(),
                    userAccount.getEmail(),
                    userAccount.getRole(),
                    userAccount.getEnabled()
            );
        }
    }
}
