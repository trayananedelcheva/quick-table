# CHANGELOG - Category → Location Refactoring

## Version 3.0.0 - 2026-03-03

### 🔄 MAJOR REFACTORING: Category → Location

**Основна промяна:** Преименуване на "category" на "location" в цялата система

#### Breaking Changes

1. **API Endpoints**
   - `PUT /restaurants/{id}/categories/{category}/toggle` → `PUT /restaurants/{id}/locations/{location}/toggle`
   - `GET /restaurants/{id}/categories` → `GET /restaurants/{id}/locations`
   - Query parameter: `?category=INSIDE` → `?location=INSIDE`

2. **JSON Request/Response Fields**
   - `category` → `location` във всички DTO класове
   - `preferredCategory` → `preferredLocation` в ReservationRequest
   - `tables[]` → `locations{}` в RestaurantResponse (структурна промяна)

3. **Database Schema**
   - Таблица: `category_availability` → `location_availability`
   - Колона: `restaurant_tables.category` → `restaurant_tables.location`
   - Колона: `location_availability.category` → `location_availability.location`

#### New JSON Response Structure

**СТАРО:**
```json
{
  "id": 8,
  "tables": [
    {"tableNumber": "1", "location": "INSIDE"},
    {"tableNumber": "2", "location": "SUMMER_GARDEN"},
    {"tableNumber": "3", "location": "INSIDE"}
  ]
}
```

**НОВО - Групиран и по location:**
```json
{
  "id": 8,
  "locations": {
    "INSIDE": {
      "displayName": "Вътре",
      "enabled": true,
      "tables": [
        {"tableNumber": "1", "capacity": 4},
        {"tableNumber": "3", "capacity": 2}
      ]
    },
    "SUMMER_GARDEN": {
      "displayName": "Лятна градина",
      "enabled": false,
      "tables": [ {"tableNumber": "2", "capacity": 6} ]
    },
    "WINTER_GARDEN": {
      "displayName": "Зимна градина",
      "enabled": true,
      "tables": []
    }
  }
}
```

#### Code Changes

**Java Classes:**
- `TableCategory enum` → `TableLocation enum`
- `CategoryAvailability entity` → `LocationAvailability entity`
- `CategoryAvailabilityRepository` → `LocationAvailabilityRepository`
- `CategoryAvailabilityResponse DTO` → `LocationAvailabilityResponse DTO`
- **NEW:** `LocationGroupResponse DTO` - за групирани маси

**Service Methods:**
- `toggleCategoryAvailability()` → `toggleLocationAvailability()`
- `getCategoryAvailability()` → `getLocationAvailability()`
- `isCategoryEnabled()` → `isLocationEnabled()`

#### Migration Script

Изпълни: `psql -U postgres -f migrate-category-to-location.sql`

Скриптът автоматично:
- Преименува всички колони
- Преименува таблицата
- Запазва всички съществуващи данни
- Запазва foreign key constraints

#### Testing

Всички модули компилирани успешно:
- ✅ common-1.0.0.jar
- ✅ restaurant-service-1.0.0.jar
- ✅ reservation-service-1.0.0.jar

#### Affected Files

**Backend:**
- `common/src/main/java/com/quicktable/common/dto/TableLocation.java` (renamed from TableCategory)
- `restaurant-service/.../entity/LocationAvailability.java` (renamed)
- `restaurant-service/.../repository/LocationAvailabilityRepository.java` (renamed)
- `restaurant-service/.../dto/LocationAvailabilityResponse.java` (renamed)
- `restaurant-service/.../dto/LocationGroupResponse.java` (NEW)
- `restaurant-service/.../dto/RestaurantResponse.java` (структурна промяна)
- `restaurant-service/.../service/RestaurantService.java` (методи променени)
- `restaurant-service/.../controller/RestaurantController.java` (endpoints променени)
- `reservation-service/.../dto/ReservationRequest.java` (`preferredCategory` → `preferredLocation`)
- `reservation-service/.../dto/TableDTO.java` (field rename)
- `reservation-service/.../service/ReservationService.java` (параметри променени)
- `reservation-service/.../controller/ReservationController.java` (query param от готово)
- `reservation-service/.../client/RestaurantServiceClient.java` (параметри променени)

**Database:**
- `migrate-category-to-location.sql` (NEW migration script)

**Documentation:**
- Всички API документи трябва да се update-нат с новите endpoint names
- Postman коле това трябва да се update-не

---

## Upgrade Instructions

1. Изпълни migration script:
   ```bash
   psql -U postgres -f migrate-category-to-location.sql
   ```

2. Restart всички services:
   ```bash
   mvn spring-boot:run
   ```

3. Update Postman collection:
   - `/categories/` → `/locations/`
   - `category` query params → `location`

4. Update всички клиенти да използват нов JSON формат с `locations` вместо `tables`
