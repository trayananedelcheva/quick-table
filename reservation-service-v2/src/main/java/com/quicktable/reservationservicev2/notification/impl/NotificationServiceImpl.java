package com.quicktable.reservationservicev2.notification.impl;

import com.quicktable.reservationservicev2.notification.NotificationData;
import com.quicktable.reservationservicev2.notification.NotificationService;
import com.quicktable.reservationservicev2.notification.NotificationType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.from-name}")
    private String fromName;

    @Override
    public void sendEmail(NotificationData data) {
        if (data.getRecipientEmail() == null || data.getRecipientEmail().isBlank()) {
            log.warn("Няма recipient email, пропускаме изпращането");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(data.getRecipientEmail());
            helper.setSubject(getSubject(data.getType()));
            helper.setText(buildContent(data), true);

            mailSender.send(message);
            log.info("Email изпратен до {}", data.getRecipientEmail());

        } catch (Exception e) {
            log.error("Грешка при изпращане на email: {}", e.getMessage());
        }
    }

    private String getSubject(NotificationType type) {
        return switch (type) {
            case RESERVATION_CONFIRMED -> "Вашата резервация е потвърдена";
        };
    }

    private String buildContent(NotificationData data) {
        Context context = new Context();
        context.setVariable("recipientName", data.getRecipientName());
        context.setVariable("reservationId", data.getReservationId());
        context.setVariable("restaurantName", data.getRestaurantName());
        context.setVariable("reservationDate", data.getReservationDate()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        context.setVariable("reservationTime", data.getReservationTime()
                .format(DateTimeFormatter.ofPattern("HH:mm")));
        context.setVariable("numberOfGuests", data.getNumberOfGuests());
        context.setVariable("specialRequests", data.getSpecialRequests());

        return switch (data.getType()) {
            case RESERVATION_CONFIRMED -> templateEngine.process("email/reservation-confirmed", context);
        };
    }
}
