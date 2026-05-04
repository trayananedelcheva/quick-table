# Notification System

## 📧 Email Notifications

Автоматично изпращане на email-и при:
- ✅ **RESERVATION_CREATED** - Когато се създаде нова резервация
- ✅ **RESERVATION_CONFIRMED** - Когато ресторантът потвърди резервацията
- ✅ **RESERVATION_CANCELLED** - Когато резервацията бъде отменена
- ⏰ **RESERVATION_REMINDER** - Напомняне 1 ден преди резервацията (TODO: scheduled task)
- 🎉 **RESERVATION_COMPLETED** - След завършване на резервацията (TODO)
- 🔔 **RESTAURANT_NEW_RESERVATION** - Notification до ресторанта за нова резервация (TODO)

## 🎨 Email Templates

Професионални HTML email templates с Thymeleaf:
- Модерен дизайн с градиент headers
- Responsive layout
- Цветово кодиране по тип notification
- Включена информация: ресторант, дата, час, брой гости, специални изисквания

### Налични templates:
1. `reservation-created.html` - Потвърждение за създадена резервация (зелен)
2. `reservation-confirmed.html` - Потвърждение от ресторанта (син)
3. `reservation-cancelled.html` - Отменена резервация (червен)
4. `reservation-reminder.html` - Напомняне 24h преди (жълт)
5. `reservation-completed.html` - Благодарствен email след посещение
6. `restaurant-new-reservation.html` - Notification до ресторанта

## ⚡ Async Processing

- Notifications се изпращат асинхронно (не блокират main thread)
- Грешки при изпращане не провалят операциите (graceful degradation)
- Response към клиента се връща незабавно

## 🔧 Конфигурация

### application.yml
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${EMAIL_USERNAME:your-email@gmail.com}
    password: ${EMAIL_PASSWORD:your-app-password}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

notification:
  email:
    enabled: true
    from: noreply@quicktable.com
    from-name: Quick Table
```

### Environment Variables
За production environment задайте:
```bash
export EMAIL_USERNAME=your-email@gmail.com
export EMAIL_PASSWORD=your-app-password
```

**⚠️ За Gmail:**
1. Включете 2-Factor Authentication
2. Създайте App Password: https://myaccount.google.com/apppasswords
3. Използвайте App Password вместо реалната парола

## 🎯 Notification Flow

### При създаване на резервация:
1. Client POST `/api/reservations`
2. `ReservationService.createReservation()` записва в DB
3. `sendReservationCreatedNotification()` се извиква асинхронно
4. `NotificationServiceImpl.sendEmail()` изпраща email
5. Email thread не блокира response към клиента

### При потвърждаване:
1. Restaurant Admin PUT `/api/reservations/{id}/status` с status=CONFIRMED
2. `ReservationService.updateReservationStatus()` обновява статуса
3. `sendReservationConfirmedNotification()` изпраща потвърждение на клиента

### При отмяна:
1. Client DELETE `/api/reservations/{id}`
2. `ReservationService.cancelReservation()` променя статуса на CANCELLED
3. `sendReservationCancelledNotification()` изпраща уведомление

## 🧪 Тестване

### Local Testing (MailHog)
За локално тестване без истински SMTP:
```bash
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

application-dev.yml:
```yaml
spring:
  mail:
    host: localhost
    port: 1025
```

Отвори http://localhost:8025 за inbox.

### Manual Testing
```bash
# Създай резервация и провери дали получаваш email
curl -X POST http://localhost:8083/api/reservations \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "reservationDate": "2026-03-20",
    "reservationTime": "19:00",
    "guestsCount": 4,
    "customerName": "Иван Иванов",
    "customerEmail": "test@example.com"
  }'
```

## 🔮 Бъдещи подобрения

1. **Scheduled Reminders** ⏰
   - Cron job за изпращане на reminders 24 часа преди резервацията
   - Използване на Spring `@Scheduled`
   - Database query за upcoming reservations

2. **Restaurant Notifications** 🔔
   - Email до ресторанта при нова резервация
   - Dashboard за real-time notifications
   - Integration с restaurant admin email

3. **Notification Preferences** ⚙️
   - User settings за enable/disable notifications
   - Избор на тип notifications (само важни, всички, etc.)

4. **Template Management** 🎨
   - Admin панел за редакция на email templates
   - Multi-language support (БГ/EN)
   - Brand customization per restaurant

5. **Notification History** 📊
   - Запис на изпратени notifications в DB
   - Retry mechanism за failed notifications
   - Статистики за delivery rates

6. **Rich Content** 📸
   - Restaurant logo в email-а
   - Карта с местоположение
   - QR код за резервацията

## 📊 Production Checklist

- [ ] Конфигурирани EMAIL_USERNAME и EMAIL_PASSWORD
- [ ] Тествани всички типове notifications
- [ ] Проверено rate limiting на SMTP провайдера (Gmail: 500/day)
- [ ] Настроен SPF/DKIM за custom domain (optional)
- [ ] Мониторинг на failed emails
- [ ] Fallback mechanism при недостъпен SMTP
- [ ] Error handling за invalid emails

## 🛠️ Troubleshooting

### Email не се получава
1. Проверете SPAM папката
2. Verify email credentials в application.yml
3. Check logs за грешки: `tail -f logs/reservation-service.log`
4. Test SMTP connection:
```bash
telnet smtp.gmail.com 587
```

### Gmail блокира изпращането
- Enable "Less secure app access" (не се препоръчва)
- Use App Password (препоръчително)
- Use OAuth2 за production (най-сигурно)

### Бавно изпращане
- Email-ите се изпращат async, не трябва да забавят API
- Ако има забавяне, check thread pool configuration
- Разгледайте external email service (SendGrid, AWS SES)
