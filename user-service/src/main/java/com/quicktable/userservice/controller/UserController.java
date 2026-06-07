package com.quicktable.userservice.controller;

import com.quicktable.common.dto.UserRole;
import com.quicktable.userservice.dto.UserResponse;
import com.quicktable.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "[Client] Get current user profile", tags = {"Client"})
    @SecurityRequirement(name = "Client")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Client] Get user by ID", tags = {"Client"})
    @SecurityRequirement(name = "Client")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        UserResponse currentUser = userService.getCurrentUser();
        if (!(currentUser.getId().equals(userId) || currentUser.getRole() == UserRole.SYSTEM_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @Operation(summary = "[System Admin] Get all users", tags = {"System Admin"})
    @SecurityRequirement(name = "System Admin")
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @Parameter(schema = @Schema(allowableValues = {"CLIENT", "RESTAURANT_ADMIN", "SYSTEM_ADMIN"}))
            @RequestParam(required = false) String role
    ) {
        List<UserResponse> response;
        if (role != null) {
            response = userService.getUsersByRole(UserRole.valueOf(role.toUpperCase()));
        } else {
            response = userService.getAllUsers();
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Client] Update profile", tags = {"Client"})
    @SecurityRequirement(name = "Client")
    @PutMapping("/me/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phoneNumber
    ) {
        return ResponseEntity.ok(userService.updateProfile(firstName, lastName, phoneNumber));
    }

    @Operation(summary = "[Client] Change password", tags = {"Client"})
    @SecurityRequirement(name = "Client")
    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword
    ) {
        userService.changePassword(currentPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Паролата е сменена успешно."));
    }

    @Operation(summary = "[System Admin] Update user role", tags = {"System Admin"})
    @SecurityRequirement(name = "System Admin")
    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long userId,
            @RequestParam String role
    ) {
        UserRole userRole = UserRole.valueOf(role.toUpperCase());
        UserResponse response = userService.updateUserRole(userId, userRole);
        return ResponseEntity.ok(response);
    }
}
