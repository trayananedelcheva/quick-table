# QuickTable — Микросервизна система за резервация на маси

## Описание

QuickTable е уеб приложение за резервация на маси в ресторанти, разработено като курсов проект по дисциплината "Софтуерни системи, базирани на услуги". Системата е изградена на микросервизна архитектура с React frontend и четири backend сервиза.

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
- Nodemailer + Gmail SMTP (порт 465, SSL)

---

## Структура на проекта

```
quick-table/
├── quicktable-ui/               # React frontend (port: 5173)
├── user-service/                # User Service (port: 8081)
├── restaurant-service/          # Restaurant Service (port: 8082)
├── reservation-service-v2/      # Reservation Service (port: 8085)
├── nodejs-notification-service/ # Notification Service (port: 3001)
├── common/                      # Споделени DTO класове
├── database/                    # SQL скриптове
├── testing/                     # Postman/APIdog колекции
├── docs/                        # Документация и диаграми
└── pom.xml                      # Root Maven конфигурация
```

---

## Микросервизи

### 1. User Service (port: 8081)
- Регистрация и вход с JWT токен (валиден 24 часа)
- Парола хеширана с BCrypt; смяна на забравена парола чрез еднократен токен
- Управление на профил (имена, телефон, парола)
- Роли: `CLIENT`, `RESTAURANT_ADMIN`, `SYSTEM_ADMIN`
- SYSTEM_ADMIN може да преглежда и сменя роли на потребители

### 2. Restaurant Service (port: 8082)
- Управление на ресторанти (CRUD) с hard delete
- Управление на маси — добавяне, редактиране, изтриване, активиране/деактивиране
- Управление на зони: `INSIDE`, `SUMMER_GARDEN`, `WINTER_GARDEN`
- Качване и сервиране на снимки на ресторант
- Геокодиране на адреси чрез Nominatim (координати за Google Maps)
- Система за отзиви и рейтинги с валидация срещу reservation-service
- При изтриване на ресторант се изтриват и всички свързани ревюта

### 3. Reservation Service (port: 8085)
- Създаване и управление на резервации
- Автоматичен избор на подходяща маса (best-fit алгоритъм)
- Свободни времеви слотове — само бъдещи часове, само налични маси, само активни зони
- Статуси: `CONFIRMED`, `CANCELLED`, `REJECTED`, `COMPLETED`, `NO_SHOW`
- Проверка за припокриване на резервации (±90 минути)
- Автоматично завършване на изтекли резервации (scheduler на всеки 30 мин.)
- Имейл уведомления при всяка промяна на статус
- Линк за оставяне на отзив в имейла след приключване

### 4. Notification Service (port: 3001)
- Node.js сервиз за изпращане на HTML имейли
- Handlebars шаблони по тип нотификация
- Gmail SMTP (smtp.gmail.com:465, SSL) чрез Nodemailer
- Запис на история на изпратените имейли в PostgreSQL

### 5. Common Module
- Споделени DTO класове и enums (`UserRole`, `ReservationStatus`, `TableLocation`)

---

## Frontend (React UI)

Достъпен на `http://localhost:5173` след `npm run dev`.

### Публични страници (без вход)
- Начална страница с всички ресторанти — търсене по град/название, default снимка при липса
- Страница за резервация — детайли на ресторант, рейтинг, отзиви, Google Maps линк, форма за резервация
- Нелогнати потребители могат да разглеждат и попълват формата; вход се изисква само при финалния submit (данните се запазват)
- Страница за оставяне на отзив (`/leave-review/:reservationId`) — достъпна от имейл линк

### CLIENT
- Разглеждане на ресторанти
- Правене на резервации с избор на дата, гости, зона и час
- Моите резервации — снимка на ресторанта, статус, отказване и оставяне на отзив
- Отзив се показва inline след изпращане (звезди + коментар); бутонът изчезва
- Настройки — промяна на имена, телефон и парола

### RESTAURANT_ADMIN
- Управление на ресторант — редактиране, снимка, зони, маси (добавяне/редактиране/изтриване)
- Резервации на ресторанта — филтри по дата и статус, действия с loading state
- Създаване на нов ресторант
- Формата за резервация е скрита (не може да резервира)

### SYSTEM_ADMIN
- Управление на всички потребители и роли
- Преглед на всички резервации в системата — филтър по ресторант и статус, пълни действия
- Създаване на ресторанти от името на RESTAURANT_ADMIN потребители

---

## Стартиране

### Предварителни изисквания
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Node.js 18+

### Стъпка 1 — Създай базите данни

```sql
CREATE DATABASE quicktable_users;
CREATE DATABASE quicktable_restaurants;
CREATE DATABASE quicktable_reservations_v2;
```

### Стъпка 2 — Конфигурирай environment variables

Копирай `.env.example` като `.env` за всеки сервиз и попълни стойностите:

```bash
# Spring Boot сервизи (user-service, restaurant-service, reservation-service-v2)
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret_min_32_chars

# Notification service (nodejs-notification-service)
DB_USER=postgres
DB_PASSWORD=your_password
DB_HOST=localhost
DB_PORT=5432
DB_NAME=quicktable_notifications_node
SMTP_HOST=smtp.gmail.com
SMTP_PORT=465
SMTP_USER=your@gmail.com
SMTP_PASSWORD=your_gmail_app_password
PORT=3001
```

### Стъпка 3 — Стартирай сервизите (в тази последователност)

Отвори отделен терминал за всеки сервиз:

```bash
# 1. Първо — User Service (създава таблиците в quicktable_users)
cd user-service
mvn spring-boot:run
# Изчакай докато видиш: Started UserServiceApplication

# 2. Restaurant Service (създава таблиците в quicktable_restaurants)
cd restaurant-service
mvn spring-boot:run
# Изчакай докато видиш: Started RestaurantServiceApplication

# 3. Reservation Service (създава таблиците в quicktable_reservations_v2)
cd reservation-service-v2
mvn spring-boot:run
# Изчакай докато видиш: Started ReservationServiceV2Application

# 4. Notification Service
cd nodejs-notification-service
npm install
npm run dev
# Изчакай докато видиш: Server running on port 3001
```

### Стъпка 4 — Създай първи SYSTEM_ADMIN потребител

След като `user-service` е стартирал напълно:

```bash
psql -U postgres -d quicktable_users -f database/create-admin-only.sql
```

Credentials:
- **Имейл:** `admin@quicktable.com`
- **Парола:** `admin123`

> ⚠️ Смени паролата след първи вход.

### Стъпка 5 — Стартирай frontend

```bash
cd quicktable-ui
npm install
npm run dev
```

### Стъпка 6 — Отвори приложението в браузъра

```
http://localhost:5173
```

Влез с admin credentials → системата те пренасочва автоматично към административния панел.

---

## API документация (Swagger UI)

Всеки endpoint е анотиран с `@Operation` и групиран по роля. JWT автентикация се въвежда чрез бутона "Authorize".

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
- **JWT автентикация** — Stateless, BCrypt хеширане на пароли, SHA-256 за reset токени
- **Role-based UI** — Напълно различен интерфейс за CLIENT, RESTAURANT_ADMIN, SYSTEM_ADMIN
- **Best-fit алгоритъм** — Автоматичен избор на маса с минимален разход на капацитет
- **Имейл уведомления** — При всяка промяна на статус на резервация чрез Gmail SMTP
- **Геокодиране** — Автоматично получаване на координати + Google Maps линк
- **Database per service** — Всеки сервиз има собствена PostgreSQL база
- **Swagger UI** — 42 документирани endpoint-а с примери и JWT автентикация

---

## Автор

**Trayana Nedelcheva**  
Курсов проект по: Софтуерни системи, базирани на услуги
