# M04 — Restaurant

## Goal

Full CRUD for `Restaurant`, owned by a `User` whose `UserType` permits
ownership, with ownership enforced at both creation time and on every
subsequent mutation — re-checked against current database state, never
against a JWT claim.

## Deliverables

- `Restaurant` — aggregate root (`domain/model/Restaurant.java`), references
  its owner via `ownerId` (UUID) only, never a live `User` object — same
  cross-aggregate-reference discipline as `User → UserType`. `ownerId` is
  immutable after creation: `atualizarDados` deliberately excludes it (no
  restaurant-transfer use case in Phase 2).
- `HorarioFuncionamento` — value object (`abertura`/`fechamento` as
  `LocalTime`), constructor enforces `abertura` strictly before
  `fechamento`. See the accepted limitation below.
- `TipoCozinha` — enum (`ITALIANA`, `JAPONESA`, `BRASILEIRA`, `ARABE`,
  `MEXICANA`, `FAST_FOOD`, `VEGETARIANA`, `OUTRA`).
- `UserType.podeSerDono` — new capability flag (`can_own_restaurant` column,
  `V4__add_can_own_restaurant_to_user_types.sql`), backfilled `TRUE` only
  for the seeded "Dono de Restaurante" row. Authorization is keyed on this
  flag, **never** on the type's name or its fixed UUID — a future type
  rename or a new type entirely could otherwise silently break or bypass
  the rule.
- Full CRUD under `/api/v1/restaurants` (`RestaurantController`,
  `CreateRestaurantUseCase`/`GetRestaurantByIdUseCase`/`ListRestaurantsUseCase`/
  `UpdateRestaurantUseCase`/`DeleteRestaurantUseCase`). `GET` (single and
  list) is open to any authenticated user — ownership only gates
  create/update/delete.
- `AuthenticatedUserProvider` port + `SpringSecurityAuthenticatedUserProvider`
  adapter — reads the caller's id from `SecurityContextHolder`, giving use
  cases a framework-free way to ask "who is making this call" without
  touching Spring Security types directly.
- **Create-time ownership check (P0)**: `CreateRestaurantUseCase` requires
  `command.ownerId().equals(authenticatedUserProvider.getCurrentUserId())`,
  thrown as `NotRestaurantOwnerException` → 403, checked *before* the
  can-own-type check. There is no admin role in Phase 2, so there is no
  legitimate create-on-behalf-of case — without this check, any
  authenticated user (even a Cliente) could create a restaurant in
  someone else's name just by knowing their id.
- **Update/delete ownership check**: `UpdateRestaurantUseCase`/
  `DeleteRestaurantUseCase` re-read the restaurant's `ownerId` from the
  database and compare it to the caller's current id on every call —
  never trusting a cached value. Mismatch → `NotRestaurantOwnerException`
  → 403.
- **`DeleteUserUseCase` gains a pre-check**: a `User` who still owns at
  least one `Restaurant` cannot be deleted (`UserOwnsRestaurantsException`
  → 409), checked via `RestaurantRepository.existsByOwnerId` before the
  delete — avoids an FK violation surfacing as a raw 500.
- N+1 avoidance: `ListRestaurantsUseCase` resolves the page's distinct
  owner ids in one `UserRepository.findAllById` call, never one `findById`
  per restaurant — same pattern as M03's `ListUsersUseCase`.
- ArchUnit: `ArchitecturePurityTest.mustNotUsePreAuthorizeAnnotations` —
  no method anywhere may carry `@PreAuthorize`. Ownership authorization is
  a business rule that lives in use cases as explicit, testable code, not
  a framework annotation that would hide the rule from unit tests.

## Status contract (M04 additions)

- **403 Forbidden** (new for this module) — authenticated, but not
  authorized: caller is not the restaurant's owner (update/delete), or is
  trying to create a restaurant on behalf of someone else (create).
  `NotRestaurantOwnerException` (`application.exception`).
- **404 Not Found** — `RestaurantNotFoundException` for
  `GET`/`PUT`/`DELETE /api/v1/restaurants/{id}` where the id is the URL's
  own missing target. Consolidated into `GlobalExceptionHandler.handleNotFound`
  alongside `UserNotFoundException`/`UserTypeNotFoundException`.
- **422 Unprocessable Entity** — two distinct causes, each with its own
  dedicated exception/handler (not the generic `DomainException`
  fallback, per the M03 rule):
  - `InvalidUserReferenceException` — `ownerId` in the request body
    references a non-existent `User`.
  - `UserCannotOwnRestaurantException` — `ownerId` refers to a real user,
    but that user's current `UserType.podeSerDono` is `false`.
- **409 Conflict** — `UserOwnsRestaurantsException` on `DELETE /api/v1/users/{id}`
  when the user still owns at least one restaurant.
- **400 Bad Request** — new `GlobalExceptionHandler` handler for
  `HttpMessageNotReadableException`, needed because an unknown
  `tipoCozinha` enum literal in the request body now fails during Jackson
  deserialization (before Bean Validation ever runs) rather than falling
  through to the generic 500.

## Accepted limitation: no past-midnight operating hours

`HorarioFuncionamento` requires `abertura` to be strictly before
`fechamento` on the same clock, so a restaurant that opens at 18:00 and
closes at 02:00 the next day cannot be represented. Modeling an
overnight window correctly (day-rollover, "open now" queries crossing
midnight) is real complexity with no product requirement driving it in
Phase 2. Documented here rather than solved; revisit if a future phase
needs overnight hours.

## Out of scope

- Restaurant ownership transfer (changing `ownerId` after creation).
- Any notion of restaurant approval/moderation before it becomes visible
  via `GET`.
- Role-based Spring Security authorization (`@PreAuthorize` etc.) — same
  reasoning as M03: ownership is a business rule, not a framework concern.

## Definition of Done

- `./mvnw verify` green: domain tests (`RestaurantTest`,
  `HorarioFuncionamentoTest`), use-case tests for all 5 use cases
  (including the P0 create-time identity check and the capability-flag
  check, both proven independent of name/UUID string comparison),
  controller tests covering the full status matrix (201/200/204/400/403/
  404/422), persistence tests (`RestaurantRepositoryAdapterTest`), and
  `RestaurantIntegrationTest` covering:
  - cold start (`coldStartCreateRestaurantWorksThroughHttpAlone`) — proves
    reachability through HTTP alone, no direct use-case/repository seeding.
  - the P0 fix end-to-end (`createForAnotherUserReturns403EndToEnd`,
    `clienteCannotCreateRestaurantForADono`).
  - the JWT-staleness correctness trap, behaviourally
    (`staleTokenClaimDoesNotGrantOwnership`): demote a logged-in user's
    type via `PUT /api/v1/users/{id}`, then reuse the *old* token to
    attempt a restaurant creation — must be 422, not 201.
  - `userWithRestaurantCannotBeDeletedReturns409`.
  - `getRestaurantIsOpenToAnyAuthenticatedUserNotJustTheOwner`.
- `scripts/audit.sh` exits 0, including its 403-specific check: every
  `HttpStatus.FORBIDDEN` emission must be asserted via `isForbidden()`
  somewhere in the suite.
- `V4`/`V5` apply cleanly; `ddl-auto` stays `validate`.
- ArchUnit layering + purity rules pass, including the new
  no-`@PreAuthorize` rule.
