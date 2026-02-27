-- Създаване на бази данни за Quick Table системата

-- User Service Database
CREATE DATABASE quicktable_users;

-- Restaurant Service Database
CREATE DATABASE quicktable_restaurants;

-- Reservation Service Database
CREATE DATABASE quicktable_reservations;

-- Създаване на потребител (опционално)
-- CREATE USER quicktable_admin WITH PASSWORD 'secure_password';
-- GRANT ALL PRIVILEGES ON DATABASE quicktable_users TO quicktable_admin;
-- GRANT ALL PRIVILEGES ON DATABASE quicktable_restaurants TO quicktable_admin;
-- GRANT ALL PRIVILEGES ON DATABASE quicktable_reservations TO quicktable_admin;

-- Създаване на първия SYSTEM_ADMIN
-- ВАЖНО: Това трябва да се изпълни СЛЕД стартиране на user-service (за да се създаде таблицата)
-- Паролата е BCrypt хеш на "admin123"

-- ЗАБЕЛЕЖКА: Този BCrypt хеш е примерен. За продукция генерирай нов с:
-- cd user-service/src/main/java/com/quicktable/userservice/util
-- javac PasswordHashGenerator.java && java PasswordHashGenerator

\c quicktable_users

-- ВАРИАНТ 1: Ръчно добавяне (препоръчително)
-- Стартирай user-service първо, след това изпълни това:
INSERT INTO users (email, password, first_name, last_name, phone_number, role, active, created_at, updated_at)
VALUES (
    'admin@quicktable.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye/JDhJvQqhxEhB8pP6bx4EsJ.3SdmGIK',  -- BCrypt hash на "admin123"
    'Системен',
    'Администратор',
    '0888000000',
    'SYSTEM_ADMIN',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;

-- ВАРИАНТ 2: Използвай PasswordHashGenerator utility
-- За генериране на нов BCrypt хеш, изпълни:
-- cd user-service && mvn exec:java -Dexec.mainClass="com.quicktable.userservice.util.PasswordHashGenerator"
