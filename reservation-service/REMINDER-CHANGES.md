# 📧 Reminder System - Summary

## ✅ Промени

### Преди:
- ❌ Изпращаше reminder **всеки час** за резервации утре
- ❌ Дублиращи emails (1 на час * 24 = 24 emails!)
- ❌ Reminder 24h преди резервацията

### Сега:
- ✅ Изпраща reminder **само веднъж**
- ✅ **2 часа преди резервацията**
- ✅ Проверява на всеки 15 минути
- ✅ 15-минутен прозорец (изпраща само ако резервацията е между 2h-2h15min в бъдещето)

## 🎯 Как работи

```
Пример:
- Резервация: 19:00
- 16:45 - проверка → твърде рано (повече от 2h15m)
- 17:00 - проверка → твърде рано (повече от 2h15m)
- 17:15 - проверка → ✅ MATCH! (резервацията е след 1h45m, влиза в прозореца)
  → Изпраща email
- 17:30 - проверка → вече е изпратен (резервацията е след 1h30m, извън прозореца)
```

## ⚙️ Конфигурация

```yaml
scheduler:
  reminders:
    enabled: true
    cron: "0 */15 * * * *"  # Every 15 minutes
    hours-before: 2  # Send 2 hours before
```

## 📊 Логика

```java
LocalDateTime now = 17:05
LocalDateTime reminderWindowStart = now + 2h = 19:05
LocalDateTime reminderWindowEnd = now + 2h + 15min = 19:20

Query: Намери резервации между 19:05 и 19:20
Резултат: Резервация за 19:00 → ✅ ИЗПРАЩА EMAIL
```

## 📝 Файлове променени

1. `ReservationScheduler.java` - Нова логика за 2-часови reminders
2. `ReservationRepository.java` - Нов query `findReservationsInTimeWindow()`
3. `application.yml` - Променен cron от `0 0 * * * *` на `0 */15 * * * *`
4. `reservation-reminder.html` - Text "след 2 часа" вместо "утре"
5. `SCHEDULER.md` - Обновена документация

## 🧪 Тестване

```sql
-- Създай резервация за след 2 часа
INSERT INTO reservations (
    user_id, restaurant_id, table_id,
    reservation_date, reservation_time,
    number_of_guests, status,
    customer_name, customer_email,
    created_at, updated_at
)
VALUES (
    1, 1, 1,
    CURRENT_DATE, (CURRENT_TIME + INTERVAL '2 hours'),
    4, 'CONFIRMED',
    'Test User', 'your-email@example.com',
    NOW(), NOW()
);

-- Изчакай до следващата 15-минутна проверка
-- Провери logs: "Found X reservations in reminder window"
-- Провери email inbox
```

## ✨ Предимства

1. **Няма дублиране** - само 1 email per резервация
2. **Актуално време** - 2 часа преди вместо 24 часа
3. **По-добра UX** - клиентът получава напомняне точно когато му трябва
4. **Ефективност** - проверява на всеки 15 мин вместо всеки час
5. **Прецизност** - 15-min time window гарантира еднократно изпращане
