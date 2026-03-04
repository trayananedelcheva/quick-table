# 🚀 Пълни инструкции за стартиране на Quick Table

## ⚠️ ПРЕДИ ДА ЗАПОЧНЕШ

Тези 4 файла са конфигурирани да се игнорират от Copilot и **ТРЯБВА** да ги създадеш **РЪЧНО**:
1. `user-service/src/main/resources/application.yml`
2. `restaurant-service/src/main/resources/application.yml`
3. `reservation-service/src/main/resources/application.yml`
4. `user-service/src/main/java/com/quicktable/userservice/security/JwtTokenProvider.java`

---

## 📋 Стъпка 1: Създай базите данни

Отвори **PowerShell** или **CMD** и изпълни:

```powershell
# Влез в PostgreSQL
psql -U postgres

# След като се логнеш, изпълни:
CREATE DATABASE quicktable_users;
CREATE DATABASE quicktable_restaurants;
CREATE DATABASE quicktable_reservations;

# Излез с:
\q
```

**Алтернативно** (ако имаш pgAdmin):
- Отвори pgAdmin
- Десен бутон на "Databases" → Create → Database
- Създай трите бази ръчно

---

## 📄 Стъпка 2: Създай application.yml файловете

### 🔹 Файл 1: user-service/src/main/resources/application.yml

1. Отвори проекта в File Explorer
2. Навигирай до `c:\develop\university\quick-table\user-service\src\main\`
3. **Създай нова папка** с име `resources` (ако не съществува)
4. Влез в папката `resources`
5. Създай нов файл с име `application.yml`
6. Отвори файла с Notepad++ или VS Code
7. **Копирай и залепи ТОЧНО това съдържание:**

```yaml
spring:
  application:
    name: user-service
  datasource:
    url: jdbc:postgresql://localhost:5432/quicktable_users
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

server:
  port: 8081

jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
  expiration: 86400000

logging:
  level:
    com.quicktable: DEBUG
```

⚠️ **ВАЖНО:** Ако паролата на твоя PostgreSQL е различна от `postgres`, смени реда:
```yaml
    password: твоята_парола_тук
```

---

### 🔹 Файл 2: restaurant-service/src/main/resources/application.yml

1. Навигирай до `c:\develop\university\quick-table\restaurant-service\src\main\`
2. Създай папка `resources`
3. Създай файл `application.yml`
4. **Копирай и залепи:**

```yaml
spring:
  application:
    name: restaurant-service
  datasource:
    url: jdbc:postgresql://localhost:5432/quicktable_restaurants
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

server:
  port: 8082

logging:
  level:
    com.quicktable: DEBUG
```

---

### 🔹 Файл 3: reservation-service/src/main/resources/application.yml

1. Навигирай до `c:\develop\university\quick-table\reservation-service\src\main\`
2. Създай папка `resources`
3. Създай файл `application.yml`
4. **Копирай и залепи:**

```yaml
spring:
  application:
    name: reservation-service
  datasource:
    url: jdbc:postgresql://localhost:5432/quicktable_reservations
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

server:
  port: 8083

logging:
  level:
    com.quicktable: DEBUG
```

---

## 🔐 Стъпка 3: Създай JwtTokenProvider.java

1. Навигирай до `c:\develop\university\quick-table\user-service\src\main\java\com\quicktable\userservice\security\`
2. Вече трябва да съществува папката `security` (има други класове там)
3. Създай нов файл с име `JwtTokenProvider.java`
4. **Копирай и залепи ЦЕЛИЯ този код:**

```java
package com.quicktable.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

⚠️ **ВАЖНО:** Ако файлът вече съществува, ИЗТРИЙ го напълно и създай нов с този код (за JJWT 0.12.3)!

---

## 🏗️ Стъпка 4: Build на проекта

Отвори **PowerShell** в главната папка на проекта:

```powershell
cd c:\develop\university\quick-table
mvn clean install
```

**Очаквано време:** 2-4 минути (при първото изпълнение)

✅ **Успех ако видиш:**
```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

❌ **Ако видиш грешки:**
- Провери дали си създал всички 4 файла
- Провери дали application.yml файловете имат правилно форматиране (YAML е чувствителен към интервалите!)
- Провери дали PostgreSQL е стартиран

---

## 🚀 Стъпка 5: Стартирай сервизите

⚠️ **ВАЖНО:** Стартирай сервизите **ПО РЕДА** (reservation-service извиква restaurant-service!)

### Отвори 3 ОТДЕЛНИ PowerShell терминала:

#### 🟢 Терминал 1 - Restaurant Service (първи!):
```powershell
cd c:\develop\university\quick-table\restaurant-service
mvn spring-boot:run
```

✅ Чакай да видиш: `Started RestaurantServiceApplication in X.XXX seconds (JVM running for X.XXX)`

---

#### 🟡 Терминал 2 - User Service:
```powershell
cd c:\develop\university\quick-table\user-service
mvn spring-boot:run
```

✅ Чакай да видиш: `Started UserServiceApplication in X.XXX seconds`

---

#### 🔵 Терминал 3 - Reservation Service:
```powershell
cd c:\develop\university\quick-table\reservation-service
mvn spring-boot:run
```

✅ Чакай да видиш: `Started ReservationServiceApplication in X.XXX seconds`

---

## 👤 Стъпка 6: Създай първия администратор

След като **user-service** е стартиран и е създал таблиците, изпълни:

```powershell
# Отвори НОВА PowerShell конзола (4-та)
cd c:\develop\university\quick-table
psql -U postgres -d quicktable_users -f database-setup.sql
```

Това създава:
- **Email:** `admin@quicktable.com`
- **Парола:** `admin123`
- **Роля:** SYSTEM_ADMIN

---

## 🧪 Стъпка 7: Тествай с Postman

1. Отвори **Postman**
2. **Import** → Избери `Quick-Table-API.postman_collection.json`
3. Ще видиш collection "Quick Table API" с всички endpoints

### Тестови сценарии:

#### ✅ Сценарий 1: Login като администратор
1. Отвори **User Service → Login**
2. Body вече съдържа: `admin@quicktable.com` / `admin123`
3. **Send**
4. **Копирай JWT токена** от response

#### ✅ Сценарий 2: Създай ресторант (с Nominatim геокодиране)
1. Отвори **Restaurant Service → Create Restaurant**
2. Body съдържа примерен ресторант с адрес в София
3. **Send**
4. Виж в response полетата `latitude` и `longitude` - попълнени автоматично от **OpenStreetMap Nominatim API**!

#### ✅ Сценарий 3: Регистрирай нов CLIENT потребител
1. Отвори **User Service → Register Client**
2. Промени email в body на нещо уникално
3. **Send**
4. Новият потребител автоматично получава роля `CLIENT` (не може да се регистрира като ADMIN - security!)

#### ✅ Сценарий 4: Виж налични часове (Time-slot UX)
1. Отвори **Reservation Service → Get Available Time Slots**
2. URL параметри:
   - `restaurantId=1`
   - `date=2026-03-15`
   - `guestsCount=4`
   - `location=Тераса` (или `Вътре`, `Градина`, `ANY`)
3. **Send**
4. Ще видиш списък само със **свободните часове** (не се показват номера на маси!)

#### ✅ Сценарий 5: Направи резервация (автоматично избрана маса)
1. Отвори **Reservation Service → Create Reservation**
2. Body съдържа:
   ```json
   {
     "restaurantId": 1,
     "reservationDate": "2026-03-15",
     "reservationTime": "19:00:00",
     "guestsCount": 4,
     "locationPreference": "Тераса",
     "customerName": "Иван Иванов",
     "customerPhone": "0888123456",
     "customerEmail": "ivan@example.com"
   }
   ```
3. **Забележи:** Няма `tableId` - системата **автоматично** избира подходяща маса!
4. **Send**
5. Виж в **reservation-service логовете** (PowerShell терминал 3):
   ```
   Извикване на restaurant-service за маси на ресторант 1
   Избрана маса 3 (капацитет: 6, локация: Тераса)
   ```
   **Това е inter-service communication в действие!**

---

## 🔍 Стъпка 8: Провери inter-service communication

След създаване на резервация гледай в **Терминал 3** (reservation-service):

```
DEBUG - Извикване на restaurant-service за маси на ресторант 1
DEBUG - Намерени 8 налични маси с капацитет >= 4
INFO  - Избрана маса 3 (капацитет: 6, локация: Тераса)
```

Това показва че:
1. **reservation-service** извиква **restaurant-service** на `http://localhost:8082`
2. Взима списък с маси
3. Филтрира по капацитет, локация и наличност
4. Избира подходяща маса автоматично

---

## 📊 Проверка на портовете

Сервизите трябва да работят на:

| Сервиз | Порт | URL |
|--------|------|-----|
| **user-service** | 8081 | http://localhost:8081/api/auth/login |
| **restaurant-service** | 8082 | http://localhost:8082/api/restaurants |
| **reservation-service** | 8083 | http://localhost:8083/api/reservations |

За да проверя дали портовете са заети:
```powershell
netstat -ano | findstr :8081
netstat -ano | findstr :8082
netstat -ano | findstr :8083
```

---

## 🐛 Често срещани проблеми

### Проблем 1: "Port 8081 already in use"
**Решение:**
```powershell
# Намери процеса:
netstat -ano | findstr :8081

# Убий процеса (замени 1234 с PID от горната команда):
taskkill /PID 1234 /F
```

### Проблем 2: "Connection refused" при psql
**Решение:**
- Провери дали PostgreSQL е стартиран:
  - Windows: Services → PostgreSQL → Start
  - Или рестартирай компютъра

### Проблем 3: YAML parsing грешки
**Решение:**
- YAML използва **интервали** (не табове!)
- Всяко ниво е с **2 интервала**
- Виж примера по-горе и копирай **точно**

### Проблем 4: "Table 'users' doesn't exist"
**Решение:**
- Hibernate създава таблиците автоматично при първо стартиране
- Стартирай user-service и чакай 10-15 секунди
- След това изпълни `database-setup.sql`

---

## ✅ Готово!

Сега имаш пълно работещо приложение с:
- ✅ 3 микросервиза (User, Restaurant, Reservation)
- ✅ JWT автентикация
- ✅ Role-based access control (CLIENT, RESTAURANT_ADMIN, SYSTEM_ADMIN)
- ✅ Външна услуга (OpenStreetMap Nominatim API)
- ✅ Inter-service communication (WebClient)
- ✅ Time-slot UX (потребителите виждат само свободни часове)
- ✅ Location preference (Вътре/Тераса/Градина)
- ✅ Автоматично избиране на маса

**Следваща стъпка:** Виж `API-EXAMPLES.md` за повече примери или `ROLE-BASED-BUSINESS-RULES.md` за бизнес логиката.
