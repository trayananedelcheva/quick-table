package com.quicktable.reservationservice.notification.impl;

import com.quicktable.reservationservice.notification.NotificationData;
import com.quicktable.reservationservice.notification.NotificationService;
import com.quicktable.reservationservice.notification.NotificationType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.from-name}")
    private String fromName;

    @Override
    @Async
    public void sendEmail(NotificationData data) {
        if (!emailEnabled) {
            log.debug("Email notifications са disabled");
            return;
        }

        if (data.getRecipientEmail() == null || data.getRecipientEmail().isEmpty()) {
            log.warn("Recipient email е празен, пропускаме изпращането");
            return;
        }

        try {
            log.info("Изпращане на email тип {} до {}", data.getType(), data.getRecipientEmail());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(data.getRecipientEmail());
            helper.setSubject(getEmailSubject(data.getType()));

            String htmlContent = buildEmailContent(data);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email успешно изпратен до {}", data.getRecipientEmail());

        } catch (MessagingException e) {
            log.error("Грешка при изпращане на email: {}", e.getMessage(), e);
            throw new RuntimeException("Грешка при изпращане на email", e);
        } catch (Exception e) {
            log.error("Unexpected грешка при изпращане на email: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected грешка при изпращане на email", e);
        }
    }

    private String getEmailSubject(NotificationType type) {
        return switch (type) {
            case RESERVATION_CREATED -> "Вашата резервация е създадена";
            case RESERVATION_CONFIRMED -> "Вашата резервация е потвърдена";
            case RESERVATION_CANCELLED -> "Вашата резервация е отменена";
            case RESERVATION_REJECTED -> "Вашата резервация е отхвърлена";
            case RESERVATION_REMINDER -> "Напомняне за резервация";
            case RESERVATION_COMPLETED -> "Благодарим ви за посещението";
            case RESTAURANT_NEW_RESERVATION -> "Нова резервация в ресторанта";
            case REVIEW_REQUEST -> "Споделете мнението си за ресторанта";
        };
    }

    private String buildEmailContent(NotificationData data) {
        Context context = new Context();
        context.setVariable("recipientName", data.getRecipientName());
        context.setVariable("reservationId", data.getReservationId());
        context.setVariable("restaurantName", data.getRestaurantName());
        context.setVariable("reservationDate", formatDate(data.getReservationDate()));
        context.setVariable("reservationTime", formatTime(data.getReservationTime()));
        context.setVariable("numberOfGuests", data.getNumberOfGuests());
        context.setVariable("tableNumber", data.getTableNumber());
        context.setVariable("specialRequests", data.getSpecialRequests());
        if (data.getAdditionalData() != null) {
            data.getAdditionalData().forEach(context::setVariable);
        }

        return templateEngine.process(getTemplateName(data.getType()), context);
    }

    private String getTemplateName(NotificationType type) {
        return switch (type) {
            case RESERVATION_CREATED -> "email/reservation-created";
            case RESERVATION_CONFIRMED -> "email/reservation-confirmed";
            case RESERVATION_CANCELLED -> "email/reservation-cancelled";
            case RESERVATION_REJECTED -> "email/reservation-rejected";
            case RESERVATION_REMINDER -> "email/reservation-reminder";
            case RESERVATION_COMPLETED -> "email/reservation-completed";
            case RESTAURANT_NEW_RESERVATION -> "email/restaurant-new-reservation";
            case REVIEW_REQUEST -> "email/review-request";
        };
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return date.format(formatter);
    }

    private String formatTime(LocalTime time) {
        if (time == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }
}
