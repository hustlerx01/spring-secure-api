# spring-secure-api

Incident-ticket CRUD REST API in Java 17 / Spring Boot 3 / Maven.
JWT auth with BCrypt-hashed passwords, USER/ADMIN roles, and per-record ownership checks (IDOR protection).
Bean validation, RFC 7807 error bodies, login lockout, OpenAPI docs, 25 JUnit 5 + MockMvc tests.

## Run

Requirements: JDK 17+ and Maven 3.9+.

```bash
mvn test                                   # run the test suite
mvn spring-boot:run                        # H2 in-memory, http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui/index.html

# Optional admin account (none is created unless you set this):
APP_ADMIN_PASSWORD='choose-a-long-one' APP_JWT_SECRET='at-least-32-bytes-of-random-secret' mvn spring-boot:run
```

Postgres profile:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/secureapi DB_USER=secureapi DB_PASSWORD=... APP_JWT_SECRET=...
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```
Note: the postgres profile uses `ddl-auto: validate`, so the schema must already exist (no migrations yet, see Limits). [RUN-THEN-FILL: not run against a real Postgres]

Docker:

```bash
docker build -t spring-secure-api .
docker run -p 8080:8080 -e APP_JWT_SECRET='at-least-32-bytes-of-random-secret' spring-secure-api
```
[RUN-THEN-FILL: Docker build not run on the dev box]

Quick curl:

```bash
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' -d '{"username":"demo","password":"DemoPassw0rd"}'
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"demo","password":"DemoPassw0rd"}' | jq -r .accessToken)
curl -X POST localhost:8080/api/tickets -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"title":"Suspicious login","severity":"HIGH"}'
```

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | /api/auth/register | public | Create a USER account (role is never taken from the request body) |
| POST | /api/auth/login | public | Returns a JWT; 401 on bad credentials, 429 + Retry-After when locked |
| GET | /api/tickets | USER/ADMIN | Page through **your own** tickets (`?page=&size=&sort=`) |
| POST | /api/tickets | USER/ADMIN | Create a ticket (title, description, severity, optional status) |
| GET | /api/tickets/{id} | owner or ADMIN | Get one ticket; 404 if it isn't yours |
| PUT | /api/tickets/{id} | owner or ADMIN | Update a ticket; 404 if it isn't yours |
| DELETE | /api/tickets/{id} | owner or ADMIN | Delete a ticket; 404 if it isn't yours |
| GET | /api/admin/tickets | ADMIN | All tickets across users |
| GET | /api/admin/users | ADMIN | List users (no password hashes) |
| GET | /v3/api-docs, /swagger-ui/index.html | public | OpenAPI spec and UI |

## Security decisions

- **Passwords**: BCrypt (cost 12). Plaintext is never stored or returned. Unknown usernames still pay one BCrypt check so response timing doesn't reveal whether an account exists. Both cases return the same 401 message.
- **JWT**: HS256, 15-minute TTL by default, issuer checked, signature required (an unsigned `alg:none` token is rejected, and a test covers this). The secret comes from `APP_JWT_SECRET`, and startup fails if it is shorter than 32 bytes. The role is loaded from the DB on each request instead of trusted from the token claim, so a deleted user's token stops working.
- **Authorization**: URL rule (`/api/admin/**` requires ADMIN) **and** `@PreAuthorize` on the admin controller, for defense in depth.
- **IDOR**: ticket lookups for non-admins use `findByIdAndOwnerId`, so ownership is enforced in the query. A ticket you don't own returns **404, not 403**, so ids can't be enumerated.
- **Mass assignment**: request DTOs are separate from entities. `owner` and `role` cannot be set by clients.
- **Brute force**: after 5 failed logins (configurable), the username is locked for 15 minutes and returns 429 with `Retry-After`, even when the correct password is sent.
- **Errors**: one `@RestControllerAdvice` returns RFC 7807 `ProblemDetail`. There are no stack traces, malformed JSON or enums return 400 instead of 500, and validation errors are listed per field.
- **Stateless**: no sessions or cookies, so CSRF protection is disabled on purpose.
- **No default admin**: an admin is created only when `APP_ADMIN_PASSWORD` is set.
- **Container**: multi-stage build, JRE-only runtime, non-root UID.

## Limits (known, deliberate for scope)

- Lockout is in-memory and per instance. It resets on restart and isn't shared across replicas (would need Redis or a DB). It is also per username, so it doesn't stop password spraying from one IP across many usernames, and an attacker can lock out a victim on purpose (DoS trade-off).
- No refresh tokens or token revocation list. Logout is client-side until the token expires.
- No DB migrations (Flyway/Liquibase). H2 uses `ddl-auto: update`.
- The dev JWT secret default in `application.yml` exists for local use only and must be overridden.
- No HTTPS termination in the app. TLS is expected at a reverse proxy.
- No audit log of ticket changes.
- Tests run on H2 only. Postgres behaviour hasn't been tested (Testcontainers would close this gap).

## Verified results

See `results/`. `mvn test` was run on 2026-10-11 (box JDK 21 compiling with `--release 17`, Maven 3.9.9): **25 tests, 0 failures, 0 errors** (`results/mvn-test-latest.txt`). A local smoke run of the packaged jar is in `results/smoke-run.txt`.
CI on GitHub Actions (JDK 17): [RUN-THEN-FILL after first push]
