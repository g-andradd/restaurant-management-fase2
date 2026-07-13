# M06 — Postman Collection + Docker Compose Validation

## Goal

A delivery module, not a feature module: no new business logic, no new
endpoints. Produces the FIAP deliverables "Collections para teste" and
"Configuração Docker Compose", and validates the Docker Compose stack from
a truly clean state for the first time since M00.

## Deliverables

- `postman/RestaurantManagement.postman_collection.json` +
  `postman/RestaurantManagement.postman_environment.json` — runs top to
  bottom in the Postman Collection Runner with zero manual steps, against a
  freshly started app with an empty database. 48 requests across 6 folders,
  69 assertions, all chained via `pm.environment.set(...)` — nothing
  hardcoded except the two seeded `UserType` UUIDs (`donoTypeId`/
  `clienteTypeId`, documented in the environment file itself).
- `scripts/audit.sh` section 10 — Postman-collection/controller route drift
  guard.
- `scripts/audit.sh` section 11 — docker-compose.yml missing-default guard.
- `docker-compose.yml` fix — the P0 of this module (below).
- README additions: importing/running the collection, corrected Docker
  instructions.

## Folder structure and the ordering resolution

`Auth → Users → UserTypes → Restaurants → MenuItems → Regras de negócio e
erros`. The `Auth` folder's first request is signup (`POST /users`,
public) — signup is a prerequisite of "getting authenticated," so it lives
there procedurally; its second request is the actual login, capturing
`{{token}}`. Every later folder reuses the identity `Auth` established.
This is the only ordering that lets every folder depend only on state a
prior folder already captured, with zero manual steps.

**Every folder demonstrates full CRUD** (a rubric that explicitly grades
"os endpoints funcionando conforme descrito" cannot be satisfied by an
incomplete-looking folder):
- `Users` and `Restaurants` each create a **throwaway** second
  resource purely to demonstrate `DELETE` (204), so the primary user/
  restaurant survives for later folders that still need it.
- `MenuItems` demonstrates full CRUD on its own items, **then** deletes the
  restaurant itself as its last two requests — doubling as the cascade-delete
  proof (a GET on the surviving item through the now-gone restaurant
  confirms 404). This is the one folder where "delete the parent" is
  deliberately placed at the end rather than mid-folder, since MenuItems
  needs the restaurant alive for its own CRUD first.

## Idempotency: `runSuffix`, not inline `{{$timestamp}}`

`{{$timestamp}}` is a Postman **dynamic** variable — re-evaluated on every
request, so a value captured at signup would differ from the value used
moments later at login, breaking the chain. Fixed with a **collection-level
pre-request script** that generates a suffix once per run and reuses it
everywhere:

```js
if (!pm.environment.get("runSuffix")) {
    pm.environment.set("runSuffix", Date.now().toString());
}
```

Every email/login/nome that must be unique interpolates `{{runSuffix}}`.
Verified this actually works: ran the full collection twice in a row
against the **same** database with **no reset in between** (via `npx
newman`, both runs against the containerized app) — both runs came back
48/48 requests, 69/69 assertions, 0 failures. A professor running the
collection a second time does not see a false "the API is broken" from a
409 on a duplicate email.

## The nested-route trap, demonstrated end-to-end

The `Regras de negócio e erros` folder creates two real restaurants owned
by two different Donos (`Dono C`/`Dono D`), puts an item under Restaurant
C, then reaches it via Restaurant D's path on `GET`, `PUT`, **and**
`DELETE` — asserting **404, never 403**, on all three. This is the same
invariant `MenuItemControllerTest`/`MenuItemIntegrationTest` assert at the
Java level, now proven again at the HTTP black-box level through the
actual running (and, in the Docker validation below, actually
containerized) app.

## `scripts/audit.sh` section 10 — the drift guard (P0 of this module)

The collection is a JSON artifact `./mvnw verify` never looks at. If a
controller route changes, the collection silently rots until someone runs
it by hand. Pure bash/grep/sed — no `jq`, no Python — consistent with
every other section, and neither tool is guaranteed to be on a grader's
machine (verified: no `jq` and no working `python3` alias on this
development machine either).

- **Controller side**: for each of the 5 controllers, the first
  `@RequestMapping("...")` match is the class prefix (verified none of them
  use `@RequestMapping` at the method level, only the `@GetMapping`/etc.
  shortcuts), then every `@(Get|Post|Put|Delete)Mapping` supplies the verb
  and optional sub-path. `{anything}` path variables normalize to `{}`.
- **Collection side**: every request URL is authored as
  `"{{baseUrl}}/api/v1/..."`, which disambiguates it from body-JSON `"raw"`
  fields (payloads never start with `{{baseUrl}}`); `"method"` is paired
  with the following URL in the same order they're authored. `{{anything}}`
  path variables normalize to the same `{}` token.
- **FAILs** if either side has an entry the other doesn't.

Verified the guard actually catches drift, not just its own absence:
temporarily renamed `RestaurantController`'s `GET /{id}` route to
`GET /{id}/DRIFTED`, confirmed section 10 went red (both directions: the
real route missing from the collection, and the collection's now-stale
`GET /{id}` request pointing at a route no controller serves), then
reverted and confirmed `bash scripts/audit.sh` exits 0 again.

## `scripts/audit.sh` section 11 — the docker-compose default guard

Added specifically because section 10 would never have caught the P0
below — a route drift guard doesn't see environment variable defaults.
FAILs if any `${VAR}` in `docker-compose.yml` has no `:-default`. Cheap,
and would have caught the P0 at M00 if it had existed then.

## P0: the JWT_SECRET Docker Compose bug

`docker-compose.yml` had `JWT_SECRET: ${JWT_SECRET}` — the **only**
variable in the file with no `:-default` (every other one, e.g.
`DB_NAME: ${DB_NAME:-restaurant_db}`, had one). Validated by actually
running `docker compose down -v && docker compose up --build` with no
`.env` file present:

- Compose itself warns: *"The JWT_SECRET variable is not set. Defaulting
  to a blank string."*
- The container receives `JWT_SECRET=` (present, but empty) as an
  environment variable.
- Spring's own fallback in `application-dev.yml`
  (`${JWT_SECRET:change-me-dev-only-not-for-prod-32c}`) only activates
  when the property is **absent** — an empty-but-present env var does not
  trigger it.
- `Keys.hmacShaKeyFor("")` throws `WeakKeyException: ... 0 bits ...` and
  the app crashes at boot.

This is distinct from the M02 finding (that one was about the fallback
being too *short*; this one is about the fallback never being *reached* at
all, specifically in the Compose path — the documented `cp .env.example
.env` step happens to route around it, since that file's secret is 33
characters, which is why this went unnoticed through M00–M05).

**Fix**: `JWT_SECRET: ${JWT_SECRET:-change-me-dev-only-not-for-prod-32c}` —
same literal default already used in `application-dev.yml`, so there is one
canonical "dev fallback" string, not two. Re-validated after the fix: clean
`down -v` → `up --build` with no `.env`, app boots (`Started
RestaurantManagementApplication`), all 6 migrations apply from an empty
schema, and the full Postman collection runs green against the
containerized app twice in a row.

## Newman: used transiently, not adopted as a dependency

`npx newman` was used to self-verify the collection (both Docker-validation
runs above) without asking for a manual Collection Runner click-through.
**Not** added as a project/CI dependency — no `package.json`, no CI step.
This is a pure-Maven codebase; the deliverable's actual audience (a
professor importing the collection into the Postman GUI) doesn't need a
Node toolchain, and adding one for a one-off validation tool would be
exactly the premature tooling this project's conventions caution against.

## Out of scope

- Newman/Postman as a CI-enforced gate (see above).
- Any new business logic or endpoint — this module changes zero production
  behavior other than the Docker Compose default fix.

## Definition of Done

- `./mvnw verify` green (unchanged from M05 — no production/test Java
  changed except the temporary, reverted `RestaurantController` drift
  check).
- `bash scripts/audit.sh` exits 0, including the new sections 10 and 11.
- Section 10's drift-catching behavior verified via a deliberate
  break-then-revert (documented above).
- `docker compose down -v && docker compose up --build` boots clean with
  no `.env` file present; all 6 migrations apply in order.
- The full Postman collection runs green (via `npx newman`, transient)
  against the **containerized** app, twice in a row, without resetting the
  database between runs.
- README documents collection import/run and the corrected (no-`.env`-
  required) Docker Compose flow.
