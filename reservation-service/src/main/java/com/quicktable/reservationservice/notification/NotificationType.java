package com.quicktable.reservationservice.notification;

/**
 * Enum за типове notification евенти
 */
public enum NotificationType {
    RESERVATION_CREATED,
    RESERVATION_CONFIRMED,
    RESERVATION_CANCELLED,
    RESERVATION_REJECTED,
    RESERVATION_REMINDER,
    RESERVATION_COMPLETED,
    RESTAURANT_NEW_RESERVATION,
    REVIEW_REQUEST
}
