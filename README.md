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

## Структура на проекта

```
quick-table/
├── 📚 docs/                  # Документация
│   ├── API-EXAMPLES.md
│   ├── CONFIGURATION.md
│   ├── DOCUMENTATION.md
│   ├── GETTING-STARTED.md
│   ├── SECURITY.md
│   ├── CHANGELOG.md
│   ├── REFACTORING-CHANGELOG.md
│   └── ... (всички .md файлове)
│
├── 🗄️ database/             # SQL скриптове
│   ├── database-setup.sql
│   ├── migrate-category-to-location.sql
│   ├── sample-data.sql
│   └── ... (всички .sql файлове)
│
├── 🧪 testing/              # Тестване
│   └── Quick-Table-API.postman_collection.json
│
├── 🔧 user-service/         # User Service (8081)
├── 🍽️ restaurant-service/   # Restaurant Service (8082)
├── 📅 reservation-service/  # Reservation Service (8083)
├── 📦 common/               # Common Module
│
├── pom.xml                  # Root Maven config
└── README.md               # Този файл
```

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

Всички документи са организирани в папка [**docs/**](docs/):

- **[GETTING-STARTED.md](docs/GETTING-STARTED.md)** - Стъпка по стъпка инструкции за стартиране
- **[DOCUMENTATION.md](docs/DOCUMENTATION.md)** - Пълна документация за курсовия проект
- **[API-EXAMPLES.md](docs/API-EXAMPLES.md)** - Примери за API заявки
- **[CONFIGURATION.md](docs/CONFIGURATION.md)** - Конфигурационни файлове
- **[SECURITY.md](docs/SECURITY.md)** - Security best practices и role management
- **[UI-PLAN.md](docs/UI-PLAN.md)** - Frontend UI план и mockups
- **[ROLE-BASED-BUSINESS-RULES.md](docs/ROLE-BASED-BUSINESS-RULES.md)** - Бизнес правила по роли
- **[CHANGELOG-UX-IMPROVEMENT.md](docs/CHANGELOG-UX-IMPROVEMENT.md)** - UX подобрения в резервационния процес
- **[REFACTORING-CHANGELOG.md](docs/REFACTORING-CHANGELOG.md)** - Category → Location рефакторинг

### Database Scripts
SQL файлове в папка [**database/**](database/):
- **[database-setup.sql](database/database-setup.sql)** - Пълна схема за всички бази данни
- **[create-admin-only.sql](database/create-admin-only.sql)** - Създава admin акаунт
- **[sample-data.sql](database/sample-data.sql)** - Примерни данни
- **[migrate-category-to-location.sql](database/migrate-category-to-location.sql)** - Последна миграция

### Testing
- **[Quick-Table-API.postman_collection.json](testing/Quick-Table-API.postman_collection.json)** - Postman collection с всички endpoints

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
# 1. Създай бази данни и схема
psql -U postgres -f database/database-setup.sql

# 2. Създай admin акаунт
psql -U postgres -f database/create-admin-only.sql

# 3. (Опционално) Добави примерни данни
psql -U postgres -f database/sample-data.sql

# 4. Изпълни последната миграция (category → location)
psql -U postgres -f database/migrate-category-to-location.sql

# 5. Build проекта
mvn clean install

# 6. Стартирай услугите (в отделни терминали)
cd user-service && mvn spring-boot:run         # port 8081
cd restaurant-service && mvn spring-boot:run   # port 8082
cd reservation-service && mvn spring-boot:run  # port 8083

# 7. Влез като admin (получи JWT token)
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@quicktable.com","password":"admin123"}'

# 8. Import Postman collection от testing/Quick-Table-API.postman_collection.json
```

Виж [docs/GETTING-STARTED.md](docs/GETTING-STARTED.md) за подробни инструкции.

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

Виж [docs/FUTURE-ENHANCEMENTS.md](docs/FUTURE-ENHANCEMENTS.md) за детайли.

## 👤 Автор
**[Твоето име]**  
Факултетен номер: [Твой номер]  
Курсов проект по: Софтуерни системи, базирани на услуги  
Дата: Февруари 2026

## 📄 Лиценз
Учебен проект - Свободна употреба
