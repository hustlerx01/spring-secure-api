# Interview notes: spring-secure-api

**1. Walk me through a request to `GET /api/tickets/5`.**
`JwtAuthFilter` reads the `Authorization: Bearer` header, and `JwtService.parse` checks the signature, issuer and expiry. The user is loaded from the DB and an `AuthUser` principal is put in the `SecurityContext`. The `authorizeHttpRequests` rules require an authenticated user. `TicketService.load` then calls `findByIdAndOwnerId(5, me.id)` for normal users or `findById` for admins. An empty result throws `NotFoundException`, and the advice maps that to a 404 ProblemDetail.

**2. What is IDOR and how did you prevent it?**
Insecure Direct Object Reference: changing an id in the URL to reach someone else's data (OWASP API1:2023, Broken Object Level Authorization). Ownership is part of the repository query, so there's no "load it, then forget to check" path. Tests cover read, update and delete by another user, and confirm the data isn't changed.

**3. Why 404 instead of 403 for someone else's ticket?**
A 403 confirms the id exists, which leaks information and helps enumeration. A 404 looks the same as a missing record. The trade-off is that debugging is a bit harder for legitimate users.

**4. Why BCrypt and not SHA-256?**
SHA-256 is fast, which helps offline cracking. BCrypt is deliberately slow, salted per password, and has a tunable cost (12 here). Argon2id is the modern first choice. BCrypt has native Spring support and truncates input at 72 bytes, which is why the password max is 72.

**5. JWT vs server sessions: what did you give up?**
Statelessness makes horizontal scaling easy. The cost is revocation: a stolen token stays valid until it expires. I kept the TTL short (15 min), and the filter re-loads the user from the DB, so deleted users lose access immediately and role changes take effect without re-login. Next steps would be refresh tokens plus a deny-list.

**6. What is the `alg:none` attack and are you exposed?**
Some libraries accepted tokens whose header said `alg: none` with no signature. jjwt's `verifyWith(key).parseSignedClaims()` requires a valid signature with the configured key. `AuthTests.unsignedAlgNoneTokenIs401` sends an unsigned token claiming ADMIN and expects 401.

**7. Why is CSRF disabled? Isn't that insecure?**
CSRF works because browsers attach cookies automatically. This API authenticates only with an `Authorization` header that the client has to set on purpose, and there's no session cookie, so a cross-site form can't send an authenticated request. If I moved the JWT into a cookie, I'd need to turn CSRF protection back on.

**8. How does account lockout work and what are its weaknesses?**
`LoginAttemptService` counts failures per lowercased username in a `ConcurrentHashMap`. After N failures it sets `lockedUntil`, and logins return 429 with `Retry-After` even with the right password. Weaknesses: it's per instance and resets on restart, an attacker can lock a victim out on purpose, and it doesn't catch spraying one password across many usernames. Fixes: Redis-backed counters, adding a per-IP rate limit (for example Bucket4j), and progressive delays instead of hard locks.

**9. How do you stop a user from making themselves ADMIN?**
`RegisterRequest` has no role field, and the service always assigns `Role.USER`. Entities are never bound directly from JSON (mass-assignment guard). A test posts `"role":"ADMIN"` and asserts the response role is USER. Admin endpoints are protected twice: by the URL matcher and by `@PreAuthorize`.

**10. How are errors handled, and why does that matter for security?**
`GlobalExceptionHandler` maps known exceptions to RFC 7807 `ProblemDetail`. Unknown exceptions are logged server-side and return a generic 500. `server.error.include-stacktrace: never` stops stack traces from leaking framework versions or internals. Bad JSON or enum values return 400, not 500, which tests cover.

**11. Bug you hit while building this?**
On the first `mvn test` run, all 23 Spring context tests failed with "No default constructor found" for `LoginAttemptService`. I had added a second, package-private constructor that takes a `Clock` for unit-testing the lock window. With two constructors, Spring couldn't tell which one to use. I fixed it by putting `@Autowired` on the public one. The second run passed: 25/25.

**12. What would you add next?**
Testcontainers with Postgres plus Flyway migrations, refresh tokens, a per-IP rate limit, an audit log table, and OWASP Dependency-Check or a CodeQL step in CI.
