# Reservation Service — Документация 2.0

---

## 1. Обзор

`reservation-service` управлява целия жизнен цикъл на резервациите в системата Quick Table. Работи на порт **8083** и комуникира с `restaurant-service` (порт **8082**) за информация за ресторанти, маси и работно време.

### Какво прави сервизът

- Приема заявки за резервация от клиенти и автоматично избира подходяща маса
- Следи статуса на всяка резервация от създаването до приключването
- Генерира свободни часове за резервация според работното време на ресторанта
- Изпраща email нотификации при всяко събитие (създаване, потвърждение, отказ и др.)
- Автоматично маркира резервации като завършени или „неявил се" чрез scheduled jobs
- Изпраща напомняния на клиентите и покани за оценка след посещение

### Роли

| Роля               | Какво може                                                                                              |
|--------------------|---------------------------------------------------------------------------------------------------------|
| `CLIENT`           | Създава резервации, вижда само своите, редактира и отказва свои резервации                              |
| `RESTAURANT_ADMIN` | Вижда резервациите само на своя ресторант, потвърждава или отхвърля резервации. **Не може да прави резервации** |
| `SYSTEM_ADMIN`     | Пълен достъп — всички операции без ownership ограничения                                                |

Ролята се извлича от JWT токена при всяка заявка. Клиентът не я подава ръчно.

### Статуси на резервация

```
CONFIRMED ──→ COMPLETED
    │              └──→ NO_SHOW
    ├──→ CANCELLED   (от клиента)
    └──→ REJECTED    (от ресторанта)
```

| Статус      | Кой го задава                  | Описание                                    |
|-------------|--------------------------------|---------------------------------------------|
| `CONFIRMED` | Система (при създаване)        | Начален статус — резервацията е потвърдена  |
| `CANCELLED` | CLIENT                         | Клиентът доброволно е отказал               |
| `REJECTED`  | RESTAURANT_ADMIN               | Ресторантът е отхвърлил                     |
| `COMPLETED` | RESTAURANT_ADMIN или scheduler | Посещението е приключило                    |
| `NO_SHOW`   | Scheduler                      | Клиентът не се е явил                       |

---

## 2. Създаване на резервация

**Endpoint:** `POST /api/reservations`  
**Роля:** `CLIENT` или `SYSTEM_ADMIN` (`RESTAURANT_ADMIN` не може)

### Заявка

```json
{
  "restaurantId": 1,
  "reservationDate": "2026-06-15",
  "reservationTime": "19:30",
  "guestsCount": 3,
  "preferredLocation": "INDOOR",
  "customerName": "Иван Иванов",
  "customerEmail": "ivan@example.com",
  "customerPhone": "+359888123456",
  "specialRequests": "Детско столче"
}
```

| Поле                | Задължително | Описание                                 |
|---------------------|:------------:|------------------------------------------|
| `restaurantId`      | да           | ID на ресторанта                         |
| `reservationDate`   | да           | Дата (не в миналото)                     |
| `reservationTime`   | да           | Час — трябва да е поне 1 час от сега     |
| `guestsCount`       | да           | Брой гости (≥ 1)                         |
| `preferredLocation` | не           | `INDOOR`, `OUTDOOR`, `BAR` — по желание  |
| `customerName`      | да           | Три имена на клиента                     |
| `customerEmail`     | да           | Email за нотификации                     |
| `customerPhone`     | не           | Телефон за контакт                       |
| `specialRequests`   | не           | Допълнителни изисквания                  |

### Реализация

**Контролер:** `ReservationController.createReservation()`  
Извлича `userId` и `userRole` от JWT токена чрез `SecurityUtils`, след което делегира на сервиза.

**Сервиз:** `ReservationService.createReservation()`  
Изпълнява валидациите и избира маса чрез `findAvailableTable()`, след което записва резервацията.

**`ReservationService.findAvailableTable(restaurantId, date, time, guestsCount, preferredLocation)`**  
Централният private метод за избор на маса:

1. Взема от `restaurant-service` всички маси с `capacity >= guestsCount` и съответната `preferredLocation` (ако е посочена) — чрез `RestaurantServiceClient.findAvailableTables()`.
2. Взема от базата всички `CONFIRMED` резервации за същия ресторант и дата — чрез `ReservationRepository.findActiveReservationsForRestaurant()`.
3. За всяка кандидат-маса проверява дали е свободна за исканото време чрез `isTableOccupied()`.
4. От свободните маси избира случайна и връща нейното `tableId`.

**`ReservationService.isTableOccupied(tableId, requestedTime, existingReservations)`**  
Private метод в същия клас. Проверява дали дадена маса е заета за исканото време. Всяка резервация заема масата за **2 часа**. Конфликт има, когато два интервала `[T, T+2h)` се припокриват — т.е. нито единият не свършва преди или точно в началото на другия:

```
[резервация: T1, T1+2h)  и  [нова заявка: T2, T2+2h)
→ конфликт, ако NOT (T1+2h ≤ T2  OR  T2+2h ≤ T1)
```

### Бизнес правила

1. `RESTAURANT_ADMIN` не може да прави резервации — **400**.
2. Резервацията трябва да е поне **1 час напред** — **400**.
3. Ако няма свободна маса с достатъчен капацитет — **409**.
4. Резервацията се записва директно със статус `CONFIRMED`.
5. Клиентът получава email потвърждение.

### Отговор (201 Created)

```json
{
  "id": 42,
  "userId": 7,
  "restaurantId": 1,
  "tableId": 5,
  "reservationDate": "2026-06-15",
  "reservationTime": "19:30",
  "numberOfGuests": 3,
  "status": "CONFIRMED",
  "customerName": "Иван Иванов",
  "customerEmail": "ivan@example.com",
  "customerPhone": "+359888123456",
  "specialRequests": "Детско столче",
  "canCancel": true,
  "canEdit": true,
  "createdAt": "2026-05-03T10:15:00"
}
```

`canCancel` и `canEdit` са `true`, когато резервацията е в бъдещето и статусът е `CONFIRMED`.
