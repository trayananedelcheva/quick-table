# Quick Table - Микросервизна система за резервация на маси

## Описание
Quick Table е микросервизна система за запазване на маси в ресторанти, разработена като курсов проект по дисциплината "Софтуерни системи, базирани на услуги".

## Технологичен стек
- Java 17
- Spring Boot 3.2.2
- Spring Security + JWT
- PostgreSQL
- REST API
- OpenStreetMap Nominatim API (геокодиране)

## Микросервизи

### 1. User Service (port: 8081)
- Регистрация и автентикация на потребители
- JWT token управление
- Роли: CLIENT, RESTAURANT_ADMIN, SYSTEM_ADMIN

### 2. Restaurant Service (port: 8082)
- Управление на ресторанти
- Управление на маси и капацитет
- Интеграция с Maps API за геолокация
- Работно време

### 3. Reservation Service (port: 8083)
- Създаване и управление на резервации
- **Автоматично разпределяне на маси** според брой гости
- **Показване на свободни времеви слотове** (без детайли за маси)
- Проверка на наличност
- История на резервации

### 4. Common Module
- Споделени DTO класове
- Общи утилити
- Exception класове

## Стартиране

### Предварителни изисквания
- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### Настройка на база данни
```sql
CREATE DATABASE quicktable_users;
CREATE DATABASE quicktable_restaurants;
CREATE DATABASE quicktable_reservations;
```

### Стартиране на услугите
```bash
# Build all services
mvn clean install

# Start user service
cd user-service
mvn spring-boot:run

# Start restaurant service
cd restaurant-service
mvn spring-boot:run

# Start reservation service
cd reservation-service
mvn spring-boot:run
```

## API Endpoints

### User Service (http://localhost:8081)
- POST `/api/auth/register` - Регистрация
- POST `/api/auth/login` - Вход
- GET `/api/users/profile` - Профил

### Restaurant Service (http://localhost:8082)
- GET `/api/restaurants` - Списък ресторанти
- POST `/api/restaurants` - Добавяне на ресторант (ADMIN)
- GET `/api/restaurants/{id}` - Детайли за ресторант

### Reservation Service (http://localhost:8083)
- GET `/api/reservations/restaurant/{id}/available-slots` - **Свободни часове** (нов UX)
- POST `/api/reservations` - Създаване на резервация (автоматично избира маса)
- GET `/api/reservations/my` - Моите резервации
- PUT `/api/reservations/{id}/status` - Промяна на статус

## 📚 Документация

- **[GETTING-STARTED.md](GETTING-STARTED.md)** - Стъпка по стъпка инструкции за стартиране
- **[DOCUMENTATION.md](DOCUMENTATION.md)** - Пълна документация за курсовия проект
- **[API-EXAMPLES.md](API-EXAMPLES.md)** - Примери за API заявки
- **[CONFIGURATION.md](CONFIGURATION.md)** - Конфигурационни файлове
- **[SECURITY.md](SECURITY.md)** - Security best practices и role management
- **[UI-PLAN.md](UI-PLAN.md)** - Frontend UI план и mockups
- **[ROLE-BASED-BUSINESS-RULES.md](ROLE-BASED-BUSINESS-RULES.md)** - Бизнес правила по роли
- **[CHANGELOG-UX-IMPROVEMENT.md](CHANGELOG-UX-IMPROVEMENT.md)** - UX подобрения в резервационния процес
- **[Quick-Table-API.postman_collection.json](Quick-Table-API.postman_collection.json)** - Postman collection

## 🎯 Покрити изисквания за курсовия проект

| Критерий | Статус | Точки |
|----------|--------|-------|
| 3 услуги на една платформа (Java/Spring Boot) | ✅ | 30 |
| REST API комуникация | ✅ | 30 |
| Интеграция с външна услуга (Nominatim API) | ✅ | 5 |
| Документация | ✅ | 10 |
| **ОБЩО** | | **75/100** |

## 🚀 Бързо стартиране

```bash
# 1. Създай бази данни
psql -U postgres -c "CREATE DATABASE quicktable_users;"
psql -U postgres -c "CREATE DATABASE quicktable_restaurants;"
psql -U postgres -c "CREATE DATABASE quicktable_reservations;"

# 2. Build проекта
mvn clean install

# 3. Стартирай user-service първо
cd user-service && mvn spring-boot:run

# 4. Създай първия SYSTEM_ADMIN (в нов терминал)
psql -U postgres -f database-setup.sql

# 5. Стартирай останалите услуги
cd restaurant-service && mvn spring-boot:run  
cd reservation-service && mvn spring-boot:run

# 6. Влез като admin
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@quicktable.com","password":"admin123"}'
```

Виж [GETTING-STARTED.md](GETTING-STARTED.md) за подробни инструкции.

## 📝 Основни характеристики

- ✅ **Микросервизна архитектура** - 3 независими услуги
- ✅ **JWT автентикация** - Сигурен stateless механизъм
- ✅ **Role-based security** - Защитена регистрация на администратори
- ✅ **Геокодиране** - Автоматично получаване на координати от адрес
- ✅ **REST API** - Консистентен RESTful дизайн
- ✅ **Database per service** - Всеки сервиз има собствена БД
- ✅ **Secure role management** - Само SYSTEM_ADMIN може да повишава роли
- ✅ **Интелигентна резервация** - Автоматично случайно избиране на подходяща маса
- ✅ **Time-slot базиран UX** - CLIENT вижда само свободни часове, не маси
- ✅ **Система за категории маси** - Избор на категория (Вътре/Лятна градина/Зимна градина) при резервация
- ✅ **Сезонно управление** - Затваряне/отваряне на цели категории маси
- ✅ **Inter-service комуникация** - WebClient за REST комуникация между микросервизите

## 🔮 Бъдещи разширения

За допълнителни точки в курсовия проект:

1. **SOAP протокол** (+10 точки)
   - SOAP wrapper за Restaurant Service
   - WSDL дефиниция

2. **.NET платформа** (+10 точки)
   - Notification Service на C#
   - Email/SMS уведомления

3. **BPEL процес**
   - Orchestration на резервационен workflow

Виж [FUTURE-ENHANCEMENTS.md](FUTURE-ENHANCEMENTS.md) за детайли.

## 👤 Автор
**[Твоето име]**  
Факултетен номер: [Твой номер]  
Курсов проект по: Софтуерни системи, базирани на услуги  
Дата: Февруари 2026

## 📄 Лиценз
Учебен проект - Свободна употреба
