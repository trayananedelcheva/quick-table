# Restaurant Integration

## ✅ Имплементирано

### 1. Restaurant Name in Notifications
**Преди:**
- Emails показваха "Ресторант #1", "Ресторант #2"

**Сега:**
- ✅ Извлича реално име от `restaurant-service`
- ✅ Fallback към "Ресторант #ID" при грешка
- ✅ Работи във всички email templates

### 2. Restaurant Ownership Validation
**Функция:**
- Проверява дали RESTAURANT_ADMIN е собственик на ресторанта
- Блокира достъп до резервации на чужди ресторанти

**Endpoints защитени:**
- `GET /api/reservations/restaurant/{id}` - Резервации на ресторант
- `GET /api/reservations/restaurant/{id}?status=CONFIRMED` - Резервации по статус

**Behavior:**
- Ако admin не е собственик → `403 Forbidden`
- Error message: "Нямате права да преглеждате резервациите на този ресторант"

## 🔧 Implementation

### RestaurantServiceClient
**Нови методи:**

```java
// Извлича информация за ресторант
RestaurantDTO getRestaurantById(Long restaurantId)

// Проверява собственост
boolean isRestaurantOwner(Long restaurantId, Long userId)
```

**Error Handling:**
- Graceful fallback при недостъпен restaurant-service
- Връща "Ресторант #ID" ако service е down
- Логва warnings вместо да хвърля exceptions

### RestaurantDTO
Нов DTO за ресторантна информация:
```java
public class RestaurantDTO {
    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private String email;
    private String description;
    private Long ownerId;  // За ownership validation
}
```

## 📊 Integration Flow

### Restaurant Name Fetching
```
1. User създава резервация
2. ReservationService.sendReservationCreatedNotification()
3. → getRestaurantName(restaurantId)
4. → RestaurantServiceClient.getRestaurantById()
5. → WebClient GET /api/restaurants/{id}
6. → restaurant-service response
7. → Extract restaurant.name
8. → Use in email template
```

### Ownership Validation
```
1. Admin GET /api/reservations/restaurant/5
2. ReservationController извлича userId от JWT
3. → ReservationService.getRestaurantReservations(5, adminUserId)
4. → isRestaurantOwner(5, adminUserId)
5. → RestaurantServiceClient.isRestaurantOwner()
6. → WebClient GET /api/restaurants/5
7. → Check if restaurant.ownerId == adminUserId
8. → If NO → throw UnauthorizedException (403)
9. → If YES → return reservations
```

## 🧪 Testing

### Test Restaurant Name
```bash
# 1. Ensure restaurant-service is running on port 8082
# 2. Create a restaurant
curl -X POST http://localhost:8082/api/restaurants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "La Bella Vita",
    "address": "Sofia, Bulgaria",
    "phoneNumber": "+359888123456",
    "email": "info@labellavita.bg"
  }'

# 3. Create reservation for that restaurant
# 4. Check email - should show "La Bella Vita" not "Ресторант #1"
```

### Test Ownership Validation
```bash
# 1. User 1 creates restaurant (becomes owner)
# 2. User 2 tries to access reservations (should fail)

# As User 2 (NOT owner):
curl -X GET http://localhost:8083/api/reservations/restaurant/1 \
  -H "Authorization: Bearer <user2-jwt-token>"

# Expected: 403 Forbidden
# Response: {"message": "Нямате права да преглеждате резервациите на този ресторант"}

# As User 1 (owner):
curl -X GET http://localhost:8083/api/reservations/restaurant/1 \
  -H "Authorization: Bearer <user1-jwt-token>"

# Expected: 200 OK with reservations list
```

## ⚠️ Important Notes

1. **Service Dependency:**
   - reservation-service зависи от restaurant-service
   - Ако restaurant-service е down → fallback поведение
   - Emails все още се изпращат (с generic име)

2. **Performance:**
   - Всяко notification извиква restaurant-service
   - Consider caching restaurant names
   - Current: No caching (fresh data)

3. **Security:**
   - Ownership се проверява на API level
   - Database constraints НЕ гарантират ownership
   - Rely on restaurant-service за truth

## 🔮 Future Improvements

1. **Caching Restaurant Data**
   ```java
   @Cacheable(value = "restaurants", key = "#restaurantId")
   public RestaurantDTO getRestaurantById(Long restaurantId)
   ```
   - Cache с TTL: 1 час
   - Invalidate при update на ресторант

2. **Async Restaurant Name Fetching**
   - Fetch restaurant name асинхронно
   - Don't block notification sending

3. **Batch Restaurant Fetching**
   - Fetch multiple restaurants в 1 request
   - Optimize for scheduler jobs

4. **Circuit Breaker**
   - Resilience4j integration
   - Fallback при repeated failures

5. **Restaurant Info in ReservationResponse**
   - Include restaurant name directly
   - Avoid frontend calling restaurant-service

## 📝 Files Modified

1. **RestaurantServiceClient.java** - Added 2 new methods
2. **RestaurantDTO.java** - NEW DTO class
3. **ReservationService.java** - Restaurant name & ownership
4. **ReservationScheduler.java** - Restaurant name in reminders
5. **CHANGELOG.md** - Updated with new features

## ✅ Benefits

- **Better UX:** Real restaurant names in emails
- **Security:** Proper authorization checks
- **Maintainability:** Centralized restaurant data
- **Reliability:** Graceful degradation when service is down
