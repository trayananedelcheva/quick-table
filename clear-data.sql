-- Clear all data from Quick Table databases
-- This will delete all data but keep the table structure

-- ==========================================
-- USERS DATABASE
-- ==========================================
\c quicktable_users;

-- Delete users (admin will be recreated from database-setup.sql if needed)
DELETE FROM users WHERE email != 'admin@quicktable.com';
-- Or delete all including admin:
-- DELETE FROM users;

SELECT 'Users database cleared!' as status;
SELECT COUNT(*) as remaining_users FROM users;


-- ==========================================
-- RESERVATIONS DATABASE
-- ==========================================
\c quicktable_reservations;

-- Delete reservations first (no foreign keys to other tables)
DELETE FROM reservations;

SELECT 'Reservations database cleared!' as status;
SELECT COUNT(*) as remaining_reservations FROM reservations;


-- ==========================================
-- RESTAURANTS DATABASE
-- ==========================================
\c quicktable_restaurants;

-- Delete in order due to foreign key constraints:
-- 1. First delete restaurant_tables (references restaurants)
DELETE FROM restaurant_tables;

-- 2. Then delete category_availability (references restaurants)
DELETE FROM category_availability;

-- 3. Finally delete restaurants
DELETE FROM restaurants;

SELECT 'Restaurants database cleared!' as status;
SELECT COUNT(*) as remaining_restaurants FROM restaurants;
SELECT COUNT(*) as remaining_tables FROM restaurant_tables;
SELECT COUNT(*) as remaining_categories FROM category_availability;


-- ==========================================
-- SUMMARY
-- ==========================================
SELECT '========== DATA CLEARING COMPLETE ==========' as summary;
