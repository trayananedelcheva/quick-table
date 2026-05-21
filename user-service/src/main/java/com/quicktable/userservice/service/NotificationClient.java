package com.quicktable.userservice.service;

import com.quicktable.userservice.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
public class NotificationClient {

    private final RestClient restClient;
    private final String baseUrl;

    public NotificationClient(
            @Value("${app.base-url}") String appBaseUrl,
            @Value("${notification-service.url}") String notificationServiceUrl
    ) {
        this.baseUrl = appBaseUrl;
        this.restClient = RestClient.builder()
                .baseUrl(notificationServiceUrl)
                .build();
    }

    public void sendPasswordReset(User user, String token) {
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        Map<String, Object> body = Map.of(
                "type", "PASSWORD_RESET",
                "recipientEmail", user.getEmail(),
                "recipientName", user.getFirstName() + " " + user.getLastName(),
                "resetUrl", resetUrl
        );
        try {
            restClient.post()
                    .uri("/api/notifications/send")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Неуспешно изпращане на имейл за забравена парола до {}: {}", user.getEmail(), e.getMessage());
        }
    }
}
