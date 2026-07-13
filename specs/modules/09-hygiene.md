# M09 — Higienização (hygiene pass)

## Goal

A closing delivery module, not a feature module: no business logic, no
new endpoints, no schema change. Closes four remaining edges an
independent audit of v1.0.0 found: near-zero class-level Javadoc (rubric
item "Código devidamente organizado e documentado"), an unguarded
README coverage/test-count claim, a permanently-on `scripts/audit.sh`
WARN that trains the reader to ignore output, and an undocumented (but
correct) Portuguese/English naming split.

## Deliverables

- Class-level Javadoc added to all 38 previously-undocumented classes
  across `domain/model`, `domain/repository`, `application/port`,
  `application/usecase`, `infrastructure/security`, and
  `infrastructure/config` — each explaining the invariant, contract, rule,
  or trap the class exists to enforce, not restating its name. 8 classes
  already had genuine WHY-quality Javadoc and were left untouched.
- `scripts/audit.sh` section 9: `IllegalStateException` explicitly
  whitelisted (it is the deliberate M04 corrupt-data fallback in
  `CreateRestaurantUseCase`, not an oversight) — the WARN that had fired
  on every run since M05 is now silent for this specific, intentional
  case.
- `scripts/audit.sh` section 13: verifies the README's stated test count
  and coverage percentage/line counts against reality (real `@Test`
  method count, and `target/site/jacoco/jacoco.xml`'s `LINE` counter) on
  every run — FAILs on any mismatch, WARNs (never silently passes) if
  `jacoco.xml` doesn't exist yet. The README's numbers were found to
  already be accurate (222 tests, 96% line coverage); this guards that
  fact going forward instead of merely restating it.
- README's "Como testar" section reframed: the 80% JaCoCo gate is stated
  as the enforced, permanent guarantee; the specific test count/coverage
  snapshot is kept as concrete evidence, explicitly cross-referenced to
  section 13 as the mechanism keeping it honest.
- `## Naming conventions` added to `CLAUDE.md` (after "Architecture",
  before "Error contract") and a matching short subsection added to
  README's "Arquitetura" section: English for technical/structural
  naming, Portuguese for domain vocabulary — a documented DDD
  ubiquitous-language decision, not an inconsistency. Zero identifiers
  renamed.

## Out of scope

- Any production code behaviour change.
- Renaming any Portuguese domain field or method.
- Refactoring the mappers into a shared abstraction.
- New dependencies.

## Definition of Done

- `./mvnw verify` unchanged (222 tests, 0 failures, coverage gate green;
  no Javadoc/doclint plugin exists in this build, so doc comments carry
  zero build risk).
- `bash scripts/audit.sh` exits 0, 13 sections.
- Section 13 proven non-vacuous three ways: a wrong test count, a wrong
  coverage percentage, and a missing `jacoco.xml` each produce a
  red/WARN result; all three reverted, `audit.sh` exits 0 again.
- `git diff --stat` shows zero renamed Portuguese identifiers anywhere.
