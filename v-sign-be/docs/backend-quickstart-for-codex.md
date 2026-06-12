# Backend Quickstart For Codex

Purpose: fast, consistent onboarding for any new Codex session in this backend.

## 1) Read Order (Mandatory)

1. `BE-init.md`
2. `docs/backend-implementation-checkpoints/00-implementation-summary.md`
3. `docs/backend-implementation-checkpoints/PLAN-backend-epic-implementation.md`
4. Epic checkpoints in order:
   - `01-foundation-api-contract.md`
   - `02-authentication-profile.md`
   - `03-dictionary.md`
   - `04-learning-signature-workflow.md`
   - `05-assessment.md`
   - `06-gamification.md`
   - `07-monetization.md`
   - `08-admin-panel.md`
5. Traceability/audit docs:
   - `docs/backend-implementation-checkpoints/01-08-implementation-audit.md`
   - `docs/backend-implementation-checkpoints/technical-decision-record.md`
   - `docs/backend-implementation-checkpoints/implementation-log.md`
6. Planning source of truth:
   - `docs/superpowers/runs/2026-05-21-backend-epic-plan/final-report.md`
   - `docs/superpowers/runs/2026-05-21-backend-epic-plan/traceability-report.md`

## 2) Current Delivery Model

- Checkpoint-first control:
  - No code edits before checkpoint/status review.
- Session split:
  - Session 1: Epics 01-03
  - Session 2: Epics 04-05
  - Session 3: Epics 06-07
  - Session 4 (optional): Epic 08
- Current implementation note:
  - Epic 6 and Epic 8 are implemented in in-memory scope.
  - DB persistence/migrations for those flows are intentionally deferred.

## 3) Backend Code Structure (Quick Map)

- App entry:
  - `src/main/java/com/vsign/backend/VSignBackendApplication.java`
- Cross-cutting:
  - `common/exception/*`
  - `common/response/*`
  - `common/security/*`
- Domain modules:
  - `auth/*`
  - `learning/*`
  - `assessment/*`
  - `dictionary/*`
  - `gamification/*`
  - `monetization/*`
  - `admin/*`
- Migrations:
  - `src/main/resources/db/migration/*`

## 4) Test Structure

- Integration tests:
  - `src/test/java/com/vsign/backend/**/**/*IT.java`
- Migration smoke:
  - `src/test/java/com/vsign/backend/migration/FlywayMigrationTest.java`

## 5) Minimum Verification Commands

- Session 1 baseline:
  - `mvn -q "-Dtest=AuthControllerIT,ProfileControllerIT,DictionaryIT" test`
- Session 2 baseline:
  - `mvn -q "-Dtest=com.vsign.backend.learning.LearningWorkflowIT,com.vsign.backend.assessment.AssessmentControllerIT" test`
- Epic 6+8 focused:
  - `mvn -q "-Dtest=com.vsign.backend.gamification.GamificationControllerIT,com.vsign.backend.admin.AdminControllerIT" test`

## 6) Required Reporting Format For Any New Work

- Before coding:
  - scope
  - API rows
  - files to change
  - tests to run
  - risks/deferred items
- After coding:
  - changed files
  - endpoint status
  - test evidence
  - checkpoint updates

## 7) Non-Negotiable Guardrails

- Do not edit outside approved epic/session scope.
- Do not claim completion without test evidence.
- Keep `implementation-log.md` and checkpoint files updated with real changes.
