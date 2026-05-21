package com.quicktable.userservice.service;

import com.quicktable.common.dto.UserRole;
import com.quicktable.userservice.dto.AuthResponse;
import com.quicktable.userservice.dto.LoginRequest;
import com.quicktable.userservice.dto.RegisterRequest;
import com.quicktable.userservice.exception.EmailAlreadyExistsException;
import com.quicktable.userservice.entity.PasswordResetToken;
import com.quicktable.userservice.entity.User;
import com.quicktable.userservice.repository.PasswordResetTokenRepository;
import com.quicktable.userservice.repository.UserRepository;
import com.quicktable.userservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final NotificationClient notificationClient;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Потребител с този имейл вече съществува. Моля, посочете друг имейл за регистрация.");
        }

        // SECURITY: Всички нови потребители са CLIENT по подразбиране
        // Само съществуващ SYSTEM_ADMIN може да променя роли
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(UserRole.CLIENT)  // Винаги CLIENT при регистрация
                .active(true)
                .build();

        user = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        // Добавяме userId, role и имена като claims в JWT токена
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        String token = jwtTokenProvider.generateToken(claims, userDetails);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Потребител не е намерен"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        // Добавяме userId, role и имена като claims в JWT токена
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        String token = jwtTokenProvider.generateToken(claims, userDetails);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.deleteAllByUser(user);
            String rawToken = UUID.randomUUID().toString();
            String hashedToken = hashToken(rawToken);
            PasswordResetToken prt = new PasswordResetToken(hashedToken, user, LocalDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(prt);
            notificationClient.sendPasswordReset(user, rawToken);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String hashedToken = hashToken(rawToken);
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new RuntimeException("Невалиден или изтекъл токен за смяна на парола."));
        if (prt.isUsed() || prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Невалиден или изтекъл токен за смяна на парола.");
        }
        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        prt.setUsed(true);
        passwordResetTokenRepository.save(prt);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 не е наличен", e);
        }
    }
}
