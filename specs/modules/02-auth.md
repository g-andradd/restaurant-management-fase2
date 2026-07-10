# M02 — JWT Authentication

## Goal

Replace M01's permissive `SecurityConfig` with real JWT authentication:
credential verification against the existing `User` aggregate, a signed
token issued on login, and a filter chain that requires that token on
every other endpoint. No role-based authorization (M03), no refresh
token, no logout/revocation store.

## Deliverables

- `UserRepository.findByLogin` (port + JPA derived query + adapter).
- `application/exception`: `InvalidCredentialsException`,
  `InvalidTokenException` — application-layer concerns (login-flow
  orchestration failures), not `domain.exception` (aggregate invariants).
- `application/port/TokenProvider`: `generateToken`, `getExpirationSeconds`,
  `validateAndGetSubject`. Application/domain have zero `io.jsonwebtoken`
  imports (enforced by `ArchitecturePurityTest`).
- `AuthenticateUserUseCase` (plain class, wired via `BeanConfiguration`,
  same pattern as the five User use cases). Subject claim = user's UUID
  (stable even if `login` changes later); `login` rides along as a
  convenience custom claim.
- `JwtTokenProvider` (HS256, JJWT 0.12.6 — already a dependency since M00,
  first used here), `JwtProperties` (`@ConfigurationProperties` record
  binding `app.jwt.*`). Relies on JJWT's own `Keys.hmacShaKeyFor` to
  fail fast on an under-256-bit secret — no hand-rolled length check.
- `JwtAuthenticationFilter` (not a `@Component` — constructed directly in
  `SecurityConfig` to avoid Spring Boot double-registering it as a raw
  servlet filter). No per-request DB hit: the authenticated principal is
  just the token's subject, no authorities yet.
- `SecurityConfig` rewritten: stateless sessions, CSRF off,
  `POST /api/v1/auth/login` and springdoc paths `permitAll()`, everything
  else `authenticated()`.
- `ProblemDetailAuthenticationEntryPoint` (401) /
  `ProblemDetailAccessDeniedHandler` (403): hand-written `ProblemDetail`
  JSON. `AuthenticationException`/`AccessDeniedException` are thrown inside
  the Spring Security filter chain (by `AuthorizationFilter`) and caught
  there by `ExceptionTranslationFilter`, which delegates to these two
  handlers — entirely within the servlet filter layer, before the request
  ever reaches `DispatcherServlet`. That's why they're standalone classes
  rather than `@ExceptionHandler` methods: `@RestControllerAdvice` only
  intercepts exceptions thrown during normal Spring MVC dispatch, which
  this path never reaches. `GlobalExceptionHandler` gets one more handler
  for `InvalidCredentialsException` (401) for that normal MVC-dispatch path
  instead (thrown by `AuthenticateUserUseCase`, called from a plain
  `@RestController` method). All three share the same
  `urn:problem-type:unauthorized`/`forbidden` type URIs and JSON shape.
- `AuthController` (`POST /api/v1/auth/login`), `LoginRequest`/
  `LoginResponse`.

## Out of scope

- Roles/authorities, refresh tokens, logout/token revocation, `UserType`.

## Definition of Done

- `./mvnw verify` green: unit tests (use case, token provider), a
  `@WebMvcTest` controller test, and a full-context
  `@SpringBootTest`+Testcontainers integration test proving a protected
  endpoint is 401 without a token and 200 with a valid one obtained via
  `/auth/login`.
- ArchUnit: layering + purity rules (including the new
  domain/application-must-not-depend-on-`io.jsonwebtoken` rule) pass.
- JaCoCo 80% `BUNDLE`/`LINE` gate passes.
