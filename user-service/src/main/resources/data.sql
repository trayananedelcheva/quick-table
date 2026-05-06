INSERT INTO users (email, password, first_name, last_name, phone_number, role, active, created_at, updated_at)
VALUES (
    'admin@gmail.com',
    '$2a$10$6Pm0INaZc3ETZGwfbBLadetk.h5KDD24GNu7Tol21f1Nb4EhKBdM2',
    'System',
    'Admin',
    NULL,
    'SYSTEM_ADMIN',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;
