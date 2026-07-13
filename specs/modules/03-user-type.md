# M03 — UserType (Tipo de Usuário)

## Goal

A first-class structure distinguishing "Dono de Restaurante" from "Cliente,"
with full CRUD, and a way to (re-)associate a type with an existing `User`.

## Deliverables

- `UserType` — its own aggregate root (`domain/model/UserType.java`),
  referenced by `User` via `userTypeId` (UUID) only, never a live object —
  standard cross-aggregate-reference discipline. `UserTypeRepository` port
  includes `findAllById(Collection<UUID>)` specifically to let list
  operations resolve many types in one query instead of N.
- Full CRUD under `/api/v1/user-types` (`UserTypeController`,
  `CreateUserTypeUseCase`/`GetUserTypeByIdUseCase`/`ListUserTypesUseCase`/
  `UpdateUserTypeUseCase`/`DeleteUserTypeUseCase`), mirroring M01's `User`
  CRUD shape exactly.
- `V3__create_user_types_table.sql` — `user_types` table, unique index on
  `nome`, two seeded rows with **fixed UUIDs**
  (`00000000-0000-0000-0000-000000000001` = "Dono de Restaurante",
  `00000000-0000-0000-0000-000000000002` = "Cliente") so the demo/Postman
  flow works without a discovery step. `users.user_type_id` added
  `NOT NULL` (backfilled to "Cliente" for any pre-existing rows) with an FK
  to `user_types`.
- Association: reuses the existing `CreateUserRequest`/`UpdateUserRequest`
  (both gain a required `userTypeId`) rather than a dedicated endpoint —
  covers both "chosen at signup" and "reassigned on an existing user" with
  no new routes.
- `UserResponse`/`UserResult` embed a nested `{ id, nome }` object
  (`userType`), not a bare id — `GetUserByIdUseCase` and `ListUsersUseCase`
  both gained a `UserTypeRepository` dependency for this.
  **`ListUsersUseCase` resolves the whole page's distinct type ids in one
  `findAllById` call** and maps in memory — never one `findById` per user.
- Unknown `userTypeId` referenced from a `POST`/`PUT /api/v1/users` body →
  `InvalidUserTypeReferenceException` → **422 Unprocessable Entity**, via
  its own explicit `@ExceptionHandler` in `GlobalExceptionHandler` (not
  the generic `DomainException` fallback — see the rule below for why this
  needed to be pinned down explicitly). Deliberately distinct from
  `UserTypeNotFoundException` → **404**, which is reserved for
  `GET`/`PUT`/`DELETE /api/v1/user-types/{id}` where the missing resource
  *is* the URL's own target.

### Rule: 404 vs. 422 for a non-existent UserType

**A non-existent `UserType` is 404 when it is the resource addressed by the
URL, and 422 when it is a reference inside a request body.** This is not
incidental — it was initially only enforced by falling through to M01's
generic `DomainException` → 422 catch-all, which meant the distinction was
protected by nothing: a future handler addition or reordering (e.g. when
M04 adds `Restaurant` domain exceptions) could have silently changed the
status with no test failing. It's now backed by both an explicit handler
(`GlobalExceptionHandler.handleInvalidUserTypeReference`) and tests that
assert the contrast directly (`UserControllerTest.create/updateReturns422OnUnknownUserTypeId`,
`UserTypeControllerTest.getByIdReturns404WhenNotFound`,
`UserTypeIntegrationTest.publicSignupWithNonExistentUserTypeIdReturns422`).
**Follow the same pattern for any future cross-aggregate reference**
(e.g. M04's `Restaurant.ownerId`): a dedicated exception + explicit handler
+ a test asserting the specific status, not a reliance on the generic
fallback.
- Deleting an in-use `UserType` → `UserTypeInUseException` → 409 Conflict,
  checked explicitly in `DeleteUserTypeUseCase` (via the new
  `UserRepository.existsByUserTypeId`) — never a raw FK violation
  surfacing as a 500.
- `GET /api/v1/user-types/**` is public (`permitAll`); `POST`/`PUT`/`DELETE`
  on it require a token — the read side has to be public so an anonymous
  self-registration can discover valid type ids at all.
- `AuthenticateUserUseCase` embeds the type's `nome` as a `"userType"` JWT
  claim, alongside the existing `"login"` claim. **This claim is a
  login-time snapshot, not re-validated per request** (see the Javadoc on
  `AuthenticateUserUseCase` and the warning below) — if a user's type is
  reassigned after login, their existing token keeps the old type name
  until it expires.

## Correctness trap flagged for M04

**Never treat the `"userType"` JWT claim as authoritative for an
authorization decision.** It's a convenience/display value only. M04's
"only a Dono de Restaurante may own a restaurant" rule — or any future
business rule gated on a user's type — must re-read the current type from
the database (`UserRepository`/`UserTypeRepository`) at decision time, not
trust the token. `JwtAuthenticationFilter` never touches the database per
request by design (see M02), so nothing in the request pipeline keeps this
claim fresh.

## Out of scope

- Role-based Spring Security authorization (`@PreAuthorize` etc.) anywhere —
  the "only a Dono may own a restaurant" rule is a business rule that
  belongs in M04's restaurant use case, not the framework/security layer.
- Admin role or approval workflow for self-registration — a self-registering
  user picks their own type freely, including "Dono de Restaurante." See
  NOTES.md for the reasoning and its accepted limitation.

## Definition of Done

- `./mvnw verify` green: domain/use-case/controller/persistence tests for
  `UserType`, updated `User`-side tests, and an integration test proving
  self-registration as "Dono de Restaurante" end-to-end, re-assignment of
  an existing user's type, and 409 on deleting an in-use type.
- The 404-vs-422 rule above is asserted by tests, not just implemented —
  both `isUnprocessableEntity()` (create/update with a bogus `userTypeId`)
  and `isNotFound()` (`GET /user-types/{unknown-id}`) appear in the suite.
- `V3` applies cleanly; `ddl-auto` stays `validate`.
- ArchUnit layering + purity rules pass unchanged (no new packages needed).
- The `fix/M02` cold-start test still passes, now supplying a seeded
  `userTypeId`.
