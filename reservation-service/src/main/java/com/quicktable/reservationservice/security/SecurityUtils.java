package com.quicktable.reservationservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility класа за достъп до информация от Security Context
 */
@Component
public class SecurityUtils {

    /**
     * Извлича userId на текущия authenticated потребител
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getDetails() instanceof JwtAuthenticationDetails) {
            JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();
            return details.getUserId();
        }

        throw new RuntimeException("Не е намерен authenticated потребител");
    }

    /**
     * Извлича user role на текущия authenticated потребител
     */
    public String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getDetails() instanceof JwtAuthenticationDetails) {
            JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();
            return details.getUserRole();
        }

        throw new RuntimeException("Не е намерен authenticated потребител");
    }

    /**
     * Извлича username (email) на текущия authenticated потребител
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            return authentication.getName();
        }

        throw new RuntimeException("Не е намерен authenticated потребител");
    }

    /**
     * Проверява дали текущият потребител е CLIENT
     */
    public boolean isClient() {
        return "CLIENT".equals(getCurrentUserRole());
    }

    /**
     * Проверява дали текущият потребител е RESTAURANT_ADMIN
     */
    public boolean isRestaurantAdmin() {
        return "RESTAURANT_ADMIN".equals(getCurrentUserRole());
    }

    /**
     * Проверява дали текущият потребител е SYSTEM_ADMIN
     */
    public boolean isSystemAdmin() {
        return "SYSTEM_ADMIN".equals(getCurrentUserRole());
    }
}
