# Backend Epic Implementation Execution Plan

## Tracking Model

This file is the source of truth for implementation progress.

- Contract source: `docs/superpowers/runs/2026-05-21-backend-epic-plan/final-report.md`
- Execution checkpoints: `docs/backend-implementation-checkpoints/`
- Verification rule: no epic is accepted without both
  - targeted test pass
  - review gate pass (spec + quality)
- Process rule: no backend code edits until the epic checkpoint is explicitly approved by reviewer.

## Status Legend

- `pending`: not started
- `in_progress`: being implemented/reviewed
- `spec_pass`: contract implemented and validated
- `accepted`: spec + quality + tests pass
- `deferred`: intentionally postponed to later batch

## Session Split For Speed

### Session 1 (Epics 1-3)

- Epic 01 Foundation API Contract
- Epic 02 Authentication & Profile
- Epic 03 Dictionary
- Goal: stabilize API envelope/auth/profile/dictionary base and keep full suite green.

### Session 2 (Epics 4-5)

- Epic 04 Learning & Signature Workflow
- Epic 05 Assessment
- Goal: deliver learning progression and quiz lifecycle on top of Session 1 contracts.

### Session 3 (Epics 6-7)

- Epic 06 Gamification
- Epic 07 Monetization
- Goal: deliver profile XP/leaderboard and payment/subscription APIs aligned with frontend verification.

### Deferred/Optional Session 4

- Epic 08 Admin Panel
- Run only after Session 1-3 acceptance or as separate admin stream.

## Completed Batches

### Batch 1 (Accepted)

- Authentication & Profile
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `GET /api/v1/me`
  - `PATCH /api/v1/me/profile`
- Dictionary
  - `GET /api/v1/dictionary`
  - `GET /api/v1/dictionary/{entryId}/practice-target`

### Batch 2 (Accepted)

- Learning
  - `GET /api/v1/units`
  - `GET /api/v1/units/{unitId}/chapters`
  - `GET /api/v1/chapters/{chapterId}/lessons`
  - `GET /api/v1/lessons/{lessonId}`
  - `PUT /api/v1/lessons/{lessonId}/progress`
- Assessment
  - `GET /api/v1/lessons/{lessonId}/quiz`
  - `POST /api/v1/quiz-attempts/{attemptId}/submit`
  - `GET /api/v1/quiz-attempts/{attemptId}/review`

## Active Batch

### Batch 3: Gamification + Monetization

#### Epic 06: Gamification

- Planned rows
  - `GET /api/v1/gamification/summary`
  - `GET /api/v1/leaderboards`
- Current state
  - `spec_pass` (from review)
  - waiting `quality` gate and explicit checkpoint approval
- Deferred
  - token-derived identity
  - persistence/migrations
  - XP/streak write rules

#### Epic 07: Monetization

- Planned rows
  - `GET /api/v1/subscription/plans`
  - `POST /api/v1/payments/orders`
  - `GET /api/v1/payments/{transactionId}`
- Current state
  - `spec_pass` (worker implementation complete and targeted IT pass)
  - waiting `quality` gate and explicit checkpoint approval
- Deferred
  - `/api/v1/me/subscription`
  - `/api/v1/me/payments`
  - webhook handling
  - persistence/migrations
  - auth enforcement

## Next Batch

### Batch 4: Admin

- Planned rows
  - `GET /api/v1/admin/users`
  - `GET /api/v1/admin/content/review-queue`
  - then admin payment/kpi rows
- Entry condition
  - Batch 3 accepted

## Review and Acceptance Gates

For each epic:

1. Implementer subagent output
2. Spec review (read-only)
3. Fix loop until `SPEC PASS`
4. Quality review (read-only)
5. Fix loop until `QUALITY PASS`
6. Main-agent targeted tests
7. Update checkpoint markdown and summary
8. Include in full-suite verification

## Verification Commands

Targeted:

```powershell
# Batch 1
mvn -q "-Dtest=AuthControllerIT,ProfileControllerIT,DictionaryIT" test

# Batch 2
mvn -q "-Dtest=com.vsign.backend.learning.LearningWorkflowIT,com.vsign.backend.assessment.AssessmentControllerIT" test

# Batch 3
mvn -q "-Dtest=com.vsign.backend.gamification.GamificationControllerIT,com.vsign.backend.monetization.SubscriptionControllerIT" test
```

Full suite:

```powershell
mvn -q test
```

## Acceptance Checklist Per Epic

- Endpoint path/method match contract row
- DTO fields match frontend-required fields
- Validation and domain error codes present
- At least one failure-path integration test per endpoint group
- No unrelated package changes

## Immediate Work Queue

1. Review and approve markdown checkpoints only (no code edits).
2. Complete Batch 3 quality reviews for Gamification and Monetization.
3. Mark `06-gamification.md` and `07-monetization.md` as accepted/revise.
4. Start Epic 08 only if reviewer opens Session 4.
