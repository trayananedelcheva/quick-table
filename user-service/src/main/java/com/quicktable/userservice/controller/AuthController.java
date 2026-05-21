package com.quicktable.userservice.controller;

import com.quicktable.userservice.dto.AuthResponse;
import com.quicktable.userservice.dto.ForgotPasswordRequest;
import com.quicktable.userservice.dto.LoginRequest;
import com.quicktable.userservice.dto.RegisterRequest;
import com.quicktable.userservice.dto.ResetPasswordRequest;
import com.quicktable.userservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "[Public] Register new user", tags = {"Public"})
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "[Public] Login", tags = {"Public"})
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Public] Request password reset link", tags = {"Public"})
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(Map.of("message",
                "Ако имейлът съществува в системата, ще получите линк за смяна на паролата."));
    }

    @Operation(summary = "[Public] Reset password using token", tags = {"Public"})
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Паролата е сменена успешно."));
    }
}
