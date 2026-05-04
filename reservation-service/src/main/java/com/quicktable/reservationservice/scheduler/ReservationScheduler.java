package com.quicktable.reservationservice.scheduler;

import com.quicktable.common.dto.ReservationStatus;
import com.quicktable.reservationservice.client.RestaurantServiceClient;
import com.quicktable.reservationservice.entity.Reservation;
import com.quicktable.reservationservice.notification.NotificationData;
import com.quicktable.reservationservice.notification.NotificationService;
import com.quicktable.reservationservice.notification.NotificationType;
import com.quicktable.reservationservice.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled tasks за reservation management
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;
    private final RestaurantServiceClient restaurantServiceClient;

    @Value("${scheduler.reminders.enabled:true}")
    private boolean remindersEnabled;

    @Value("${scheduler.day-before-reminder.enabled:true}")
    private boolean dayBeforeReminderEnabled;

    @Value("${scheduler.review-request.enabled:true}")
    private boolean reviewRequestEnabled;

    @Value("${app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    @Value("${scheduler.auto-complete.enabled:true}")
    private boolean autoCompleteEnabled;

    @Value("${scheduler.auto-complete.hours-after:3}")
    private int autoCompleteHoursAfter;

    @Value("${scheduler.no-show.enabled:true}")
    private boolean noShowEnabled;

    @Value("${scheduler.no-show.hours-after:1}")
    private int noShowHoursAfter;

    /**
     * Изпраща reminder email точно 2 часа преди резервацията.
     * Runs every hour — проверява резервации в прозорец от следващия час,
     * като взима тези между now+2h и now+2h+1h (т.е. ще се изпрати точно веднъж).
     */
    @Scheduled(cron = "${scheduler.reminders.cron:0 0 * * * *}")
    public void sendTwoHourReminders() {
        if (!remindersEnabled) {
            log.debug("2-hour reminder job is disabled");
            return;
        }

        log.info("Starting 2-hour reservation reminder job...");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime windowStart = now.plusHours(2);
            LocalDateTime windowEnd = now.plusHours(3);

            List<Reservation> upcomingReservations = reservationRepository
                    .findReservationsInTimeWindow(
                            windowStart.toLocalDate(),
                            windowStart.toLocalTime(),
                            windowEnd.toLocalTime()
                    );

            log.info("Found {} reservations for 2-hour reminder", upcomingReservations.size());

            int sentCount = 0;
            for (Reservation reservation : upcomingReservations) {
                try {
                    if (reservation.isTwoHourReminderSent()) {
                        log.debug("2-hour reminder already sent for reservation {}, skipping", reservation.getId());
                        continue;
                    }
                    sendReminderEmail(reservation, "2-часово");
                    reservation.setTwoHourReminderSent(true);
                    reservationRepository.save(reservation);
                    sentCount++;
                } catch (Exception e) {
                    log.error("Failed to send 2-hour reminder for reservation {}: {}",
                            reservation.getId(), e.getMessage());
                }
            }

            log.info("2-hour reminder job completed. Sent {}/{} reminders", sentCount, upcomingReservations.size());

        } catch (Exception e) {
            log.error("Error in 2-hour reminder job: {}", e.getMessage(), e);
        }
    }

    /**
     * Изпраща reminder email предния ден в часа на резервацията.
     * Runs every hour — проверява резервации утре в текущия час (прозорец от 1 час),
     * направени поне 1 ден предварително.
     */
    @Scheduled(cron = "${scheduler.day-before-reminder.cron:0 0 * * * *}")
    public void sendDayBeforeReminders() {
        if (!dayBeforeReminderEnabled) {
            log.debug("Day-before reminder job is disabled");
            return;
        }

        log.info("Starting day-before reservation reminder job...");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDate tomorrow = now.toLocalDate().plusDays(1);
            LocalDateTime todayStart = now.toLocalDate().atStartOfDay();

            // Намираме резервации за утре, направени преди днес (поне 1 ден предварително)
            List<Reservation> tomorrowReservations = reservationRepository
                    .findReservationsForTomorrowMadeInAdvance(tomorrow, todayStart);

            // Филтрираме само тези, чийто час съвпада с текущия (прозорец от 1 час)
            List<Reservation> toRemind = tomorrowReservations.stream()
                    .filter(r -> {
                        int reservationHour = r.getReservationTime().getHour();
                        int currentHour = now.getHour();
                        return reservationHour == currentHour;
                    })
                    .toList();

            log.info("Found {} reservations for day-before reminder (hour {})", toRemind.size(), now.getHour());

            int sentCount = 0;
            for (Reservation reservation : toRemind) {
                try {
                    if (reservation.isDayBeforeReminderSent()) {
                        log.debug("Day-before reminder already sent for reservation {}, skipping", reservation.getId());
                        continue;
                    }
                    sendReminderEmail(reservation, "предния ден");
                    reservation.setDayBeforeReminderSent(true);
                    reservationRepository.save(reservation);
                    sentCount++;
                } catch (Exception e) {
                    log.error("Failed to send day-before reminder for reservation {}: {}",
                            reservation.getId(), e.getMessage());
                }
            }

            log.info("Day-before reminder job completed. Sent {}/{} reminders", sentCount, toRemind.size());

        } catch (Exception e) {
            log.error("Error in day-before reminder job: {}", e.getMessage(), e);
        }
    }

    /**
     * Автоматично маркира резервации като COMPLETED ако са минали 3 часа след reservation time
     * Runs every 30 minutes
     */
    @Scheduled(cron = "${scheduler.auto-complete.cron:0 */30 * * * *}")
    public void autoCompleteReservations() {
        if (!autoCompleteEnabled) {
            log.debug("Auto-complete job is disabled");
            return;
        }

        log.info("Starting auto-complete reservations job...");

        try {
            LocalDate today = LocalDate.now();
            LocalDateTime hoursAgo = LocalDateTime.now().minusHours(autoCompleteHoursAfter);

            List<Reservation> activeReservations = reservationRepository
                    .findActiveReservationsBeforeTime(today, hoursAgo.toLocalTime());

            log.info("Found {} reservations to auto-complete", activeReservations.size());

            int completedCount = 0;
            for (Reservation reservation : activeReservations) {
                try {
                    reservation.setStatus(ReservationStatus.COMPLETED);
                    reservationRepository.save(reservation);

                    // TODO: Send completion email (optional)
                    // sendCompletionEmail(reservation);

                    completedCount++;
                    log.debug("Auto-completed reservation {}", reservation.getId());
                } catch (Exception e) {
                    log.error("Failed to complete reservation {}: {}",
                            reservation.getId(), e.getMessage());
                }
            }

            log.info("Auto-complete job finished. Completed {}/{} reservations",
                    completedCount, activeReservations.size());

        } catch (Exception e) {
            log.error("Error in auto-complete job: {}", e.getMessage(), e);
        }
    }

    /**
     * Автоматично маркира резервации като NO_SHOW ако клиентът не се е явил
     * Runs every hour at :15
     */
    @Scheduled(cron = "${scheduler.no-show.cron:0 15 * * * *}")
    public void markNoShowReservations() {
        if (!noShowEnabled) {
            log.debug("No-show detection job is disabled");
            return;
        }

        log.info("Starting no-show detection job...");

        try {
            LocalDate today = LocalDate.now();
            LocalDateTime hoursAgo = LocalDateTime.now().minusHours(noShowHoursAfter);

            // Търсим резервации които са били преди >1 час и все още са CONFIRMED
            List<Reservation> missedReservations = reservationRepository
                    .findActiveReservationsBeforeTime(today, hoursAgo.toLocalTime());

            log.info("Found {} potential no-show reservations", missedReservations.size());

            int noShowCount = 0;
            for (Reservation reservation : missedReservations) {
                try {
                    reservation.setStatus(ReservationStatus.NO_SHOW);
                    reservationRepository.save(reservation);

                    noShowCount++;
                    log.debug("Marked reservation {} as NO_SHOW", reservation.getId());
                } catch (Exception e) {
                    log.error("Failed to mark reservation {} as no-show: {}",
                            reservation.getId(), e.getMessage());
                }
            }

            log.info("No-show detection job finished. Marked {}/{} as NO_SHOW",
                    noShowCount, missedReservations.size());

        } catch (Exception e) {
            log.error("Error in no-show detection job: {}", e.getMessage(), e);
        }
    }

    /**
     * Изпраща покана за оставяне на review на следващия ден след COMPLETED резервация.
     * Runs once daily at 10:00.
     */
    @Scheduled(cron = "${scheduler.review-request.cron:0 0 10 * * *}")
    public void sendReviewRequests() {
        if (!reviewRequestEnabled) {
            log.debug("Review request job is disabled");
            return;
        }

        log.info("Starting review request job...");

        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);

            List<Reservation> completedReservations = reservationRepository
                    .findCompletedReservationsFromYesterdayWithoutReviewRequest(yesterday);

            log.info("Found {} completed reservations for review request", completedReservations.size());

            int sentCount = 0;
            for (Reservation reservation : completedReservations) {
                try {
                    sendReviewRequestEmail(reservation);
                    reservation.setReviewRequestSent(true);
                    reservationRepository.save(reservation);
                    sentCount++;
                } catch (Exception e) {
                    log.error("Failed to send review request for reservation {}: {}",
                            reservation.getId(), e.getMessage());
                }
            }

            log.info("Review request job completed. Sent {}/{} emails", sentCount, completedReservations.size());

        } catch (Exception e) {
            log.error("Error in review request job: {}", e.getMessage(), e);
        }
    }

    private void sendReminderEmail(Reservation reservation, String reminderType) {
        String restaurantName = getRestaurantName(reservation.getRestaurantId());

        NotificationData notificationData = NotificationData.builder()
                .recipientEmail(reservation.getCustomerEmail())
                .recipientName(reservation.getCustomerName())
                .type(NotificationType.RESERVATION_REMINDER)
                .reservationId(reservation.getId())
                .restaurantName(restaurantName)
                .reservationDate(reservation.getReservationDate())
                .reservationTime(reservation.getReservationTime())
                .numberOfGuests(reservation.getNumberOfGuests())
                .specialRequests(reservation.getSpecialRequests())
                .build();

        notificationService.sendEmail(notificationData);
        log.info("Sent {} reminder email for reservation {} to {}",
                reminderType, reservation.getId(), reservation.getCustomerEmail());
    }

    private void sendReviewRequestEmail(Reservation reservation) {
        String restaurantName = getRestaurantName(reservation.getRestaurantId());
        String reviewUrl = appBaseUrl + "/restaurants/" + reservation.getRestaurantId()
                + "/reviews/new?reservationId=" + reservation.getId();

        NotificationData notificationData = NotificationData.builder()
                .recipientEmail(reservation.getCustomerEmail())
                .recipientName(reservation.getCustomerName())
                .type(NotificationType.REVIEW_REQUEST)
                .reservationId(reservation.getId())
                .restaurantName(restaurantName)
                .reservationDate(reservation.getReservationDate())
                .reservationTime(reservation.getReservationTime())
                .numberOfGuests(reservation.getNumberOfGuests())
                .additionalData(java.util.Map.of("reviewUrl", reviewUrl))
                .build();

        notificationService.sendEmail(notificationData);
        log.info("Sent review request email for reservation {} to {}",
                reservation.getId(), reservation.getCustomerEmail());
    }

    private void sendCompletionEmail(Reservation reservation) {
        String restaurantName = getRestaurantName(reservation.getRestaurantId());

        NotificationData notificationData = NotificationData.builder()
                .recipientEmail(reservation.getCustomerEmail())
                .recipientName(reservation.getCustomerName())
                .type(NotificationType.RESERVATION_COMPLETED)
                .reservationId(reservation.getId())
                .restaurantName(restaurantName)
                .reservationDate(reservation.getReservationDate())
                .reservationTime(reservation.getReservationTime())
                .numberOfGuests(reservation.getNumberOfGuests())
                .build();

        notificationService.sendEmail(notificationData);
        log.info("Sent completion email for reservation {} to {}",
                reservation.getId(), reservation.getCustomerEmail());
    }

    /**
     * Helper метод за извличане на име на ресторант с fallback
     */
    private String getRestaurantName(Long restaurantId) {
        try {
            var restaurant = restaurantServiceClient.getRestaurantById(restaurantId);
            return restaurant != null ? restaurant.getName() : "Ресторант #" + restaurantId;
        } catch (Exception e) {
            log.warn("Не успя извличането на име на ресторант {}: {}", restaurantId, e.getMessage());
            return "Ресторант #" + restaurantId;
        }
    }
}
