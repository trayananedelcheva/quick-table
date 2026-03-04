# 🗄️ Database Scripts

Тази папка съдържа всички SQL скриптове за база данни PostgreSQL.

## Файлове

### Основна инсталация
- **[database-setup.sql](database-setup.sql)** - Пълна схема и структура на всички три databases
  - `quicktable_users` - User Service database
  - `quicktable_restaurants` - Restaurant Service database
  - `quicktable_reservations` - Reservation Service database

### Първоначални данни
- **[create-admin-only.sql](create-admin-only.sql)** - Създава САМО admin акаунт
- **[sample-data.sql](sample-data.sql)** - Примерни данни за тестване (ресторанти, маси, резервации)

### Миграции
- **[migrate-category-to-location.sql](migrate-category-to-location.sql)** - ✨ **ПОСЛЕДНА МИГРАЦИЯ**: Преименува category → location
  - Преименува колони в `restaurant_tables` и `location_availability`
  - Запазва всички данни и constraints
  
- **[migrate-to-categories.sql](migrate-to-categories.sql)** - Стара миграция за категории

### Корекции и фиксове
- **[fix-categories.sql](fix-categories.sql)** - Поправки на категории
- **[fix-category-cascade.sql](fix-category-cascade.sql)** - Поправки на CASCADE constraints

### Utility скриптове
- **[check-admin.sql](check-admin.sql)** - Проверка на admin акаунти
- **[clear-data.sql](clear-data.sql)** - Изтрива тестови данни (WARNING: използвай внимателно!)

## 🚀 Начална инсталация

```bash
# 1. Създай databases и схема
psql -U postgres -f database-setup.sql

# 2. Създай admin акаунт
psql -U postgres -f create-admin-only.sql

# 3. (Опционално) Добави примерни данни
psql -U postgres -f sample-data.sql

# 4. Изпълни последната миграция
psql -U postgres -f migrate-category-to-location.sql
```

## 🔄 Миграции

Миграциите трябва да се изпълняват в този ред:
1. `database-setup.sql` - първоначална схема
2. `migrate-category-to-location.sql` - актуална версия

## ⚠️ Внимание

- **clear-data.sql** - Изтрива ВСИЧКИ данни! Използвай само за development reset.
- Всички миграции включват проверки и запазват съществуващите данни.
- Foreign key constraints са с `ON DELETE CASCADE` - внимавай при изтриване!

## Навигация

- [📚 Documentation](../docs/) - Документация
- [🧪 Testing](../testing/) - Postman колекции
- [🏠 Root](../) - Обратно към главната директория
