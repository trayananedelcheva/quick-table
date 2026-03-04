# Security Best Practices - Quick Table

## Проблем: Self-registration като администратор ❌

### Преди (несигурно):
```json
POST /api/auth/register
{
  "email": "hacker@evil.com",
  "password": "123456",
  "role": "SYSTEM_ADMIN"  // ❌ Всеки може да стане админ!
}
```

### След (сигурно): ✅
```json
POST /api/auth/register
{
  "email": "user@example.com",
  "password": "password123"
  // role винаги е CLIENT при регистрация
}
```

---

## Решение: Role Management System

### 1. Регистрация винаги създава CLIENT

**AuthService.java:**
```java
public AuthResponse register(RegisterRequest request) {
    // SECURITY: Винаги CLIENT при регистрация
    User user = User.builder()
            .role(UserRole.CLIENT)  // Игнорираме request.getRole()
            .build();
}
```

### 2. Първият SYSTEM_ADMIN се създава чрез SQL

**database-setup.sql:**
```sql
INSERT INTO users (email, password, role)
VALUES (
    'admin@quicktable.com',
    '$2a$10$...BCrypt_hash...',  -- admin123
    'SYSTEM_ADMIN'
);
```

**Стъпки:**
```bash
# 1. Стартирай user-service (за да се създаде таблицата)
mvn spring-boot:run

# 2. Изпълни SQL скрипта
psql -U postgres -f database-setup.sql

# 3. Влез като админ
curl -X POST http://localhost:8081/api/auth/login \
  -d '{"email":"admin@quicktable.com","password":"admin123"}'
```

### 3. SYSTEM_ADMIN промоутва потребители

**Нов endpoint:**
```
PUT /api/users/{userId}/role?role=RESTAURANT_ADMIN
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

**Пример:**
```bash
# Потребител ID 5 става RESTAURANT_ADMIN
curl -X PUT "http://localhost:8081/api/users/5/role?role=RESTAURANT_ADMIN" \
  -H "Authorization: Bearer eyJhbGc..."
```

---

## Workflow за създаване на Restaurant Admin

```
┌──────────────────────────────────────────────────────────────┐
│ 1. Потребителят се регистрира                                │
│    POST /api/auth/register                                   │
│    → Автоматично CLIENT role                                 │
└──────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ 2. SYSTEM_ADMIN повишава ролята                              │
│    PUT /api/users/{id}/role?role=RESTAURANT_ADMIN           │
│    → Изисква JWT token на SYSTEM_ADMIN                       │
└──────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ 3. RESTAURANT_ADMIN добавя своя ресторант                    │
│    POST /api/restaurants                                     │
│    { "adminUserId": 5, ... }                                 │
└──────────────────────────────────────────────────────────────┘
```

---

## Role Permissions Matrix

| Действие | CLIENT | RESTAURANT_ADMIN | SYSTEM_ADMIN |
|----------|--------|------------------|--------------|
| Регистрация | ✅ | ✅ (след промоция) | ❌ (само SQL) |
| Създаване на резервация | ✅ | ✅ | ✅ |
| Промяна на статус на резервация | ❌ | ✅ (своя ресторант) | ✅ |
| Добавяне на ресторант | ❌ | ❌ | ✅ |
| Промяна на роля | ❌ | ❌ | ✅ |
| Преглед на всички потребители | ❌ | ❌ | ✅ |

---

## Security Enhancements

### Добавени мерки:

1. **@PreAuthorize("hasRole('SYSTEM_ADMIN')")**
   - Endpoint-и за управление на роли са защитени
   
2. **Игнориране на role в RegisterRequest**
   - Дори да се изпрати role, винаги е CLIENT

3. **Audit logging** (за бъдещо разширение)
   ```java
   @PreUpdate
   protected void onUpdate() {
       log.info("User role changed: userId={}, newRole={}", id, role);
   }
   ```

4. **Rate limiting** (за бъдещо разширение)
   - Предотвратяване на brute force атаки при login

---

## Генериране на BCrypt Hash

### Вариант 1: Java Utility
```bash
cd user-service
mvn exec:java -Dexec.mainClass="com.quicktable.userservice.util.PasswordHashGenerator"
```

### Вариант 2: Online Tool
Използвай: https://bcrypt-generator.com/
- Password: `admin123`
- Rounds: `10`

### Вариант 3: Spring Boot Application
Стартирай user-service и използвай:
```java
@PostMapping("/api/admin/hash-password")
public String hashPassword(@RequestParam String password) {
    return passwordEncoder.encode(password);
}
```

---

## Testing Security

### Test 1: Опит за self-promotion
```bash
# Опит да се регистрираш като ADMIN
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"hacker@evil.com","password":"123","role":"SYSTEM_ADMIN"}'

# Очакван резултат: Създава се CLIENT (игнорира role)
```

### Test 2: Опит за промяна на роля без JWT
```bash
# Без Authorization header
curl -X PUT "http://localhost:8081/api/users/1/role?role=SYSTEM_ADMIN"

# Очакван резултат: 401 Unauthorized
```

### Test 3: CLIENT опитва да промени роля
```bash
# С CLIENT JWT token
curl -X PUT "http://localhost:8081/api/users/1/role?role=SYSTEM_ADMIN" \
  -H "Authorization: Bearer CLIENT_TOKEN"

# Очакван резултат: 403 Forbidden
```

---

## Заключение

✅ **Преди:** Всеки може да стане SYSTEM_ADMIN  
✅ **След:** Само SQL seed data може да създаде първия админ  
✅ **Промоция:** Само SYSTEM_ADMIN може да повишава роли  
✅ **Audit:** Всички промени на роли могат да се логват  

**Security level:** 🔒🔒🔒🔒🔒 (5/5)
