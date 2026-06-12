# Superpower Orchestrator Plan

- Generated At: 2026-05-21T09:56:08.814Z
- Task Count: 5

## Backend API contract plan

## Objective

Define implementation boundaries and API contracts for "Backend API contract plan".

## Constraints

- Use Spring Boot 3.x, Java 17+, Spring Security 6, PostgreSQL, Flyway, and centralized validation/error handling.
- Preserve API contract traceability to instruction, user stories, and frontend verification gaps.
- Do not proxy uploaded document/media bytes through planning assumptions unless the instruction requires it; prefer presigned upload/download contracts for large files.
- All protected workflows must state authentication and authorization requirements.

## Proposed Architecture

- Epic/user story alignment: group backend modules by authentication/profile, signature workflow, document upload, API contract support, and operational reporting when present in the source docs.
- API contract: define method, path, role, request DTO, response DTO, validation errors, and frontend state fields for each workflow.
- Persistence: model PostgreSQL tables with Flyway migrations, foreign keys, uniqueness constraints, and indexes before service/controller work.
- Validation: centralize request validation and business errors through reusable error codes and the global exception handler.
- Testing: pair every epic with controller integration tests, service tests, repository/Flyway tests, and frontend contract smoke cases.

### Endpoint-Level API Contract Table

| Epic | User Story IDs | Endpoint | Method | Auth Role | Request DTO | Response DTO | Validation Rules | Error Codes | FE Dependency |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Authentication & Profile | US-01, US-02 | /api/v1/auth/register | POST | Public | RegisterRequest | AuthResponse | email valid+unique; password >= 8 with uppercase and number; fullName not blank | VALIDATION_FAILED, EMAIL_ALREADY_EXISTS | LoginModal signup needs dedicated register API |
| Authentication & Profile | US-02 | /api/v1/auth/login | POST | Public | LoginRequest | AuthResponse | email valid; password not blank; active account required | VALIDATION_FAILED, INVALID_CREDENTIALS, ACCOUNT_DISABLED | LoginModal submit must verify entered credentials |
| Authentication & Profile | US-04, US-05 | /api/v1/me | GET | USER | None | ProfileResponse | valid Bearer token | UNAUTHORIZED, USER_NOT_FOUND | Dashboard/Profile needs fullName, avatarUrl, accountType, totalXp, currentStreak, longestStreak, badges, subscription |
| Authentication & Profile | US-05 | /api/v1/me/profile | PATCH | USER | UpdateProfileRequest | ProfileResponse | fullName not blank; avatarUrl valid URL when present | VALIDATION_FAILED, UNAUTHORIZED | Profile save needs persisted name/avatar response |
| Authentication & Profile | US-06 | /api/v1/me/change-password | POST | USER | ChangePasswordRequest | SuccessResponse<Void> | currentPassword required; newPassword policy; newPassword != currentPassword | VALIDATION_FAILED, INVALID_CURRENT_PASSWORD, PASSWORD_REUSED | Change password screen requires field-level errors |
| Authentication & Profile | US-08 | /api/v1/auth/password-reset/request | POST | Public | PasswordResetRequest | SuccessResponse<Void> | email valid; always return generic success | VALIDATION_FAILED | Forgot-password flow requires non-enumerating request API |
| Content Management (Learning) | US-09 | /api/v1/units | GET | USER | UnitSearchRequest | UnitListResponse | publishedOnly for non-admin; page/size bounds | VALIDATION_FAILED | VocabularyPack unit cards need title, thumbnail, chapterCount, orderIndex |
| Content Management (Learning) | US-10, US-11, US-17 | /api/v1/units/{unitId}/chapters | GET | USER | None | ChapterListResponse | unitId exists; premium lock evaluated by accountType | UNIT_NOT_FOUND, UNAUTHORIZED | Chapter list needs isPremium, locked, progressPct, lessonCount |
| Content Management (Learning) | US-12, US-16 | /api/v1/chapters/{chapterId}/lessons | GET | USER | None | LessonListResponse | chapterId exists; sequential unlock computed from progress | CHAPTER_NOT_FOUND, UNAUTHORIZED | Lesson timeline needs status badges and locked state |
| Content Management (Learning) | US-13, US-17, US-18 | /api/v1/lessons/{lessonId} | GET | USER | None | LessonDetailResponse | lesson exists; premium access required; presigned URL expiration configured | LESSON_NOT_FOUND, PREMIUM_REQUIRED, UNAUTHORIZED | Video screen needs presigned video URL and resume checkpoint |
| Content Management (Learning) | US-14, US-15, US-18 | /api/v1/lessons/{lessonId}/progress | PUT | USER | UpdateProgressRequest | ProgressResponse | completionPct 0..100; status enum; lastPositionSeconds >= 0; idempotent completion | VALIDATION_FAILED, LESSON_NOT_FOUND | Autosave and chapter progress need updated completion state |
| Assessment | US-19, US-23 | /api/v1/lessons/{lessonId}/quiz | GET | USER | None | QuizResponse | lesson exists; premium quiz blocked for Basic user | LESSON_NOT_FOUND, PREMIUM_REQUIRED | MockExam/lesson quiz needs questions without correct answers |
| Assessment | US-20, US-22, US-24, US-26 | /api/v1/quiz-attempts/{attemptId}/submit | POST | USER | SubmitAttemptRequest | QuizResultResponse | attempt owned by user; unanswered allowed but scored incorrect; server duration validation | VALIDATION_FAILED, ATTEMPT_NOT_FOUND, ATTEMPT_ALREADY_SUBMITTED | Result screen needs score, passed, xpAwarded, reviewAvailable, timedOut |
| Assessment | US-21 | /api/v1/quiz-attempts/{attemptId}/review | GET | USER | None | QuizReviewResponse | attempt owned by user; submitted attempt required | ATTEMPT_NOT_FOUND, FORBIDDEN, BAD_REQUEST | Review UI needs selected answer, correct answer, explanation |
| Assessment | US-27, US-28, US-32 | /api/v1/ai-quiz/attempts | POST | USER/PREMIUM | AiAttemptRequest | AiAttemptResultResponse | metadata only; confidence 0..1; reject media payloads; preview quota for Basic | VALIDATION_FAILED, PREMIUM_REQUIRED, PREVIEW_LIMIT_EXCEEDED | AI quiz sends predictedSign, confidenceScore, isCorrect only |
| Gamification | US-33, US-34, US-36, US-37, US-40, US-45, US-46, US-48 | /api/v1/gamification/summary | GET | USER | None | GamificationSummaryResponse | valid token; aggregate from XP/streak/badge ledgers | UNAUTHORIZED | Profile needs XP, current/longest streak, unlocked badges |
| Gamification | US-42, US-43, US-44 | /api/v1/leaderboards | GET | USER | LeaderboardRequest | LeaderboardResponse | period in WEEKLY/MONTHLY; page/size bounds | VALIDATION_FAILED | Leaderboard needs dynamic rankings and current-user highlight |
| VSL Dictionary | US-50, US-51, US-52, US-53, US-54 | /api/v1/dictionary | GET | Public | DictionarySearchRequest | DictionaryEntryPageResponse | query length <= 100; category/difficulty enum; published only | VALIDATION_FAILED | Public dictionary route needs guest browse/search/filter |
| VSL Dictionary | US-55 | /api/v1/dictionary/{entryId}/practice-target | GET | Optional USER | None | PracticeTargetResponse | entry exists; linked lesson may require premium | DICTIONARY_ENTRY_NOT_FOUND | Practice CTA needs lesson/quiz deep-link target |
| Monetization | US-56, US-57 | /api/v1/subscription/plans | GET | Public | None | PlanListResponse | active plans only | NONE | PremiumModal needs price and benefit list from backend |
| Monetization | US-58, US-59, US-62 | /api/v1/payments/orders | POST | USER | CreatePaymentOrderRequest | PaymentOrderResponse | provider MOMO/ZALOPAY; planId active; amount matches plan | VALIDATION_FAILED, PLAN_NOT_FOUND, PAYMENT_PROVIDER_UNAVAILABLE | Payment modal needs provider-specific QR payload and expiry |
| Monetization | US-60, US-63 | /api/v1/me/subscription | GET | USER | None | SubscriptionResponse | valid token | UNAUTHORIZED | Frontend refreshes premium state after payment success |
| Monetization | US-64 | /api/v1/me/payments | GET | USER | PaymentHistoryRequest | PaymentHistoryResponse | page/size bounds; user-owned records only | VALIDATION_FAILED | Payment history screen needs transaction list/status |
| Admin Panel | US-65, US-66, US-67, US-68, US-69, US-70 | /api/v1/admin/content/** | GET/POST/PATCH/DELETE | ADMIN | AdminContentRequest | AdminContentResponse | role ADMIN; required fields; publish constraints; file size/type checks | FORBIDDEN, VALIDATION_FAILED, DUPLICATE_RESOURCE | Phase-2 admin content management depends on CRUD APIs |
| Admin Panel | US-71, US-72, US-73, US-74, US-75 | /api/v1/admin/users|payments|kpis | GET/PATCH | ADMIN | AdminQueryRequest/ManualPaymentStatusRequest | AdminPageResponse/AdminKpiResponse | role ADMIN; date range valid; manual override reason required | FORBIDDEN, VALIDATION_FAILED, INVALID_STATUS_TRANSITION | Admin dashboard needs users, subscriptions, transactions, KPIs |

### Epic Implementation Plan Table

| Epic | Covered Stories | Backend Module | Controller | Service | Repository | API Group | Database Tables/Migrations | Validation Rules | Test Gates | Dependencies | Implementation Sequence |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Authentication & Profile | US-01..US-08 | auth | AuthController, ProfileController | AuthService, ProfileService, PasswordResetService | UserRepository, RoleRepository, PasswordResetTokenRepository | /auth, /me | V1__create_users_and_roles.sql; V2__create_password_reset_tokens.sql | email unique; password policy; active account; avatar URL/file limit | AuthControllerIT; ProfileControllerIT; SecurityConfigIT | Foundation security and exception components | 1. users/roles migration; 2. register/login; 3. /me profile; 4. change/reset password; 5. OAuth v2 |
| Content Management (Learning) | US-09..US-18 | learning | UnitController, ChapterController, LessonController, ProgressController | LearningCatalogService, ProgressService, PremiumAccessService | UnitRepository, ChapterRepository, LessonRepository, UserProgressRepository | /units, /chapters, /lessons | V3__create_learning_catalog.sql; V4__create_user_progress.sql | published content only; premium gating; progress 0..100; idempotent completion | LearningControllerIT; ProgressServiceTest; PremiumAccessServiceTest | Auth/profile and subscription status | 1. catalog migrations; 2. list APIs; 3. premium lock metadata; 4. progress checkpoint; 5. completion hooks |
| Assessment | US-19..US-32 | assessment | QuizController, QuizAttemptController, AiQuizController | QuizService, QuizScoringService, AiAttemptService | QuizRepository, QuizQuestionRepository, QuizAttemptRepository, AiAttemptLogRepository | /quizzes, /quiz-attempts, /ai-quiz | V5__create_quizzes.sql; V6__create_quiz_attempts.sql; V7__create_ai_attempt_logs.sql | attempt ownership; timed duration validation; metadata-only AI payload; premium/preview quota | QuizControllerIT; QuizScoringServiceTest; AiQuizPrivacyContractTest | Learning lessons and premium access | 1. quiz schema; 2. load quiz; 3. submit/review; 4. timed status; 5. AI metadata and preview |
| Gamification | US-33..US-49 | gamification | GamificationController, LeaderboardController, BadgeController | XpService, StreakService, BadgeService, LeaderboardService | XpLogRepository, StreakRepository, BadgeRepository, UserBadgeRepository, LeaderboardSnapshotRepository | /gamification, /leaderboards, /badges | V8__create_gamification.sql; V9__create_leaderboard_snapshots.sql | XP idempotency; one streak update per activity day; badge uniqueness; period enum | XpServiceTest; StreakServiceTest; BadgeRuleServiceTest; LeaderboardControllerIT | Lesson/quiz completion events | 1. XP ledger; 2. streak rules; 3. badge rules; 4. summary API; 5. leaderboard periods |
| VSL Dictionary | US-50..US-55 | dictionary | DictionaryPublicController, AdminDictionaryController | DictionaryService, PracticeTargetService | DictionaryEntryRepository, DictionaryPracticeTargetRepository | /dictionary | V10__create_dictionary.sql; V11__create_dictionary_practice_targets.sql | public only published entries; query length; category/difficulty enums; optional auth for practice target | DictionaryPublicControllerIT; DictionarySearchServiceTest | Learning target IDs for practice links | 1. public search/list; 2. video/detail; 3. difficulty filters; 4. practice target mapping; 5. admin CRUD phase-2 |
| Monetization | US-56..US-64 | monetization | SubscriptionController, PaymentController, PaymentWebhookController | PlanService, PaymentOrderService, PaymentWebhookService, SubscriptionService | PlanRepository, SubscriptionRepository, PaymentTransactionRepository, PaymentWebhookEventRepository | /subscription, /payments | V12__create_subscriptions.sql; V13__create_payment_transactions.sql | provider enum; amount matches plan; signed webhook; idempotent transaction updates | PaymentControllerIT; PaymentWebhookServiceTest; SubscriptionServiceTest | Auth user identity and premium gating | 1. plans API; 2. order creation; 3. status polling; 4. webhook success/failure; 5. subscription refresh/history |
| Admin Panel | US-65..US-75 | admin | AdminContentController, AdminUsersController, AdminPaymentsController, AdminKpiController | AdminContentService, AdminUserService, AdminPaymentService, AdminKpiService, AuditLogService | all domain repositories plus AuditLogRepository | /admin/** | V14__create_audit_logs.sql; V15__create_media_assets.sql | ADMIN role; pagination; manual override reason; allowed status transitions | AdminSecurityIT; AdminContentControllerIT; AdminPaymentControllerIT; AdminKpiControllerIT | RBAC, content, assessment, payment domains | 1. RBAC claims; 2. content CRUD; 3. media presign/finalize; 4. user/payment admin; 5. KPI aggregation |

## Execution Notes

- Start with authentication and user profile because later protected APIs depend on JWT identity and role claims.
- Implement document upload/signature workflow contracts after identity and persistence foundations are available.
- Treat frontend mismatches as backend contract obligations only when the source report lists a backend API dependency.
- The delivery roadmap should order foundation, API contracts, persistence, service rules, frontend integration, then admin/reporting work.

## Frontend/backend alignment plan

## Objective

Convert frontend verification findings into backend contract requirements for "Frontend/backend alignment plan".

## Findings

- Frontend mismatches and not-implemented rows are backend requirements only when they depend on real API contracts or response fields.
- Authentication, progress, dictionary, payment, subscription, and gamification gaps need concrete endpoint contracts before frontend integration.

### Frontend Mismatch to Backend API Mapping

| FE Finding | Backend API Needed | Missing Request/Response Fields | Priority | Contract Test Required |
| --- | --- | --- | --- | --- |
| US-01 signup reuses generic login and lacks real account creation | POST /api/v1/auth/register | RegisterRequest.email/password/fullName; AuthResponse.accessToken/user/accountType; field validation errors | Must v1.1 | Register contract test: duplicate email, invalid password, success token |
| US-02 login ignores entered email/password | POST /api/v1/auth/login | LoginRequest.email/password; AuthResponse.accessToken/user; INVALID_CREDENTIALS/ACCOUNT_DISABLED errors | Must v1.1 | Login contract test: wrong password, inactive account, success |
| US-06 change password screen missing backend dependency | POST /api/v1/me/change-password | ChangePasswordRequest.currentPassword/newPassword; PASSWORD_REUSED and INVALID_CURRENT_PASSWORD codes | Must v1.1 | Change-password contract test with 400/401/business errors |
| US-08 forgot password has no recovery route | POST /api/v1/auth/password-reset/request and /confirm | PasswordResetRequest.email; PasswordResetConfirmRequest.token/newPassword; generic success response | Must v1.1 | Password reset contract test ensures email enumeration is impossible |
| US-14 progress only marks completion, not checkpoints | PUT /api/v1/lessons/{lessonId}/progress | status, completionPct, lastPositionSeconds, phase, currentQuestionIndex; updated chapter progress | Should v1.1 | Progress autosave contract test with IN_PROGRESS and COMPLETED transitions |
| US-17 FE uses modal paywall instead of redirect | GET /api/v1/lessons/{lessonId} and chapter list lock metadata | ChapterResponse.locked/requiresPremium; ErrorResponse code PREMIUM_REQUIRED | Must v1.1 | Premium gating contract test for Basic vs Premium user |
| US-18 resume lacks exact timestamp/session state | GET /api/v1/lessons/{lessonId}; PUT /progress | LessonDetailResponse.progress.lastPositionSeconds/phase/currentQuestionIndex | Should v1.1 | Resume contract test restores saved checkpoint |
| US-26 no explicit unanswered warning, backend must score safely | POST /api/v1/quiz-attempts/{attemptId}/submit | answers[] can omit question IDs; QuizResultResponse.unansweredCount | Should v1.1 | Submit contract test scores unanswered as incorrect |
| US-33/34 no XP state or reward pipeline | POST /api/v1/lessons/{lessonId}/complete; POST /quiz-attempts/{id}/submit | xpAwarded,totalXp,badgesUnlocked; duplicate award idempotency | Should v1.1 | XP idempotency contract test |
| US-37 streak increments on login instead of activity | GET /api/v1/me/streak and completion event side effect | StreakResponse.currentStreak/longestStreak/resetReason/timezone | Must v1.1 | Streak activity-day contract test |
| US-50 dictionary is behind dashboard auth | GET /api/v1/dictionary | Public DictionaryEntryPageResponse with word/category/videoUrl/difficulty | Must v1.1 | Guest dictionary contract test |
| US-54 difficulty missing from dictionary model | GET /api/v1/dictionary | difficulty enum and difficulty filter | Should v1.1 | Dictionary difficulty filter contract test |
| US-55 dictionary lacks practice CTA target | GET /api/v1/dictionary/{entryId}/practice-target | unitId/chapterId/lessonId/quizId/requiresPremium | Should v1.1 | Practice target contract test for guest and logged-in user |
| US-58/59 QR payments are generic/static | POST /api/v1/payments/orders | provider, qrPayload, expiresAt, transactionId, providerTransactionId | Must v1.1 | MoMo and ZaloPay order contract tests |
| US-62 payment flow has no failure state | GET /api/v1/payments/{transactionId} | status, reasonCode, retryable, userMessage | Must v1.1 | Payment failed/expired status contract test |
| US-63 subscription is boolean only | GET /api/v1/me/subscription | planType,status,startDate,endDate,remainingDays | Should v1.1 | Subscription refresh contract test after webhook success |
| US-64 payment history missing | GET /api/v1/me/payments | items[].provider/status/amount/createdAt/paidAt; pagination metadata | Should v1.1 | Payment history ownership and pagination contract test |

## Recommended Inputs

- Use this mapping as the contract-test backlog.
- Treat Must v1.1 rows as blocking backend API deliverables.
- Keep matched frontend stories as regression/hardening targets unless they need persistence or contract fields.

## Domain risk and validation review

## Objective

Review domain risks and validation gates for "Domain risk and validation review".

## Risks

- Generic planning risk: worker output can drift back to orchestrator process notes instead of backend epic/API decisions.
- API contract risk: frontend authentication, user profile, signature workflow, and document upload screens need stable request/response fields before integration.
- Persistence risk: PostgreSQL and Flyway constraints must match service validation or runtime errors will bypass standard error responses.
- Security risk: missing JWT role checks can expose protected profile, upload, or signature APIs.
  - # Backend Epic Implementation Plan Request (V-SIGN) Reference baseline (must follow): - EXE101_Project_V-Sign_BE/BE-init.md Primary objective: - Produce a deterministic backend implementation plan per epic for V-SIGN with Java Spring Boot 3.x architecture, JWT security, Flyway migrations, and centralized exception handling as specified in BE-init.md. Planning context documents (must be used): - EXE101_Project_V-Sign_
- writing-plan-principles.md: # Writing Plan Principles - Start by clarifying the user outcome, scope boundary, and definition of done. - Prefer explicit assumptions over hidden assumptions. - Organize plans into concrete phases that can be valida...
  - # Writing Plan Principles
  - - Start by clarifying the user outcome, scope boundary, and definition of done.
  - - Prefer explicit assumptions over hidden assumptions.
  - - Organize plans into concrete phases that can be validated independently.
- task-decomposition-principles.md: # Task Decomposition Principles - Break the planning problem into a small graph of worker tasks with clear ownership. - Separate evidence gathering, architecture, risk review, and final synthesis. - Pass only the cont...
  - # Task Decomposition Principles
  - - Break the planning problem into a small graph of worker tasks with clear ownership.
  - - Separate evidence gathering, architecture, risk review, and final synthesis.
  - - Pass only the context each worker needs to reduce drift.
- risk-audit-checklist.md: # Risk Audit Checklist - Check for missing assumptions, context overload, and ambiguous ownership. - Check for optional inputs becoming accidental prerequisites. - Check whether the merge output can be audited without...
  - # Risk Audit Checklist
  - - Check for missing assumptions, context overload, and ambiguous ownership.
  - - Check for optional inputs becoming accidental prerequisites.
- Domain anchors:

## Validation Plan

- Require domain anchors in each worker output and in the final report.
- Verify planning brief references instruction excerpts, relevant user stories, frontend verification excerpts, and selected knowledge-base guidance.
- Verify task graph includes API contract, frontend alignment, delivery roadmap, and risk tasks for backend epic plans.
- Test authentication, user profile, signature workflow, document upload, PostgreSQL migrations, validation errors, and delivery roadmap traceability in the generated report.

## Mitigations

- Add a generic-output detector that marks `needs_revision` when required domain anchors are absent.
- Keep worker context narrow by passing excerpts, summaries, task contracts, and domain checklists instead of raw repo dumps.
- Preserve traceability from instruction/user stories to planning brief, task graph, worker outputs, final report, and audit.

## Delivery roadmap and sequencing

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

## Final backend epic plan assembly

## Objective

Assemble a backend epic/API alignment plan for "Final backend epic plan assembly".

## Summary

- The final plan must align instruction requirements and user stories to backend epics, API contract decisions, PostgreSQL persistence, validation, testing, and a delivery roadmap.
- Authentication and user profile are foundational because protected workflows depend on JWT identity, account status, and role claims.
- Signature workflow and document upload require explicit API contracts, request/response DTOs, validation rules, and frontend contract tests.
- Frontend verification gaps become backend deliverables only when they require backend API support or response-field changes.

## Decisions

- Use `/api/v1` REST endpoints with standard success and error envelopes.
- Use PostgreSQL with Flyway migrations and production `ddl-auto=validate` behavior.
- Use centralized validation and exception handling so frontend errors are consistent.
- Build the delivery roadmap in dependency order: foundation, authentication/user profile, document upload/signature workflow, API contract integration, testing, and later admin/reporting extensions.

## Next Actions

- Turn each epic into endpoint-level tasks with DTOs, validation codes, database migrations, service rules, and tests.
- Run the frontend/backend contract smoke suite against authentication, user profile, signature workflow, and document upload.
- Keep audit status `pass`; treat `needs_revision` as a failed planning run requiring regenerated worker outputs.
