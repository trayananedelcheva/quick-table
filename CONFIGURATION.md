# Конфигурация за User Service

## application.yml (или application.properties)

За да създадете конфигурационните файлове, които бяха игнорирани:

### user-service/src/main/resources/application.yml
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

---

### restaurant-service/src/main/resources/application.yml
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

### reservation-service/src/main/resources/application.yml
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

## JWT Secret генериране

JWT secret-ът в user-service е Base64 encoded string. За генериране на нов:

```java
import java.security.SecureRandom;
import java.util.Base64;

public class GenerateSecret {
    public static void main(String[] args) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = Base64.getEncoder().encodeToString(bytes);
        System.out.println("JWT Secret: " + secret);
    }
}
```

---

## Променливи на средата (Environment Variables)

Алтернативно, можете да използвате environment variables:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/quicktable_users}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

jwt:
  secret: ${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
```

Стартиране с променливи:
```bash
DB_PASSWORD=my_secure_password mvn spring-boot:run
```

---

## application-prod.yml (Production)

За production, създайте profile-specific конфигурация:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://production-db-server:5432/quicktable_users
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

logging:
  level:
    com.quicktable: INFO
```

Стартиране:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```
