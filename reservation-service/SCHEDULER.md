# Scheduled Tasks

## ⏰ Автоматични задачи

`ReservationScheduler` изпълнява 3 автоматични задачи за управление на резервации:

### 1. 📧 Reminder Emails (Напомняния)
**Кога:** Всеки 15 минути
**Какво прави:**
- Проверява за резервации, които са **след точно 2 часа**
- Изпраща reminder email на клиента
- Всяка резервация получава **само 1 email** (в 15-минутния прозорец преди 2 часа)

**Конфигурация:**
```yaml
scheduler:
  reminders:
    enabled: true
    cron: "0 */15 * * * *"  # Every 15 minutes
    hours-before: 2  # Send reminder 2 hours before
```

**Пример:**
- Резервация е за днес в 19:00
- На 17:00, 17:15, 17:30... системата проверява
- Между 17:00-17:15 ще намери резервацията (2h преди 19:00)
- Изпраща email **само веднъж** в този прозорец

**Логика:**
```
Сега: 17:05
Търси резервации между: 19:05 - 19:20 (2h + 15min window)
Намира резервация за 19:00 → изпраща email
```

### 2. ✅ Auto-Complete (Автоматично завършване)
**Кога:** Всеки 30 минути (например 09:00, 09:30, 10:00, 10:30...)
**Какво прави:**
- Намира CONFIRMED/PENDING резервации, които са били преди **3 часа**
- Автоматично ги маркира като COMPLETED
- (Опционално) Изпраща благодарствен email

**Конфигурация:**
```yaml
scheduler:
  auto-complete:
    enabled: true
    cron: "0 */30 * * * *"  # Every 30 minutes
    hours-after: 3  # Complete after 3 hours
```

**Логика:**
- Резервация е за 12.03.2026 в 19:00
- На 12.03.2026 в 22:00 (след 3 часа) → маркира като COMPLETED
- Предполага се, че средна продължителност на вечеря е ~2 часа

### 3. 🚫 No-Show Detection (Откриване на неявявания)
**Кога:** Всеки час в :15 минути (например 09:15, 10:15, 11:15...)
**Какво прави:**
- Намира CONFIRMED/PENDING резервации, които са били преди **1 час**
- Маркира ги като NO_SHOW (клиентът не се е явил)

**Конфигурация:**
```yaml
scheduler:
  no-show:
    enabled: true
    cron: "0 15 * * * *"  # Every hour at :15
    hours-after: 1  # Mark no-show after 1 hour
```

**Логика:**
- Резервация е за 12.03.2026 в 19:00
- Ако на 12.03.2026 в 20:15 все още е CONFIRMED/PENDING → NO_SHOW
- Grace period: 1 час (клиент може да закъснее до 1 час)

## 📊 Reservation Status Flow

```
PENDING → CONFIRMED → COMPLETED (normal flow)
                   ↘ NO_SHOW (if customer doesn't show up)
                   ↘ CANCELLED (if customer cancels)
```

## 🔧 Конфигурация

### Disable Specific Jobs
```yaml
scheduler:
  reminders:
    enabled: false  # Disable reminder emails
  auto-complete:
    enabled: false  # Disable auto-completion
  no-show:
    enabled: false  # Disable no-show detection
```

### Custom Timing
```yaml
scheduler:
  reminders:
    hours-before: 3  # Send reminder 3 hours before instead of 2
  auto-complete:
    hours-after: 4  # Complete after 4 hours instead of 3
  no-show:
    hours-after: 2  # Mark no-show after 2 hours instead of 1
```

### Custom Cron Expressions
```yaml
scheduler:
  reminders:
    cron: "0 */10 * * * *"  # Check every 10 minutes (more frequent)
  auto-complete:
    cron: "0 0 * * * *"  # Run every hour instead of 30min
```

## 📈 Monitoring

### Actuator Endpoints
Проверете scheduled tasks:
```bash
curl http://localhost:8083/actuator/scheduledtasks
```

Response:
```json
{
  "cron": [
    {
      "runnable": {
        "target": "com.quicktable.reservationservice.scheduler.ReservationScheduler.sendReservationReminders"
      },
      "expression": "0 0 * * * *"
    },
    {
      "runnable": {
        "target": "com.quicktable.reservationservice.scheduler.ReservationScheduler.autoCompleteReservations"
      },
      "expression": "0 */30 * * * *"
    },
    {
      "runnable": {
        "target": "com.quicktable.reservationservice.scheduler.ReservationScheduler.markNoShowReservations"
      },
      "expression": "0 15 * * * *"
    }
  ]
}
```

### Health Check
```bash
curl http://localhost:8083/actuator/health
```

### Logs
Всички scheduled tasks логват:
- Начало на изпълнение
- Брой намерени резервации
- Брой успешно обработени
- Грешки (ако има)

```log
2026-03-12 09:00:00 INFO  ReservationScheduler - Starting reservation reminder job...
2026-03-12 09:00:01 INFO  ReservationScheduler - Found 5 reservations for tomorrow (2026-03-13)
2026-03-12 09:00:05 INFO  ReservationScheduler - Reservation reminder job completed. Sent 5/5 reminders
```

## 🧪 Testing

### Manual Testing
1. **Test Reminders (2 hours before):**
```sql
-- Create a reservation for 2 hours from now
INSERT INTO reservations (user_id, restaurant_id, table_id, reservation_date, reservation_time,
                          number_of_guests, status, customer_name, customer_email, created_at, updated_at)
VALUES (1, 1, 1, CURRENT_DATE, (CURRENT_TIME + INTERVAL '2 hours'), 4, 'CONFIRMED',
        'Test User', 'test@example.com', NOW(), NOW());

-- Wait for next 15-minute check or trigger manually
-- Check logs for "Found X reservations in reminder window"
```

2. **Test Auto-Complete:**
```sql
-- Create a reservation 4 hours ago
INSERT INTO reservations (user_id, restaurant_id, table_id, reservation_date, reservation_time,
                          number_of_guests, status, customer_name, customer_email, created_at, updated_at)
VALUES (1, 1, 1, CURRENT_DATE, (CURRENT_TIME - INTERVAL '4 hours'), 4, 'CONFIRMED', 'Test User', 'test@example.com', NOW(), NOW());

-- Wait for next scheduler run (30min) or trigger manually
```

3. **Test No-Show:**
```sql
-- Create a reservation 2 hours ago
INSERT INTO reservations (user_id, restaurant_id, table_id, reservation_date, reservation_time,
                          number_of_guests, status, customer_name, customer_email, created_at, updated_at)
VALUES (1, 1, 1, CURRENT_DATE, (CURRENT_TIME - INTERVAL '2 hours'), 4, 'CONFIRMED', 'Test User', 'test@example.com', NOW(), NOW());

-- Wait for next scheduler run (hour :15) or trigger manually
```

### Disable in Development
Ако scheduled tasks пречат по време на development:
```yaml
# application-dev.yml
scheduler:
  reminders:
    enabled: false
  auto-complete:
    enabled: false
  no-show:
    enabled: false
```

## ⚠️ Important Notes

1. **Idempotency:** ✅ FIXED! Reminder се изпраща само веднъж - 2 часа преди резервацията (в 15-min прозорец).

2. **Time Zones:** Сега използва server time zone. За multi-region deployment трябва да се обработва правилно timezone на ресторанта.

3. **Database Load:** Scheduled queries могат да натоварят DB при много резервации. Разгледайте database indexes:
```sql
CREATE INDEX idx_reservation_date_status ON reservations(reservation_date, status);
CREATE INDEX idx_reservation_date_time_status ON reservations(reservation_date, reservation_time, status);
```

4. **Error Handling:** Грешки в един job не спират другите. Всеки job има try-catch и продължава дори при exception.

## 🔮 Future Improvements

1. ~~**Notification Tracking Table**~~ ✅ FIXED - Reminders се изпращат само веднъж
   - История на изпълнени scheduled tasks (optional)

2. **Restaurant Working Hours**
   - Auto-complete само в рамките на работното време
   - No-show detection спрямо closing time

3. **Custom Schedules per Restaurant**
   - Различни grace periods за различни ресторанти
   - Opt-in/opt-out от auto-completion

4. **Manual Triggers**
   - Admin endpoint за ръчно стартиране на jobs
   - Retry failed notifications

5. **Metrics & Analytics**
   - Брой no-shows per restaurant
   - Average completion time
   - Reminder effectiveness (open rates)
