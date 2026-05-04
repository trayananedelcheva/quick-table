# Reservation Service — Документация

## Обзор

`reservation-service` е микросервиз, отговорен за целия жизнен цикъл на ресторантските резервации в Quick Table. Работи на порт **8083** и комуникира с `restaurant-service` (порт 8082).

**Основни отговорности:** създаване и управление на резервации, автоматичен избор на маса, генериране на времеви слотове, email нотификации, автоматизирани статус промени и напомняния.

---

## Технически стек

| Компонент  | Технология |
|------------|-----------|
| Framework  | Spring Boot 3.x |
| База данни | PostgreSQL (`quicktable_reservations`) |
| Сигурност  | JWT (JJWT), Spring Security |
| Email      | Spring Mail + Thymeleaf |
| Scheduler  | Spring `@Scheduled` |
| HTTP Client| Spring WebClient |
| API Docs   | Swagger UI (springdoc-openapi 2.3.0) |

---

## Конфигурация и стартиране

**Изисквания:** Java 17+, PostgreSQL, `restaurant-service` на `http://localhost:8082`, Gmail App Password.

```bash
# Environment variables
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password
```

```yaml
# application.yml (основни настройки)
server.port: 8083
spring.datasource.url: jdbc:postgresql://localhost:5432/quicktable_reservations
jwt.secret: <base64-secret>
jwt.expiration: 86400000
restaurant-service.url: http://localhost:8082
app.base-url: http://localhost:3000  # за review линкове в emails
```

```bash
cd reservation-service && mvn spring-boot:run
```

---

## Сигурност и автентикация

Всички endpoints изискват `Authorization: Bearer <jwt_token>`, освен:
- `GET /api/reservations/restaurant/{id}/available-slots`
- `GET /api/reservations/restaurant/{id}/time-slots`
- `GET /actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`

**Swagger UI:** `http://localhost:8083/swagger-ui/index.html` — натисни **Authorize** и въведи JWT токена.

### Роли

| Роля | Права |
|------|-------|
| `CLIENT` | Създава резервации, вижда само своите, редактира/отказва свои |
| `RESTAURANT_ADMIN` | Вижда и управлява статуси само на **своя** ресторант. **Не може** да прави резервации |
| `SYSTEM_ADMIN` | Пълен достъп без ownership ограничения |

---

## Бизнес правила

### 1. Ролеви ограничения

- Само **`CLIENT`** може да създава резервации. `RESTAURANT_ADMIN` получава **400**.
- **`CLIENT`** вижда само своята резервация — при чужда → **403**.
- **`RESTAURANT_ADMIN`** вижда само резервации от своя ресторант (ownership check срещу `restaurant-service`) — при чужд → **403**.
- **`SYSTEM_ADMIN`** вижда всякаква резервация без ограничения.
- `RESTAURANT_ADMIN` управлява статуси само на своя ресторант; `SYSTEM_ADMIN` прескача ownership проверката и може да управлява статуси на всяка резервация.
- Ownership се верифицира при всяко извикване чрез `restaurantServiceClient.isRestaurantOwner()`. При недостъпен `restaurant-service` → **403** (безопасна страна).

---

### 2. Създаване на резервация

**Bean Validation** (при нарушение → **400** с `{ fieldName: "errorMessage" }`):
- `restaurantId` — задължително
- `reservationDate` — задължително, `@FutureOrPresent`
- `reservationTime` — задължително
- `guestsCount` — задължително, минимум 1
- `customerName`, `customerPhone` — задължителни
- `customerEmail` — задължително, валиден email формат
- `preferredLocation` — незадължително; `INSIDE`, `SUMMER_GARDEN`, `WINTER_GARDEN`

**Бизнес правила в service-а:**
- Резервацията трябва да е **поне 1 час напред** → **400** при нарушение.
- `RESTAURANT_ADMIN` → **400** незабавно.
- Масата се избира **автоматично** (клиентът не я избира):
  1. Взимат се всички маси с `capacity >= guestsCount` и (ако е подадена) `location == preferredLocation` от restaurant-service.
  2. За всяка маса се проверява дали има активна резервация (`PENDING` или `CONFIRMED`) за същата дата, чийто 2-часов интервал се припокрива с интервала на новата заявка.
  3. Random избор измежду свободните → **409** ако няма нито една.
- Статус при запис: **`PENDING`**. Reminder флагове: `false`. Email `RESERVATION_CREATED` — async.

---

### 3. Времеви прозорец на заетост (2 часа)

Всяка резервация блокира масата за **2 часа**. Проверка за припокриване:

```
requestedEnd   = requestedTime + 2h
reservationEnd = existing.time + 2h

Припокриване = NOT (requestedTime >= reservationEnd OR requestedEnd <= existing.time)
```

Граничните стойности са **изключени** (18:00–20:00 и 20:00–22:00 не се припокриват).
Участват само резервации за **същата маса**, **същата дата**, в статус `PENDING` или `CONFIRMED`.

---

### 4. Генериране на времеви слотове

- Работното време се взима от `restaurant-service` (fallback: 10:00–22:00).
- Слотове на всеки **30 минути** от `openingTime` до `closingTime - 1h`.
- Слотове < 1 час напред:
  - `/available-slots` — **не се включват**
  - `/time-slots` — включват се с `available: false`
- Само слотовете с поне 1 свободна маса (с нужен капацитет и локация) са налични.

---

### 5. Редакция на резервация

- Само собственикът (`userId == JWT userId`) може да редактира; `SYSTEM_ADMIN` прескача.
- Само `PENDING` или `CONFIRMED` резервации са редактируеми → **400** иначе.
- Не може да се смени ресторантът или клиентските данни. Масата се преизчислява автоматично.

**При промяна на дата/час или брой гости:**
1. Нова дата/час трябва да е поне **1 час напред** → **400** иначе.
2. Търси се нова свободна маса с капацитет ≥ новия `guestsCount` за новото дата/час.
3. Random избор измежду свободните → **409** ако няма.
4. `tableId` се обновява. При промяна на дата/час: `twoHourReminderSent` и `dayBeforeReminderSent` → `false`.

**При промяна само на `specialRequests`:** без проверка за наличност, reminder флагове не се нулират.

---

### 6. Отказване на резервация (CANCELLED)

- Само собственикът може да откаже → **403** иначе; `SYSTEM_ADMIN` прескача.
- `COMPLETED` резервации не могат да се откажат → **400**.
- Резервации с минал час не могат да се откажат → **400**.
- Статус → **`CANCELLED`**. Email `RESERVATION_CANCELLED` — async.

---

### 7. Разлика между CANCELLED и REJECTED

| Характеристика | `CANCELLED` | `REJECTED` |
|----------------|-------------|-----------|
| Кой го задава  | Клиентът (сам) | Ресторантът (RESTAURANT_ADMIN) |
| Как | `DELETE /api/reservations/{id}` | `PUT /api/reservations/{id}/status` с `{ "status": "REJECTED" }` |
| Значение | Доброволен отказ от клиента | Ресторантът не приема резервацията |
| Email | `RESERVATION_CANCELLED` | `RESERVATION_REJECTED` |

---

### 8. Смяна на статус от ресторанта

- `PUT /api/reservations/{id}/status` с `{ "status": "...", "note": "..." }`.
- Ownership проверка: `RESTAURANT_ADMIN` трябва да е собственик → **403** иначе. `SYSTEM_ADMIN` прескача.
- `CONFIRMED` → изпраща email `RESERVATION_CONFIRMED`.
- `REJECTED` → изпраща email `RESERVATION_REJECTED`.
- `COMPLETED`, `NO_SHOW` → без директен email (scheduler изпраща review покана след COMPLETED).
- Полето `note` не се записва в БД.

---

## REST API Endpoints

**Base URL:** `http://localhost:8083`

| Метод  | Път | Автентикация | Описание |
|--------|-----|:------------:|----------|
| POST   | `/api/reservations` | JWT (CLIENT) | Създаване на резервация |
| GET    | `/api/reservations/{id}` | JWT | Детайли по ID |
| GET    | `/api/reservations/my` | JWT (CLIENT) | Моите резервации |
| GET    | `/api/reservations/restaurant/{id}` | JWT (RESTAURANT_ADMIN / SYSTEM_ADMIN) | Резервации на ресторант |
| PUT    | `/api/reservations/{id}` | JWT (CLIENT / SYSTEM_ADMIN) | Редакция |
| PUT    | `/api/reservations/{id}/status` | JWT (RESTAURANT_ADMIN / SYSTEM_ADMIN) | Смяна на статус |
| DELETE | `/api/reservations/{id}` | JWT (CLIENT / SYSTEM_ADMIN) | Отказване → CANCELLED |
| GET    | `/api/reservations/restaurant/{id}/available-slots` | — | Само свободни слотове |
| GET    | `/api/reservations/restaurant/{id}/time-slots` | — | Всички слотове с `available` флаг |

---

### POST `/api/reservations`

```json
{
  "restaurantId": 1,
  "reservationDate": "2026-05-15",
  "reservationTime": "19:00",
  "guestsCount": 4,
  "preferredLocation": "INSIDE",
  "specialRequests": "Алергия към ядки",
  "customerName": "Иван Иванов",
  "customerPhone": "+359888123456",
  "customerEmail": "ivan@example.com"
}
```

**Response:** `201 Created` — `ReservationResponse` (включва `canCancel: true`, `canEdit: true`)
**Грешки:** `400` невалидни данни / RESTAURANT_ADMIN, `409` няма свободна маса

---

### PUT `/api/reservations/{id}`

```json
{
  "reservationDate": "2026-05-16",
  "reservationTime": "20:00",
  "guestsCount": 3,
  "specialRequests": "Предпочитаме тихо място"
}
```

**Response:** `200 OK` — обновен `ReservationResponse`
**Грешки:** `403` не е собственик, `400` краен статус, `409` няма свободна маса, `404` не съществува

---

### PUT `/api/reservations/{id}/status`

```json
{ "status": "CONFIRMED", "note": "Потвърдено от управителя" }
```

Налични стойности: `CONFIRMED`, `REJECTED`, `COMPLETED`, `NO_SHOW`

**Response:** `200 OK` — обновен `ReservationResponse`

---

### DELETE `/api/reservations/{id}`

**Response:** `204 No Content`
**Грешки:** `403` не е собственик, `400` COMPLETED или минал час, `404` не съществува

---

### GET `/api/reservations/restaurant/{id}/available-slots`

Query параметри: `date` (YYYY-MM-DD, задължително), `guestsCount` (задължително), `location` (незадължително).

```json
["11:00", "11:30", "12:00", "19:00", "19:30"]
```

---

### GET `/api/reservations/restaurant/{id}/time-slots`

Същите query параметри. Връща всички слотове — заетите с `available: false` (за disabled бутони в UI).

```json
[
  { "time": "11:00", "available": true },
  { "time": "19:00", "available": false }
]
```

---

## Модел на данните

### Таблица `reservations`

| Колона | Тип | Описание |
|--------|-----|----------|
| `id` | BIGINT (PK) | Авто-генериран |
| `user_id` | BIGINT | ID от User Service |
| `restaurant_id` | BIGINT | ID от Restaurant Service |
| `table_id` | BIGINT | ID на избраната маса |
| `reservation_date` | DATE | Дата |
| `reservation_time` | TIME | Час |
| `number_of_guests` | INTEGER | Брой гости (≥ 1) |
| `status` | VARCHAR | Enum стойност |
| `special_requests` | TEXT | Незадължително |
| `customer_name` | VARCHAR | Контактно лице |
| `customer_phone` | VARCHAR | Телефон |
| `customer_email` | VARCHAR | Email за нотификации |
| `two_hour_reminder_sent` | BOOLEAN | Дедупликация за 2ч reminder |
| `day_before_reminder_sent` | BOOLEAN | Дедупликация за ден-преди reminder |
| `review_request_sent` | BOOLEAN | Дедупликация за review покана |
| `created_at` | TIMESTAMP | Immutable |
| `updated_at` | TIMESTAMP | Последна промяна |

---

## Статуси на резервация

```
PENDING ──→ CONFIRMED ──→ COMPLETED
   │              └──→ NO_SHOW
   ├──→ CANCELLED  (от клиента)
   └──→ REJECTED   (от ресторанта)
```

| Статус | Описание | Кой го задава | Редактируем |
|--------|----------|--------------|:-----------:|
| `PENDING` | Начален | Система | Да |
| `CONFIRMED` | Потвърден | RESTAURANT_ADMIN | Да |
| `CANCELLED` | Отказан от клиента | CLIENT | Не |
| `REJECTED` | Отхвърлен от ресторанта | RESTAURANT_ADMIN | Не |
| `COMPLETED` | Приключило посещение | RESTAURANT_ADMIN или scheduler | Не |
| `NO_SHOW` | Клиентът не се е явил | Scheduler | Не |

---

## Автоматизирани задачи (Scheduler)

Всички jobs се деактивират от `application.yml` (виж конфигурацията по-долу).

| Job | Cron | Логика |
|-----|------|--------|
| 2ч reminder | `0 0,30 * * * *` | Резервации с час между `now+2h` и `now+3h`; флаг `twoHourReminderSent` |
| Ден-преди reminder | `0 0,30 * * * *` | Резервации за утре, при `createdAt < днес`, час == текущ час; флаг `dayBeforeReminderSent` |
| Auto-complete | `0 */30 * * * *` | `PENDING`/`CONFIRMED` с час ≤ `now - 3h` → `COMPLETED` |
| No-show | `0 15 * * * *` | `PENDING`/`CONFIRMED` с час ≤ `now - 1h` → `NO_SHOW` |
| Review покана | `0 0 10 * * *` | `COMPLETED` резервации от вчера → email с линк за оценка |

**Нулиране на reminder флагове:** при редакция на дата/час → `twoHourReminderSent` и `dayBeforeReminderSent` → `false`; `reviewRequestSent` не се нулира.

**Auto-complete vs No-show:** auto-complete (на :00 и :30) „печели" над no-show (на :15) — вече завършените резервации са `COMPLETED` и no-show job-ът ги пропуска.

```yaml
scheduler:
  reminders.enabled: true
  day-before-reminder.enabled: true
  review-request.enabled: true
  auto-complete:
    enabled: true
    hours-after: 3
  no-show:
    enabled: true
    hours-after: 1

notification.email.enabled: true
```

---

## Email нотификации

Всички emails са **асинхронни** (`@Async`).

| Тип | Кога | Шаблон |
|-----|------|--------|
| `RESERVATION_CREATED` | При успешно създаване | `reservation-created.html` |
| `RESERVATION_CONFIRMED` | Статус → CONFIRMED | `reservation-confirmed.html` |
| `RESERVATION_CANCELLED` | Клиентът откаже | `reservation-cancelled.html` |
| `RESERVATION_REJECTED` | Статус → REJECTED | `reservation-rejected.html` |
| `RESERVATION_REMINDER` | 2ч преди и ден преди (scheduler) | `reservation-reminder.html` |
| `REVIEW_REQUEST` | Следващия ден след COMPLETED (scheduler) | `review-request.html` |

**Локално тестване с MailHog:**
```bash
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
```
```yaml
# application-dev.yml
spring.mail.host: localhost
spring.mail.port: 1025
```
Inbox: http://localhost:8025

---

## Интеграция с restaurant-service

| Метод | Endpoint | Използва се за |
|-------|----------|---------------|
| `getRestaurantById(id)` | `GET /api/restaurants/{id}` | Работно време, ресторантско име |
| `isRestaurantOwner(id, uid)` | `GET /api/restaurants/{id}` | Ownership validation |
| `findAvailableTables(...)` | `GET /api/restaurants/{id}/tables` | Маси по капацитет и локация |

**Fallback:**
- Недостъпен при извличане на ресторантско ime → `"Ресторант #<id>"`
- Недостъпен при ownership check → **403** (безопасна страна)
- Недостъпен при извличане на маси → изключение

---

## Error handling

| Код | Изключение | Кога |
|-----|-----------|------|
| 400 | `InvalidReservationException` | Нарушено бизнес правило |
| 400 | `MethodArgumentNotValidException` | Невалидни входни данни |
| 403 | `UnauthorizedException` | Нямате права |
| 404 | `ReservationNotFoundException` | Резервацията не съществува |
| 409 | `TableNotAvailableException` | Няма свободна маса |

```json
// Стандартна грешка
{ "timestamp": "2026-04-22T10:30:00", "status": 404, "error": "Not Found",
  "message": "Резервация с ID 99 не е намерена", "path": "/api/reservations/99" }

// Validation грешка (400)
{ "reservationDate": "Датата трябва да е в бъдещето", "guestsCount": "Броят гости трябва да е поне 1" }
```
