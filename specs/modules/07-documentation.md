# M07 — Project Documentation

## Goal

A delivery module, not a feature module: no new business logic, no new
endpoints. Produces the FIAP rubric deliverable "Documentação do projeto:
descrição detalhada do projeto, incluindo a arquitetura, os endpoints da
API e as instruções de configuração e execução" via a restructured
`README.md` — the first thing a reader sees, and the only artifact this
rubric line requires (see the reading recorded in `NOTES.md`: the brief
names Postman and Docker Compose as separate deliverables explicitly
elsewhere; "Documentação do projeto" names content, not a distinct
document format, so no separate DOCX/PDF report was produced).

## Deliverables

- `README.md` rewritten in Brazilian Portuguese (the project's single
  entry point; code/`specs/`/`CLAUDE.md` stay in English per the existing
  convention): problem statement, stack/requirements, how to run (Docker
  one-command + local/Maven alternative), how to test (`mvnw verify` +
  coverage + the Postman collection), architecture (three Mermaid diagrams
  + how the dependency rule is *enforced*, not just asserted), a 21-row
  endpoint catalog, a "Decisões de Arquitetura" section (7 decisions + 3
  accepted limitations, each linked to a spec file and a currently-passing
  test), and a folder-structure overview.
- Three Mermaid diagrams (layers/dependency rule, ER schema, the
  stale-JWT-claim sequence) — validated with a transient `npx
  @mermaid-js/mermaid-cli` render (not added as a project dependency)
  before being committed, since neither `mvnw verify` nor `scripts/audit.sh`
  can catch a Mermaid syntax error.
- `scripts/audit.sh` section 12 — README endpoint table drift guard,
  reusing the same route extractor section 10 already built for the
  Postman collection (hoisted out of section 10 so both share it).
- Two one-sentence factual corrections in `specs/product/overview.md` and
  `specs/technical/architecture.md` (both had gone stale — see `NOTES.md`).

## Out of scope

- Any production code change (this module is documentation-only).
- A separate technical report document — see the reading above.
- Newman/mermaid-cli as project dependencies — both used transiently only,
  matching M06's precedent with Newman.

## Definition of Done

- `README.md` covers all required sections; no invented numbers (the
  222-tests/96%-coverage figures are a snapshot from an actual `./mvnw
  verify` run this session, presented with the regeneration command).
- All three Mermaid diagrams render without error (verified via
  `mermaid-cli`, transiently).
- `bash scripts/audit.sh` exits 0, including section 12; its
  non-vacuousness verified by a deliberate break-then-revert, same
  pattern as section 10's verification in M06.
- Every decision/limitation in the README's "Decisões de Arquitetura"
  section links to a real `specs/modules/NN-*.md` file and a real,
  currently-passing test — no unlinked claims.
- `./mvnw verify` unchanged (no production/test Java touched).
