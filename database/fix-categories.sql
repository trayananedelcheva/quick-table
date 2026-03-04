-- Migration Script: Set default categories for existing tables
-- Run this on quicktable_restaurants database

\c quicktable_restaurants;

-- Set all existing NULL categories to INSIDE as default
UPDATE restaurant_tables 
SET category = 'INSIDE'
WHERE category IS NULL;

-- Verification
SELECT 'Update complete!' as status;
SELECT id, restaurant_id, table_number, capacity, category, available 
FROM restaurant_tables 
ORDER BY restaurant_id, table_number;
