# User Service — Пълно обяснение

## Какво прави user-service?

Отговаря за **регистрация, вход и управление на потребители**. Всеки друг сервиз (restaurant, reservation) трябва да се "довери" на токена, издаден от user-service. Той е единственият, който знае дали даден потребител съществува и какви права има.

---

## Какво трябва да има при стартиране на Spring Boot

При стартиране Spring Boot:
1. Зарежда всички `@Configuration` класове и регистрира beans
2. `SecurityConfig` конфигурира как всяка HTTP заявка трябва да се обработи
3. `JwtAuthenticationFilter` се регистрира в "верига от филтри" (filter chain) — тоест всяка заявка минава през него **преди** да стигне до контролера
4. `UserDetailsService` се регистрира, за да може Spring Security да зарежда потребители от базата
5. `JwtTokenProvider` се зарежда с `jwt.secret` и `jwt.expiration` от `application.properties`
6. База данни (PostgreSQL) трябва да работи и таблицата `users` да съществува
7. Swagger UI се конфигурира автоматично от `OpenApiConfig`

---

## Редът на имплементация — като начинаещ разработчик

Нека минем през мисловния процес: **"Какво ми трябва, за да имплементирам регистрация и вход с JWT?"**

---

### Стъпка 1 — Потребителят трябва да се съхранява някъде → `User.java`

Първо се питаш: *"Какво е един потребител в моята система?"* Той има email, парола, имена, роля.

```
entity/User.java
```

Първо анотациите на класа:

- `@Entity` — **задължителна JPA анотация**; казва на Hibernate "това е клас, който се мапва към таблица в базата". Без нея Hibernate изобщо не знае за класа
- `@Table(name = "users")` — указва точното **име на таблицата**. Ако я пропуснеш, Hibernate ще използва името на класа ("User"), което може да е проблем — "user" е запазена дума в PostgreSQL
- `@Data` — Lombok анотация, която **генерира автоматично** `getters`, `setters`, `equals()`, `hashCode()`, `toString()` за всички полета, плюс включва `@RequiredArgsConstructor` — конструктор за всички `final` полета и полета с `@NonNull`. Без нея трябва да ги пишеш ръчно
- `@Builder` — Lombok анотация, която позволява **builder pattern**: `User.builder().email("x").password("y").build()`. Използва се в `AuthService` при създаване на нов потребител — по-четим от `new User()` с много setter извиквания
- `@NoArgsConstructor` — генерира **конструктор без аргументи**. Hibernate изисква задължително такъв, за да може да създава entity обекти при зареждане от базата (десериализация)
- `@AllArgsConstructor` — генерира **конструктор с всички полета**. Нужен е, защото `@Builder` го изисква вътрешно

След това полетата:

- `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` — `@Id` маркира полето като **primary key**. `GenerationType.IDENTITY` означава, че **базата данни (PostgreSQL) сама генерира следващото id** чрез `auto_increment` / `SERIAL` колона — не ти, не Hibernate. Алтернативите са `SEQUENCE` (Hibernate управлява sequence обект) и `AUTO` (Hibernate избира сам), но `IDENTITY` е най-прост при PostgreSQL
- `email` е `unique` и `nullable = false` — **не може двама да имат един email**
- `password` — засега просто `String` поле. На този етап не знаеш как ще се пази паролата, само знаеш, че трябва да я пазиш. **Начинът на хеширане ще разбереш когато стигнеш до `SecurityConfig` (Стъпка 4)**
- `role` е enum `UserRole` от common модула — три стойности: `CLIENT`, `RESTAURANT_ADMIN`, `SYSTEM_ADMIN`. `@Enumerated(EnumType.STRING)` казва на Hibernate да **записва текста ("CLIENT"), не числото (0)** — ако добавиш нова роля в средата, редът на enum стойностите няма да обърка старите записи в базата
- `active` — дали акаунтът е активен, по подразбиране `true`
- `@PrePersist onCreate()` — Spring извиква това **преди първото записване**; тук задаваш `createdAt`, `updatedAt`, `role = CLIENT` и `active = true` ако не са зададени. Защо? **За да не разчиташ на клиента да ги подаде**
- `@PreUpdate onUpdate()` — обновява `updatedAt` при всяка промяна. Защо? **За одит — знаеш кога последно е редактиран записа**

---

### Стъпка 2 — Трябва начин да четеш от базата → `UserRepository.java`

Имаш entity, трябва и достъп до базата:

```
repository/UserRepository.java
```

- Разширяваш `JpaRepository<User, Long>` — получаваш безплатно `save()`, `findById()`, `findAll()` и др.
- `findByEmail(String email)` — при вход потребителят дава email, не id. Затова трябва търсене по email.
- `existsByEmail(String email)` — при регистрация трябва да провериш дали email е зает преди да се опиташ да записваш (ако записваш с дублиран email, базата ще хвърли грешка — по-добре да проверяваш сам с четим error message)

---

### Стъпка 3 — Какво ще получаваш и изпращаш? → DTOs

Сега се питаш: *"Какви данни ще ми изпраща клиентът и какви ще му връщам?"*

```
dto/RegisterRequest.java
dto/LoginRequest.java
dto/AuthResponse.java
dto/UserResponse.java
```

**`RegisterRequest`** — данните при регистрация:
- `@NotBlank @Email` на email — Spring Validation ги проверява автоматично преди да влязат в сервиза
- `@Size(min = 6)` на password — минимална дължина
- `role` **нарочно липсва** в регистрацията (или се игнорира) — всеки се регистрира като `CLIENT`. Само `SYSTEM_ADMIN` може да смени роля после. Ако го оставиш, злонамерен потребител може да се регистрира директно като admin

**`LoginRequest`** — само email и password, нищо повече

**`AuthResponse`** — това връщаш след успешен вход/регистрация:
- `token` — JWT токена
- `type = "Bearer"` — стандартен тип за JWT
- `userId`, `email`, `firstName`, `lastName`, `role` — базова информация за потребителя, за да не се налага допълнителна заявка

**`UserResponse`** — използва се в `UserController` когато трябва да върнеш потребителски данни **без** токен (профил, списък с потребители)

---

### Стъпка 4 — Регистрация → `AuthService.java` (частично) + `SecurityConfig.java` (частично)

Започваш да пишеш `register()` в `AuthService`:

```
service/AuthService.java  ← Започваш да го пишеш
```

**`register(RegisterRequest request)`:**
1. Проверяваш дали email е зает — `userRepository.existsByEmail(email)`; ако да, хвърляш `RuntimeException`
2. Стигаш до реда където трябва да запишеш паролата:
```java
user.setPassword(request.getPassword()); // ← стоп, не може plain text
```
Тук се сещаш: *"Паролата не може да се пази като обикновен текст."* Трябва ти нещо, което да я хешира.

Това е **първата причина** да отвориш `SecurityConfig.java` и да добавиш:

```
security/SecurityConfig.java  ← Започваш да го пишеш
```

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Защо не можеш да запишеш паролата директно? Ако базата ти изтече (хакване, backup leak), нападателят вижда паролите на всички потребители — а хората използват едни и същи пароли в много сайтове.

Защо не MD5 или SHA-256? Те са **бързи** — модерен GPU изчислява милиарди SHA-256 хешове в секунда. Нападателят просто хешира речник от популярни пароли и сравнява. Освен това, ако двама потребители имат еднаква парола, ще имат еднакъв хеш — веднага се вижда.

BCrypt решава и двата проблема:

- **Автоматичен "salt"** — преди хеширане BCrypt добавя случайна стойност (salt) към паролата. `"password123"` + `"xK9mQ2"` → хеш, `"password123"` + `"zR4nL7"` → съвсем различен хеш. Двама потребители с еднаква парола имат различни хешове. Нападателят не може да ползва предварително изчислени таблици
- **Бавен по дизайн** — BCrypt има "work factor" (по подразбиране 10 рунда). Един хеш отнема ~100ms на сървъра — приемливо за вход. Но за нападател, който проверява милиони пароли, 100ms × 1 000 000 = 27 часа само за един акаунт. При MD5 същото би отнело секунди

Затова `BCryptPasswordEncoder` е стандартният избор в Spring Security. Връщаш се в `AuthService` и използваш:

```java
passwordEncoder.encode(request.getPassword())
```

3. Създаваш `User` entity и го записваш
4. Трябва да върнеш JWT токен, но нямаш нищо за генерирането му. **Спираш и отиваш да имплементираш JWT логика**

---

### Стъпка 4.1 — JWT генерация → `JwtTokenProvider.java`

```
security/JwtTokenProvider.java
```

Това е utility клас — инструмент за JWT операции.

Стойностите идват от `application.properties`:
```properties
jwt.secret=много-дълъг-секретен-ключ
jwt.expiration=86400000  # 24 часа в ms
```

**`generateToken(UserDetails userDetails)`** — прост wrapper за долния метод

**`generateToken(Map<String, Object> extraClaims, UserDetails userDetails)`** — истинската генерация. Тук забелязваш, че методът приема `UserDetails`, не твоя `User` entity. Защо? Защото `JwtTokenProvider` е написан по Spring Security стандарта — не зависи от конкретния entity на проекта. Какво е `UserDetails` — ще разбереш в Стъпка 4.2:
- `subject` = email на потребителя (стандартно JWT claim)
- `issuedAt` = текущо време
- `expiration` = текущо + `jwt.expiration`
- `signWith(getSigningKey())` — подписва с HMAC-SHA256, използвайки тайния ключ
- `extraClaims` = допълнителни данни в токена (userId, role) — AuthService ги подава, за да може всеки сервиз да знае кой е потребителят и каква е ролята му без да пита базата

**`getSigningKey()`** — декодира base64 secret и го конвертира до `SecretKey` обект

**`extractUsername(String token)`** — чете subject claim-а от токена (email-а)

**`extractAllClaims(String token)`** — private метод; тук реално се парсира токенът чрез `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`. Ако подписът не съвпада или токенът е изтекъл, JJWT хвърля exception **тук** — при самото парсиране

**`extractClaim(String token, Function<Claims, T> claimsResolver)`** — вика `extractAllClaims` и прилага функция върху резултата. Ако `extractAllClaims` хвърли exception, той се разпространява нагоре оттук. Всички публични `extract*` методи минават през него

**`isTokenValid(String token, UserDetails userDetails)`** — проверява:
1. Username в токена = username на UserDetails (email-ите съвпадат)
2. Токенът не е изтекъл

**`isTokenExpired()`** + **`extractExpiration()`** — helpers за проверка на expiration claim-а

---

### Стъпка 4.2 — Spring Security не знае за твоя `User` entity → `UserDetails` и `UserDetailsService`

Видя, че `generateToken()` приема `UserDetails`. Spring Security има собствена абстракция за потребител — интерфейса `UserDetails`. Той не знае нищо за твоя `User` entity. Затова трябва мост между двете.

**`UserDetails`** е интерфейс с методи:
- `getUsername()` — в нашия случай email-ът
- `getPassword()` — хешираната парола
- `getAuthorities()` — списък с роли/права (напр. `ROLE_CLIENT`)
- `isAccountNonExpired()`, `isEnabled()` и др. — статус на акаунта

**`UserDetailsService`** е интерфейс с **един единствен абстрактен метод**, което го прави **функционален интерфейс**:
```java
UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
```
Spring Security го вика при нужда — "дай ми потребителя с това username". Ти имплементираш метода: намираш потребителя в базата и го конвертираш до `UserDetails`.

Тъй като е функционален интерфейс, вместо да създаваш отделен клас `implements UserDetailsService`, можеш да го имплементираш директно с **lambda**. Компилаторът знае, че lambda-та с един аргумент съответства на `loadUserByUsername(String username)`:

Имплементацията отива в `SecurityConfig` като `@Bean`:

```
security/SecurityConfig.java  ← Добавяш втори bean
```

```java
@Bean
public UserDetailsService userDetailsService() {
    return username -> userRepository.findByEmail(username)
            .map(user -> User.builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .roles(user.getRole().name())
                    .build())
            .orElseThrow(() -> new UsernameNotFoundException("Потребител не е намерен"));
}
```

Какво се случва тук:
- Методът приема `username` — в нашия случай email
- `userRepository.findByEmail(username)` — търсиш потребителя в базата
- `.map(...)` — ако го намериш, конвертираш твоя `User` entity до Spring Security's `UserDetails` обект чрез неговия собствен builder (`org.springframework.security.core.userdetails.User.builder()`). Задаваш username, хешираната парола и ролята
- `.roles(user.getRole().name())` — Spring автоматично добавя префикс `ROLE_`, така `CLIENT` става `ROLE_CLIENT` в authorities
- `.orElseThrow(...)` — ако потребителят не е намерен, Spring Security очаква точно `UsernameNotFoundException`

`SecurityConfig` инжектира `UserRepository` за да може да достъпва базата:
```java
private final UserRepository userRepository;
```

Сега добавяш `UserDetailsService` като dependency в `AuthService`:

```java
private final UserDetailsService userDetailsService;
```

Сега се връщаш към `AuthService` и го довършваш — зареждаш потребителя като `UserDetails` и генерираш токена:

```java
UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

Map<String, Object> claims = new HashMap<>();
claims.put("userId", user.getId());
claims.put("role", user.getRole().name());
String token = jwtTokenProvider.generateToken(claims, userDetails);
```

Накрая строиш `AuthResponse` и го връщаш — тук влиза `@Builder` от DTO-то, дефинирано в Стъпка 3:

```java
return AuthResponse.builder()
        .token(token)
        .userId(user.getId())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .role(user.getRole())
        .build();
```

`register()` е завършен.

---

### Стъпка 6 — Вход изисква проверка на credentials → нужен е `AuthenticationManager`

Пишеш `login()` метода и се питаш: *"Как проверявам email + password без ръчно да хеширам и сравнявам?"*

Spring Security предлага `AuthenticationManager.authenticate()` — подаваш `UsernamePasswordAuthenticationToken` и той сам:
1. Зарежда потребителя от базата по email (чрез `UserDetailsService`)
2. Сравнява паролата с BCrypt
3. Хвърля `BadCredentialsException` ако не съвпадат

За да работи `AuthenticationManager`, трябва да го регистрираш в `SecurityConfig`:

```
security/SecurityConfig.java  ← Добавяш нови beans
```

**`userDetailsService()`** — Spring Security изисква bean от тип `UserDetailsService` (интерфейс). Имплементираш го inline:
- Търси потребителя по email в базата
- Ако не го намери — хвърля `UsernameNotFoundException`
- Ако го намери — конвертира го до `UserDetails` (Spring Security's представяне на потребителя)

**`authenticationProvider()`** — `DaoAuthenticationProvider` е Spring Security клас, който:
- Използва `userDetailsService()` за зареждане
- Използва `passwordEncoder()` за сравняване на пароли

**`authenticationManager()`** — регистрираш го като bean, за да можеш да го inject-ваш в `AuthService`

---

### Стъпка 7 — При всяка заявка трябва да знаеш кой е потребителят → `JwtAuthenticationFilter.java`

Сега се питаш: *"Потребителят се е логнал, има токен. Как при следваща заявка знаем кой е?"*

HTTP е stateless — всяка заявка е независима. Затова клиентът изпраща токена в header-а:
```
Authorization: Bearer eyJhbGc...
```

Нужен е филтър, който при **всяка** заявка:
1. Чете Authorization header
2. Извлича токена
3. Валидира го
4. Казва на Spring Security "тази заявка е от потребител X с роля Y"

```
security/JwtAuthenticationFilter.java
```

Разширяваш `OncePerRequestFilter` — Spring гарантира, че `doFilterInternal()` се извиква **точно веднъж** на заявка (не повторно при forward/include).

**`doFilterInternal()`:**
1. Чете `Authorization` header
2. Ако липсва или не започва с "Bearer " — пуска заявката напред без аутентикация (ще я спре `SecurityConfig` ако endpoint-ът изисква аутентикация)
3. Извлича JWT-то (маха "Bearer " prefix-а)
4. Вика `jwtTokenProvider.extractUsername(token)` — получава email
5. Проверява `SecurityContextHolder.getContext().getAuthentication() == null` — ако вече има аутентикация, не презаписваш
6. Зарежда `UserDetails` от `userDetailsService`
7. Вика `jwtTokenProvider.isTokenValid(token, userDetails)`
8. Ако е валиден — създава `UsernamePasswordAuthenticationToken` и го слага в `SecurityContextHolder`:

```java
UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
        userDetails,   // principal — кой е потребителят
        null,          // credentials — паролата
        userDetails.getAuthorities()  // authorities — ролите
);
```

Трите аргумента имат конкретни причини:
- **`userDetails`** (principal) — обектът "кой е логнат". Spring Security го използва когато викаш `authentication.getPrincipal()` или `authentication.getName()`
- **`null`** вместо паролата — токенът вече е верифициран, паролата не е нужна повече. Пазенето й в памет е ненужен риск
- **`userDetails.getAuthorities()`** — списъкът с роли (`ROLE_CLIENT`, `ROLE_ADMIN`...). Точно това проверява `@PreAuthorize("hasRole('ADMIN')")` по-нататък

Важно: `UsernamePasswordAuthenticationToken` има **два конструктора**. Конструкторът с два аргумента *(principal + credentials)* създава **неаутентикиран** токен (`isAuthenticated() = false`). Конструкторът с три аргумента *(principal + credentials + authorities)* автоматично вика `setAuthenticated(true)`. Затова authorities не е просто бонус — без тях Spring Security би отхвърлил токена

9. `filterChain.doFilter()` — пуска заявката нататък

`SecurityContextHolder` е thread-local storage — само текущата заявка вижда своя потребител. След края на заявката се изчиства.

---

### Стъпка 8 — Кои endpoints са публични, кои изискват токен? → Довършваш `SecurityConfig.java`

```
security/SecurityConfig.java  ← Добавяш securityFilterChain()
```

**`securityFilterChain(HttpSecurity http)`:**
- `csrf().disable()` — CSRF защита не е нужна при stateless JWT API (тя е за сесийни приложения)
- `.authorizeHttpRequests()` — дефинираш правилата:
  - `/api/auth/**` — permit all (регистрация и вход са публични)
  - `/swagger-ui/**`, `/v3/api-docs/**` — permit all (документацията е публична)
  - всичко останало — `authenticated()` (трябва токен)
- `sessionManagement(STATELESS)` — казваш на Spring Security да **не** създава HTTP сесии; всяка заявка е независима
- `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` — слагаш JWT филтъра преди стандартния login филтър

**`@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)`** — активира анотации като `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` в контролерите. Без това анотациите се игнорират.

---

### Стъпка 9 — Контролери → `AuthController.java` и `UserController.java`

```
controller/AuthController.java
controller/UserController.java
```

**`AuthController`:**
- `POST /api/auth/register` — приема `@Valid RegisterRequest`, Spring автоматично валидира полетата, при грешка хвърля `MethodArgumentNotValidException`
- `POST /api/auth/login` — приема `@Valid LoginRequest`

**`UserController`:**
- `GET /api/users/me` — вика `userService.getCurrentUser()` който чете от `SecurityContextHolder` (попълнен от JWT филтъра)
- `GET /api/users/{userId}` — всеки аутентикиран потребител може да види профил по id
- `GET /api/users` — `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` — само SYSTEM_ADMIN. Приема опционален `role` query parameter
- `PUT /api/users/{userId}/role` — `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` — само SYSTEM_ADMIN може да сменя роли

---

### Стъпка 10 — Потребителски операции → `UserService.java`

```
service/UserService.java
```

**`getCurrentUser()`** — чете от `SecurityContextHolder.getContext().getAuthentication()`:
- `authentication.getName()` връща username-а (email), зареден от JWT филтъра
- Търси потребителя по email в базата

**`getUserById(Long userId)`** — проста заявка; хвърля `RuntimeException` ако не съществува

**`getAllUsers()`** и **`getUsersByRole(UserRole role)`** — за admin панел

**`updateUserRole(Long userId, UserRole newRole)`** — `@Transactional` — промяната се записва в базата в рамките на транзакция

**`mapToUserResponse(User user)`** — конвертира entity към DTO; **не** включва паролата

---

### Стъпка 11 — Единно обработване на грешки → `GlobalExceptionHandler.java`

```
exception/GlobalExceptionHandler.java
```

Без това всяка необработена грешка би върнала Spring's default 500 страница.

- `RuntimeException` → 400 Bad Request — хвърля се от `AuthService` при "email вече съществува" и от `UserService` при "не е намерен"
- `BadCredentialsException` → 401 Unauthorized — хвърля се от `AuthenticationManager` при грешна парола; "Невалиден email или парола" (намерено съобщение не разкрива дали проблемът е email-ът или паролата)
- `MethodArgumentNotValidException` → 400 с map от field → error message — хвърля се от Spring Validation при `@Valid`

---

### Стъпка 12 — Документация → `OpenApiConfig.java`

```
config/OpenApiConfig.java
```

Добавя JWT схема в Swagger UI — появява се бутон "Authorize" където можеш да въведеш Bearer токен и да тестваш защитените endpoints директно от браузъра.

---

## Пълната верига при заявка

### Регистрация / Вход

```
Client → POST /api/auth/register
       → SecurityConfig: permit all ✓
       → AuthController.register()
       → @Valid проверява RegisterRequest
       → AuthService.register()
           → userRepository.existsByEmail() → не съществува ✓
           → passwordEncoder.encode(password) → хешира
           → userRepository.save(user)
           → jwtTokenProvider.generateToken(extraClaims, userDetails)
       → AuthResponse { token, userId, email, role }
```

### Защитена заявка (напр. GET /api/users/me)

```
Client → GET /api/users/me
         Authorization: Bearer eyJhbGc...
       → JwtAuthenticationFilter.doFilterInternal()
           → extractUsername(token) → "user@example.com"
           → userDetailsService.loadUserByUsername("user@example.com")
           → isTokenValid(token, userDetails) → true ✓
           → SecurityContextHolder ← UsernamePasswordAuthenticationToken
       → SecurityConfig: authenticated() ✓ (вече има в SecurityContext)
       → UserController.getCurrentUser()
       → @PreAuthorize: няма ограничение тук
       → UserService.getCurrentUser()
           → SecurityContextHolder.getAuthentication().getName()
           → userRepository.findByEmail(email)
       → UserResponse { id, email, firstName, lastName, role }
```

### Admin заявка (напр. PUT /api/users/5/role)

```
Client → PUT /api/users/5/role
         Authorization: Bearer eyJhbGc...  (трябва да е SYSTEM_ADMIN токен)
       → JwtAuthenticationFilter → попълва SecurityContext
       → SecurityConfig: authenticated() ✓
       → UserController.updateUserRole()
       → @PreAuthorize("hasRole('SYSTEM_ADMIN')") → проверява authorities
           → ако CLIENT → 403 Forbidden
           → ако SYSTEM_ADMIN → продължава
       → UserService.updateUserRole()
```

---

## Dependency Graph — кой зависи от кого

```
SecurityConfig
    ├── PasswordEncoder          (BCryptPasswordEncoder)
    ├── UserDetailsService       (зарежда User от UserRepository)
    ├── AuthenticationProvider   (DaoAuthenticationProvider)
    │       ├── UserDetailsService
    │       └── PasswordEncoder
    ├── AuthenticationManager    (от AuthenticationConfiguration)
    └── SecurityFilterChain
            └── JwtAuthenticationFilter
                    ├── JwtTokenProvider
                    └── UserDetailsService

AuthService
    ├── UserRepository
    ├── PasswordEncoder          (от SecurityConfig)
    ├── JwtTokenProvider
    ├── AuthenticationManager    (от SecurityConfig)
    └── UserDetailsService       (от SecurityConfig)

UserService
    └── UserRepository

AuthController
    └── AuthService

UserController
    └── UserService
```

---

## Защо точно тази архитектура?

| Решение | Защо |
|---|---|
| Stateless JWT (без сесии) | Микросервисите са stateless — всяка инстанция може да обработи заявката |
| BCrypt за пароли | Бавен по дизайн → защита от brute force |
| `extraClaims` с userId и role | Reservation/Restaurant service не питат user-service за всяка заявка |
| `@PreAuthorize` вместо само SecurityConfig | По-финозернест контрол на ниво метод |
| `GlobalExceptionHandler` | Единна точка за error responses — не се повтаря код |
| `@PrePersist` за defaults | Данните са консистентни независимо как е създаден entity-то |
