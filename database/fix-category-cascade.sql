-- Fix CASCADE delete for category_availability and restaurant_tables
\c quicktable_restaurants;

-- Fix category_availability constraint
ALTER TABLE category_availability 
DROP CONSTRAINT IF EXISTS fk16ncb9i4pj9h32vwpiu75yah6;

ALTER TABLE category_availability
ADD CONSTRAINT fk_category_restaurant 
FOREIGN KEY (restaurant_id) 
REFERENCES restaurants(id) 
ON DELETE CASCADE;

-- Fix restaurant_tables constraint (може да има различно име)
-- Намираме името на съществуващия constraint
DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT conname INTO constraint_name
    FROM pg_constraint
    WHERE conrelid = 'restaurant_tables'::regclass
    AND confrelid = 'restaurants'::regclass;
    
    IF constraint_name IS NOT NULL THEN
        EXECUTE 'ALTER TABLE restaurant_tables DROP CONSTRAINT ' || constraint_name;
    END IF;
END $$;

-- Добавяме нов constraint с CASCADE
ALTER TABLE restaurant_tables
ADD CONSTRAINT fk_table_restaurant 
FOREIGN KEY (restaurant_id) 
REFERENCES restaurants(id) 
ON DELETE CASCADE;

-- Verify
SELECT 'Cascade delete constraints fixed!' as status;
SELECT 
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    rc.delete_rule
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
JOIN information_schema.referential_constraints AS rc
    ON rc.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
AND tc.table_name IN ('category_availability', 'restaurant_tables')
ORDER BY tc.table_name;
