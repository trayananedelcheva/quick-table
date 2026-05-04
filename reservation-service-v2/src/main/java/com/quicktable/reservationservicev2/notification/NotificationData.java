package com.quicktable.reservationservicev2.notification;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class NotificationData {

    private NotificationType type;
    private String recipientEmail;
    private String recipientName;
    private Long reservationId;
    private String restaurantName;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private Integer numberOfGuests;
    private String specialRequests;
}
