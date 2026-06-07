package com.quicktable.restaurantservice.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

@Getter
public class JwtAuthenticationDetails extends WebAuthenticationDetails {

    private final Long userId;
    private final String role;
    private final String firstName;
    private final String lastName;

    public JwtAuthenticationDetails(Long userId, String role, String firstName, String lastName, HttpServletRequest request) {
        super(request);
        this.userId = userId;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
