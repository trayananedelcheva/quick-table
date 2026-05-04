package com.quicktable.reservationservice.notification;

/**
 * Interface за notification service
 */
public interface NotificationService {

    /**
     * Изпраща email notification
     */
    void sendEmail(NotificationData data);
}
