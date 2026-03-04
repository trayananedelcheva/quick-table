# 🧪 Testing

Тази папка съдържа ресурси за тестване на Quick Table API.

## Файлове

### Postman Collections
- **[Quick-Table-API.postman_collection.json](Quick-Table-API.postman_collection.json)** - Пълна Postman колекция с всички API endpoints

## 📦 Импортиране в Postman

1. Отвори **Postman**
2. Click **Import** (горе вляво)
3. Избери файла `Quick-Table-API.postman_collection.json`
4. Готово! Виж всички заявки организирани по services

## 🎯 Використване

### Variables
Колекцията използва environment variables:
- `user_service_url` - http://localhost:8081
- `restaurant_service_url` - http://localhost:8082
- `reservation_service_url` - http://localhost:8083
- `jwt_token` - Автоматично се попълва след Login

### Authentication Flow
1. **Login** → автоматично запазва JWT token
2. Всички други заявки използват `{{jwt_token}}` автоматично

## 📋 Налични Endpoints

### User Service (8081)
- **POST** /api/auth/register - Регистрация
- **POST** /api/auth/login - Login (запазва JWT)
- **GET** /api/users/me - Моят профил

### Restaurant Service (8082)
- **POST** /api/restaurants - Създаване на ресторант
- **GET** /api/restaurants - Всички ресторанти
- **GET** /api/restaurants/{id} - Детайли за ресторант (групирани маси по location)
- **POST** /api/restaurants/{id}/tables - Добавяне на маса
- **PUT** /api/restaurants/{id}/locations/{location}/toggle - Enable/Disable location
- **GET** /api/restaurants/{id}/locations - Location availability status
- **PUT** /api/restaurants/{id}/tables/{tableNumber}/availability - Отвори/затвори маса
- **DELETE** /api/restaurants/{id} - Изтриване (soft/hard)

### Reservation Service (8083)
- **GET** /api/reservations/restaurant/{id}/available-slots - Свободни часове
  - Query params: `date`, `guestsCount`, `location` (optional)
- **POST** /api/reservations - Създаване на резервация
  - Body: `preferredLocation` (opcional)
- **GET** /api/reservations/my - Моите резервации
- **GET** /api/reservations/{id} - Детайли за резервация
- **PUT** /api/reservations/{id}/status - Промяна на статус
- **DELETE** /api/reservations/{id} - Отказ на резервация

## 🔄 Промени след Refactoring

След category → location рефакторинга:

**Променени endpoints:**
- `/categories/` → `/locations/`
- `/categories/{category}/enable` + `/disable` → `/locations/{location}/toggle?enabled=true/false`

**Променени fields:**
- `category` → `location`
- `preferredCategory` → `preferredLocation`

**Нова JSON структура:**
```json
{
  "locations": {
    "INSIDE": {
      "displayName": "Вътре",
      "enabled": true,
      "tables": [...]
    },
    "SUMMER_GARDEN": {...},
    "WINTER_GARDEN": {...}
  }
}
```

## 🚀 Quick Start

1. Стартирай всички services
2. Import Postman collection
3. Run "Login" заявка → get JWT token
4. Експериментирай с другите endpoints!

## Навигация

- [📚 Documentation](../docs/) - Документация
- [🗄️ Database Scripts](../database/) - SQL файлове
- [🏠 Root](../) - Обратно към главната директория
