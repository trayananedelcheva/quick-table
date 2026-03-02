# КУРСОВ ПРОЕКТ - Quick Table
## Дисциплина: Софтуерни системи, базирани на услуги

---

## 1. ЦЕЛИ НА ПРОЕКТА

Quick Table е микросервизна система за **автоматизиране на процеса по запазване на маси в ресторанти**. Системата позволява на потребителите да преглеждат налични ресторанти, да проверяват свободни маси и да резервират маса за определена дата и час. Администраторите на ресторанти могат да управляват резервации, а системните администратори да добавят нови ресторанти.

**Основни цели:**
- Улесняване на процеса по резервация на маси
- Централизирана платформа за множество ресторанти
- Автоматизация на управлението на резервации
- Геолокация на ресторанти чрез интеграция с външни API

---

## 2. АНАЛИЗ НА ИЗИСК ВАНИЯТА

### 2.1 Функционални изисквания

**За клиенти (CLIENT):**
- Регистрация и вход в системата (всички нови потребители се създават като CLIENT)
- Преглед на списък с ресторанти
- Филтриране на ресторанти по локация
- Преглед на детайли за ресторант (работно време, маси, локация)
- Създаване на резервация за конкретна маса, дата и час
- Преглед на собствени резервации (само своите)
- Отмяна на резервация

**За администратори на ресторанти (RESTAURANT_ADMIN):**
- **НЕ МОГАТ** да правят резервации (трябва да използват CLIENT акаунт)
- Преглед на резервации **само за техния ресторант**
- Потвърждаване или отхвърляне на резервации
- Промяна на статус на резервации
- Управление на работно време на ресторанта
- Управление на маси (добавяне, отваряне/затваряне на маси)
- Промяна на описание и контактна информация на ресторанта

**За системни администратори (SYSTEM_ADMIN):**
- Преглед на всички потребители в системата
- Филтриране на потребители по роля (CLIENT, RESTAURANT_ADMIN, SYSTEM_ADMIN)
- **Промяна на роли на потребители** (повишаване CLIENT → RESTAURANT_ADMIN)
- Добавяне на нови ресторанти
- Добавяне и управление на маси в ресторантите
- Супер администраторски права

### 2.2 Нефункционални изисквания

**Производителност:**
- Време за отговор < 2 секунди за стандартни заявки
- Време за външно API (геокодиране) < 5 секунди

**Сигурност:**
- JWT базирана автентикация
- BCrypt хеширане на пароли
- Role-based access control (RBAC)
- **Защитена регистрация на администратори** - само чрез SQL seed data
- Само SYSTEM_ADMIN може да повишава роли на потребители
- @PreAuthorize annotations за endpoint protection

**Мащабируемост:**
- Микросервизна архитектура позволява независимо мащабиране
- Всеки сервиз има собствена база данни

**Надеждност:**
- Graceful handling при недостъпност на външна услуга
- Транзакционна консистентност на данни

**Поддържаемост:**
- Модулна структура с ясно разделение на отговорностите
- RESTful API с консистентна структура

---

## 3. ИЗПОЛЗВАНИ ТЕХНОЛОГИИ

### 3.1 Backend Framework
- **Java 17** - Програмен език
- **Spring Boot 3.2.2** - Framework за микросервизи
- **Spring Data JPA** - ORM за работа с база данни
- **Spring Security** - Сигурност и автентикация
- **Spring Web** - REST API endpoints

### 3.2 База данни
- **PostgreSQL 14+** - Релационна база данни
- Отделни бази за всеки микросервиз (database-per-service pattern)

### 3.3 Сигурност
- **JWT (JSON Web Tokens)** - Stateless автентикация
- **BCrypt** - Хеширане на пароли

### 3.4 Комуникация
- **REST API** - HTTP/JSON базирана комуникация
- **WebFlux/WebClient** - Асинхронни HTTP заявки към външни услуги

### 3.5 Външни услуги
- **OpenStreetMap Nominatim API** - Геокодиране на адреси
  - URL: https://nominatim.openstreetmap.org
  - Безплатна услуга за конвертиране на адрес → координати

### 3.6 Build Tool
- **Maven** - Dependency management и build automation

### 3.7 Обосновка на избраните технологии

| Технология | Обосновка |
|------------|-----------|
| **Spring Boot** | Индустриален стандарт за Java микросервизи, богата екосистема, auto-configuration |
| **PostgreSQL** | Open-source, мощна релационна БД, добра поддръжка на JSON типове |
| **JWT** | Stateless, лесно мащабируем, поддържа се от всички платформи |
| **REST** | Широко разпространен, лесен за имплементация и тестване |
| **Nominatim** | Безплатен, надежден, добра документация, не изисква API key |

---

## 4. АРХИТЕКТУРА НА СИСТЕМАТА

### 4.1 Микросервизна архитектура

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Client    │      │   Client    │      │   Admin     │
│ Application │      │ Application │      │   Panel     │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │                    │                    │
       └────────────────────┼────────────────────┘
                            │
                   ┌────────▼────────┐
                   │   API Gateway   │ (Опционално)
                   │   (Бъдещо)      │
                   └────────┬────────┘
                            │
         ┌──────── ─────────┼─────────────────┐
         │                  │                 │
    ┌────▼────┐      ┌──────▼──────┐   ┌─────▼──────┐
    │  User   │      │ Restaurant  │   │Reservation │
    │ Service │      │   Service   │   │  Service   │
    │         │      │             │   │            │
    │ :8081   │      │   :8082     │   │   :8083    │
    └────┬────┘      └──────┬──────┘   └─────┬──────┘
         │                  │                 │
    ┌────▼────┐      ┌──────▼──────┐   ┌─────▼──────┐
    │  User   │      │ Restaurant  │   │Reservation │
    │   DB    │      │     DB      │   │     DB     │
    └─────────┘      └──────┬──────┘   └────────────┘
                            │
                     ┌──────▼────────┐
                     │  Nominatim    │
                     │  API (OSM)    │
                     │  (External)   │
                     └───────────────┘
```

### 4.2 Описание на микросервизите

#### User Service (Port 8081)
**Отговорности:**
- Управление на потребителски акаунти
- Автентикация и авторизация
- Генериране и валидиране на JWT токени
- Управление на роли (CLIENT, RESTAURANT_ADMIN, SYSTEM_ADMIN)

**Endpoints:**
- `POST /api/auth/register` - Регистрация (винаги като CLIENT)
- `POST /api/auth/login` - Вход
- `GET /api/users/me` - Профил на текущия потребител
- `GET /api/users/{id}` - Информация за потребител
- `GET /api/users` - Списък с потребители (само ADMIN)
- `PUT /api/users/{id}/role` - Промяна на роля (само SYSTEM_ADMIN)

**Security Note:** 
Първият SYSTEM_ADMIN се създава чрез SQL seed data (`database-setup.sql`), не чрез API регистрация. Това предотвратява self-promotion attacks.

**База данни:** `quicktable_users`

#### Restaurant Service (Port 8082)
**Отговорности:**
- Управление на ресторанти
- Управление на маси
- **Интеграция с Nominatim API за геолокация**
- Работно време и капацитет

**Endpoints:**
- `POST /api/restaurants` - Създаване на ресторант
- `GET /api/restaurants` - Списък с ресторанти
- `GET /api/restaurants?city=София` - Филтриране по град
- `GET /api/restaurants/{id}` - Детайли за ресторант
- `PUT /api/restaurants/{id}` - Актуализация
- `PUT /api/restaurants/{id}/hours` - Промяна на работно време
- `DELETE /api/restaurants/{id}` - Деактивиране
- `POST /api/restaurants/{id}/tables` - Добавяне на маса
- `GET /api/restaurants/{id}/tables` - Маси на ресторант
- `PUT /api/restaurants/tables/{id}/availability` - Затваряне/отваряне на маса
- `PUT /api/restaurants/{id}/categories/{category}/toggle` - Затваряне/отваряне на категория
- `GET /api/restaurants/{id}/categories` - Проверка статус на категории

**База данни:** `quicktable_restaurants`

**Външни услуги:**
- OpenStreetMap Nominatim API (geocoding)

#### Reservation Service (Port 8083)
**Отговорности:**
- Създаване и управление на резервации
- Проверка на наличност на маси
- Управление на статуси на резервации
- Проверка за конфликти при резервиране

**Endpoints:**
- `POST /api/reservations` - Създаване на резервация (с автоматичен избор на маса)
- `GET /api/reservations/my` - Моите резервации
- `GET /api/reservations/restaurant/{id}` - Резервации на ресторант
- `PUT /api/reservations/{id}/status` - Промяна на статус
- `DELETE /api/reservations/{id}` - Отмяна
- `GET /api/reservations/restaurant/{id}/availability` - Наличност
- `GET /api/reservations/restaurant/{id}/available-slots` - Свободни часове по категория

**База данни:** `quicktable_reservations`

### 4.3 Common Module
**Споделени компоненти:**
- `UserRole` enum - Роли на потребители
- `ReservationStatus` enum - Статуси на резервации
- `TableCategory` enum - Категории на маси (INSIDE, SUMMER_GARDEN, WINTER_GARDEN)
- `ErrorResponse` DTO - Унифициран формат за грешки

---

## 5. КОМУНИКАЦИОННИ ПРОТОКОЛИ

### 5.1 REST API
Всички микросервизи предоставят **RESTful API** с JSON формат.

**Характеристики:**
- HTTP методи: GET, POST, PUT, DELETE
- Content-Type: `application/json`
- Stateless комуникация
- JWT токен в `Authorization: Bearer <token>` header

**Пример за заявка:**
```http
POST /api/restaurants HTTP/1.1
Host: localhost:8082
Content-Type: application/json

{
  "name": "Ресторант Копитото",
  "address": "ул. Витоша 15, София"
}
```

**Пример за отговор:**
```json
{
  "id": 1,
  "name": "Ресторант Копитото",
  "latitude": 42.6977,
  "longitude": 23.3219,
  "city": "София",
  "country": "България"
}
```

### 5.2 Интеграция с външна услуга

Restaurant Service интегрира **Nominatim API** (OpenStreetMap):

**Заявка към Nominatim:**
```http
GET /search?q=ул.+Витоша+15,+София&format=json&addressdetails=1 HTTP/1.1
Host: nominatim.openstreetmap.org
User-Agent: QuickTable/1.0
```

**Отговор:**
```json
[
  {
    "lat": "42.6977",
    "lon": "23.3219",
    "display_name": "15, Витоша, София, България",
    "address": {
      "road": "Витоша",
      "city": "София",
      "country": "България"
    }
  }
]
```

---

## 6. ПРОГРАМНА РЕАЛИЗАЦИЯ

### 6.1 Структура на проекта

```
quick-table/
├── pom.xml (Parent POM)
├── common/
│   ├── src/main/java/com/quicktable/common/
│   │   └── dto/
│   │       ├── UserRole.java
│   │       ├── ReservationStatus.java
│   │       └── ErrorResponse.java
│   └── pom.xml
├── user-service/
│   ├── src/main/java/com/quicktable/userservice/
│   │   ├── UserServiceApplication.java
│   │   ├── entity/ (User)
│   │   ├── repository/ (UserRepository)
│   │   ├── service/ (AuthService, UserService)
│   │   ├── controller/ (AuthController, UserController)
│   │   ├── security/ (JWT, SecurityConfig)
│   │   └── dto/
│   └── pom.xml
├── restaurant-service/
│   ├── src/main/java/com/quicktable/restaurantservice/
│   │   ├── RestaurantServiceApplication.java
│   │   ├── entity/ (Restaurant, RestaurantTable)
│   │   ├── repository/
│   │   ├── service/ (RestaurantService, GeocodingService ⭐)
│   │   ├── controller/ (RestaurantController)
│   │   └── dto/
│   └── pom.xml
└── reservation-service/
    ├── src/main/java/com/quicktable/reservationservice/
    │   ├── ReservationServiceApplication.java
    │   ├── entity/ (Reservation)
    │   ├── repository/
    │   ├── service/ (ReservationService)
    │   ├── controller/ (ReservationController)
    │   └── dto/
    └── pom.xml
```

### 6.2 Ключови имплементации

#### JWT Автентикация (User Service)
```java
@Component
public class JwtTokenProvider {
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
    }
}
```

#### Geocoding Service (Restaurant Service) ⭐ ВЪНШНА УСЛУГА
```java
@Service
public class GeocodingService {
    private final WebClient.Builder webClientBuilder;
    
    public GeoLocation geocodeAddress(String address) {
        WebClient webClient = webClientBuilder
            .baseUrl("https://nominatim.openstreetmap.org").build();
            
        NominatimResponse[] responses = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/search")
                .queryParam("q", address)
                .queryParam("format", "json")
                .build())
            .retrieve()
            .bodyToMono(NominatimResponse[].class)
            .block();
            
        // Парсиране и връщане на координати
    }
}
```

#### Проверка за наличност (Reservation Service)
```java
@Service
public class ReservationService {
    private boolean isTableOccupied(
        List<Reservation> reservations, 
        LocalTime requestedTime
    ) {
        for (Reservation reservation : reservations) {
            LocalTime end = reservation.getReservationTime().plusHours(2);
            LocalTime requestedEnd = requestedTime.plusHours(2);
            
            // Проверка за припокриване
            if (!(requestedTime.isAfter(end) || 
                  requestedEnd.isBefore(reservation.getReservationTime()))) {
                return true; // Заето
            }
        }
        return false;
    }
}
```

### 6.3 Database Schema

**Users Table (user-service):**
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

**Restaurants Table (restaurant-service):**
```sql
CREATE TABLE restaurants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address VARCHAR(500) NOT NULL,
    city VARCHAR(100),
    country VARCHAR(100),
    latitude DOUBLE PRECISION,  -- От Nominatim API
    longitude DOUBLE PRECISION, -- От Nominatim API
    phone VARCHAR(20),
    email VARCHAR(255),
    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL,
    admin_user_id BIGINT,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

**Restaurant Tables (restaurant-service):**
```sql
CREATE TABLE restaurant_tables (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id),
    table_number VARCHAR(50) NOT NULL,
    capacity INTEGER NOT NULL,
    category VARCHAR(50) NOT NULL, -- INSIDE, SUMMER_GARDEN, WINTER_GARDEN
    available BOOLEAN NOT NULL
);
```

**Category Availability (restaurant-service):**
```sql
CREATE TABLE category_availability (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id),
    category VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    UNIQUE(restaurant_id, category)
);
```

**Категории на маси:**
- `INSIDE` - "Вътре"
- `SUMMER_GARDEN` - "Лятна градина"
- `WINTER_GARDEN` - "Зимна градина"

**Логика:**
- Всяка маса принадлежи към една категория
- RESTAURANT_ADMIN може да затвори цяла категория (напр. Лятна градина през зимата)
- При резервация, системата **автоматично избира random маса** от свободните в категорията
- Това осигурява равномерно натоварване на масите

**Reservations Table (reservation-service):**
```sql
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    reservation_date DATE NOT NULL,
    reservation_time TIME NOT NULL,
    number_of_guests INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    special_requests TEXT,
    customer_name VARCHAR(200) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_email VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

---

## 7. ОЦЕНКА СПОРЕД КРИТЕРИИТЕ

| № | Критерий | Точки | Реализация |
|---|----------|-------|------------|
| 1 | Разработване на услуги върху една платформа с един комуникационен протокол | 30 | ✅ 3 микросервиза (User, Restaurant, Reservation) на Java/Spring Boot с REST API |
| 2 | Разработване на услуги върху различни платформи | 10 | ❌ Не е реализирано (може да се добави .NET услуга по-късно) |
| 3 | Използване на различни комуникационни протоколи (SOAP, REST) | 10 | ❌ Само REST (може да се добави SOAP wrapper) |
| 4 | Интегриране на външна услуга | 5 | ✅ **Nominatim API (OpenStreetMap)** за геокодиране |
| 5 | Услуга достъпна при различни протоколи | 5 | ❌ Само REST |
| 6 | Интегриране на услугите | 30 | ✅ REST API комуникация между сервизите |
| 7 | Документация | 10 | ✅ Този документ + README + API примери |
| | **ОБЩО** | **100** | **75 точки** (над минимум от 40) |

---

## 8. ИНСТРУКЦИИ ЗА СТАРТИРАНЕ

### 8.1 Предварителни изисквания
```bash
java -version  # Java 17+
mvn -version   # Maven 3.8+
psql --version # PostgreSQL 14+
```

### 8.2 Настройка на база данни
```bash
# 1. Създай базите данни
psql -U postgres -c "CREATE DATABASE quicktable_users;"
psql -U postgres -c "CREATE DATABASE quicktable_restaurants;"
psql -U postgres -c "CREATE DATABASE quicktable_reservations;"

# 2. Стартирай user-service (за да се създадат таблиците)
cd user-service && mvn spring-boot:run

# 3. Създай първия SYSTEM_ADMIN (в друг терминал)
psql -U postgres -f database-setup.sql
```

**Важно:** Първият администратор не се създава чрезAPI регистрация!

### 8.3 Build на проекта
```bash
cd c:\develop\university\quick-table
mvn clean install
```

### 8.4 Стартиране на услугите

**Терминал 1 - User Service:**
```bash
cd user-service
mvn spring-boot:run
```

**Терминал 2 - Restaurant Service:**
```bash
cd restaurant-service
mvn spring-boot:run
```

**Терминал 3 - Reservation Service:**
```bash
cd reservation-service
mvn spring-boot:run
```

### 8.5 Тестване
Виж `API-EXAMPLES.md` за примери на заявки.

---

## 9. БЪДЕЩИ РАЗШИРЕНИЯ

**За повече точки в курсовия проект:**

1. **SOAP услуга** (10 точки)
   - Добави SOAP wrapper за Restaurant Service
   - WSDL дефиниция

2. **Мулти-платформа** (10 точки)
   - Напиши Notification Service на .NET/C#
   - Изпращане на email/SMS уведомления

3. **BPEL процес** (потенциално)
   - Orchestration на reservation workflow
   - Автоматично потвърждаване

4. **API Gateway**
   - Spring Cloud Gateway
   - Централизирана автентикация

5. **Service Discovery**
   - Eureka Server
   - Динамично откриване на услуги

---

## 10. ЗАКЛЮЧЕНИЕ

Quick Table системата демонстрира:
- ✅ **Микросервизна архитектура** с ясно разделение на отговорностите
- ✅ **REST API комуникация** между услугите
- ✅ **Интеграция с външна услуга** (Nominatim)
- ✅ **JWT автентикация** и сигурност
- ✅ **Database-per-service** pattern
- ✅ **Асинхронна комуникация** с външни API (WebFlux)

Проектът покрива основните изисквания за курсовия проект и може лесно да бъде разширен с допълнителни протоколи и платформи.

---

**Разработила:** [Твоето име]  
**Факултетен номер:** [Твой номер]  
**Дата:** 24.02.2026  
**Дисциплина:** Софтуерни системи, базирани на услуги
