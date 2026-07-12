# M05 — MenuItem (Item de Cardápio)

## Goal

Full CRUD for the items a restaurant sells, nested under the owning
restaurant since a `MenuItem` has no meaning outside one. This is the last
feature module (4th and final aggregate per `specs/product/overview.md`).

## Deliverables

- `MenuItem` — aggregate root (`domain/model/MenuItem.java`): `nome`,
  `descricao` (nullable), `preco` (`BigDecimal`, `preco > 0` enforced via
  `compareTo`, never `<=`/`==` on `BigDecimal`), `disponivelSomenteNoLocal`
  (boolean, dine-in-only flag), `fotoPath` (nullable `String` — a storage
  path only; no upload, no file I/O, per the brief), `restaurantId`
  (immutable, referenced by UUID only — same cross-aggregate-reference
  discipline as `Restaurant.ownerId`).
- Nested routes under `/api/v1/restaurants/{restaurantId}/menu-items`
  (`MenuItemController`, 5 use cases mirroring the CRUD shape of every
  prior module). `GET` (one/list) is open to any authenticated user
  (customers browse menus); `POST`/`PUT`/`DELETE` require the caller to
  own the restaurant.
- `V6__create_menu_items_table.sql` — `menu_items` table, FK to
  `restaurants` (plain constraint, **no** `ON DELETE CASCADE`), `preco
  NUMERIC(10,2) NOT NULL`, index on `restaurant_id` (every list/lookup in
  this module filters by it).

### The nested-route ownership/existence trap (P0 of this module)

A naive by-id lookup (`GET`/`PUT`/`DELETE /restaurants/{restaurantId}/menu-items/{id}`)
that only checks "does this item exist" — ignoring whether it belongs to
`{restaurantId}` — lets anyone read, edit, or delete another restaurant's
item just by wrapping its id in their own restaurant's path. No naive test
catches this, because no naive test tries mismatched ids.

**Fix**: every by-id use case (`GetMenuItemByIdUseCase`,
`UpdateMenuItemUseCase`, `DeleteMenuItemUseCase`) checks, in this exact
order:

1. Does the restaurant exist? → `RestaurantNotFoundException` (404) if not.
2. Does the item exist **and** does its `restaurantId` match the path? →
   **one** `MenuItemNotFoundException` (404) if either is false —
   evaluated **before** the ownership check, unconditionally, so a
   mismatched item always 404s regardless of who's calling. A 403 here
   would confirm the item exists somewhere, which is exactly the leak
   this check prevents.
3. *(mutations only)* Does the caller own the restaurant? →
   `NotRestaurantOwnerException` (403).

One shared `MenuItemNotFoundException` is used for both "doesn't exist"
and "belongs to a different restaurant" — the client can't tell the
difference, deliberately.

### Authorization is indirect

The caller must own the **restaurant**, not the item — there is no
per-item ownership concept. `AuthenticatedUserProvider` and
`NotRestaurantOwnerException` are reused as-is from M04. No
`@PreAuthorize` anywhere (`ArchitecturePurityTest.mustNotUsePreAuthorizeAnnotations`
already covers the new controller with no edit needed — it scans the
whole codebase).

### Cascade delete, deliberately breaking the M03/M04 pattern

Deleting a restaurant deletes its menu items. This is the **opposite**
choice from `User`→`UserType` and `User`→`Restaurant` (both blocked from
deletion while referenced, via 409): those are independent-lifecycle
aggregates, but a `MenuItem` is a composed child with no life of its own —
forcing an owner to delete 30 items before deleting the restaurant would
be a bad API. `DeleteUserUseCase`'s existing 409-on-owns-restaurants check
is **unchanged**; the cascade only applies restaurant → menu items.

Implemented **explicitly** in `DeleteRestaurantUseCase` (calls
`MenuItemRepository.deleteByRestaurantId(UUID)` before
`RestaurantRepository.deleteById(UUID)`), never as a database-level
`ON DELETE CASCADE` — invisible behaviour the domain doesn't express. The
FK stays a plain constraint.

### `TransactionRunner` — atomic cascade without leaking `@Transactional`

`application/usecase/*` classes may carry zero Spring annotations
(including `@Transactional`), so `DeleteRestaurantUseCase` can't wrap its
two writes (bulk-delete menu items, then delete the restaurant) in one
transaction directly. Solved with a new Spring-free port,
`application/port/TransactionRunner` (`void run(Runnable action)`),
implemented by `infrastructure/config/SpringTransactionRunner`
(`@Component`, backed by a `TransactionTemplate` constructed from the
auto-configured `PlatformTransactionManager` — Spring Boot doesn't
auto-configure a `TransactionTemplate` bean itself). `DeleteRestaurantUseCase`
wraps both writes in `transactionRunner.run(...)`, in the order the FK
requires (items first). ArchUnit's application-purity rule enforces the
port itself stays Spring-free; only the `infrastructure`-side
implementation touches Spring's transaction machinery.

### `DomainValidationException` — 400 without a blanket `IllegalArgumentException` handler

`preco <= 0` needs the same shape of fix M04 needed for
`HorarioFuncionamento`'s `abertura`/`fechamento` invariant (see below).
Rather than mapping plain `IllegalArgumentException` globally to 400 — a
type also thrown by library code and genuine programming bugs, which
would tell a client "your request is bad" for a server problem — a new
`domain/exception/DomainValidationException extends DomainException` was
introduced. Every domain invariant validator (`User`, `UserType`,
`Restaurant`, `HorarioFuncionamento`, `MenuItem`) now throws this instead
of `IllegalArgumentException`, and `GlobalExceptionHandler` maps it to
400. Plain `IllegalArgumentException` still falls through to the generic
500 fallback — correctly, since it now means an actual bug, not bad user
input.

### `scripts/audit.sh` section 9 — catching the blind spot that let this ship

Section 7 (status-code contract coverage) only compares statuses the
handler **emits** against statuses tests **assert** — it can't see a
status that *should* be emitted but was never wired up at all, which is
exactly how the `HorarioFuncionamento` gap shipped in M04 undetected. New
section 9 scans every `new XxxException(` construction (both literal
`throw new X(...)` and the `.orElseThrow(() -> new X(...))` idiom used
throughout this codebase) in `domain/`/`application/` and **WARNs** (not
fails) if `GlobalExceptionHandler` never mentions that type.
`IllegalStateException` (the M04 nit fix, below) correctly WARNs here —
that's intentional, not a bug: it's meant to fall through to 500.

### M04 nit fixed: `CreateRestaurantUseCase`'s dead 404 branch

`CreateRestaurantUseCase` used to throw `UserTypeNotFoundException` (404)
if the owner's `UserType` couldn't be resolved — unreachable in practice
(`users.user_type_id` is `NOT NULL` with an FK to `user_types`), but a 404
there would have violated "404 only for the URL's own target" if it ever
somehow fired. Replaced with `IllegalStateException` ("data corruption,
should be unreachable"). Confirmed by reviewing every `application/usecase/*.java`:
no other `POST`/`PUT` use case throws a `*NotFoundException` for a body
reference (`CreateUserUseCase`/`UpdateUserUseCase` already use
`InvalidUserTypeReferenceException` → 422 for a bad `userTypeId`, and
`UpdateRestaurantCommand` carries no cross-aggregate reference at all).

## Status contract for M05

- **400** — malformed body / blank `nome` / `preco <= 0` (Bean Validation
  on the request DTO, plus the domain's own `DomainValidationException`
  as defense-in-depth).
- **401** — no/invalid token (unchanged, existing JWT filter chain).
- **403** — authenticated but not the restaurant's owner
  (`POST`/`PUT`/`DELETE` only).
- **404** — unknown `restaurantId`, unknown item id, **or** an item that
  exists but belongs to a different restaurant (the trap — all three
  collapse to 404; the latter two share `MenuItemNotFoundException`).
- **422** — **none in this module.** `MenuItem`'s body has no
  cross-aggregate reference (`restaurantId` comes only from the URL path,
  never accepted in the request body — accepting it in the body too would
  recreate the exact two-sources-of-truth trap the P0 fix prevents).
  `preco <= 0` is a single-field constraint on the body itself, so it's
  400, not 422. Stated explicitly rather than inventing a 422 case.

## Out of scope

- Photo upload/storage/serving — `fotoPath` is a string column only, per
  the brief ("this is a back-end service and the photo is not used").
- Restaurant/menu-item transfer between restaurants.
- Role-based Spring Security authorization (`@PreAuthorize` etc.) — same
  reasoning as M03/M04.

## Definition of Done

- `./mvnw verify` green: domain tests (`MenuItemTest`, plus updated
  `UserTest`/`UserTypeTest`/`RestaurantTest`/`HorarioFuncionamentoTest`
  asserting `DomainValidationException` instead of
  `IllegalArgumentException`), 5 use-case test classes (including the
  trap test on `GetMenuItemByIdUseCase`/`UpdateMenuItemUseCase`/
  `DeleteMenuItemUseCase`, each proving `MenuItemNotFoundException`, never
  `NotRestaurantOwnerException`, for a cross-restaurant item),
  `MenuItemControllerTest` (full status matrix), `MenuItemRepositoryAdapterTest`
  (including a real bulk `deleteByRestaurantId` proof), `DeleteRestaurantUseCaseTest`
  (cascade ordering + `TransactionRunner` wrapping verified via `InOrder`),
  `CreateRestaurantUseCaseTest` (`IllegalStateException` replacing
  `UserTypeNotFoundException`), and `MenuItemIntegrationTest` covering:
  - cold start (`coldStartCreateMenuItemWorksThroughHttpAlone`) — signup
    as Dono → login → create restaurant → create menu item, HTTP only.
  - the P0 trap end-to-end on GET, PUT, **and** DELETE, with two real
    restaurants owned by two different Donos
    (`crossRestaurantItemAccessReturns404NotForbiddenOnGetPutAndDelete`).
  - non-owner mutation → 403 end-to-end.
  - `deletingARestaurantCascadesToItsMenuItems`.
  - `getMenuItemIsOpenToAnyAuthenticatedUserNotJustTheOwner`.
  - `RestaurantIntegrationTest.aberturaNotBeforeFechamentoReturns400NotServerError`
    — the M04 regression, proven through the real use case, not a mock.
- `bash scripts/audit.sh` exits 0, including new section 9 (every
  constructed exception type either has a handler or is a deliberate,
  reviewable WARN).
- `V6` applies cleanly; `ddl-auto` stays `validate`;
  `MenuItemJpaEntity.preco`'s `@Column(precision = 10, scale = 2)` matches
  the migration exactly.
- ArchUnit layering + purity rules pass unchanged (no new packages
  needed; `TransactionRunner` is a port in `application`, its Spring
  implementation lives in `infrastructure`).
