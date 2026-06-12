# Backend Restart Implementation Plan - 2026-05-23

## Source Documents

- `docs/v-sign-spring-boot-backend-architecture-coding-standards.md`
- `docs/app-docs/V-SIGN_UserStories_Full.md`

## Working Rules

- Implement by backend feature slice, not by scattered technical layer.
- Keep Controllers thin: validation, delegation, response envelope only.
- Keep domain logic in Services.
- Do not expose JPA entities through HTTP.
- Use request/response records for DTOs.
- Use centralized API success/error format.
- Update `implementation-log.md` after each coding session with commands and verification status.

## Current Repo Reality

- `src/main/java` has been recreated for BE-1 through BE-5 without using `git restore` or repository reset.
- Existing integration tests remain the first executable contract.
- `../openapi.json` from FE has been reviewed and used for compatibility aliases where feasible.

## FE OpenAPI Compatibility Notes

- Auth/profile accepts FE `displayName` while preserving legacy `fullName` compatibility.
- Payment order supports FE `planType` flow while preserving legacy `planId` tests.
- Resolved: FE `GET /subscription/plans` now returns `data` as an array. Legacy `/subscriptions/plans` remains available.
- Resolved: FE dictionary contract now supports `items/total`, string difficulty (`CO_BAN`, `TRUNG_BINH`, `NANG_CAO`), and integer `id`.
- Added FE monetization profile endpoints: `GET /api/v1/me/subscription` and `GET /api/v1/me/payments`.

## Sprint BE-1: Foundation + Authentication/Profile

Status: `completed`

Scope:

- Common API response envelope.
- Global error handling and domain error codes.
- Spring Security filter chain.
- Minimal JWT service using configured HMAC secret.
- Password encoder.
- User JPA entity/repository.
- Register, login, authenticated profile read, profile update.
- Test profile must run locally against H2, not remote Supabase.

Acceptance:

- `mvn -q "-Dtest=AuthControllerIT,ProfileControllerIT,BootSmokeTest,FlywayMigrationTest" test` passes.

## Sprint BE-2: Public Dictionary + Learning Catalog

Status: `completed`

Scope:

- Public dictionary browsing/search/filter.
- Practice target lookup.
- Units, chapters, lessons, progress checkpoint/update.
- Premium lock metadata.

Acceptance:

- `mvn -q "-Dtest=DictionaryIT,LearningWorkflowIT" test` passes.

## Sprint BE-3: Assessment

Status: `completed`

Scope:

- Assessment summary/detail.
- Legacy assessment submission.
- Lesson quiz attempt lifecycle.
- Submit/review attempts, scoring, validation.

Acceptance:

- `mvn -q "-Dtest=AssessmentControllerIT" test` passes.

## Sprint BE-4: Gamification + Monetization

Status: `completed`

Scope:

- Token-scoped gamification summary.
- Leaderboards.
- XP award idempotency.
- Subscription plans.
- Payment order/status mock gateway contracts.

Acceptance:

- `mvn -q "-Dtest=GamificationControllerIT,SubscriptionControllerIT" test` passes.

## Sprint BE-5: Admin APIs

Status: `completed`

Scope:

- Admin RBAC.
- User list.
- Content review queue.
- Payment override with audit log.
- KPI endpoint.

Acceptance:

- `mvn -q "-Dtest=AdminControllerIT" test` passes.

## Current Session Target

Completed:

- All restart sprints BE-1 through BE-5 are implemented and retested against current integration tests.
- Backend OpenAPI output is generated at `openapi-backend.json`.
- `OpenApiContractIT` verifies generated backend OpenAPI covers FE `../openapi.json` paths using the `/api/v1` backend prefix.
- Dictionary public read flow is no longer in-memory:
  - `V3__dictionary_entries.sql` creates/seeds `dictionary_entries`.
  - `DictionaryEntryEntity` and `DictionaryEntryRepository` back `DictionaryService`.
  - `FlywayMigrationTest` verifies V1-V3 and six published seed entries.
- Learning catalog/practice flow is no longer in-memory:
  - `V4__learning_catalog.sql` creates/seeds units, chapters, lessons, practice items, rubrics, and lesson progress.
  - Vocabulary taxonomy is selected from `D:\raw_videos\archive\Dataset\Labels\label.csv`.
  - Current seed structure: 8 units, 16 chapters, 20 lessons, 80 practice items.
  - Unit split: daily communication, education/places, specialized terms, time/numbers, emotions/description, travel/transport, work/career, culture/nature.
  - Difficulty split: `beginner` for common daily use/time/animals, `intermediate` for school/location/emotion/travel/culture, `advanced` for health/technology/legal/business/workplace terms.
  - Lesson `video_url` and unit `thumbnail_url` are intentionally nullable; `practice_items.source_video_file` preserves the raw dataset filename for later S3/CDN attachment.
  - `LearningWorkflowService` now reads catalog/practice/progress through Spring Data JPA.
- Assessment/Quiz flow is no longer in-memory:
  - `V5__assessment_quiz.sql` creates/seeds assessments, assessment questions/options, submissions, lesson quizzes, quiz questions/options, attempts, and attempt answers.
  - `AssessmentService` and `QuizAttemptService` now use DB-backed repositories.
  - Fixed test attempts (`attempt-greetings-ready`, `attempt-greetings-partial`, `attempt-greetings-mixed`, `attempt-greetings-reviewed`) are seeded for regression coverage.
  - Submission and review behavior remains compatible with existing FE/OpenAPI response shapes.
- Gamification flow is no longer in-memory:
  - `V6__gamification.sql` creates/seeds gamification profiles, earned badges, and idempotent XP award records.
  - `GamificationService` now reads/writes summary, leaderboard, and XP award state through Spring Data JPA repositories.
  - XP award duplicate protection is enforced by persisted `(user_id, event_id)` uniqueness.
  - Existing FE/OpenAPI response shapes remain unchanged.
- Monetization/Payment flow is no longer in-memory:
  - `V7__monetization.sql` creates/seeds subscription plans, checkout intents, user subscriptions, and payment orders.
  - `SubscriptionService` now reads plans/subscription summary from DB and persists checkout intents.
  - `PaymentService` now persists payment orders and reads payment status/history from DB.
  - Existing FE/OpenAPI response shapes remain unchanged.
- Admin flow is no longer in-memory:
  - `V8__admin_state.sql` creates/seeds admin user accounts, content review queue, and audit logs.
  - Admin users/review/audit services now read/write through Spring Data JPA repositories.
  - Admin payment override and KPI read from the persisted `payment_orders` table.
- Signature attempt logging is privacy-safe and DB-backed:
  - `V9__signature_attempt_logs.sql` creates `signature_attempt_logs`.
  - `LearningWorkflowService` persists attempt metadata, score, feedback codes, and a SHA-256 hash of the submitted vector.
  - Raw user video is not accepted or stored. Raw signature vector is not stored.

## Recommended Video Attachment Architecture

- Store official lesson/dataset videos in object storage, not in the application server filesystem.
- Preferred stack for production: AWS S3 private bucket + CloudFront signed/public CDN URLs.
- Backend DB should store media metadata only:
  - `source_video_file`: original dataset filename, e.g. `W00489.mp4`.
  - `storage_key`: stable object key, e.g. `learning/practice/W00489.mp4`.
  - `video_url`: public/signed CDN URL exposed to FE.
  - Optional checksum/size/duration fields for validation.
- Upload options:
  - Batch import script for dataset files from `D:\raw_videos\archive\Dataset`.
  - Admin upload endpoint later for new lesson videos.
- Privacy rule remains unchanged: these are official curriculum videos. User camera/video data is still never uploaded or stored; FE sends only MediaPipe metadata/result logs.

Next target:

- Backend persistence hardening is complete for the currently implemented API surface.
- Keep lesson/practice `video_url` nullable; user will populate uploaded video URLs directly in DB later.
- Review `openapi-backend.json` against FE usage, then start the FE real API integration phase when approved.
