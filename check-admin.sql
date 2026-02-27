-- Check admin user
SELECT id, email, first_name, last_name, role, active, 
       LEFT(password, 7) as pass_start, LENGTH(password) as pass_length 
FROM users 
WHERE email = 'admin@quicktable.com';
