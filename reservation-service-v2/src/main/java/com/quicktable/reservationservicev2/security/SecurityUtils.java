package com.quicktable.reservationservicev2.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public Long getCurrentUserId() {
        return getDetails().getUserId();
    }

    public String getCurrentUserRole() {
        return getDetails().getRole();
    }

    private JwtAuthenticationDetails getDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof JwtAuthenticationDetails details)) {
            throw new IllegalStateException("Няма автентикиран потребител");
        }
        return details;
    }
}
