-- Fix duplicate table numbers before adding unique constraint
-- Date: 2026-03-04

\c quicktable_restaurants

-- 1. Show all duplicates
SELECT restaurant_id, table_number, COUNT(*) as count, STRING_AGG(id::text, ', ') as table_ids
FROM restaurant_tables
GROUP BY restaurant_id, table_number
HAVING COUNT(*) > 1
ORDER BY restaurant_id, table_number;

-- 2. Delete duplicate entries (keeps only the first one by lowest id)
DELETE FROM restaurant_tables
WHERE id IN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY restaurant_id, table_number ORDER BY id) as rn
        FROM restaurant_tables
    ) t
    WHERE rn > 1
);

-- 3. Show result
SELECT 'Deleted duplicate tables. Now checking for remaining duplicates...' as status;

SELECT restaurant_id, table_number, COUNT(*) as count
FROM restaurant_tables
GROUP BY restaurant_id, table_number
HAVING COUNT(*) > 1;

-- 4. If no duplicates, we can now add the constraint
ALTER TABLE restaurant_tables
ADD CONSTRAINT uk_restaurant_table_number 
UNIQUE (restaurant_id, table_number);

-- 5. Verify
SELECT conname, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conname = 'uk_restaurant_table_number';

SELECT '✓ Constraint successfully added!' as result;
