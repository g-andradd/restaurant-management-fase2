# M00 — Bootstrap

## Goal

Stand up an empty, well-structured, buildable project skeleton. No business
logic.

## Deliverables

- Maven project (`pom.xml`) with all locked dependencies, `${revision}` +
  `flatten-maven-plugin`, JaCoCo (80% gate, configured now).
- Clean Architecture package skeleton under `br.com.fiap.restaurant`
  (`domain`, `application`, `infrastructure`), enforced by ArchUnit.
- Configuration profiles: `application.yml`, `application-dev.yml`,
  `application-test.yml`.
- Docker Compose (postgres + app, healthchecked) and a multi-stage
  `Dockerfile`.
- Flyway baseline: empty `V1__baseline.sql` (see
  `specs/technical/architecture.md` for the tradeoff behind this choice).
- Repo meta files: `CLAUDE.md`, `NOTES.md`, `README.md`, `.gitignore`,
  `.gitattributes`, `.env.example`.
- Specs seed (`specs/product/overview.md`,
  `specs/technical/architecture.md`, this file).
- Verification: `LayeredArchitectureTest` (ArchUnit) and
  `RestaurantManagementApplicationTests` (Spring Boot context-load smoke
  test against a real Testcontainers Postgres).

## Out of scope

- No `User`, `UserType`, `Restaurant`, or `MenuItem` entities.
- No CRUD endpoints, no controllers beyond what the smoke test needs
  (none — the smoke test is a context-load test, not an HTTP call).
- No `SecurityConfig`/JWT wiring — deferred to the module that introduces
  the first controller.
- No `ProblemDetail` error handling — deferred likewise.

## Definition of Done

- `mvn verify` is green on a clean checkout.
- `docker compose up --build` brings up `postgres` (healthy) then `app`.
- `LayeredArchitectureTest` and `RestaurantManagementApplicationTests` pass.
- No business/domain entities, no controllers.
- All M00 commits present on `develop`, pushed to `origin/develop`.
