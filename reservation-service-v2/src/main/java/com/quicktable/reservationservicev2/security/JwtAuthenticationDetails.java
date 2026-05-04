package com.quicktable.reservationservicev2.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

@Getter
public class JwtAuthenticationDetails extends WebAuthenticationDetails {

    private final Long userId;
    private final String role;

    public JwtAuthenticationDetails(Long userId, String role, HttpServletRequest request) {
        super(request);
        this.userId = userId;
        this.role = role;
    }
}
