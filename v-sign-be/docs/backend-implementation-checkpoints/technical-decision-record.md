# Technical Decision Record (TDR)

Purpose: track key technical decisions with rationale and tradeoffs.

---

## TDR-001 - Checkpoint-First Delivery Governance

- Date: 2026-05-22
- Context:
  - Need strict reviewer-controlled progression across epics.
- Decision:
  - No backend code edits before checkpoint approval.
  - Acceptance requires targeted tests + checkpoint update.
- Alternatives considered:
  - Free-flow implementation then retrospective review.
- Why rejected:
  - Lower control, harder rollback, weaker auditability.
- Consequences:
  - Slower start per epic, higher traceability and lower scope drift.

---

## TDR-002 - Session-Based Epic Batching

- Date: 2026-05-22
- Context:
  - Need predictable implementation slices.
- Decision:
  - Session 1: Epics 01-03
  - Session 2: Epics 04-05
  - Session 3: Epics 06-07
  - Session 4 (optional): Epic 08
- Alternatives considered:
  - Implement all epics in one stream.
- Why rejected:
  - Higher integration risk and less reviewer visibility.
- Consequences:
  - Better milestone control; cross-epic dependencies must be explicitly documented.

---

## TDR-003 - Epic 06/07 Partial Acceptance Strategy

- Date: 2026-05-22
- Context:
  - Epic 06 and 07 include both contract-read APIs and heavier persistence/operational features.
- Decision:
  - Accept contract-critical rows first (summary/leaderboards; plans/orders/status).
  - Defer write-side domain logic and persistence hardening.
- Alternatives considered:
  - Block acceptance until full domain completion.
- Why rejected:
  - Would delay frontend/backend contract alignment and session throughput.
- Consequences:
  - Clear accepted scope with known technical debt that must be closed in follow-up checkpoints.

---

## TDR-004 - Verification Baseline

- Date: 2026-05-22
- Context:
  - Need objective evidence for each accepted batch.
- Decision:
  - Use targeted integration-test commands per batch as acceptance evidence.
- Alternatives considered:
  - Unit-test-only validation.
- Why rejected:
  - Lower confidence for API contract correctness.
- Consequences:
  - Better end-to-end confidence, with higher run time.

---

## Next TDR Candidates

- Epic 06 full completion design:
  - XP idempotency model, streak day-boundary logic, badge unlock strategy, persistence schema.
- Epic 08 implementation design:
  - ADMIN RBAC boundaries, audit-log requirements, transaction override governance.
