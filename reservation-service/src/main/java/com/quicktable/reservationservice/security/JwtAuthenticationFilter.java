package com.quicktable.reservationservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Проверка дали има Authorization header с Bearer токен
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);
            final Long userId = jwtService.extractUserId(jwt);
            final String userRole = jwtService.extractUserRole(jwt);

            // Ако има валиден токен и няма Authentication в SecurityContext
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Създаваме Authentication обект
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userRole))
                );

                // Добавяме допълнителни детайли (userId)
                authToken.setDetails(new JwtAuthenticationDetails(userId, userRole, request));

                // Задаваме Authentication в SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWTAuthentiation успешна за user: {} (ID: {}, Role: {})", userEmail, userId, userRole);
            }

        } catch (Exception e) {
            log.error("JWT Authentication грешка: {}", e.getMessage());
            // Не пропагираме exception-а, за да може филтърът да продължи
        }

        filterChain.doFilter(request, response);
    }
}
