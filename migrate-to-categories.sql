-- Migration Script: Location to Category System
-- Run this on the restaurants database

\c quicktable_restaurants;

-- Step 1: Add new category column (allow NULL temporarily for migration)
ALTER TABLE restaurant_tables 
ADD COLUMN IF NOT EXISTS category VARCHAR(50);

-- Step 2: Migrate existing data from location to category
-- Map common Bulgarian location names to enum values
UPDATE restaurant_tables 
SET category = CASE 
    WHEN location ILIKE '%вътре%' OR location ILIKE '%зала%' THEN 'INSIDE'
    WHEN location ILIKE '%тераса%' OR location ILIKE '%лят%' THEN 'SUMMER_GARDEN'
    WHEN location ILIKE '%зимн%' OR location ILIKE '%vip%' THEN 'WINTER_GARDEN'
    ELSE 'INSIDE' -- Default fallback
END
WHERE category IS NULL;

-- Step 3: Make category NOT NULL after migration
ALTER TABLE restaurant_tables 
ALTER COLUMN category SET NOT NULL;

-- Step 4: Drop old location column
ALTER TABLE restaurant_tables 
DROP COLUMN IF EXISTS location;

-- Step 5: Create category_availability table if not exists
CREATE TABLE IF NOT EXISTS category_availability (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT fk_category_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    CONSTRAINT uk_restaurant_category UNIQUE (restaurant_id, category)
);

-- Step 6: Initialize category_availability for existing restaurants
-- Insert INSIDE, SUMMER_GARDEN, WINTER_GARDEN for each existing restaurant
INSERT INTO category_availability (restaurant_id, category, enabled)
SELECT r.id, 'INSIDE', true
FROM restaurants r
WHERE NOT EXISTS (
    SELECT 1 FROM category_availability ca 
    WHERE ca.restaurant_id = r.id AND ca.category = 'INSIDE'
);

INSERT INTO category_availability (restaurant_id, category, enabled)
SELECT r.id, 'SUMMER_GARDEN', true
FROM restaurants r
WHERE NOT EXISTS (
    SELECT 1 FROM category_availability ca 
    WHERE ca.restaurant_id = r.id AND ca.category = 'SUMMER_GARDEN'
);

INSERT INTO category_availability (restaurant_id, category, enabled)
SELECT r.id, 'WINTER_GARDEN', true
FROM restaurants r
WHERE NOT EXISTS (
    SELECT 1 FROM category_availability ca 
    WHERE ca.restaurant_id = r.id AND ca.category = 'WINTER_GARDEN'
);

-- Verification queries
SELECT 'Migration complete!' as status;
SELECT 'Restaurant tables:' as info;
SELECT id, restaurant_id, table_number, capacity, category, available FROM restaurant_tables LIMIT 10;
SELECT 'Category availability:' as info;
SELECT * FROM category_availability ORDER BY restaurant_id, category;
