## Objective

Sequence backend epic/API delivery for "Delivery roadmap and sequencing".

## Phase 1

- Scope: foundation, centralized exceptions, JWT security, CORS, Flyway baseline, authentication/profile, public dictionary.
- Deliverables: V1__create_users_and_roles.sql, V2__create_password_reset_tokens.sql, V10__create_dictionary.sql, AuthController, ProfileController, DictionaryPublicController.
- APIs: POST /api/v1/auth/register, POST /api/v1/auth/login, GET /api/v1/me, PATCH /api/v1/me/profile, GET /api/v1/dictionary.
- Gate: AuthControllerIT, ProfileControllerIT, DictionaryPublicControllerIT, global error envelope tests.

## Phase 2

- Scope: learning catalog, premium gating, lesson details, progress checkpoints, assessment submission/review.
- Deliverables: V3__create_learning_catalog.sql, V4__create_user_progress.sql, V5__create_quizzes.sql, V6__create_quiz_attempts.sql.
- APIs: GET /api/v1/units, GET /api/v1/units/{unitId}/chapters, GET /api/v1/lessons/{lessonId}, PUT /api/v1/lessons/{lessonId}/progress, POST /api/v1/quiz-attempts/{attemptId}/submit.
- Gate: progress idempotency tests, PREMIUM_REQUIRED contract tests, quiz scoring and timed submit tests.

## Phase 3

- Scope: gamification, monetization, subscription status, payment history, admin phase-2 APIs.
- Deliverables: V8__create_gamification.sql, V12__create_subscriptions.sql, V13__create_payment_transactions.sql, V14__create_audit_logs.sql.
- APIs: GET /api/v1/gamification/summary, GET /api/v1/leaderboards, POST /api/v1/payments/orders, GET /api/v1/me/subscription, GET /api/v1/admin/kpis.
- Gate: XP idempotency tests, payment webhook idempotency tests, subscription refresh contract tests, admin RBAC tests.

## Verification Gates

- Contract gate: every endpoint row in the API contract table has request DTO, response DTO, validation rules, and error codes.
- Migration gate: each epic has named Flyway migrations and repository tests for constraints.
- Frontend gate: every Must v1.1 frontend mismatch has a backend contract test.
- Release gate: no audit `needs_revision` issues and final report contains endpoint table, epic mapping, frontend mapping, roadmap phases, and audit report.
