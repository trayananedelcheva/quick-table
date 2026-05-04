package com.quicktable.reservationservice.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * Данни за email notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationData {
    private String recipientEmail;
    private String recipientName;

    private NotificationType type;

    // Reservation details
    private Long reservationId;
    private String restaurantName;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private Integer numberOfGuests;
    private String tableNumber;
    private String specialRequests;

    // Допълнителни данни (за flexibility)
    private Map<String, Object> additionalData;
}
