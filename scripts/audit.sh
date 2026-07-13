#!/usr/bin/env bash
# =============================================================================
# audit.sh - Structural audit for restaurant-management-fase2
#
# These are the invariants a GREEN BUILD DOES NOT CATCH. `mvnw verify` proves
# the tests pass; this proves the tests are testing the right things and that
# the architecture rules are real rather than decorative.
#
# Run from the repo root:  bash scripts/audit.sh
# Exit code 0 = all pass. Non-zero = at least one FAIL.
#
# This is part of the Definition of Done for every module.
# =============================================================================
set -uo pipefail

PKG="src/main/java/br/com/fiap/restaurant"
FAILURES=0

pass() { printf '  \033[32mPASS\033[0m  %s\n' "$1"; }
fail() { printf '  \033[31mFAIL\033[0m  %s\n' "$1"; FAILURES=$((FAILURES + 1)); }
warn() { printf '  \033[33mWARN\033[0m  %s\n' "$1"; }
section() { printf '\n\033[1m%s\033[0m\n' "$1"; }

# -----------------------------------------------------------------------------
section "1. ARCHITECTURAL PURITY (independent of ArchUnit)"
# Re-checks the same rule ArchUnit claims to enforce. If an ArchUnit rule is
# ever mis-scoped and passes vacuously, this catches it anyway.
# -----------------------------------------------------------------------------
if grep -rhE "^import" "$PKG/domain/" | grep -qE "org\.springframework|jakarta\.persistence|jakarta\.validation|io\.jsonwebtoken"; then
  fail "domain/ imports a framework (Spring/JPA/Validation/JJWT)"
  grep -rnE "^import" "$PKG/domain/" | grep -E "org\.springframework|jakarta\.|io\.jsonwebtoken"
else
  pass "domain/ is framework-free"
fi

if grep -rhE "^import" "$PKG/application/" | grep -qE "org\.springframework|jakarta\.persistence|io\.jsonwebtoken"; then
  fail "application/ imports Spring/JPA/JJWT"
  grep -rnE "^import" "$PKG/application/" | grep -E "org\.springframework|jakarta\.persistence|io\.jsonwebtoken"
else
  pass "application/ is framework-free"
fi

if grep -rqE "@(Service|Component|Repository|Configuration|Autowired|Bean)" "$PKG/application/"; then
  fail "application/ carries Spring stereotypes (must be wired in BeanConfiguration)"
else
  pass "application/ has no Spring stereotypes"
fi

# -----------------------------------------------------------------------------
section "2. WIRING COMPLETENESS"
# Every use case must be declared as a @Bean. A missing one only explodes at
# runtime when that endpoint is first hit - not at build time.
# -----------------------------------------------------------------------------
MISSING_BEANS=""
for uc in $(ls "$PKG/application/usecase/" 2>/dev/null | sed 's/\.java$//'); do
  grep -q "$uc" "$PKG/infrastructure/config/BeanConfiguration.java" || MISSING_BEANS="$MISSING_BEANS $uc"
done
if [ -n "$MISSING_BEANS" ]; then
  fail "use cases not wired in BeanConfiguration:$MISSING_BEANS"
else
  pass "all use cases wired in BeanConfiguration"
fi

# -----------------------------------------------------------------------------
section "3. ARCHUNIT RULES ARE REAL, NOT VACUOUS"
# -----------------------------------------------------------------------------
ARCH_DIR="src/test/java/br/com/fiap/restaurant/architecture"
if grep -rq "DO_NOT_INCLUDE_TESTS" "$ARCH_DIR"; then
  pass "ArchUnit scoped to production code (DO_NOT_INCLUDE_TESTS)"
else
  fail "ArchUnit is also importing test classes - rules can fail spuriously"
fi
if grep -rq "mayNotAccessAnyLayer\|mayOnlyBeAccessedByLayers" "$ARCH_DIR"; then
  pass "ArchUnit layered rules present and assertive"
else
  fail "ArchUnit layered rules missing or non-assertive"
fi

# -----------------------------------------------------------------------------
section "4. SCHEMA <-> ENTITY DRIFT"
# ddl-auto=validate means any drift is a startup crash in the reviewer's face,
# and no unit test catches it.
# -----------------------------------------------------------------------------
for sql in src/main/resources/db/migration/V*.sql; do
  tbl=$(grep -oiE "CREATE TABLE [a-z_]+" "$sql" | head -1 | awk '{print $3}')
  [ -z "$tbl" ] && continue
  ecount=$(grep -rl "name = \"$tbl\"" "$PKG/infrastructure/persistence/entity/" 2>/dev/null | wc -l)
  if [ "$ecount" -eq 0 ]; then
    warn "table '$tbl' ($(basename "$sql")) has no matching @Table entity"
  else
    ent=$(grep -rl "name = \"$tbl\"" "$PKG/infrastructure/persistence/entity/")
    cols=$(sed -n '/CREATE TABLE/,/);/p' "$sql" | grep -oE "^\s{4}[a-z_]+" | tr -d ' ' | sort -u)
    miss=""
    for c in $cols; do
      grep -q "\"$c\"" "$ent" || miss="$miss $c"
    done
    if [ -n "$miss" ]; then
      fail "$tbl: columns in SQL but not mapped in $(basename "$ent"):$miss"
    else
      pass "$tbl: every column mapped in $(basename "$ent")"
    fi
  fi
done

# -----------------------------------------------------------------------------
section "5. SECRET / PASSWORD LEAKAGE"
# -----------------------------------------------------------------------------
LEAK=$(grep -rliE "senha|password|hash" "$PKG"/*/dto/ 2>/dev/null | grep -iE "Response|Result" || true)
if [ -n "$LEAK" ]; then
  fail "password-ish field in an OUTPUT DTO: $LEAK"
else
  pass "no password/hash fields in Response/Result DTOs"
fi

# -----------------------------------------------------------------------------
section "6. COLD-START REACHABILITY  <-- the check that would have caught the P0"
# Every non-public endpoint must be reachable by a brand-new deployment through
# HTTP ALONE. Tests that seed via a use case walk around the locked door and
# hide bootstrap deadlocks.
# -----------------------------------------------------------------------------
IT="src/test/java/br/com/fiap/restaurant"
if grep -rq "coldStart\|ColdStart" "$IT"; then
  CS_FILE=$(grep -rl "coldStart\|ColdStart" "$IT" | head -1)
  # the cold-start test itself must not seed via a use case
  if sed -n '/coldStart/,/^    }/p' "$CS_FILE" | grep -qE "UseCase\.execute|Repository\.save"; then
    fail "cold-start test seeds out-of-band - it bypasses the filter chain"
  else
    pass "cold-start test uses HTTP only (no out-of-band seeding)"
  fi
else
  fail "no cold-start test: nothing proves a fresh deployment is usable"
fi

# -----------------------------------------------------------------------------
section "7. STATUS-CODE CONTRACT COVERAGE"
# Every status the GlobalExceptionHandler can emit must be asserted somewhere.
# A deliberate status choice with no test is an unprotected decision.
# -----------------------------------------------------------------------------
declare -A M=( [BAD_REQUEST]=isBadRequest [UNAUTHORIZED]=isUnauthorized [FORBIDDEN]=isForbidden
               [NOT_FOUND]=isNotFound [CONFLICT]=isConflict [UNPROCESSABLE_ENTITY]=isUnprocessableEntity )
HANDLER="$PKG/infrastructure/web/advice/GlobalExceptionHandler.java"
for st in "${!M[@]}"; do
  if grep -q "HttpStatus.$st" "$HANDLER" 2>/dev/null; then
    if grep -rq "${M[$st]}()" "$IT"; then
      pass "$st is emitted AND asserted in tests"
    else
      fail "$st is emitted by GlobalExceptionHandler but NEVER asserted in any test"
    fi
  fi
done

# every domain exception must be reachable from a handler
for ex in $(ls "$PKG/domain/exception/" 2>/dev/null | sed 's/\.java$//' | grep -v "^DomainException$"); do
  grep -q "$ex" "$HANDLER" || warn "$ex has no explicit handler (relies on the DomainException fallback)"
done

# -----------------------------------------------------------------------------
section "8. N+1 SMELL IN LIST PATHS"
# -----------------------------------------------------------------------------
for f in "$PKG"/application/usecase/List*.java; do
  [ -e "$f" ] || continue
  if grep -qE "\.map\(.*findById|forEach.*findById|stream\(\).*findById" "$f"; then
    fail "$(basename "$f"): findById inside a stream/loop - N+1"
  else
    pass "$(basename "$f"): no per-item repository lookup"
  fi
done

# -----------------------------------------------------------------------------
section "9. EXCEPTIONS WITH NO HANDLER AT ALL (section 7's blind spot)"
# Section 7 only compares statuses the handler EMITS against statuses tests
# ASSERT - it can't see a status that SHOULD be emitted but was never wired up
# at all (this is exactly how a plain IllegalArgumentException with no
# handler shipped in M04: HorarioFuncionamento's invariant 500'd instead of
# 400ing, and nothing here or in section 7 would have caught it before this
# section existed). WARN, not FAIL: some exceptions are legitimately
# internal-only (e.g. IllegalStateException for a should-be-unreachable
# state) and are meant to fall through to the generic 500.
# -----------------------------------------------------------------------------
# Matches both `throw new X(...)` and the `.orElseThrow(() -> new X(...))`
# idiom used throughout this codebase - a literal "throw" keyword isn't
# required for an exception to actually be thrown by production code.
THROWN=$(grep -rhoE "new [A-Za-z]+Exception\(" "$PKG/domain/" "$PKG/application/" 2>/dev/null \
  | sed -E 's/^new (.*)\($/\1/' | sort -u)
for ex in $THROWN; do
  case "$ex" in
    DomainException|DomainValidationException) continue ;;
  esac
  if grep -q "$ex" "$HANDLER" 2>/dev/null; then
    pass "$ex is referenced by GlobalExceptionHandler"
  else
    warn "$ex is thrown in domain/application but GlobalExceptionHandler never mentions it - falls through to the generic fallback"
  fi
done

# -----------------------------------------------------------------------------
section "ROUTE EXTRACTION (shared by sections 10 and 12)"
# Every controller route, normalized, computed ONCE here and reused by both
# the Postman drift guard (10) and the README drift guard (12) - one
# extractor, not two copies that could drift from each other.
# -----------------------------------------------------------------------------
CONTROLLER_ROUTES=$(
  for f in "$PKG"/infrastructure/web/controller/*.java; do
    [ -e "$f" ] || continue
    prefix=$(grep -oE '@RequestMapping\("[^"]*"\)' "$f" | head -1 | sed -E 's/^@RequestMapping\("(.*)"\)$/\1/')
    grep -oE '@(Get|Post|Put|Delete)Mapping(\("[^"]*"\))?' "$f" | while read -r ann; do
      verb=$(echo "$ann" | sed -E 's/@(Get|Post|Put|Delete)Mapping.*/\1/' | tr '[:lower:]' '[:upper:]')
      subpath=$(echo "$ann" | grep -oE '"[^"]*"' | tr -d '"')
      norm=$(echo "${prefix}${subpath}" | sed -E 's/\{[a-zA-Z0-9_]+\}/{}/g')
      echo "$verb $norm"
    done
  done
)
pass "extracted $(echo "$CONTROLLER_ROUTES" | sort -u | wc -l) distinct controller routes"

# -----------------------------------------------------------------------------
section "10. POSTMAN COLLECTION <-> CONTROLLER ROUTE DRIFT"
# The collection is a JSON artifact `mvnw verify` never looks at. If an
# endpoint path changes, the collection silently rots until someone runs it
# by hand. Pure bash/grep/sed (no jq/python - neither is guaranteed to be on
# a grader's machine, and this stays consistent with every other section).
# -----------------------------------------------------------------------------
COLLECTION="postman/RestaurantManagement.postman_collection.json"
if [ ! -f "$COLLECTION" ]; then
  fail "postman collection not found at $COLLECTION"
else
  # Every request's URL is authored as "{{baseUrl}}/api/v1/..." (raw), which
  # disambiguates it from body-JSON "raw" fields (payloads never start with
  # {{baseUrl}}). "method" is assumed to precede "url" for each request, in
  # the same order - true for how this file is authored/generated.
  COLLECTION_ROUTES=$(
    METHODS=$(grep -oE '"method": "[A-Z]+"' "$COLLECTION" | sed -E 's/"method": "([A-Z]+)"/\1/')
    URLS=$(grep -oE '"raw": "\{\{baseUrl\}\}[^"]*"' "$COLLECTION" | sed -E 's/"raw": "\{\{baseUrl\}\}([^"]*)"/\1/')
    paste -d' ' <(echo "$METHODS") <(echo "$URLS") | while read -r method url; do
      path=$(echo "$url" | sed -E 's/\?.*$//')
      norm=$(echo "$path" | sed -E 's/\{\{[a-zA-Z0-9_]+\}\}/{}/g')
      echo "$method $norm"
    done
  )

  MISSING_IN_COLLECTION=$(comm -23 <(echo "$CONTROLLER_ROUTES" | sort -u) <(echo "$COLLECTION_ROUTES" | sort -u))
  EXTRA_IN_COLLECTION=$(comm -13 <(echo "$CONTROLLER_ROUTES" | sort -u) <(echo "$COLLECTION_ROUTES" | sort -u))

  if [ -n "$MISSING_IN_COLLECTION" ]; then
    fail "controller route(s) with no matching Postman request:"
    echo "$MISSING_IN_COLLECTION" | sed 's/^/    /'
  fi
  if [ -n "$EXTRA_IN_COLLECTION" ]; then
    fail "Postman request(s) referencing a path no controller serves:"
    echo "$EXTRA_IN_COLLECTION" | sed 's/^/    /'
  fi
  if [ -z "$MISSING_IN_COLLECTION" ] && [ -z "$EXTRA_IN_COLLECTION" ]; then
    pass "every controller route has a matching Postman request, and vice versa"
  fi
fi

# -----------------------------------------------------------------------------
section "11. DOCKER COMPOSE - EVERY \${VAR} MUST HAVE A DEFAULT"
# A ${VAR} with no :-default silently becomes an empty string when unset -
# and an empty-but-present env var is NOT the same as an absent one to
# Spring's own ${VAR:default} placeholder resolution, which only falls back
# on absence. This exact gap let JWT_SECRET reach the container as an empty
# string with no .env file present, crashing boot with WeakKeyException
# (see NOTES.md / specs/modules/06-postman-docker.md). Cheap to catch here.
# -----------------------------------------------------------------------------
COMPOSE_FILE="docker-compose.yml"
if [ ! -f "$COMPOSE_FILE" ]; then
  warn "no docker-compose.yml found - skipping section 11"
else
  NO_DEFAULT=$(grep -oE '\$\{[A-Za-z_][A-Za-z0-9_]*\}' "$COMPOSE_FILE" | sort -u)
  if [ -n "$NO_DEFAULT" ]; then
    fail "docker-compose.yml has \${VAR} with no :- default:"
    echo "$NO_DEFAULT" | sed 's/^/    /'
  else
    pass "every \${VAR} in docker-compose.yml has a :- default"
  fi
fi

# -----------------------------------------------------------------------------
section "12. README ENDPOINT TABLE <-> CONTROLLER ROUTE DRIFT"
# Same class of rot as section 10, different artifact: the README's endpoint
# catalog is Markdown `mvnw verify` never reads. Reuses $CONTROLLER_ROUTES
# computed once above. README rows already use the same {id}/{restaurantId}
# bracket notation the controllers do, so no {{}}-style translation is
# needed here (unlike the Postman JSON, which needed its own convention).
# -----------------------------------------------------------------------------
README_FILE="README.md"
if [ ! -f "$README_FILE" ]; then
  fail "README.md not found"
else
  README_ROUTES=$(
    grep -E '^\| *(GET|POST|PUT|DELETE) *\|' "$README_FILE" | while IFS='|' read -r _ method route _rest; do
      method=$(echo "$method" | tr -d ' ')
      route=$(echo "$route" | tr -d ' ' | sed -E 's/\?.*$//')
      norm=$(echo "$route" | sed -E 's/\{[a-zA-Z0-9_]+\}/{}/g')
      echo "$method $norm"
    done
  )

  MISSING_IN_README=$(comm -23 <(echo "$CONTROLLER_ROUTES" | sort -u) <(echo "$README_ROUTES" | sort -u))
  EXTRA_IN_README=$(comm -13 <(echo "$CONTROLLER_ROUTES" | sort -u) <(echo "$README_ROUTES" | sort -u))

  if [ -n "$MISSING_IN_README" ]; then
    fail "controller route(s) missing from the README endpoint table:"
    echo "$MISSING_IN_README" | sed 's/^/    /'
  fi
  if [ -n "$EXTRA_IN_README" ]; then
    fail "README endpoint table row(s) referencing a path no controller serves:"
    echo "$EXTRA_IN_README" | sed 's/^/    /'
  fi
  if [ -z "$MISSING_IN_README" ] && [ -z "$EXTRA_IN_README" ]; then
    pass "every controller route is in the README table, and vice versa"
  fi
fi

# -----------------------------------------------------------------------------
printf '\n============================================\n'
if [ "$FAILURES" -eq 0 ]; then
  printf '\033[32mAUDIT PASSED\033[0m - 0 failures\n'
else
  printf '\033[31mAUDIT FAILED\033[0m - %s failure(s)\n' "$FAILURES"
fi
printf '============================================\n'
exit "$FAILURES"