# QuickTable — Микросервизна система за резервация на маси

## Описание

QuickTable е уеб приложение за резервация на маси в ресторанти, разработено като курсов проект по дисциплината "Софтуерни системи, базирани на услуги". Системата е изградена на микросервизна архитектура с React frontend и четири Spring Boot backend сервиза.

---

## Технологичен стек

**Backend:**
- Java 17 + Spring Boot 3.2.2
- Spring Security + JWT автентикация
- PostgreSQL (отделна база за всеки сервиз)
- REST API + inter-service комуникация чрез WebClient
- OpenStreetMap Nominatim API (геокодиране)

**Frontend:**
- React 19 + TypeScript
- MUI (Material UI) v9
- React Router v7
- Axios

**Notification Service:**
- Node.js + TypeScript + Express
- Nodemailer (изпращане на имейли)

---

## Структура на проекта

```
quick-table/
├── quicktable-ui/              # React frontend (port: 5173)
├── user-service/               # User Service (port: 8081)
├── restaurant-service/         # Restaurant Service (port: 8082)
├── reservation-service-v2/     # Reservation Service (port: 8085)
├── nodejs-notification-service/ # Notification Service (port: 3001)
├── common/                     # Споделени DTO класове
├── database/                   # SQL скриптове
├── testing/                    # Postman/APIdog колекции
├── docs/                       # Документация
└── pom.xml                     # Root Maven конфигурация
```

---

## Микросервизи

### 1. User Service (port: 8081)
- Регистрация и вход с JWT токен
- Забравена/смяна на парола
- Управление на профил (имена, телефон, парола)
- Роли: `CLIENT`, `RESTAURANT_ADMIN`, `SYSTEM_ADMIN`
- SYSTEM_ADMIN може да сменя роли на потребители

### 2. Restaurant Service (port: 8082)
- Управление на ресторанти (CRUD)
- Управление на маси — добавяне, редактиране, изтриване, активиране/деактивиране
- Управление на зони: `INSIDE`, `SUMMER_GARDEN`, `WINTER_GARDEN`
- Качване на снимки на ресторант
- Геокодиране на адреси чрез Nominatim
- Система за отзиви и рейтинги

### 3. Reservation Service (port: 8085)
- Създаване и управление на резервации
- Автоматичен избор на подходяща маса (best-fit алгоритъм)
- Свободни времеви слотове с филтри по дата, брой гости и зона
- Статуси: `CONFIRMED`, `CANCELLED`, `REJECTED`, `COMPLETED`, `NO_SHOW`
- Проверка за припокриващи се резервации
- Автоматично завършване на изтекли резервации

### 4. Notification Service (port: 3001)
- Node.js сервиз за изпращане на имейли
- Уведомления при: потвърждение, отказ, отхвърляне, завършване, неявяване
- Интеграция с Nodemailer

### 5. Common Module
- Споделени DTO класове и enums (`UserRole`, `ReservationStatus`, `TableLocation`)

---

## Frontend (React UI)

Достъпен на `http://localhost:5173` след `npm run dev`.

### Публични страници
- Начална страница с всички ресторанти (търсене по град/название)
- Страница за резервация с детайли на ресторант, отзиви и форма за резервация
- Нелогнати потребители могат да разглеждат и попълват формата; вход се изисква само при финалния submit

### CLIENT
- Разглеждане на ресторанти
- Правене на резервации
- Преглед, отказване и оставяне на отзиви за резервации

### RESTAURANT_ADMIN
- Управление на ресторант (информация, снимка, зони, маси)
- Преглед на резервации с филтри и управление на статуси
- Създаване на нов ресторант

### SYSTEM_ADMIN
- Управление на всички потребители и роли
- Създаване на ресторанти от името на RESTAURANT_ADMIN потребители

---

## Стартиране

### Предварителни изисквания
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Node.js 18+

### Бази данни

```sql
CREATE DATABASE quicktable_users;
CREATE DATABASE quicktable_restaurants;
CREATE DATABASE quicktable_reservations_v2;
```

### Environment variables

Всеки Spring Boot сервиз изисква `.env` файл или системни променливи:

```
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret
```

### Стартиране на backend сервизите

```bash
# Всеки в отделен терминал
cd user-service && mvn spring-boot:run           # port 8081
cd restaurant-service && mvn spring-boot:run     # port 8082
cd reservation-service-v2 && mvn spring-boot:run # port 8085

# Notification service
cd nodejs-notification-service && npm install && npm run dev  # port 3001
```

### Стартиране на frontend

```bash
cd quicktable-ui
npm install
npm run dev   # http://localhost:5173
```

---

## API документация (Swagger UI)

| Сервиз | URL |
|--------|-----|
| User Service | http://localhost:8081/swagger-ui/index.html |
| Restaurant Service | http://localhost:8082/swagger-ui/index.html |
| Reservation Service | http://localhost:8085/swagger-ui/index.html |

---

## Тестване

Postman и APIdog колекции в папка `testing/`.

---

## Основни характеристики

- **Микросервизна архитектура** — 4 независими сервиза на различни платформи (Java + Node.js)
- **JWT автентикация** — Stateless, role-based security
- **Role-based UI** — Различен интерфейс за CLIENT, RESTAURANT_ADMIN, SYSTEM_ADMIN
- **Best-fit алгоритъм** — Автоматичен избор на маса с минимален разход на капацитет
- **Имейл уведомления** — При всяка промяна на статус на резервация
- **Геокодиране** — Автоматично получаване на координати от адрес
- **Database per service** — Всеки сервиз има собствена PostgreSQL база

---

## Автор

**Trayana Nedelcheva**  
Курсов проект по: Софтуерни системи, базирани на услуги
