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
printf '\n============================================\n'
if [ "$FAILURES" -eq 0 ]; then
  printf '\033[32mAUDIT PASSED\033[0m - 0 failures\n'
else
  printf '\033[31mAUDIT FAILED\033[0m - %s failure(s)\n' "$FAILURES"
fi
printf '============================================\n'
exit "$FAILURES"