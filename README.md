# GameFlix — Authentication Service (HCDD 412, Module 4)

Spring Boot service that registers users, authenticates them, issues a signed JWT, and uses that 
token to protect a route. Users are persisted in MySQL.

**Stack:** Java 21 · Spring Boot 4.1.1 · Spring Security 7 (OAuth2 Resource Server / Nimbus JOSE) · 
Spring Data JPA (Hibernate 7) · MySQL 8.0

## Endpoints

| Endpoint | Auth | Success | Failure |
|---|---|---|---|
| `POST /api/auth/register` | none | `201` + profile (no hash) | `400` invalid input · `409` email or display name already taken |
| `POST /api/auth/login` | none | `200` + `{accessToken, tokenType, expiresIn}` | `401` `{"error":"Invalid email or password"}` |
| `GET /api/users/me` | Bearer token | `200` + caller's profile | `401` missing, invalid, tampered, or expired token |

## How to run

### 1. MySQL setup

Run as root (MySQL 8):

```sql
CREATE DATABASE gameflix_db;
CREATE USER 'YOUR_DB_USER'@'localhost' IDENTIFIED BY 'YOUR_DB_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON gameflix_db.* TO 'YOUR_DB_USER'@'localhost';
```

The app expects database `gameflix_db` on `localhost:3306`. `CREATE`/`ALTER`/`INDEX`/`REFERENCES` 
are needed only because Hibernate manages the schema (`ddl-auto=update`).

### 2. Local configuration (not committed)

Copy `application.properties.example` to `application-local.properties` in the project root 
(next to `pom.xml`) and fill in:

```properties
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD
gameflix.jwt.secret=YOUR_BASE64_SECRET
```

Generate the secret (32 random bytes, Base64) in PowerShell:

```powershell
$b = New-Object byte[] 32; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); 
[Convert]::ToBase64String($b)
```

`application-local.properties` is in `.gitignore`. Environment variables work instead: 
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `GAMEFLIX_JWT_SECRET`. 
There is no default secret anywhere in committed files — if it is missing, 
the app fails at startup rather than signing tokens with a known key.

### 3. Run

```powershell
.\mvnw.cmd spring-boot:run
```

The server listens on **port 8081** (`server.port` in `application.properties`).

### 4. Test

```powershell
.\mvnw.cmd test
```

Tests run against the MySQL database above; each test registers its own 
random `test-<uuid>@example.com` user.

For manual checks, `register.json`, `login.json` and `badlogin.json` in the 
project root are request bodies for curl, e.g.:

```powershell
curl.exe -i -X POST http://localhost:8081/api/auth/register -H "Content-Type: application/json" --data-binary "@register.json"
```

## Schema

The schema follows the `auth` tables of the Module 3 ERD: `users`, `roles`, 
and the `user_roles` junction (M:N). Hibernate creates all three from 
the `User` and `Role` entities (`ddl-auto=update`); `RoleSeeder` inserts 
the `USER` and `ADMIN` rows at startup if they are missing. 
Full DDL is committed in `schema.sql`. Output of `SHOW CREATE TABLE users;`:

```sql
CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `display_name` varchar(50) NOT NULL,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(100) NOT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `last_name` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `status` enum('ACTIVE','SUSPENDED') NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_users_email` (`email`),
  UNIQUE KEY `uk_users_display_name` (`display_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
```

- `password_hash` is `VARCHAR(255)`. A delegating-encoder bcrypt hash is 68 characters 
(`{bcrypt}` + 60), so a `VARCHAR(60)` column would silently truncate it.
- Email uniqueness is enforced by the named constraint `uk_users_email`, 
display-name uniqueness by `uk_users_display_name`. 
Registration does **not** check "does this exist" first; 
it inserts and maps the constraint violation to `409`, so 
two concurrent registrations cannot both succeed.
- Emails are trimmed and lowercased before save and lookup, so `A@x.com` and `a@x.com` cannot 
both register.
- `status` is an `ENUM('ACTIVE','SUSPENDED')`. Login and `/me` only succeed for `ACTIVE` accounts; 
a suspended account gets the same generic `401` as a wrong password.
- `last_login_at` is set on each successful login.

## `ddl-auto` setting

`spring.jpa.hibernate.ddl-auto=update`. Hibernate creates the table on first run and keeps 
existing rows between restarts, so registered users survive for testing and screenshots. 
`create-drop` would wipe data on every run. In production this would be `validate`, with schema 
changes managed by migrations (e.g. Flyway).

## Password hashing

**Algorithm:** bcrypt, through Spring's `DelegatingPasswordEncoder`, so hashes are stored 
as `{bcrypt}$2a$12$...`. bcrypt is on the OWASP Password Storage Cheat Sheet list and ships with 
Spring Security. The `{bcrypt}` prefix records how each hash was produced, so the algorithm can 
be changed later while old hashes still verify.

**Work factor:** strength **12** (default is 10). Each step doubles the cost, so 12 is 4× the default. 
On my machine one hash takes about **427 ms**. 
That makes offline guessing expensive (about 2 guesses per second per CPU core against a stolen hash),
while a user notices well under half a second at login. Strength 13 would be about 850 ms, too slow 
for an interactive login; strength 10 would be about 107 ms, only a quarter of the cost to an attacker.

**Length limit:** passwords are 8–64 characters. bcrypt silently ignores input 
past 72 bytes, so the 64-character cap keeps the whole password significant.

No plaintext password is persisted, returned in any response, or logged. 
Validation errors report the field name and rule, never the submitted value.

## JWT

- **Signing:** HS256 (HMAC-SHA256) with a 256-bit key read from `gameflix.jwt.secret` 
(config file or environment variable — never committed). The decoder is pinned to HS256, 
so tokens claiming any other algorithm are rejected.
- **Validation:** Spring Security's resource-server filter verifies the signature **before** 
any claim is used, then checks `exp`/`nbf` (60-second clock-skew allowance). 
Missing, malformed, tampered, and expired tokens all return `401` with a `WWW-Authenticate: Bearer` 
header.
- **Claims:** `iss` (`gameflix`), `sub` (user id), `iat`, `exp`, `roles` (e.g. `["USER"]`). 
No email, name, password, or hash. The payload is Base64URL-encoded, not encrypted — anyone 
holding the token can read it, so it holds nothing confidential.
- **Lifetime:** **30 minutes** (`gameflix.jwt.ttl=PT30M`). 
There is no refresh-token flow yet, so the lifetime is the exposure window if a token is stolen; 
30 minutes limits that window while covering a normal browsing session without constant re-login.

## Error handling and CORS

- Login failure returns one response — `401 {"error":"Invalid email or password"}` — 
whether the email is unknown or the password is wrong. 
When the email is unknown, the service still runs a bcrypt comparison against a dummy hash 
so response timing does not reveal which emails are registered.
- `400` responses list invalid fields without echoing values.
- CORS allows exactly one origin (`gameflix.cors.allowed-origin`, 
currently `http://localhost:5173`), only `GET`/`POST`, only the `Authorization` 
and `Content-Type` headers, only under `/api/**`. No wildcard.
- CSRF protection is disabled because the API is stateless and authenticates with a 
bearer header, not cookies; browsers never attach the token automatically.

## Test evidence

Automated: `@SpringBootTest` + MockMvc in `AuthFlowTests.java`.

| Required case | Test | Expected |
|---|---|---|
| Successful registration | `registerReturns201AndNoHash` | 201, no hash in body |
| Duplicate email | `duplicateEmailReturns409` | 409 |
| *(extra)* Duplicate display name | `duplicateDisplayNameReturns409` | 409 |
| Successful login | `loginReturns200WithToken` | 200, token, `expiresIn` 1800 |
| Wrong password | `wrongPasswordReturns401Generic` | 401, generic message |
| Valid token on protected route | `meWithValidTokenReturnsProfile` | 200, caller's profile |
| Missing token | `meWithoutTokenReturns401` | 401 |
| Tampered token | `meWithTamperedTokenReturns401` | 401, `invalid_token` |
| *(extra)* Unknown email | `unknownEmailReturnsSameGeneric401` | 401, same message |
| *(extra)* Invalid input | `invalidRegistrationReturns400` | 400 with field errors |

Screenshots (in the accompanying Word document):
1. Application startup — connected to MySQL `gameflix_db`, Tomcat on port 8081
2. `POST /api/auth/register` — `201` with profile, no hash
3. Duplicate email — `409 {"error":"Email already registered"}`
4. Wrong password — `401 {"error":"Invalid email or password"}`
5. Tampered token — `401 ... Signed JWT rejected: Invalid signature`
6. `SELECT user_id, email, password_hash FROM users;` — stored value begins `{bcrypt}$2a$12$` (the assignment's `SELECT id, email, password` query, with the ERD's column names)
7. `mvnw clean test` output — `Tests run: 11, Failures: 0, Errors: 0`

## Module 3 design-change note

The first Module 4 build used a simplified `users` table (`id`, `password`, a single `role` column, no names or status). Checking it against the Module 3 ERD showed it did not follow the ERD, so the schema was reworked to match `auth.users`, `auth.roles`, and `auth.user_roles`: `user_id`, `password_hash`, `first_name`, `last_name`, a unique 50-character `display_name`, the `status` enum, `created_at`, `last_login_at`, and roles as a real M:N junction table. Registration now collects first and last name, and the JWT carries the user's role names.

Remaining differences from the ERD:

- **Database:** the ERD used PostgreSQL types (`public.` enums, `timestamp`) and a 
separate `auth` schema. The course requires MySQL, so the tables live in `gameflix_db`, '
`status` is a MySQL `ENUM`, and timestamps are `DATETIME(6)`.
- **Account-status values:** the ERD named the `account_status` type without 
listing values; this build uses `ACTIVE` and `SUSPENDED`.
- **ADMIN** is seeded so the role model is complete, but no endpoint uses it yet.

Implementation lesson: the original table was first created by hand with a 
misnamed unique constraint (`us_users_email`). Hibernate's `update` mode then 
added `uk_users_email` alongside it rather than replacing it. 
MySQL reported duplicates against the misnamed key, the 409 mapping did not recognize it, 
and duplicate registrations returned `500`. Dropping the stray index fixed it,
a reminder that `ddl-auto=update` adds to a schema but never renames or cleans it up. 
The same limitation is why the rework required dropping the old `users` table rather 
than letting Hibernate migrate it.

## AI use
I used Claude to help work on and understand the project. 
It walked me through setting up Spring Security, the bcrypt hashing, and the JWT signing, 
gave me the commands to run, and helped me debug errors and write the tests.

One thing I rejected: the first database schema it gave me didn’t match my Module 3 ERD. 
It used id and password instead of user_id 
and password_hash, left out first name, last name, and account status, 
and used a single role column instead of the roles and user_roles tables. 
When I compared it to my ERD, Claude suggested just documenting the differences in the README. 
I rejected that because the assignment says the schema should follow the ERD, 
so I had it rebuilt to match. I confirmed the rebuild by dropping the old table, 
running all 11 tests again, and checking the new columns with SHOW CREATE TABLE.