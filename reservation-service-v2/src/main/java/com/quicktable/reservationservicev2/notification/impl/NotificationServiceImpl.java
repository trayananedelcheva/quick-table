package com.quicktable.reservationservicev2.notification.impl;

import com.quicktable.reservationservicev2.notification.NotificationData;
import com.quicktable.reservationservicev2.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final WebClient webClient;

    public NotificationServiceImpl(@Value("${notification-service.url}") String notificationServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(notificationServiceUrl)
                .build();
    }

    @Override
    public void sendEmail(NotificationData data) {
        if (data.getRecipientEmail() == null || data.getRecipientEmail().isBlank()) {
            log.warn("Няма recipient email, пропускаме изпращането");
            return;
        }

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("type", data.getType().name());
            request.put("recipientEmail", data.getRecipientEmail());
            request.put("recipientName", data.getRecipientName());
            request.put("reservationId", data.getReservationId());
            request.put("restaurantName", data.getRestaurantName());
            request.put("reservationDate", data.getReservationDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            request.put("reservationTime", data.getReservationTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            request.put("numberOfGuests", data.getNumberOfGuests());
            request.put("specialRequests", data.getSpecialRequests() != null ? data.getSpecialRequests() : "");

            webClient.post()
                    .uri("/api/notifications/send")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Нотификация изпратена към notification-service за {}", data.getRecipientEmail());

        } catch (Exception e) {
            log.error("Грешка при изпращане на нотификация: {}", e.getMessage());
        }
    }
}
