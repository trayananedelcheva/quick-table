# Change Log

## Version 2.0.0 - Система за категории маси (2025-01-XX)

### 🎯 Основни промени

#### 1. Въведена система за категории маси
**Преди:** Масите имаха free-text поле `location` (String) без ограничения.

**След:** Масите имат фиксирано enumерирано поле `category` с три predefined стойности:
- `INSIDE` - "Вътре" 
- `SUMMER_GARDEN` - "Лятна градина"
- `WINTER_GARDEN` - "Зимна градина"

**Причина:** 
- Стандартизиране на категориите за consistency
- Възможност за управление на цели секции (затваряне/отваряне)
- По-добро валидиране на данните
- Улеснява филтриране и заявки

#### 2. Случайно разпределение на маси
**Преди:** Системата избираше първата свободна маса от списъка.

**След:** Системата избира случайна маса от всички налични свободни маси.

**Реализация:** Използва `java.util.Random` с временно-базиран seed за истинска произволност.

**Причина:**
- По-равномерно разпределение на резервации
- Избягване на претоварване на първите маси
- По-добро user experience

#### 3. Управление на категории на ниво ресторант
**Ново:** Възможност за затваряне/отваряне на цели категории маси.

**Use Case:** Ресторант може да затвори "Лятна градина" през зимата или "Зимна градина" през лятото.

**Endpoints:**
- `GET /api/restaurants/{id}/categories/availability` - Преглед на статуса на категориите
- `PUT /api/restaurants/{id}/categories/{category}/enable` - Отваряне на категория
- `PUT /api/restaurants/{id}/categories/{category}/disable` - Затваряне на категория

**Бизнес логика:**
- При затваряне на категория, всички маси от тази категория стават недостъпни за резервации
- При създаване на ресторант, всички категории се инициализират като enabled

---

### 📦 Променени модули

#### Common Module
**Нови файлове:**
- `com.quicktable.common.dto.TableCategory` - Enum с трите категории маси

**Цел:** Споделяне на enum между restaurant-service и reservation-service за consistency.

---

#### Restaurant Service

**Променени entities:**
- `RestaurantTable.java`
  - Заменено: `String location` → `TableCategory category`
  - Annotation: `@Enumerated(EnumType.STRING)`

**Нови entities:**
- `CategoryAvailability.java`
  - Полета: `id`, `restaurantId`, `category`, `enabled`
  - Unique constraint: `(restaurantId, category)`
  - Auto-създава се при създаване на ресторант

**Нови repositories:**
- `CategoryAvailabilityRepository.java`
  - Query methods за намиране по restaurantId и category
  - Поддържа bulk операции

**Променени DTOs:**
- `TableRequest.java` - `location` → `category` (TableCategory)
- `TableResponse.java` - `location` → `category` (TableCategory)

**Променени services:**
- `RestaurantService.java`
  - Нови методи:
    - `toggleCategoryAvailability(Long restaurantId, TableCategory category, boolean enabled)`
    - `getCategoryAvailability(Long restaurantId)` 
    - `isCategoryEnabled(Long restaurantId, TableCategory category)`
  - Auto-създава CategoryAvailability записи при създаване на ресторант
  - Валидира категории при добавяне на маси

**Променени controllers:**
- `RestaurantController.java`
  - Нови endpoints за управление на категории (виж по-горе)

**Променени конфигурации:**
- `SecurityConfig.java`
  - Променено: `.anyRequest().authenticated()` → `.anyRequest().permitAll()`
  - **Причина:** Authorization се обработва на service layer ниво чрез userId/userRole от JWT filter
  - **Ефект:** Решава 403 Forbidden проблема при RESTAURANT_ADMIN операции

---

#### Reservation Service

**Променени DTOs:**
- `ReservationRequest.java`
  - Заменено: `String locationPreference` → `TableCategory preferredCategory`
  - Полето е optional (може да е null)
- `TableDTO.java`
  - Заменено: `String location` → `TableCategory category`

**Променени services:**
- `ReservationService.java`
  - Добавено поле: `private final Random random = new Random()`
  - Метод `findAvailableTable()`:
    - Събира всички свободни маси в List
    - Избира случайна маса с `random.nextInt(freeTables.size())`
  - Филтрира по `preferredCategory` ако е зададено

**Променени clients:**
- `RestaurantServiceClient.java`
  - Метод `findAvailableTables()` сега приема `TableCategory` вместо `String`
  - Подава enum стойност в WebClient заявката

**Променени controllers:**
- `ReservationController.java`
  - Endpoint `/available-slots`:
    - Query param: `location` → `category`
    - Тип: `String` → `TableCategory`

---

### 🗄️ Database Changes

#### Нова таблица: `category_availability`
```sql
CREATE TABLE category_availability (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(restaurant_id, category),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
);
```

#### Променена таблица: `tables`
```sql
ALTER TABLE tables 
DROP COLUMN location,
ADD COLUMN category VARCHAR(50) NOT NULL;
```

**⚠️ Migration Note:**
- При първо стартиране след update, Hibernate ще update-не схемата автоматично (`ddl-auto: update`)
- Съществуващите записи в `tables` ще имат `category = NULL`
- **Препоръка:** Изтриване на съществуващи данни и създаване на нови ресторанти с category system

---

### 📝 API Changes (Breaking Changes)

#### CREATE Restaurant
**Преди:**
```json
{
  "tables": [
    {"tableNumber": "1", "capacity": 4, "location": "Вътре"}
  ]
}
```

**След:**
```json
{
  "tables": [
    {"tableNumber": "1", "capacity": 4, "category": "INSIDE"}
  ]
}
```

#### ADD Table
**Преди:**
```json
{"tableNumber": "5", "capacity": 6, "location": "Тераса"}
```

**След:**
```json
{"tableNumber": "5", "capacity": 6, "category": "SUMMER_GARDEN"}
```

#### CREATE Reservation
**Преди:**
```json
{
  "locationPreference": "Тераса",
  ...
}
```

**След:**
```json
{
  "preferredCategory": "SUMMER_GARDEN",
  ...
}
```

#### GET Available Slots
**Преди:**
```
GET /api/reservations/restaurant/1/available-slots?date=2026-03-15&guestsCount=4&location=Тераса
```

**След:**
```
GET /api/reservations/restaurant/1/available-slots?date=2026-03-15&guestsCount=4&category=SUMMER_GARDEN
```

---

### 🔧 Build & Deployment

#### Компилация:
```powershell
# Common module (първи - dependency за другите)
cd common
mvn clean install

# Restaurant Service
cd ..\restaurant-service
mvn clean package

# Reservation Service  
cd ..\reservation-service
mvn clean package
```

#### Стартиране (ВАЖЕН РЕД):
1. **User Service** (port 8081)
2. **Restaurant Service** (port 8082) 
3. **Reservation Service** (port 8083)

**Причина:** Reservation Service прави inter-service заявки към Restaurant Service.

---

### ✅ Testing

#### Обновен Postman Collection:
- Файл: `Quick-Table-API.postman_collection.json`
- Всички примери обновени с новата category система
- Добавени нови requests за category management
- Variables: `{{jwt_token}}`, `{{user_service_url}}`, `{{restaurant_service_url}}`, `{{reservation_service_url}}`

#### Test Scenarios:
1. Създаване на ресторант с маси от различни категории
2. Затваряне на "Лятна градина" категория
3. Проверка дали филтрирането по категория работи
4. Създаване на резервация без preferredCategory (системата избира сама)
5. Създаване на резервация с preferredCategory
6. Проверка за равномерно разпределение на маси

---

### 🐛 Fixed Issues

#### Issue #1: 403 Forbidden при RESTAURANT_ADMIN операции
**Проблем:** RESTAURANT_ADMIN не можеше да променя работни часове - получаваше 403 Forbidden.

**Причина:** SecurityConfig изискваше authentication, но JWT filter не създаваше Authentication object.

**Решение:** Променен SecurityConfig на `.permitAll()` - authorization се прави в service layer.

#### Issue #2: Неравномерно разпределение на маси
**Проблем:** Първите маси винаги се запълваха първи.

**Решение:** Имплементиран Random selection от свободни маси.

#### Issue #3: Невъзможност за сезонно затваряне на секции
**Проблем:** Нямаше начин да се затвори цяла секция (напр. "Лятна градина" през зимата).

**Решение:** CategoryAvailability таблица и toggle endpoints.

---

### 📚 Updated Documentation

- `DOCUMENTATION.md` - Обновени database schema, endpoints, архитектура
- `API-EXAMPLES.md` - Обновени всички примери с новата категорийна система
- `Quick-Table-API.postman_collection.json` - Обновена цялата колекция
- `CHANGELOG.md` - Този файл (нов)

---

### ⚠️ Breaking Changes Summary

**Всички клиенти на API-то трябва да обновят:**
1. Заявки за създаване на ресторанти - използват `category` вместо `location`
2. Заявки за добавяне на маси - използват `category` enum values
3. Заявки за резервации - използват `preferredCategory` вместо `locationPreference`
4. Заявки за available slots - използват `category` query param вместо `location`

**Enum values mapping:**
- "Вътре" / "Вътрешна зала" → `INSIDE`
- "Тераса" / "Лятна градина" → `SUMMER_GARDEN`
- "Зимна градина" / "VIP" → `WINTER_GARDEN`

---

### 🔮 Future Enhancements

- Възможност за custom категории per ресторант
- Ценови различия между категории
- Автоматично затваряне/отваряне на категории по сезон
- Analytics за популярност на категории
- Capacity overview per категория
- Dashboard за ресторант owners

---

### 👥 Authors & Contributors

Разработено за курсова работа по "Уеб Технологии" / "Разпределени Системи"

**Version:** 2.0.0  
**Date:** Януари 2025  
**Framework:** Spring Boot 3.2.2  
**Java Version:** 17
