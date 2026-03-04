# Стъпка по стъпка ръководство за Quick Table

## 🚀 Стъпка 1: Настройка на PostgreSQL

### Windows:
1. Свали PostgreSQL от https://www.postgresql.org/download/windows/
2. Инсталирай с password: `postgres`
3. Отвори `pgAdmin` или `psql` и изпълни:

```sql
CREATE DATABASE quicktable_users;
CREATE DATABASE quicktable_restaurants;
CREATE DATABASE quicktable_reservations;
```

### Проверка:
```bash
psql -U postgres -l
```

Трябва да видиш трите нови бази данни.

---

## 🏗️ Стъпка 2: Build на проекта

Отвори терминал (PowerShell или CMD) в папката на проекта:

```powershell
cd c:\develop\university\quick-table
mvn clean install
```

Това ще:
- Компилира всички микросервизи
- Изтегли dependencies
- Създаде JAR файлове

**Очаквано време:** 2-3 минути при първото изпълнение

---

## ▶️ Стъпка 3: Създаване на конфигурационни файлове

Създай следните файлове **ръчно** (игнорирани от Copilot):

### 📄 user-service/src/main/resources/application.yml
```yaml
spring:
  application:
    name: user-service
  datasource:
    url: jdbc:postgresql://localhost:5432/quicktable_users
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

server:
  port: 8081

jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
  expiration: 86400000
```

### 📄 restaurant-service/src/main/resources/application.yml
```yaml
spring:
  application:
    name: restaurant-service
  datasource:
    url: jdbc:postgresql://localhost:5432/quicktable_restaurants
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

server:
  port: 8082
```

### 📄 reservation-service/src/main/resources/application.yml
```yaml
spring:
  application:
    name: reservation-service
  datasource:
    url: jdbc:postgresql://localhost:5432/quicktable_reservations
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

server:
  port: 8083
```

### 📄 user-service/src/main/java/com/quicktable/userservice/security/JwtTokenProvider.java

Този файл също беше игнориран. Виж пълния код в `CONFIGURATION.md` или копирай от GitHub (ако качиш проекта).

---

## 🏃 Стъпка 4: Стартиране на услугите

Отвори **3 отделни терминала** (PowerShell):

### Терминал 1 - User Service:
```powershell
cd c:\develop\university\quick-table\user-service
mvn spring-boot:run
```
✅ Чакай докато видиш: `Started UserServiceApplication in X seconds`

### Терминал 2 - Restaurant Service:
```powershell
cd c:\develop\university\quick-table\restaurant-service
mvn spring-boot:run
```
✅ Чакай докато видиш: `Started RestaurantServiceApplication in X seconds`

### Терминал 3 - Reservation Service:
```powershell
cd c:\develop\university\quick-table\reservation-service
mvn spring-boot:run
```
✅ Чакай докато видиш: `Started ReservationServiceApplication in X seconds`

---

## 🧪 Стъпка 5: Тестване на API endpoints

### ⚠️ ВАЖНО: Първоначален SYSTEM_ADMIN

Първият администратор НЕ се създава чрез API (security мярка). Вместо това:

```powershell
# След стартиране на user-service, изпълни:
psql -U postgres -f database-setup.sql
```

Това ще създаде:
- Email: `admin@quicktable.com`
- Password: `admin123`

### Вариант 1: С Postman
1. Отвори Postman
2. Import → `Quick-Table-API.postman_collection.json`
3. Изпълни заявките в следния ред:
   - User Service → Login (с admin@quicktable.com)
   - Restaurant Service → Create Restaurant (⭐ вика Nominatim API)
   - User Service → Register Client (нов потребител без админ права)
   - Reservation Service → Create Reservation

### Вариант 2: С cURL (PowerShell)

#### 1. Вход като администратор:
```powershell
curl -X POST http://localhost:8081/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"admin@quicktable.com","password":"admin123"}'
```

**Копирай JWT токена!**

#### 2. Създаване на ресторант (с геокодиране):
```powershell
curl -X POST http://localhost:8082/api/restaurants `
  -H "Content-Type: application/json" `
  -d '{\"name\":\"Ресторант Копитото\",\"address\":\"ул. Витоша 15, София, България\",\"phone\":\"+359 2 123 4567\",\"openingTime\":\"10:00:00\",\"closingTime\":\"23:00:00\",\"tables\":[{\"tableNumber\":\"1\",\"capacity\":4}]}'
```

**Забележи:** Системата автоматично ще извика **Nominatim API** и ще попълни координатите!

#### 4. Проверка на ресторанта:
```powershell
curl http://localhost:8082/api/restaurants/1
```

Трябва да видиш `latitude` и `longitude` попълнени!

#### 5. Създаване на резервация:
```powershell
curl -X POST "http://localhost:8083/api/reservations?userId=1" `
  -H "Content-Type: application/json" `
  -d '{\"restaurantId\":1,\"tableId\":1,\"reservationDate\":\"2026-03-15\",\"reservationTime\":\"19:00:00\",\"numberOfGuests\":4,\"customerName\":\"Иван Иванов\",\"customerPhone\":\"0888123456\"}'
```

---

## ✅ Стъпка 6: Проверка на външната услуга

Отвори логовете на **restaurant-service** терминала и потърси:

```
Геокодиране на адрес: ул. Витоша 15, София, България
Успешно геокодиране: lat=42.6977, lon=23.3219
```

Това потвърждава, че **Nominatim API (OpenStreetMap)** е извикан успешно! 🎉

---

## 📊 Проверка на базата данни

```sql
-- PostgreSQL
\c quicktable_restaurants

SELECT name, city, latitude, longitude FROM restaurants;
```

Трябва да видиш координатите на ресторанта.

---

## 🐛 Troubleshooting

### Проблем: Port вече е зает
```
Port 8081 was already in use
```

**Решение:** Убий процеса или промени порта в `application.yml`

Windows:
```powershell
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

### Проблем: Database connection failed
```
org.postgresql.util.PSQLException: Connection refused
```

**Решение:** 
1. Провери дали PostgreSQL върви: `pg_ctl status`
2. Провери username/password в `application.yml`

### Проблем: JWT файлът липсва
```
Cannot resolve class JwtTokenProvider
```

**Решение:** Създай файла ръчно (виж `CONFIGURATION.md`)

---

## 📝 Готово за защита!

Имаш:
- ✅ 3 микросервиза (User, Restaurant, Reservation)
- ✅ REST API комуникация
- ✅ Интеграция с външна услуга (Nominatim)
- ✅ JWT автентикация
- ✅ PostgreSQL бази данни
- ✅ Документация

**Следваща стъпка:** Подготви презентация (PowerPoint) с:
- Архитектурна диаграма
- Demo на API заявки
- Код на GeocodingService (покажи външната интеграция)
- Database schema
