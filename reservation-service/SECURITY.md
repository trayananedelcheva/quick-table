# Reservation Service - JWT Security

## 🔐 Имплементирани функции

### 1. JWT Authentication
- **JwtService** - Извлича информация от JWT токени (userId, username, role)
- **JwtAuthenticationFilter** - Автоматично валидира JWT токени и задава Authentication в Security Context
- **SecurityConfig** - Конфигурация на Spring Security за stateless JWT authentication
- **SecurityUtils** - Utility класа за достъп до информация за текущия потребител

### 2. Автоматично извличане на userId
Всички endpoint-и вече автоматично извличат `userId` и `userRole` от JWT токена:
- ❌ **Преди**: `POST /api/reservations?userId=1&userRole=CLIENT`
- ✅ **Сега**: `POST /api/reservations` (userId се извлича автоматично от токена)

### 3. Custom Exceptions
- `ReservationNotFoundException` - Резервация не е намерена (404)
- `TableNotAvailableException` - Маса не е налична (409 Conflict)
- `UnauthorizedException` - Неоторизиран достъп (403 Forbidden)
- `InvalidReservationException` - Невалидна резервация (400 Bad Request)

### 4. Security Checks
- ✅ Проверка дали CLIENT отменя само свои резервации
- ✅ RESTAURANT_ADMIN не може да прави резервации
- ⚠️ TODO: Проверка дали RESTAURANT_ADMIN има достъп само до свой ресторант

## 📋 Защитени Endpoints

Всички endpoint-и изискват валиден JWT токен в Header:
```
Authorization: Bearer <jwt_token>
```

### Client Endpoints
- `POST /api/reservations` - Създаване на резервация
- `GET /api/reservations/my` - Моите резервации
- `DELETE /api/reservations/{id}` - Отмяна на резервация

### Restaurant Admin Endpoints
- `GET /api/reservations/restaurant/{restaurantId}` - Резервации на ресторанта
- `PUT /api/reservations/{id}/status` - Промяна на статус

### Public Endpoints (не изискват JWT)
- `GET /api/reservations/restaurant/{restaurantId}/available-slots` - Налични часове

## 🔧 Следващи подобрения

1. **Ownership validation** - Проверка дали RESTAURANT_ADMIN е собственик на ресторанта
2. **Rate limiting** - Ограничаване на заявки
3. **Audit logging** - Логване на security евенти
4. **Role-based access control** - @PreAuthorize аннотации

## 🧪 Тестване

```bash
# Създаване на резервация с JWT токен
curl -X POST http://localhost:8083/api/reservations \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "reservationDate": "2026-03-15",
    "reservationTime": "19:00",
    "guestsCount": 4
  }'

# Извличане на моите резервации
curl -X GET http://localhost:8083/api/reservations/my \
  -H "Authorization: Bearer <jwt_token>"
```
