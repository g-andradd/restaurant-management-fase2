# M01 — User Aggregate (CRUD, no authentication)

## Goal

Implement the `User` aggregate with full CRUD, and establish the
`ProblemDetail` (RFC 7807) error-handling foundation reused by all later
modules. No authentication yet — JWT/login lands in M02.

## Deliverables

- Domain: `User` (invariants in constructor/mutators, zero framework
  imports), `UserRepository` port (including `existsBy...AndIdNot` variants
  for the update flow), `DomainException` hierarchy
  (`UserNotFoundException`, `EmailAlreadyExistsException`,
  `LoginAlreadyExistsException`).
- Application: five use cases (`CreateUserUseCase`, `GetUserByIdUseCase`,
  `ListUsersUseCase`, `UpdateUserUseCase`, `DeleteUserUseCase`) as plain
  classes with no Spring annotations; `PasswordEncoder` port; `PageQuery`/
  `PageResult` records keeping pagination framework-agnostic (the domain
  port takes `(page, size)` + `count()`, never `Pageable`).
- Infrastructure: JPA entity/repository/adapter/mapper for persistence;
  REST controller + request/response DTOs + web mapper; `SecurityConfig`
  (`permitAll()` — required now because `spring-boot-starter-security`,
  added in M00, would otherwise Basic-auth-protect these new endpoints);
  `BCryptPasswordEncoderAdapter`; `BeanConfiguration` wiring the
  framework-free use cases as `@Bean`s; `GlobalExceptionHandler`
  (`ProblemDetail` for 400/404/409/422/500).
- `V2__create_users_table.sql` (unique indexes on `email` and `login`).
- `ArchitecturePurityTest`: domain has zero `org.springframework..` /
  `jakarta.persistence..` / `jakarta.validation..` dependencies; application
  has zero `org.springframework..` dependencies.

## Out of scope

- JWT, login endpoint, `UserType` field on `User` (M03), Restaurant/MenuItem
  aggregates.

## Endpoints (`/api/v1/users`)

`POST` create, `GET /{id}` get one, `GET` list (paginated,
`page`/`size` query params), `PUT /{id}` update (password change optional),
`DELETE /{id}` delete.

## Definition of Done

- `./mvnw verify` green: 43 tests, ArchUnit layering + purity rules pass,
  JaCoCo `BUNDLE`/`LINE` check passes (92.4% actual, gate is 80%).
- All five endpoints implemented; `ProblemDetail` on 400/404/409.
- Flyway `V2` applies cleanly; `ddl-auto` stays `validate`.
