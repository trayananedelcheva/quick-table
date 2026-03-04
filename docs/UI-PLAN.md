# Quick Table - UI/Frontend План

## Общ преглед

Това е план за бъдещо frontend приложение, което ще използва REST API'тата на системата.  
Frontend-ът може да бъде разработен с React, Angular, Vue.js или друга JavaScript библиотека.

---

## Архитектура

```
┌─────────────────────────────────────────┐
│         Frontend (React/Vue)            │
│  ┌─────────────────────────────────┐    │
│  │   User Interface Components     │    │
│  └─────────────────────────────────┘    │
│  ┌─────────────────────────────────┐    │
│  │  API Client (Axios/Fetch)       │    │
│  └─────────────────────────────────┘    │
│  ┌─────────────────────────────────┐    │
│  │  JWT Token Storage (LocalStorage)│   │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
              ↓ REST API
┌─────────────────────────────────────────┐
│      Quick Table Microservices          │
│  ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│  │  User    │ │Restaurant│ │Reserv-  │ │
│  │ Service  │ │ Service  │ │ation    │ │
│  │  :8081   │ │  :8082   │ │Service  │ │
│  └──────────┘ └──────────┘ │ :8083   │ │
└─────────────────────────────┴─────────┘
```

---

## Екрани по роли

### 1. Публични екрани (Неаутентикирани потребители)

#### 1.1. Login Page (`/login`)
- Email поле
- Password поле
- "Вход" бутон
- Link към Registration page

**API:** `POST /api/auth/login`

#### 1.2. Registration Page (`/register`)
- Име
- Email
- Телефон
- Password
- Confirm Password
- "Регистрация" бутон (създава CLIENT потребител)
- Link към Login page

**API:** `POST /api/auth/register`

#### 1.3. Restaurant Browse Page (`/restaurants`)
- Списък с всички ресторанти (карти с име, адрес, часове)
- Филтри: по име, локация, налични маси
- Търсене
- Clicking на ресторант → вижда детайли + маси

**API:** `GET /api/restaurants`

---

### 2. CLIENT екрани (Обикновени потребители)

После като се логнат като CLIENT, те виждат:

#### 2.1. Dashboard (`/client/dashboard`)
- Welcome message
- Quick links: "Резервирай маса", "Моите резервации"
- Бутон за Logout

#### 2.2. Browse Restaurants (`/client/restaurants`)
- Списък с всички ресторанти (карти с име, адрес, часове)
- Филтри: по име, локация
- Clicking на ресторант → Redirects към резервационна форма

**API:** `GET /api/restaurants`

#### 2.3. Create Reservation (`/client/reserve/{restaurantId}`)
**Нов UX - без показване на маси:**

**Стъпки:**
1. Избор на брой гости (dropdown: 1-10)
2. **Избор на локация** (radio buttons: "Вътре" / "Тераса" / "Без значение")
3. Избор на дата (calendar picker)
4. **Системата показва само свободни часове** (не маси!)
5. Избор на час от списък

**API Calls:**
```javascript
// Стъпка 1: Избери брой гости
const guestsCount = 4;

// Стъпка 2: Избери локация
const location = "Тераса"; // или "Вътре" или "ANY"

// Стъпка 3: Избери дата
const date = "2026-03-15";

// Стъпка 4: Вземи свободни часове (с location филтър!)
GET /api/reservations/restaurant/{restaurantId}/available-slots?date=2026-03-15&guestsCount=4&location=Тераса
// Отговор: ["10:00:00", "11:00:00", "14:00:00", "19:00:00", "20:00:00"]

// Стъпка 5: CLIENT избира час (напр. 19:00)
const selectedTime = "19:00:00";

// Стъпка 6: Създай резервация (БЕЗ tableId - системата автоматично избира маса)
POST /api/reservations?userId={userId}&userRole=CLIENT
Body: {
  restaurantId: 1,
  reservationDate: "2026-03-15",
  reservationTime: "19:00:00",
  guestsCount: 4,
  locationPreference: "Тераса",
  specialRequests: "Празнуваме рожден ден"
}

// Inter-service комуникация:
// reservation-service извиква restaurant-service за да вземе налични маси на терасата
// Проверява коя маса е свободна за избрания час
// Автоматично assign-ва подходяща маса
```

**Важно:** CLIENT НЕ вижда:
- Номера на маси
- Къде ТОЧНО се намират масите (номер на маса, позиция)
- Капацитет на отделни маси

CLIENT вижда само:
- ✅ Избор на локация (Вътре/Тераса)
- ✅ Свободни часове за избрания брой гости и локация
- ✅ Статус на резервацията след създаване

#### 2.4. My Reservations (`/client/my-reservations`)
- Списък с всички мои резервации
  - Статус: PENDING, CONFIRMED, CANCELLED
  - Дата и час
  - Ресторант
  - Брой гости
- Бутон "Откажи" за PENDING/CONFIRMED резервации

**API:** `GET /api/reservations?userId={userId}`  
**API:** `PUT /api/reservations/{id}/status?status=CANCELLED`

---

### 3. RESTAURANT_ADMIN екрани (Собственици на ресторанти)

#### 3.1. Dashboard (`/admin/dashboard`)
- Welcome message: "Управление на {Restaurant Name}"
- Статистики: Брой резервации днес, брой маси, работно време
- Quick links: "Резервации", "Управление на маси", "Настройки"

#### 3.2. My Restaurant Reservations (`/admin/reservations`)
- **Календар изглед** с всички резервации
- Филтри: по дата, статус, маса
- Таблица:
  - Дата/час
  - Клиент (име, телефон)
  - Маса (номер, капацитет)
  - Брой гости
  - Статус
  - Действия: "Потвърди", "Откажи"

**API:** `GET /api/reservations/restaurant/{restaurantId}?adminUserId={adminUserId}`  
**API:** `PUT /api/reservations/{id}/status?status=CONFIRMED` (RESTAURANT_ADMIN трябва да може)

#### 3.3. Manage Tables (`/admin/tables`)
- Списък с всички маси в ресторанта
- Всяка маса показва:
  - Номер
  - Капацитет
  - Налична/Недостъпна
- Бутон "Добави маса"
- Бутон "Промени налично състояние" (toggle available/unavailable)

**API:** `GET /api/restaurants/{id}/tables`  
**API:** `POST /api/restaurants/{id}/tables`  
**API:** `PUT /api/restaurants/{restaurantId}/tables/{tableNumber}/availability?available={true|false}`

**Забележка:** Използваме `tableNumber` (напр. "5"), не техническото database ID!

#### 3.4. Restaurant Settings (`/admin/settings`)
- Промяна на име
- Промяна на адрес
- **Промяна на работно време** (opening/closing time)
- Промяна на описание
- "Запази промените"

**API:** `PUT /api/restaurants/{id}?adminUserId={adminUserId}`  
**API:** `PUT /api/restaurants/{id}/hours?adminUserId={adminUserId}&openingTime={time}&closingTime={time}`

#### 3.5. **НЕ МОЖЕ** да прави резервации
- Ако RESTAURANT_ADMIN опита да отиде на `/client/reserve`, показва се грешка:
  > "Администраторите на ресторанти не могат да правят резервации. Използвайте CLIENT акаунт."

**API валидация:** `POST /api/reservations?userRole=RESTAURANT_ADMIN` → 400 Bad Request

---

### 4. SYSTEM_ADMIN екрани (Супер администратор)

#### 4.1. Dashboard (`/system-admin/dashboard`)
- Статистики:
  - Общ брой потребители
  - Общ брой ресторанти
  - Общ брой резервации
- Quick links: "Потребители", "Ресторанти", "Системни логове"

#### 4.2. User Management (`/system-admin/users`)
- **Таблица с всички потребители**:
  - ID
  - Име
  - Email
  - Роля (CLIENT, RESTAURANT_ADMIN, SYSTEM_ADMIN)
  - Дата на създаване
- **Филтър по роля** (падащо меню)
  - "Всички"
  - "Само CLIENT"
  - "Само RESTAURANT_ADMIN"
  - "Само SYSTEM_ADMIN"
- **Действия**:
  - Бутон "Промени роля" → открива модал:
    - Падащо меню с роли
    - "Потвърди"

**API:** `GET /api/users` (всички потребители)  
**API:** `GET /api/users?role=CLIENT` (филтрирани по роля)  
**API:** `PUT /api/users/{userId}/role?role={role}` (промяна на роля)

##### Пример за UI:

```
┌────────────────────────────────────────────────────────────────┐
│  User Management                                [Filter: All ▼]│
├────┬──────────────┬─────────────────┬──────────────┬───────────┤
│ ID │ Name         │ Email           │ Role         │ Actions   │
├────┼──────────────┼─────────────────┼──────────────┼───────────┤
│ 1  │ Admin User   │ admin@qt.com    │ SYSTEM_ADMIN │ -         │
│ 2  │ Ivan Petrov  │ ivan@mail.com   │ CLIENT       │ [Change]  │
│ 3  │ Maria        │ maria@mail.com  │ CLIENT       │ [Change]  │
│ 4  │ Restaurant X │ owner@rest.com  │ CLIENT       │ [Change]  │
└────┴──────────────┴─────────────────┴──────────────┴───────────┘
```

При натискане на [Change] бутона:

```
┌───────────────────────────────────────┐
│  Change Role for: Ivan Petrov         │
│                                       │
│  Current Role: CLIENT                 │
│                                       │
│  New Role:  [Select Role ▼]           │
│              - CLIENT                 │
│              - RESTAURANT_ADMIN       │
│              - SYSTEM_ADMIN           │
│                                       │
│           [Cancel]   [Confirm]        │
└───────────────────────────────────────┘
```

#### 4.3. Restaurant Management (`/system-admin/restaurants`)
- Списък с всички ресторанти
- Може да вижда детайли
- Може да изтрива ресторанти (ако е нужно)

**API:** `GET /api/restaurants`, `DELETE /api/restaurants/{id}` (може да добавим)

---

## Как работи промяната на роля (Workflow)

### Стъпка 1: Потребителят се регистрира
1. Отваря `/register`
2. Попълва формата
3. **Системата автоматично го създава като CLIENT**

### Стъпка 2: SYSTEM_ADMIN вижда новия потребител
1. Логва се в системата като SYSTEM_ADMIN
2. Отваря `/system-admin/users`
3. Вижда списък с всички потребители:
   ```
   ID: 4, Name: Owner X, Email: owner@rest.com, Role: CLIENT
   ```

### Стъпка 3: SYSTEM_ADMIN повишава потребителя
1. Натиска бутон "Change Role" до потребител 4
2. Избира "RESTAURANT_ADMIN" от падащото меню
3. Натиска "Confirm"
4. Frontend изпраща: `PUT /api/users/4/role?role=RESTAURANT_ADMIN`
5. **Потребителят вече е RESTAURANT_ADMIN**

### Стъпка 4: Новият RESTAURANT_ADMIN създава ресторант
1. Излиза от системата и влиза отново (JWT token се обновява с новата роля)
2. Отваря `/admin/create-restaurant`
3. Попълва данни за ресторанта
4. Системата създава ресторанта и го свързва с `adminUserId`

---

## JWT Token Management

### При Login
- Frontend получава JWT token
- Съхранява го в `localStorage` или `sessionStorage`
- Token съдържа:
  ```json
  {
    "userId": 2,
    "email": "ivan@mail.com",
    "role": "CLIENT",
    "exp": 1640000000
  }
  ```

### При всяка заявка
- Frontend добавя header:
  ```
  Authorization: Bearer {JWT_TOKEN}
  ```

### При промяна на роля
- Потребителят трябва да се logout и login отново
- Новият JWT token ще съдържа новата роля

---

## Технически изисквания за Frontend

### Библиотеки
- **React/Vue.js/Angular** - UI framework
- **Axios** - HTTP client
- **React Router / Vue Router** - Routing
- **JWT Decode** - Decode JWT tokens за извличане на роля
- **Tailwind CSS / Bootstrap** - Styling

### Структура на компонентите

```
src/
├── components/
│   ├── auth/
│   │   ├── LoginForm.jsx
│   │   └── RegisterForm.jsx
│   ├── client/
│   │   ├── RestaurantList.jsx
│   │   ├── ReservationForm.jsx
│   │   └── MyReservations.jsx
│   ├── restaurantAdmin/
│   │   ├── Dashboard.jsx
│   │   ├── ReservationList.jsx
│   │   ├── TableManagement.jsx
│   │   └── RestaurantSettings.jsx
│   ├── systemAdmin/
│   │   ├── UserManagement.jsx
│   │   └── UserRoleModal.jsx
│   └── shared/
│       ├── Navbar.jsx
│       └── ProtectedRoute.jsx
├── services/
│   ├── authService.js
│   ├── userService.js
│   ├── restaurantService.js
│   └── reservationService.js
└── utils/
    ├── jwtHelper.js
    └── apiClient.js
```

### Пример за API Client

```javascript
// apiClient.js
import axios from 'axios';

const userServiceClient = axios.create({
  baseURL: 'http://localhost:8081/api',
});

const restaurantServiceClient = axios.create({
  baseURL: 'http://localhost:8082/api',
});

const reservationServiceClient = axios.create({
  baseURL: 'http://localhost:8083/api',
});

// Add JWT token to all requests
[userServiceClient, restaurantServiceClient, reservationServiceClient].forEach(client => {
  client.interceptors.request.use(config => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });
});

export { userServiceClient, restaurantServiceClient, reservationServiceClient };
```

### Пример за userService.js

```javascript
// userService.js
import { userServiceClient } from './apiClient';

export const getAllUsers = async (role = null) => {
  const url = role ? `/users?role=${role}` : '/users';
  const response = await userServiceClient.get(url);
  return response.data;
};

export const updateUserRole = async (userId, newRole) => {
  const response = await userServiceClient.put(`/users/${userId}/role`, null, {
    params: { role: newRole }
  });
  return response.data;
};
```

### Пример за UserManagement компонент (React)

```jsx
// UserManagement.jsx
import React, { useState, useEffect } from 'react';
import { getAllUsers, updateUserRole } from '../../services/userService';

function UserManagement() {
  const [users, setUsers] = useState([]);
  const [roleFilter, setRoleFilter] = useState('');
  const [selectedUser, setSelectedUser] = useState(null);
  const [newRole, setNewRole] = useState('');

  useEffect(() => {
    loadUsers();
  }, [roleFilter]);

  const loadUsers = async () => {
    const data = await getAllUsers(roleFilter || null);
    setUsers(data);
  };

  const handleChangeRole = async () => {
    if (!selectedUser || !newRole) return;
    
    await updateUserRole(selectedUser.id, newRole);
    alert(`Ролята на ${selectedUser.name} е променена на ${newRole}`);
    
    setSelectedUser(null);
    setNewRole('');
    loadUsers();
  };

  return (
    <div className="user-management">
      <h2>User Management</h2>
      
      <select onChange={(e) => setRoleFilter(e.target.value)} value={roleFilter}>
        <option value="">Всички</option>
        <option value="CLIENT">CLIENT</option>
        <option value="RESTAURANT_ADMIN">RESTAURANT_ADMIN</option>
        <option value="SYSTEM_ADMIN">SYSTEM_ADMIN</option>
      </select>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {users.map(user => (
            <tr key={user.id}>
              <td>{user.id}</td>
              <td>{user.name}</td>
              <td>{user.email}</td>
              <td>{user.role}</td>
              <td>
                <button onClick={() => setSelectedUser(user)}>
                  Change Role
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {selectedUser && (
        <div className="modal">
          <h3>Change Role for: {selectedUser.name}</h3>
          <p>Current Role: {selectedUser.role}</p>
          <select onChange={(e) => setNewRole(e.target.value)} value={newRole}>
            <option value="">Select Role</option>
            <option value="CLIENT">CLIENT</option>
            <option value="RESTAURANT_ADMIN">RESTAURANT_ADMIN</option>
            <option value="SYSTEM_ADMIN">SYSTEM_ADMIN</option>
          </select>
          <button onClick={() => setSelectedUser(null)}>Cancel</button>
          <button onClick={handleChangeRole}>Confirm</button>
        </div>
      )}
    </div>
  );
}

export default UserManagement;
```

---

## Бизнес правила в UI

### CLIENT
✅ Може да вижда всички ресторанти  
✅ Може да резервира маси  
✅ Може да вижда САМО своите резервации  
❌ НЕ може да променя чужди резервации  
❌ НЕ може да управлява ресторанти

### RESTAURANT_ADMIN
✅ Може да вижда всички резервации за СВОЯ ресторант  
✅ Може да променя работно време на СВОЯ ресторант  
✅ Може да добавя/премахва маси в СВОЯ ресторант  
✅ Може да потвърждава/отказва резервации  
❌ **НЕ може да прави резервации** (показва се грешка)  
❌ НЕ може да вижда други ресторанти  
❌ НЕ може да променя потребителски роли

### SYSTEM_ADMIN
✅ Може да вижда всички потребители  
✅ Може да филтрира потребители по роля  
✅ Може да променя роли на всички потребители  
✅ Може да вижда всички ресторанти  
✅ Може да управлява системни настройки

---

## Security в Frontend

### Route Protection

```jsx
// ProtectedRoute.jsx
import { Navigate } from 'react-router-dom';
import { decodeToken } from '../utils/jwtHelper';

function ProtectedRoute({ children, allowedRoles }) {
  const token = localStorage.getItem('jwt_token');
  
  if (!token) {
    return <Navigate to="/login" />;
  }
  
  const decoded = decodeToken(token);
  
  if (!allowedRoles.includes(decoded.role)) {
    return <Navigate to="/unauthorized" />;
  }
  
  return children;
}

export default ProtectedRoute;
```

### Използване в Router

```jsx
<Routes>
  <Route path="/login" element={<LoginForm />} />
  <Route path="/register" element={<RegisterForm />} />
  
  <Route path="/client/*" element={
    <ProtectedRoute allowedRoles={['CLIENT']}>
      <ClientDashboard />
    </ProtectedRoute>
  } />
  
  <Route path="/admin/*" element={
    <ProtectedRoute allowedRoles={['RESTAURANT_ADMIN']}>
      <RestaurantAdminDashboard />
    </ProtectedRoute>
  } />
  
  <Route path="/system-admin/*" element={
    <ProtectedRoute allowedRoles={['SYSTEM_ADMIN']}>
      <SystemAdminDashboard />
    </ProtectedRoute>
  } />
</Routes>
```

---

## Заключение

Този UI план показва как frontend приложението трябва да взаимодейства с микросервизите.  

**Ключови точки:**
1. **SYSTEM_ADMIN** вижда всички потребители и може да променя роли през UI
2. **RESTAURANT_ADMIN** управлява само своя ресторант и НЕ прави резервации
3. **CLIENT** прави резервации и вижда само своите резервации
4. JWT token съхранява роля и се използва за route protection
5. Всички действия минават през REST API endpoints

**Следващи стъпки:**
- Изберете frontend framework (React/Vue/Angular)
- Имплементирайте API client services
- Създайте UI компоненти по роли
- Добавете route protection
- Тествайте workflow-ите
