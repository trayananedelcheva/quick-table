package com.quicktable.userservice.controller;

import com.quicktable.common.dto.UserRole;
import com.quicktable.userservice.dto.UserResponse;
import com.quicktable.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers(
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
