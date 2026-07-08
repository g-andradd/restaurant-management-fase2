# CLAUDE.md

Project conventions for AI-assisted development on this repository.

## Project

Restaurant Management System — Phase 2 of the FIAP Pos Tech "Arquitetura e
Desenvolvimento Java" Tech Challenge. See `specs/product/overview.md` for
scope and `specs/technical/architecture.md` for the technical decisions.

## Base package

`br.com.fiap.restaurant`

## Architecture rule

Clean Architecture, three layers, dependency direction:
`infrastructure -> application -> domain`.

- `domain` must have **zero** framework dependencies (no Spring, no JPA
  annotations, no Jackson).
- `application` may depend only on `domain`.
- `infrastructure` may depend on `domain` and `application`.

This is enforced by
`src/test/java/br/com/fiap/restaurant/architecture/LayeredArchitectureTest.java`
(ArchUnit). Any change that violates the rule must fail that test — do not
weaken the test to make code pass.

## Testing

- JUnit 5, Mockito, AssertJ for unit tests.
- Testcontainers (Postgres) for integration tests that touch persistence —
  use `@ServiceConnection` on a `PostgreSQLContainer`, not manual
  `@DynamicPropertySource`, unless there's a specific reason not to.
- ArchUnit for architecture rules.
- JaCoCo gate: 80% line coverage (`BUNDLE`/`LINE`/`COVEREDRATIO`), enforced
  on `mvn verify`. This became a real gate once production code exists
  (from the module after M00 onward) — do not lower the threshold to make a
  build pass; write tests instead.

## Branching (GitFlow)

- `main`: stable, only moves via a release merge. Never commit scaffold or
  in-progress module work directly to `main`.
- `develop`: integration branch. All module work lands here first.

## Commit convention

- `feat(MXX): ...` — new functionality for module XX.
- `chore(MXX): ...` — scaffolding, config, build tooling for module XX.
- `docs(MXX): ...` — documentation/specs for module XX.
- `test(MXX): ...` — test-only changes for module XX.

## Error handling

Error responses use `ProblemDetail` (RFC 7807). Apply this from the first
module that exposes a real HTTP endpoint onward.

## Specs

Specs live under `specs/`:
- `specs/product/` — product-level scope and actors.
- `specs/technical/` — architecture and stack decisions.
- `specs/modules/` — one file per module (`NN-name.md`), each stating goal,
  deliverables, out-of-scope, and Definition of Done.

When starting a new module, read the relevant `specs/modules/NN-*.md` first,
and update it if the implementation diverges from what was planned.
