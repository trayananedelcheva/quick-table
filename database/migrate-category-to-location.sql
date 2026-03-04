-- ================================================================================
-- Migration Script: Rename category → location
-- ================================================================================
-- Описание: Преименува "category" на "location" в цялата система
-- Дата: 2026-03-02
-- Автор: Quick Table Development Team
-- ================================================================================

\connect quicktable_restaurants;

-- 1. Преименуване на колоната category → location в restaurant_tables
ALTER TABLE restaurant_tables 
RENAME COLUMN category TO location;

-- 2. Преименуване на таблицата category_availability → location_availability
ALTER TABLE category_availability 
RENAME TO location_availability;

-- 3. Преименуване на колоната category → location в location_availability
ALTER TABLE location_availability 
RENAME COLUMN category TO location;

-- 4. Проверка на резултатите
SELECT 'Migration completed successfully!' AS status;

-- Показване на новата структура
\d restaurant_tables;
\d location_availability;

-- Показване на данните
SELECT COUNT(*) AS tables_count, location FROM restaurant_tables GROUP BY location;
SELECT COUNT(*) AS availability_count, location FROM location_availability GROUP BY location;
