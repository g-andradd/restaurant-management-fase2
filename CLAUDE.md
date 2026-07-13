# CLAUDE.md

Project conventions for AI-assisted development on this repository. This
file carries ALL standing project rules — module prompts should not need to
restate them.

## Project

Restaurant Management System — Phase 2 of the FIAP Pos Tech "Arquitetura e
Desenvolvimento Java" Tech Challenge. See `specs/product/overview.md` for
scope and `specs/technical/architecture.md` for the technical decisions.
Author: Gabriel Andrade Almeida.

## Git workflow

Full GitFlow:
- `main` ← `release/*` only. Never commit scaffold or in-progress module
  work directly to `main`.
- `develop` = integration branch. All module work lands here first, via PR.
- Each module/fix is a `feature/MXX-slug` (or `fix/...`, `chore/...`)
  branch off `develop`, merged back via PR (`--no-ff`).

**You create the branch and do file work ONLY. You never run `git add`,
`git commit`, or `git push`** — Gabriel reviews the diff and commits
himself. You end every module by printing the per-commit git commands for
him to run (see "Execution output format" below).

Commit message convention:
- `feat(MXX): ...` — new functionality for module XX.
- `chore(MXX): ...` — scaffolding, config, build tooling for module XX.
- `docs(MXX): ...` — documentation/specs for module XX.
- `test(MXX): ...` — test-only changes for module XX.
- `fix(MXX): ...` — a correction to already-landed module XX work.

## Architecture

Clean Architecture, base package `br.com.fiap.restaurant`, three layers,
dependency direction `infrastructure -> application -> domain`.

- `domain/` imports **no framework** — no Spring, no JPA, no Jakarta
  Validation, no JJWT.
- `application/` imports **no Spring and no JJWT**. Use cases are plain
  classes with zero Spring annotations (no `@Service`/`@Component`/etc.),
  wired as `@Bean`s in `infrastructure/config/BeanConfiguration.java`.
- `infrastructure/` may depend on `domain` and `application`.
- Spring's paging types (`Pageable`/`PageRequest`) never leave the JPA
  adapter — domain/application pagination is `(int page, int size)` +
  `count()`/`findAllById(...)`, assembled into `PageResult` in the use case.
- No `@PreAuthorize` / role-based Spring authorization anywhere.
  Authorization is a business rule and lives in use cases, not the
  framework/security layer.
- Manual mappers (no MapStruct). No Lombok. Java records for DTOs.
- `ddl-auto` stays `validate`. Flyway owns the schema — never edit an
  already-applied migration; add a new `V{n}__...sql` instead.

Enforced by `src/test/java/br/com/fiap/restaurant/architecture/` (ArchUnit:
`LayeredArchitectureTest` + `ArchitecturePurityTest`, scoped to production
code only via `DO_NOT_INCLUDE_TESTS`) and by `scripts/audit.sh` (re-checks
the same invariants independently of ArchUnit, plus structural checks
ArchUnit can't express — see "Definition of Done" below). Do not weaken
either to make code pass.

## Naming conventions

English for technical/structural naming — classes, packages, layers,
method names that describe mechanics (`create`, `reconstitute`, `execute`,
`findById`). Portuguese for domain vocabulary the brief itself uses —
field/method names like `nome`, `senha`, `preco`, `atualizarDados`,
`renomear`, `podeSerDono`, and terms like "Dono de Restaurante", "Tipo de
Cozinha", "Horário de Funcionamento". This is Domain-Driven Design's
ubiquitous-language principle: the domain vocabulary should match the
language the business brief and stakeholders actually use, not be
translated into English and back. It looks like inconsistency at a
glance; it is a deliberate choice, and existing Portuguese domain
identifiers are not to be renamed.

## Error contract

All errors are RFC 7807 `ProblemDetail`, produced by
`infrastructure/web/advice/GlobalExceptionHandler` (plus
`ProblemDetailAuthenticationEntryPoint`/`ProblemDetailAccessDeniedHandler`
for exceptions thrown inside the security filter chain, which
`@RestControllerAdvice` never sees).

- **400** = malformed/invalid request syntax (Bean Validation).
- **401** = missing/invalid credentials or token.
- **404** = the resource addressed **by the URL** does not exist.
- **409** = uniqueness conflict, or deleting a resource still referenced by
  another.
- **422** = the request is well-formed but references something that does
  not exist, or violates a business rule (distinct from 404: the missing
  thing is a *reference inside the request*, not the URL's own target).

**Every status `GlobalExceptionHandler` can emit MUST be asserted by at
least one test.** A deliberate status choice with no test is an
unprotected decision — the next handler added or reordered can silently
change it. Give every new domain/application exception its own explicit
`@ExceptionHandler`; don't rely on the generic `DomainException` fallback
for a status you actually care about.

## Security

- JWT claims are a **login-time snapshot**. Never make an authorization or
  business decision from a token claim — always re-read the current state
  from the database at decision time. `JwtAuthenticationFilter` never hits
  the database per request by design, so nothing keeps a claim fresh after
  login.
- Public routes today: `POST /api/v1/auth/login`, `POST /api/v1/users`
  (self-registration), `GET /api/v1/user-types/**` (signup discovery),
  springdoc/swagger. Everything else requires a valid Bearer token.

## Testing

- JUnit 5, Mockito, AssertJ for unit tests.
- Testcontainers (Postgres) for integration tests that touch persistence —
  use `@ServiceConnection` on a `PostgreSQLContainer`, not manual
  `@DynamicPropertySource`, unless there's a specific reason not to.
- ArchUnit for architecture rules (see "Architecture" above).
- JaCoCo gate: 80% line coverage (`BUNDLE`/`LINE`/`COVEREDRATIO`), enforced
  on `mvn verify`. Do not lower the threshold to make a build pass — write
  tests instead.

## Definition of Done (every module, non-negotiable)

- `./mvnw verify` green (tests + ArchUnit + JaCoCo 80% `BUNDLE`/`LINE` gate).
- `bash scripts/audit.sh` exits 0.
- **Cold-start proof**: every new endpoint is reachable by a brand-new
  deployment through HTTP alone. If a test seeds data by calling a use
  case directly, it walks around the security filter chain and proves
  nothing about reachability. At least one test per module must go through
  HTTP end-to-end with no out-of-band seeding.
- Every Acceptance Criterion (from planning) is mapped to a named test.

## Output formats

**Planning output must end with an Acceptance Criteria table:**

| AC | Criterion (observable behaviour) | How it will be proven (test name) |
|----|-----------------------------------|-------------------------------------|

Every design decision Gabriel approves becomes a numbered AC. An AC with
no named test is not a plan.

**Execution output must end with an AC Evidence table:**

| AC | Test class#method | Assertion | Status |
|----|--------------------|-----------|--------|

Plus: `./mvnw verify` result, `bash scripts/audit.sh` result, changed-file
list, and the per-commit git commands (per "Git workflow" above — do not
run them). If any AC lacks a test, say so explicitly instead of reporting
done.

## Specs

Specs live under `specs/`:
- `specs/product/` — product-level scope and actors.
- `specs/technical/` — architecture and stack decisions.
- `specs/modules/` — one file per module (`NN-name.md`), each stating goal,
  deliverables, out-of-scope, and Definition of Done.

When starting a new module, read the relevant `specs/modules/NN-*.md` first,
and update it if the implementation diverges from what was planned.
