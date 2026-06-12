## Objective

Convert frontend verification findings into backend contract requirements for "Frontend/backend alignment plan".

## Repository Evidence

- Original instruction summary:
  - # Backend Epic Implementation Plan Request (V-SIGN) Reference baseline (must follow): - EXE101_Project_V-Sign_BE/BE-init.md Primary objective: - Produce a deterministic backend implementation plan per epic for V-SIGN with Java Spring Boot 3.x architecture, JWT security, Flyway migrations, and centralized exception handling as specified in BE-init.md. Planning context documents (must be used): - EXE101_Project_V-Sign_
- Selected knowledge base:
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
  - authentication, user profile, document upload, API contract, PostgreSQL, validation, testing, delivery roadmap, epic/user story alignment
- Instruction excerpts:
- - REST API endpoints
- - Input validation
- - Validation rules
- - JWT authentication integration
- - PostgreSQL database
- - JWT authentication and authorization
- - RESTful API design
- - Use PostgreSQL
- Relevant user stories:
- EPIC 1: Authentication & Profile
- EPIC 2: Content Management (Learning)
- EPIC 3: Assessment
- EPIC 4: Gamification
- EPIC 5: VSL Dictionary
- EPIC 6: Monetization (Freemium & Payment)
- EPIC 7: Admin Panel
- Epic 1: Authentication | Epic 2: Learning | Epic 3: Assessment
- EPIC 1: Authentication & Profile
- US-01 â€” ÄÄƒng kÃ½ tÃ i khoáº£n
- AC3: Given password < 8 characters or missing uppercase/number â†’ inline validation error shown before submission.
- US-02 â€” ÄÄƒng nháº­p
- US-03 â€” ÄÄƒng nháº­p Google OAuth
- US-04 â€” Xem Profile
- Story: As a User, I want to view my profile page, so that I can see my account info, streak, and XP.
- AC1: Profile page displays: full name, avatar, account type (Basic/Premium), total XP, current streak, longest streak.
- Assigned task details:
  - Translate frontend verification gaps into backend contract requirements and integration checks.
- Expected output contract:
- Connect frontend mismatch/not-implemented rows to backend APIs.
- Identify response fields the frontend needs.
- Define contract verification gates.
- Domain-specific checklist:
- API contract
- testing
- user profile
- document upload

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
