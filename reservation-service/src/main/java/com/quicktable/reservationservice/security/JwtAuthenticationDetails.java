package com.quicktable.reservationservice.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * Разширение на WebAuthenticationDetails за съхранение на userId и role от JWT токена
 */
@Getter
public class JwtAuthenticationDetails extends WebAuthenticationDetails {

    private final Long userId;
    private final String userRole;

    public JwtAuthenticationDetails(Long userId, String userRole, HttpServletRequest request) {
        super(request);
        this.userId = userId;
        this.userRole = userRole;
    }
}
