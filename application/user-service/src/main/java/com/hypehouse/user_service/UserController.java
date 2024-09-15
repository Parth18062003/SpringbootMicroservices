package com.hypehouse.user_service;

import com.hypehouse.user_service.exception.UserNotFoundException;
import com.hypehouse.user_service.rate_limit.RateLimit;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @RateLimit(limitForPeriod = 5, limitRefreshPeriod = 60)
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @RateLimit(limitForPeriod = 5, limitRefreshPeriod = 60)
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));
    }

    @RateLimit(limitForPeriod = 3, limitRefreshPeriod = 60)
    @PostMapping("/users/register")
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        userService.getUserByEmailOrUsername(user.getEmail(), user.getUsername())
                .ifPresent(existingUser -> {
                    throw new RuntimeException("User with email or username already exists");
                });

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        log.info("PasswordEncoder instance: {}", passwordEncoder);
        User createdUser = userService.saveUser(user);
        return ResponseEntity.status(201).body(createdUser); // Return 201 Created status
    }

    @RateLimit(limitForPeriod = 5, limitRefreshPeriod = 60)
    @PutMapping("/users/update-profile/{id}")
    public ResponseEntity<User> updateUser(@PathVariable UUID id, @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));

        updateUserFields(user, userUpdateDTO);

        User updatedUser = userService.saveUser(user);
        return ResponseEntity.ok(updatedUser);
    }

    @RateLimit(limitForPeriod = 5, limitRefreshPeriod = 60)
    @DeleteMapping("/users/delete-profile/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        if (userService.getUserById(id).isEmpty()) {
            throw new UserNotFoundException(id.toString());
        }

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @RateLimit(limitForPeriod = 5, limitRefreshPeriod = 60)
    @PostMapping("/users/enable-2fa/{id}")
    public ResponseEntity<String> enable2FA(@PathVariable UUID id) {
        userService.enable2FA(id);
        return ResponseEntity.ok("2FA enabled");
    }

    @RateLimit(limitForPeriod = 5, limitRefreshPeriod = 60)
    @PostMapping("/users/disable-2fa/{id}")
    public ResponseEntity<String> disable2FA(@PathVariable UUID id) {
        userService.disable2FA(id);
        return ResponseEntity.ok("2FA disabled");
    }

    private void updateUserFields(User user, UserUpdateDTO dto) {
        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }
        if (dto.getFirstName() != null) {
            user.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            user.setLastName(dto.getLastName());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getAddress() != null) {
            user.setAddress(dto.getAddress());
        }
        if (dto.getCity() != null) {
            user.setCity(dto.getCity());
        }
        if (dto.getState() != null) {
            user.setState(dto.getState());
        }
        if (dto.getPostalCode() != null) {
            user.setPostalCode(dto.getPostalCode());
        }
        if (dto.getCountry() != null) {
            user.setCountry(dto.getCountry());
        }
    }
}
