# Technical Architecture

## Style: Clean Architecture, three layers

```
infrastructure -> application -> domain
```

- **domain**: entities, value objects, domain services, repository port
  interfaces. Zero framework dependencies (no Spring, no JPA annotations).
- **application**: use cases orchestrating domain logic. Depends only on
  domain.
- **infrastructure**: Spring MVC controllers, Spring Data JPA entities and
  repositories, security, external adapters. Depends on domain and
  application.

The dependency rule is enforced by an ArchUnit test
(`br.com.fiap.restaurant.architecture.LayeredArchitectureTest`), not just by
convention.

Base package: `br.com.fiap.restaurant`.

## Stack

- Java 21, Spring Boot 3.3.5, single-module Maven.
- PostgreSQL + Flyway (`flyway-core` + `flyway-database-postgresql`).
- Spring Data JPA in the infrastructure layer only.
- Spring Security 6 + JWT (JJWT 0.12.6).
- springdoc-openapi 2.6.0 (Swagger UI at `/swagger-ui.html`).
- Testing: JUnit 5, Mockito, AssertJ (all via `spring-boot-starter-test`),
  Testcontainers (Postgres, via `spring-boot-testcontainers` +
  `@ServiceConnection`), ArchUnit 1.3.0.
- JaCoCo 0.8.12, 80% line-coverage gate (`BUNDLE`/`LINE`/`COVEREDRATIO`),
  bound to `verify`.
- Versioning: `${revision}` property + `flatten-maven-plugin`
  (`resolveCiFriendliesOnly`).
- Docker + docker-compose (app + postgres), both with healthchecks/depends_on.

## Error handling

Error responses use `ProblemDetail` (RFC 7807). Wired starting with the first
module that exposes a real endpoint — not part of M00.

## Branching

GitFlow: `main` (stable, only moves via release merge) + `develop`
(integration branch — all module work lands here first).

## Commit convention

`feat(MXX): ...` / `chore(MXX): ...` / `docs(MXX): ...` / `test(MXX): ...`
