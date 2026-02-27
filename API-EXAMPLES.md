# Quick Table - API Тестове с Postman/cURL

## 1. User Service (port 8081)

### Регистрация на потребител
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "client@example.com",
    "password": "password123",
    "firstName": "Иван",
    "lastName": "Иванов",
    "phoneNumber": "0888123456"
  }'
```

**Забележка:** Всички нови потребители се регистрират като **CLIENT**. Само съществуващ SYSTEM_ADMIN може да повиши роля.

### Първоначален SYSTEM_ADMIN
Първият администратор се създава чрез SQL скрипт:
```bash
psql -U postgres -f database-setup.sql
```

Email: `admin@quicktable.com`  
Password: `admin123` (трябва да се генерира BCrypt hash)

### Вход (Login)
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@quicktable.com",
    "password": "admin123"
  }'
```

**Важно:** Копирай JWT токена от отговора за следващите заявки!

Отговор:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "admin@quicktable.com",
  "firstName": "Админ",
  "lastName": "Администратор",
  "role": "SYSTEM_ADMIN"
}
```

### Профил на текущия потребител
```bash
curl -X GET http://localhost:8081/api/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Промяна на роля на потребител (само SYSTEM_ADMIN)
```bash
curl -X PUT "http://localhost:8081/api/users/2/role?role=RESTAURANT_ADMIN" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

**Възможни роли:**
- `CLIENT` - Обикновен потребител
- `RESTAURANT_ADMIN` - Администратор на ресторант
- `SYSTEM_ADMIN` - Системен администратор

**Стъпки за създаване на RESTAURANT_ADMIN:**
1. Потребителят се регистрира като CLIENT
2. SYSTEM_ADMIN повишава ролята му до RESTAURANT_ADMIN
3. RESTAURANT_ADMIN получава `adminUserId` при създаване на ресторант

---

## 2. Restaurant Service (port 8082)

### Създаване на ресторант (с автоматично геокодиране)
```bash
curl -X POST http://localhost:8082/api/restaurants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ресторант Копитото",
    "description": "Традиционна българска кухня с модерен акцент",
    "address": "ул. Витоша 15, София, България",
    "phone": "+359 2 123 4567",
    "email": "info@kopitoto.bg",
    "openingTime": "10:00:00",
    "closingTime": "23:00:00",
    "adminUserId": 1,
    "tables": [
      {
        "tableNumber": "1",
        "capacity": 4,
        "location": "Вътре"
      },
      {
        "tableNumber": "2",
        "capacity": 2,
        "location": "До прозорец"
      },
      {
        "tableNumber": "3",
        "capacity": 6,
        "location": "Тераса"
      }
    ]
  }'
```

**Забележка:** Адресът ще бъде автоматично геокодиран чрез **OpenStreetMap Nominatim API** (външна услуга) и ще се попълнят latitude, longitude, city и country.

### Списък на всички ресторанти
```bash
curl -X GET http://localhost:8082/api/restaurants
```

### Търсене по град
```bash
curl -X GET "http://localhost:8082/api/restaurants?city=София"
```

### Детайли за ресторант
```bash
curl -X GET http://localhost:8082/api/restaurants/1
```

### Добавяне на маса към ресторант
```bash
curl -X POST http://localhost:8082/api/restaurants/1/tables \
  -H "Content-Type: application/json" \
  -d '{
    "tableNumber": "4",
    "capacity": 8,
    "location": "Зала за събития"
  }'
```

### Маси на ресторант (само за RESTAURANT_ADMIN)
```bash
curl -X GET http://localhost:8082/api/restaurants/1/tables
```

**Забележка:** CLIENT НЕ вижда маси. Вместо това вижда само свободни часове.

### Виж свободни часове за резервация (CLIENT)
```bash
# Всички локации
curl -X GET "http://localhost:8083/api/reservations/restaurant/1/available-slots?date=2026-03-15&guestsCount=4&location=ANY"

# Само Тераса
curl -X GET "http://localhost:8083/api/reservations/restaurant/1/available-slots?date=2026-03-15&guestsCount=4&location=Тераса"

# Само Вътре
curl -X GET "http://localhost:8083/api/reservations/restaurant/1/available-slots?date=2026-03-15&guestsCount=4&location=Вътре"
```

**Отговор:**
```json
["10:00:00", "11:00:00", "12:00:00", "14:00:00", "15:00:00", "19:00:00", "20:00:00", "21:00:00"]
```

**Как работи:**
1. CLIENT избира ресторант
2. Избира брой гости (напр. 4)
3. **Избира локация** ("Вътре", "Тераса", "ANY")
4. Избира дата (напр. 2026-03-15)
5. Системата връща само свободни часове за избраната локация
6. CLIENT избира час и резервира

**Предимства:**
- CLIENT не вижда номера на маси
- Не вижда къде се намират масите (номера/детайли)
- Вижда само свободни часове за предпочитаната локация
- По-прост и по-интуитивен UX

---

## 3. Reservation Service (port 8083)

### Създаване на резервация (нов UX - без tableId)
```bash
# Резервация на тераса
curl -X POST "http://localhost:8083/api/reservations?userId=1&userRole=CLIENT" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "reservationDate": "2026-03-15",
    "reservationTime": "19:00:00",
    "guestsCount": 4,
    "locationPreference": "Тераса",
    "specialRequests": "Алергия към ядки",
    "customerName": "Иван Иванов",
    "customerPhone": "0888123456",
    "customerEmail": "ivan@example.com"
  }'

# Резервация вътре
curl -X POST "http://localhost:8083/api/reservations?userId=1&userRole=CLIENT" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "reservationDate": "2026-03-15",
    "reservationTime": "19:00:00",
    "guestsCount": 4,
    "locationPreference": "Вътре",
    "customerName": "Иван Иванов",
    "customerPhone": "0888123456",
    "customerEmail": "ivan@example.com"
  }'

# Без предпочитание (системата избира автоматично)
curl -X POST "http://localhost:8083/api/reservations?userId=1&userRole=CLIENT" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "reservationDate": "2026-03-15",
    "reservationTime": "19:00:00",
    "guestsCount": 4,
    "customerName": "Иван Иванов",
    "customerPhone": "0888123456",
    "customerEmail": "ivan@example.com"
  }'
```

**Важно:** 
- `tableId` НЕ се подава - системата автоматично избира подходяща маса
- `locationPreference` е опционално - "Вътре", "Тераса", "Градина" или null
- Системата намира маса с капацитет >= `guestsCount` на избраната локация
- **Inter-service комуникация:** `reservation-service` извиква `restaurant-service` за да вземе налични маси
- Проверява дали масата е свободна за избрания час
- Ако няма свободна маса с избраната локация, връща грешка

**Стари начин (deprecated):**
Преди CLIENT трябваше да подаде `tableId`, но сега това е автоматично.

### Моите резервации
```bash
curl -X GET "http://localhost:8083/api/reservations/my?userId=1"
```

### Резервации за ресторант
```bash
curl -X GET http://localhost:8083/api/reservations/restaurant/1
```

### Филтриране по статус
```bash
curl -X GET "http://localhost:8083/api/reservations/restaurant/1?status=PENDING"
```

### Промяна на статус на резервация
```bash
curl -X PUT http://localhost:8083/api/reservations/1/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CONFIRMED"
  }'
```

### Отмяна на резервация
```bash
curl -X DELETE http://localhost:8083/api/reservations/1
```

### Проверка на наличност
```bash
curl -X GET "http://localhost:8083/api/reservations/restaurant/1/availability?date=2026-03-15"
```

---

## Статуси на резервации

- **PENDING** - Изчаква потвърждение
- **CONFIRMED** - Потвърдена от ресторанта
- **CANCELLED** - Отказана от клиента
- **REJECTED** - Отхвърлена от ресторанта
- **COMPLETED** - Завършена (клиентът е посетил ресторанта)

---

## Ролите на потребителите

### CLIENT
✅ Може да прави резервации  
✅ Може да вижда само своите резервации  
✅ Може да отменя своите резервации  
❌ НЕ може да управлява ресторанти  

### RESTAURANT_ADMIN
✅ Може да вижда всички резервации за СВОЯ ресторант  
✅ Може да потвърждава/отхвърля резервации  
✅ Може да променя работното време на ресторанта  
✅ Може да управлява маси (добавя, премахва, отваря/затваря)  
❌ **НЕ може да прави резервации** (трябва да използва CLIENT акаунт)  
❌ НЕ може да управлява чужди ресторанти  

### SYSTEM_ADMIN
✅ Може да вижда всички потребители  
✅ Може да променя роли на потребители  
✅ Може да вижда всички ресторанти  
✅ Супер администратор на системата  

---

## SYSTEM_ADMIN функционалности

### Виж всички потребители
```bash
curl -X GET http://localhost:8081/api/users \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

### Филтрирай потребители по роля
```bash
# Само CLIENT потребители
curl -X GET "http://localhost:8081/api/users?role=CLIENT" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"

# Само RESTAURANT_ADMIN потребители
curl -X GET "http://localhost:8081/api/users?role=RESTAURANT_ADMIN" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

**Отговор:**
```json
[
  {
    "id": 2,
    "email": "client@example.com",
    "firstName": "Иван",
    "lastName": "Иванов",
    "phoneNumber": "0888123456",
    "role": "CLIENT",
    "createdAt": "2024-01-15T10:30:00"
  },
  {
    "id": 3,
    "email": "owner@restaurant.com",
    "firstName": "Мария",
    "lastName": "Петрова",
    "phoneNumber": "0888654321",
    "role": "CLIENT",
    "createdAt": "2024-01-16T14:20:00"
  }
]
```

**Workflow за промяна на роля:**
1. SYSTEM_ADMIN вижда списък с всички потребители
2. Намира потребител с `id=3` (Мария Петрова)
3. Изпраща `PUT /api/users/3/role?role=RESTAURANT_ADMIN`
4. Мария Петрова вече може да създава и управлява ресторант

---

## RESTAURANT_ADMIN функционалности

### Виж всички резервации за СВОЯ ресторант
```bash
curl -X GET "http://localhost:8083/api/reservations/restaurant/1?adminUserId=2" \
  -H "Authorization: Bearer RESTAURANT_ADMIN_JWT_TOKEN"
```

**Параметри:**
- `restaurantId=1` - ID на ресторанта
- `adminUserId=2` - ID на администратора (само собственикът може да вижда)

**Отговор:**
```json
[
  {
    "id": 10,
    "userId": 5,
    "restaurantId": 1,
    "tableId": 3,
    "reservationDate": "2024-01-20",
    "reservationTime": "19:00:00",
    "guestsCount": 4,
    "specialRequests": "Празнуваме рожден ден",
    "status": "PENDING"
  },
  {
    "id": 11,
    "userId": 6,
    "restaurantId": 1,
    "tableId": 5,
    "reservationDate": "2024-01-20",
    "reservationTime": "20:30:00",
    "guestsCount": 2,
    "specialRequests": null,
    "status": "CONFIRMED"
  }
]
```

### Промени работното време на ресторанта
```bash
curl -X PUT "http://localhost:8082/api/restaurants/1/hours?adminUserId=2&openingTime=09:00:00&closingTime=22:00:00" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer RESTAURANT_ADMIN_JWT_TOKEN"
```

**Валидация:**
- Само собственикът (`adminUserId=2`) може да променя часовете
- Ако друг RESTAURANT_ADMIN опита, получава грешка: "Нямате права да променяте работното време на този ресторант"

### Управление на маси (отваряне/затваряне)
```bash
# Затваряне на маса (временно недостъпна)
curl -X PUT "http://localhost:8082/api/restaurants/tables/3/availability?adminUserId=2&available=false" \
  -H "Authorization: Bearer RESTAURANT_ADMIN_JWT_TOKEN"

# Отваряне на маса
curl -X PUT "http://localhost:8082/api/restaurants/tables/3/availability?adminUserId=2&available=true" \
  -H "Authorization: Bearer RESTAURANT_ADMIN_JWT_TOKEN"
```

**Отговор:**
```json
{
  "id": 3,
  "tableNumber": "A5",
  "capacity": 6,
  "location": "Терасата",
  "available": false
}
```

---

## Бизнес правила при резервация

### CLIENT може да прави резервации
```bash
curl -X POST "http://localhost:8083/api/reservations?userId=5&userRole=CLIENT" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "tableId": 3,
    "reservationDate": "2024-01-25",
    "reservationTime": "19:00:00",
    "guestsCount": 4,
    "specialRequests": "Алергии към морски храни"
  }'
```

✅ **Успех** - Резервацията се създава със статус PENDING

### RESTAURANT_ADMIN НЕ може да прави резервации
```bash
curl -X POST "http://localhost:8083/api/reservations?userId=2&userRole=RESTAURANT_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "tableId": 3,
    "reservationDate": "2024-01-25",
    "reservationTime": "19:00:00",
    "guestsCount": 4
  }'
```

❌ **Грешка 400 Bad Request:**
```json
{
  "message": "Администраторите на ресторанти не могат да правят резервации. Използвайте CLIENT акаунт.",
  "timestamp": "2024-01-15T14:30:00",
  "path": "/api/reservations"
}
```

**Решение:** RESTAURANT_ADMIN трябва да си създаде отделен CLIENT акаунт за лични резервации.

---

## UI/Frontend перспектива

За пълен преглед на UI екраните и workflow-ове, виж [UI-PLAN.md](UI-PLAN.md).

### Как SYSTEM_ADMIN управлява потребители в UI

1. **Login като SYSTEM_ADMIN** → вижда Dashboard
2. **Отваря "User Management"** → таблица с всички потребители
3. **Филтрира по роля** (падащо меню: All, CLIENT, RESTAURANT_ADMIN, SYSTEM_ADMIN)
4. **Натиска "Change Role"** до потребител → модал с падащо меню
5. **Избира нова роля** → `PUT /api/users/{id}/role?role={role}`
6. **Потребителят излиза и влиза отново** → JWT token се обновява с новата роля

### Как RESTAURANT_ADMIN управлява ресторант

1. **Login като RESTAURANT_ADMIN** → вижда Dashboard за своя ресторант
2. **Отваря "Reservations"** → календар с всички резервации (само за негов ресторант)
3. **Отваря "Manage Tables"** → списък с маси (може да затваря/отваря маси)
4. **Отваря "Settings"** → променя работно време, описание, адрес

### CLIENT работа с резервации

1. **Login като CLIENT** → вижда списък с ресторанти
2. **Избира ресторант** → вижда налични маси
3. **Резервира маса** → `POST /api/reservations?userId={id}&userRole=CLIENT`
4. **Отваря "My Reservations"** → вижда само своите резервации

---

## Външна услуга - OpenStreetMap Nominatim API

Системата интегрира **Nominatim Geocoding API** за:
- Автоматично определяне на географски координати (latitude/longitude) от адрес
- Извличане на град и държава
- Обратно геокодиране (координати → адрес)

**API Endpoint:** https://nominatim.openstreetmap.org/search

Това покрива изискването за **интегриране на външна услуга** в курсовия проект.
