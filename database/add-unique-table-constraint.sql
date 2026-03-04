-- Migration: Add unique constraint for tableNumber per restaurant
-- Date: 2026-03-04
-- Description: Prevents duplicate table numbers within the same restaurant

-- Connect to the database
\c quicktable_restaurants

-- Check for existing duplicate table numbers before adding constraint
DO $$
DECLARE
    duplicate_count INTEGER;
BEGIN
    -- Count duplicates
    SELECT COUNT(*) INTO duplicate_count
    FROM (
        SELECT restaurant_id, table_number, COUNT(*) as cnt
        FROM restaurant_tables
        GROUP BY restaurant_id, table_number
        HAVING COUNT(*) > 1
    ) duplicates;

    -- Report duplicates if any
    IF duplicate_count > 0 THEN
        RAISE NOTICE 'WARNING: Found % duplicate table number(s) in the database!', duplicate_count;
        RAISE NOTICE 'Showing duplicates:';
        
        FOR rec IN 
            SELECT restaurant_id, table_number, COUNT(*) as count
            FROM restaurant_tables
            GROUP BY restaurant_id, table_number
            HAVING COUNT(*) > 1
            ORDER BY restaurant_id, table_number
        LOOP
            RAISE NOTICE 'Restaurant ID: %, Table Number: %, Count: %', rec.restaurant_id, rec.table_number, rec.count;
        END LOOP;
        
        RAISE EXCEPTION 'Cannot add unique constraint while duplicates exist. Please fix duplicates first.';
    ELSE
        RAISE NOTICE 'No duplicates found. Safe to add unique constraint.';
    END IF;
END $$;

-- Add unique constraint
ALTER TABLE restaurant_tables
ADD CONSTRAINT uk_restaurant_table_number 
UNIQUE (restaurant_id, table_number);

-- Verify constraint was added
SELECT 
    conname as constraint_name,
    contype as constraint_type,
    pg_get_constraintdef(oid) as definition
FROM pg_constraint
WHERE conname = 'uk_restaurant_table_number';

-- Show confirmation
DO $$
BEGIN
    RAISE NOTICE '✓ Successfully added unique constraint: uk_restaurant_table_number';
    RAISE NOTICE '✓ Table numbers are now unique per restaurant';
    RAISE NOTICE '';
    RAISE NOTICE 'Test the constraint:';
    RAISE NOTICE '  - Try adding duplicate tableNumber → Should fail with error';
    RAISE NOTICE '  - Different restaurants CAN have same tableNumber → Should work';
END $$;
