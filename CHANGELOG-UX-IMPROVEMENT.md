# Улучшен UX за резервации - Changelog

## Дата: 24 Февруари 2026

### Проблем (преди)
CLIENT трябваше да:
1. Вижда всички маси в ресторанта
2. Вижда номера на масите
3. Вижда къде се намират (вътре/тераса/прозорец)
4. Вижда капацитета на всяка маса
5. **Ръчно да избира** конкретна маса

Това е **технически изглед**, а не потребителски!

---

## Решение (сега)

### Нов UX Workflow за CLIENT

CLIENT вижда само:
1. Ресторанти
2. **Избира брой гости** (dropdown 1-10)
3. **Избира дата** (calendar picker)
4. Системата показва **само свободни часове** as chips/buttons
5. Избира час
6. Резервацията се създава **автоматично** (системата избира подходяща маса)

---

## Технически промени

### 1. Inter-service комуникация (WebClient)

**Проблем:** `reservation-service` НЕ знае кои маси има ресторантът и техния капацитет.

**Решение:** WebClient за REST комуникация между микросервизите.

**Нови файлове:**
```
reservation-service/
├── config/
│   └── WebClientConfig.java        // WebClient bean конфигурация
├── client/
│   └── RestaurantServiceClient.java // Client за извикване на restaurant-service
└── dto/
    └── TableDTO.java                 // DTO за маса от restaurant-service
```

**WebClientConfig.java:**
```java
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient restaurantServiceWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://localhost:8082") // Restaurant Service URL
                .build();
    }
}
```

**RestaurantServiceClient.java:**
```java
@Service
public class RestaurantServiceClient {
    private final WebClient restaurantServiceWebClient;
    
    public List<TableDTO> getRestaurantTables(Long restaurantId) {
        return restaurantServiceWebClient.get()
                .uri("/api/restaurants/{id}/tables", restaurantId)
                .retrieve()
                .bodyToFlux(TableDTO.class)
                .collectList()
                .block();
    }
    
    public List<TableDTO> findAvailableTables(Long restaurantId, 
                                              Integer guestsCount, 
                                              String location) {
        return getRestaurantTables(restaurantId).stream()
                .filter(t -> t.getAvailable())
                .filter(t -> t.getCapacity() >= guestsCount)
                .filter(t -> location == null || "ANY".equals(location) || 
                        t.getLocation().equalsIgnoreCase(location))
                .toList();
    }
}
```

**ReservationService.java:**
```java
@Service
public class ReservationService {
    private final RestaurantServiceClient restaurantServiceClient; // Inject-ваме client
    
    private Long findAvailableTable(Long restaurantId, LocalDate date, LocalTime time,
                                     Integer guestsCount, String locationPreference) {
        // Извличаме налични маси от restaurant-service
        List<TableDTO> availableTables = restaurantServiceClient.findAvailableTables(
                restaurantId, guestsCount, locationPreference);
        
        // Проверяваме коя маса е свободна за избрания час
        for (TableDTO table : availableTables) {
            if (!isTableOccupied(table.getId(), time, existingReservations)) {
                return table.getId(); // Намерихме свободна маса!
            }
        }
        
        return null; // Няма свободни маси
    }
}
```

**Комуникация:**
```
CLIENT → reservation-service:8083 → restaurant-service:8082
        POST /reservations             GET /restaurants/{id}/tables
```

---

### 2. Location Preference (Избор на локация)

**Нова функционалност:** CLIENT може да избере локация на масата.

**Промени в ReservationRequest.java:**
```java
@Data
public class ReservationRequest {
    private Integer guestsCount;
    
    // НОВО: Предпочитание за локация
    private String locationPreference; // "Вътре", "Тераса", "Градина", "ANY", null
}
```

**Endpoint с location филтър:**
```
GET /api/reservations/restaurant/1/available-slots
    ?date=2026-03-15
    &guestsCount=4
    &location=Тераса  // НОВО!
```

**Response (само часове с налична маса на терасата):**
```json
["10:00:00", "14:00:00", "19:00:00", "20:00:00"]
```

**Логика:**
1. CLIENT избира "Тераса"
2. `reservation-service` извиква `restaurant-service` за всички маси
3. Филтрира само маси с `location == "Тераса"`
4. Проверява кои часове са свободни
5. Връща само часовете с налична маса на терасата

---

### 3. Нов endpoint: Свободни времеви слотове

**Restaurant Service:**
```
GET /api/restaurants/{id}/available-time-slots?date=2026-03-15&guestsCount=4
```

Връща списък с часове според работното време на ресторанта (напр. 10:00-23:00).

**Reservation Service:**
```
GET /api/reservations/restaurant/{id}/available-slots?date=2026-03-15&guestsCount=4
```

Филтрира заетите часове и връща само **свободни**:
```json
["10:00:00", "11:00:00", "14:00:00", "19:00:00", "20:00:00"]
```

### 2. Автоматичен избор на маса

**Промени в ReservationRequest.java:**
```java
// ПРЕДИ:
@NotNull
private Long tableId; // Задължително

// СЕГА:
private Long tableId; // Optional - системата избира автоматично
```

**Промени в ReservationService.java:**
```java
@Transactional
public ReservationResponse createReservation(Long userId, ReservationRequest request) {
    // АКО tableId НЕ Е ПОДАДЕНО, автоматично избираме подходяща маса
    if (request.getTableId() == null) {
        Long autoSelectedTableId = findAvailableTable(
                request.getRestaurantId(),
                request.getReservationDate(),
                request.getReservationTime(),
                request.getGuestsCount()
        );
        if (autoSelectedTableId == null) {
            throw new RuntimeException("Няма налична маса за избраното време и брой гости");
        }
        request.setTableId(autoSelectedTableId);
    }
    
    // Продължи с резервацията...
}
```

### 3. Премахване на `numberOfGuests` → `guestsCount`

За консистентност, преименувахме:
- `numberOfGuests` → `guestsCount`

---

## Сравнение преди/след

### Преди (стар UX):

```
1. GET /api/restaurants → Списък с ресторанти
2. GET /api/restaurants/1/tables → Вижда всички маси:
   [
     { id: 1, tableNumber: "A1", capacity: 4, location: "Вътре" },
     { id: 2, tableNumber: "A2", capacity: 2, location: "До прозорец" },
     { id: 3, tableNumber: "T1", capacity: 6, location: "Тераса" }
   ]
3. CLIENT избира маса с id=1
4. POST /api/reservations?userId=3
   Body: { restaurantId: 1, tableId: 1, date, time, numberOfGuests: 4 }
```

**Проблеми:**
- CLIENT вижда технически детайли (номера, локации)
- Трябва сам да оцени дали масата му подхожда
- Може да избере маса за 6 души, а са само 2 гости (неефективно)

---

### Сега (нов UX):

```
1. GET /api/restaurants → Списък с ресторанти
2. CLIENT избира брой гости: 4
3. CLIENT избира дата: 2026-03-15
4. GET /api/reservations/restaurant/1/available-slots?date=2026-03-15&guestsCount=4
   Отговор: ["10:00:00", "11:00:00", "14:00:00", "19:00:00", "20:00:00"]
5. CLIENT избира час: 19:00
6. POST /api/reservations?userId=3&userRole=CLIENT
   Body: { restaurantId: 1, date: "2026-03-15", time: "19:00", guestsCount: 4 }
7. Системата автоматично намира подходяща маса (capacity >= 4)
8. Резервацията е завършена!
```

**Предимства:**
- ✅ CLIENT НЕ вижда технически детайли
- ✅ Системата оптимално разпределя маси според капацитет
- ✅ По-прост и интуитивен process
- ✅ По-малко грешки от потребителска страна

---

## UI Mockup

### Стар UX (deprecated):
```
┌─────────────────────────────────────────────────────┐
│  Избери маса:                                       │
├─────────────────────────────────────────────────────┤
│  ☐ Маса A1 (4 места, Вътре)                        │
│  ☐ Маса A2 (2 места, До прозорец)                  │
│  ☐ Маса T1 (6 места, Тераса)                       │
│                                                     │
│  Дата: [________]  Час: [________]                 │
│  Брой гости: [____]                                │
│                                                     │
│                    [Резервирай]                     │
└─────────────────────────────────────────────────────┘
```

**Проблем:** Твърде много информация, технически naming (A1, T1)

---

### Нов UX:
```
┌─────────────────────────────────────────────────────┐
│  Ресторант Копитото                                 │
│  📍 ул. Витоша 15, София                            │
│  🕐 Работно време: 10:00 - 23:00                    │
├─────────────────────────────────────────────────────┤
│  За колко души?                                     │
│  [  1 ▼]  [  2  ]  [  3  ]  [ ★4★ ]  [  5  ]      │
│                                                     │
│  Локация:                                           │
│  ( ) Вътре   (●) Тераса   ( ) Без значение         │
│                                                     │
│  Дата:                                              │
│  📅 [15 Март 2026 ▼]                                │
│                                                     │
│  Избери час:                                        │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐                │
│  │10:00│ │11:00│ │14:00│ │19:00│ │20:00│            │
│  └────┘ └────┘ └────┘ └────┘ └────┘                │
│  (Само часове със свободна маса на терасата)        │
│                                                     │
│  Специални желания (опционално):                    │
│  [_____________________________________________]    │
│                                                     │
│                    [Резервирай]                     │
└─────────────────────────────────────────────────────┘
```

**Предимства:** 
- Чисто, минималистично
- Избор на локация (Вътре/Тераса/Без значение)
- Само свободни часове за избраната локация
- CLIENT контролира къде иска да седи, без да вижда техническите детайли
- Inter-service комуникация: reservation-service → restaurant-service

---

## Бъдещи подобрения

### 1. ✅ Интеграция между микросервизите (ЗАВЪРШЕНО)
~~Понастоящем `findAvailableTable()` в `ReservationService` връща `null`, защото не може да извика `restaurant-service` за да вземе маси.~~

**✅ ИМПЛЕМЕНТИРАНО:** WebClient за inter-service комуникация между reservation-service и restaurant-service вече работи!

**Имплементация:**
- `WebClientConfig` - конфигурира WebClient bean
- `RestaurantServiceClient` - извиква `GET /api/restaurants/{id}/tables`
- `ReservationService.findAvailableTable()` - използва client за да вземе налични маси
- Филтриране по capacity, availability, и location preference

```java
@Service
public class RestaurantServiceClient {
    private final WebClient restaurantServiceWebClient;
    
    public List<TableDTO> findAvailableTables(Long restaurantId, 
                                              Integer guestsCount, 
                                              String location) {
        List<TableDTO> allTables = getRestaurantTables(restaurantId);
        
        return allTables.stream()
                .filter(table -> table.getAvailable())
                .filter(table -> table.getCapacity() >= guestsCount)
                .filter(table -> location == null || "ANY".equals(location) || 
                        table.getLocation().equalsIgnoreCase(location))
                .toList();
    }
}
```
    private final WebClient webClient;
    
    public RestaurantServiceClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://localhost:8082").build();
    }
    
    public List<TableDTO> getAvailableTables(Long restaurantId, Integer guestsCount) {
        return webClient.get()
                .uri("/api/restaurants/{id}/tables", restaurantId)
                .retrieve()
                .bodyToFlux(TableDTO.class)
                .filter(table -> table.getCapacity() >= guestsCount)
                .filter(TableDTO::isAvailable)
                .collectList()
                .block();
    }
}
```

### 2. Интелигентно разпределение на маси
Понастоящем избираме първата свободна маса с достатъчен капацитет.

**Подобрение:** Алгоритъм за оптимално разпределение:
- За 2 гости → Предпочитай маси за 2-4 места
- За 4 гости → Предпочитай маси за 4-6 места
- За 8 гости → Само маси за 6+ места

Така максимизираме броя резервации и не "пилеем" големи маси за малко гости.

### 3. Real-time наличност
Добави WebSocket за real-time обновяване на свободни часове:
```javascript
const ws = new WebSocket('ws://localhost:8083/availability');
ws.onmessage = (event) => {
    const availableSlots = JSON.parse(event.data);
    updateUI(availableSlots);
};
```

---

## Резюме

### Какво се промени:
1. ✅ **Премахнахме** видимостта на маси за CLIENT
2. ✅ **Добавихме** endpoint за свободни времеви слотове с location филтър
3. ✅ **Автоматизирахме** избора на маса според guestsCount и locationPreference
4. ✅ **Имплементирахме** inter-service комуникация (WebClient) между reservation-service и restaurant-service
5. ✅ **Добавихме** location preference - CLIENT избира "Вътре", "Тераса" или "Без значение"
6. ✅ **Опростихме** UX - само 5 стъпки (гости → локация → дата → час → резервирай)
7. ✅ **Документирахме** промените в API-EXAMPLES.md, UI-PLAN.md, ROLE-BASED-BUSINESS-RULES.md, README.md

### Какво е ЗАВЪРШЕНО:
- ✅ Inter-service communication с WebClient за `findAvailableTable()` 
- ✅ RestaurantServiceClient за извикване на restaurant-service от reservation-service
- ✅ Филтриране по capacity, availability И location
- ✅ Автоматично избиране на първата свободна маса с подходящи параметри

### Какво остава да се направи (бъдещи подобрения):
- 🔄 Интелигентен алгоритъм за оптимално разпределение (предпочитай маси близки до guestsCount)
- 🔄 Real-time updates с WebSocket
- 🔄 Frontend имплементация (React/Vue)
- 🔄 Кеширане на налични маси за по-добра performance

---

## Приоритет: ⭐⭐⭐⭐⭐ (Критично за UX)

Тази промяна значително подобрява потребителското изживяване и прави системата по-интуитивна за крайните потребители.

**Архитектурна стойност:** Демонстрира **inter-service комуникация** в микросервизна архитектура чрез WebClient.
