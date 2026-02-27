package com.quicktable.restaurantservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String jwt = authHeader.substring(7);
                
                if (jwtService.isTokenValid(jwt)) {
                    Long userId = jwtService.extractUserId(jwt);
                    String userRole = jwtService.extractRole(jwt);
                    String userEmail = jwtService.extractEmail(jwt);

                    // Записваме информацията в request attributes
                    request.setAttribute("userId", userId);
                    request.setAttribute("userRole", userRole);
                    request.setAttribute("userEmail", userEmail);
                    
                    log.debug("JWT validated for user: {} (ID: {}, Role: {})", userEmail, userId, userRole);
                }
            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
