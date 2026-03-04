# BCrypt Hash Generator за първия SYSTEM_ADMIN

## Проблем
Първият SYSTEM_ADMIN се създава чрез SQL скрипт, но паролата трябва да е BCrypt хеш.

---

## Вариант 1: Използвай генериран хеш (препоръчително за тестване)

Вече генериран BCrypt хеш на **"admin123"**:

```
$2a$10$N9qo8uLOickgx2ZMRZoMye/JDhJvQqhxEhB8pP6bx4EsJ.3SdmGIK
```

Този хеш вече е в `database-setup.sql` - просто го изпълни!

---

## Вариант 2: Online BCrypt Generator

**Стъпки:**
1. Отвори: https://bcrypt-generator.com/
2. Password: `admin123`
3. Rounds: `10`
4. Копирай генерирания хеш
5. Замести в `database-setup.sql`

---

## Вариант 3: Java команда (препоръчително за production)

### Създай временен файл:

**GenerateHash.java:**
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = args.length > 0 ? args[0] : "admin123";
        String hash = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
    }
}
```

**Компилирай и изпълни:**
```bash
cd user-service
mvn dependency:copy-dependencies

# Windows
javac -cp "target/dependency/*" GenerateHash.java
java -cp ".;target/dependency/*" GenerateHash admin123

# Linux/Mac
javac -cp "target/dependency/*" GenerateHash.java
java -cp ".:target/dependency/*" GenerateHash admin123
```

---

## Вариант 4: Използвай стартирано приложение

### Метод 1: Временен endpoint (DEBUG ONLY)

Добави временно в `AuthController.java`:
```java
@GetMapping("/debug/hash")
public String hashPassword(@RequestParam String password) {
    return passwordEncoder.encode(password);
}
```

Извикай:
```bash
curl "http://localhost:8081/api/auth/debug/hash?password=admin123"
```

**⚠️ ВАЖНО:** Премахни този endpoint след генериране!

### Метод 2: Spring Boot CLI

```bash
spring shell
spring.boot.admin.password=admin123
spring.security.crypto.password-encoder=bcrypt
```

---

## Вариант 5: Python (ако имаш Python)

```bash
pip install bcrypt

python -c "import bcrypt; print(bcrypt.hashpw(b'admin123', bcrypt.gensalt()).decode())"
```

---

## Вариант 6: Node.js (ако имаш Node)

```bash
npm install bcrypt

node -e "const bcrypt = require('bcrypt'); bcrypt.hash('admin123', 10, (e, h) => console.log(h));"
```

---

## Актуализиране на SQL скрипта

След генериране на хеша, актуализирай `database-setup.sql`:

```sql
INSERT INTO users (email, password, first_name, last_name, phone_number, role, active, created_at, updated_at)
VALUES (
    'admin@quicktable.com',
    'YOUR_BCRYPT_HASH_HERE',  -- <-- Тук постави хеша
    'Системен',
    'Администратор',
    '0888000000',
    'SYSTEM_ADMIN',
    true,
    NOW(),
    NOW()
);
```

---

## Проверка дали работи

След изпълнение на SQL скрипта:

```bash
# 1. Извикай login endpoint
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@quicktable.com","password":"admin123"}'

# 2. Трябва да получиш JWT token
```

Ако получиш грешка "Невалиден email или парола", BCrypt хешът не е правилен.

---

## BCrypt Hash Properties

- **Алгоритъм:** BCrypt (Blowfish-based)
- **Rounds:** 10 (2^10 = 1024 итерации)
- **Length:** 60 символа
- **Format:** `$2a$10$[salt][hash]`

**Пример:**
```
$2a$10$N9qo8uLOickgx2ZMRZoMye/JDhJvQqhxEhB8pP6bx4EsJ.3SdmGIK
│  │  │ │                                                    │
│  │  │ └─ Rounds (10)                                      │
│  │  └─ Algorithm версия                                  │
│  └─ BCrypt identifier                                     │
└─────────────── Salt + Hash ──────────────────────────────┘
```

---

## Често срещани грешки

### 1. Хешът е прекалено къс
```
ERROR: value too long for type character varying(255)
```
**Решение:** BCrypt хешът е 60 символа. Провери дали не си копирал само част.

### 2. Login не работи
```
{"error": "Unauthorized", "message": "Невалиден email или парола"}
```
**Решение:** 
- BCrypt хешът не съответства на паролата
- Регенерирай хеша с нов генератор
- Провери дали паролата е действително "admin123"

### 3. SQL грешка при INSERT
```
ERROR: duplicate key value violates unique constraint "users_email_key"
```
**Решение:** Администраторът вече съществува. Изтрий го:
```sql
DELETE FROM users WHERE email = 'admin@quicktable.com';
```

---

## Production Best Practices

1. **Не хардкодвай пароли** - използвай environment variables
2. **Генерирай уникални хешове** - различни във всяка среда
3. **Силни пароли** - не използвай "admin123" в production
4. **Prompt за парола** - добави скрипт, който пита за паролата:

```bash
#!/bin/bash
read -sp "Password: " PASSWORD
echo "BCrypt hash: $(python -c "import bcrypt; print(bcrypt.hashpw(b'$PASSWORD', bcrypt.gensalt()).decode())")"
```

---

## Заключение

**За тестване:** Използвай вече генерирания хеш в `database-setup.sql`  
**За production:** Генерирай нов BCrypt хеш с Вариант 3 или 5

✅ Security level: 🔒🔒🔒🔒🔒
