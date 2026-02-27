-- Create ONLY the first SYSTEM_ADMIN user
-- Execute this in quicktable_users database

-- First delete if exists
DELETE FROM users WHERE email = 'admin@quicktable.com';

INSERT INTO users (email, password, first_name, last_name, phone_number, role, active, created_at, updated_at)
VALUES (
    'admin@quicktable.com',
    '$2a$10$jSy1K9rsGa1nQ6RzsPJFweP.3Yv/72nNspt2BV6A.yk3s2Nbst/wO',  -- BCrypt hash of "admin123" (CORRECT)
    'System',
    'Administrator',
    '0888000000',
    'SYSTEM_ADMIN',
    true,
    NOW(),
    NOW()
);

-- Verify if created successfully
SELECT id, email, first_name, last_name, role FROM users WHERE email = 'admin@quicktable.com';
