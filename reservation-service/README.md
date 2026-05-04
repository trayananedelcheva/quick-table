# 🍽️ Reservation Service - Quick Summary

## Текущи функционалности

### ✅ Core Features
1. **Reservation Management** - CRUD операции за резервации
2. **Table Auto-Assignment** - Автоматично избира подходяща маса
3. **Available Time Slots** - Показва свободни часове за резервация
4. **Table Categories** - Поддръжка на INSIDE, SUMMER_GARDEN, WINTER_GARDEN
5. **Restaurant Integration** - Real restaurant names & ownership validation ✨ NEW!

### 🔐 Security
- JWT Authentication (автоматично извличане на userId от token)
- Role-based access control (CLIENT, RESTAURANT_ADMIN, SYSTEM_ADMIN)
- Authorization checks за операции
- Restaurant ownership validation ✨ NEW!

### 📧 Notifications
- Email notifications при създаване, потвърждаване, отмяна
- 6 HTML email templates (Thymeleaf)
- Асинхронно изпращане
- Real restaurant names in emails ✨ NEW!

### ⏰ Scheduled Tasks
- **Reminders:** Email 2h преди резервация (every 15min check) ✨ UPDATED!
- **Auto-Complete:** Автоматично завършване след 3h
- **No-Show:** Маркиране на неявили се клиенти

### 📊 Monitoring
- Actuator endpoints (health, scheduledtasks, metrics)

## Статуси на резервация

```
PENDING → CONFIRMED → COMPLETED
                   ↘ NO_SHOW
       ↘ CANCELLED
       ↘ REJECTED
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/reservations` | Създай резервация |
| GET | `/api/reservations/my` | Моите резервации |
| GET | `/api/reservations/{id}` | Детайли за резервация |
| PUT | `/api/reservations/{id}/status` | Промени статус |
| DELETE | `/api/reservations/{id}` | Отмени резервация |
| GET | `/api/reservations/restaurant/{id}` | Резервации на ресторант |
| GET | `/api/reservations/restaurant/{id}/available-slots` | Налични часове |

## Конфигурация

```yaml
# Email
spring.mail.username=${EMAIL_USERNAME}
spring.mail.password=${EMAIL_PASSWORD}

# Notifications
notification.email.enabled=true

# Scheduler
scheduler.reminders.enabled=true
scheduler.auto-complete.enabled=true
scheduler.no-show.enabled=true
```

## Документация

📖 **SECURITY.md** - JWT Authentication
📧 **NOTIFICATIONS.md** - Email System
⏰ **SCHEDULER.md** - Scheduled Tasks
🏢 **RESTAURANT-INTEGRATION.md** - Restaurant Service Integration ✨ NEW!
📝 **CHANGELOG.md** - Recent Changes
📧 **REMINDER-CHANGES.md** - Reminder System Updates

## Run

```bash
mvn spring-boot:run -pl reservation-service
```

Port: **8083**
