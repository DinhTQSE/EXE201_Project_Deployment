# Backend Implementation Log

Purpose: chronological engineering log for transparent human tracking.

Required per entry:
- Timestamp
- Scope (epic/session)
- What changed (files/endpoints)
- Technical method used
- Commands run
- Verification result
- Deferred items

---

## 2026-05-22 - Session Kickoff Review + Verification

- Scope:
  - Reviewed and validated sessions 1-3 checkpoint state.
- What changed:
  - No backend code changes.
  - Documentation added for tracking:
    - `docs/backend-implementation-checkpoints/US-coverage-sessions-1-3.md`
    - `docs/backend-implementation-checkpoints/01-08-implementation-audit.md`
    - `docs/backend-implementation-checkpoints/implementation-log.md`
    - `docs/backend-implementation-checkpoints/technical-decision-record.md`
- Technical method used:
  - Checkpoint-first control.
  - Contract validation through existing integration tests.
  - Retrospective audit synthesis from checkpoint/source-of-truth docs plus current code/test structure.
- Commands run:
  - `mvn -q "-Dtest=AuthControllerIT,ProfileControllerIT,DictionaryIT" test`
  - `mvn -q "-Dtest=com.vsign.backend.learning.LearningWorkflowIT,com.vsign.backend.assessment.AssessmentControllerIT" test`
  - `mvn -q "-Dtest=com.vsign.backend.gamification.GamificationControllerIT,com.vsign.backend.monetization.SubscriptionControllerIT" test`
- Verification result:
  - All three command groups passed on 2026-05-22.
- Deferred items:
  - Epic 06 write-side/persistence hardening.
  - Epic 07 subscription/history/webhook/persistence hardening.
  - Epic 08 full implementation and acceptance.

---

## 2026-05-22 - Epic 6 + Epic 8 Full In-Memory Implementation

- Scope:
  - Epic 06 full behavior (without DB migrations).
  - Epic 08 full admin API scope (without DB migrations).
- What changed:
  - Security/JWT:
    - Role-aware token generation and claim extraction.
    - JWT filter coverage expanded to `/api/v1/admin/**`, `/api/v1/gamification/**`, `/api/v1/leaderboards`.
  - Epic 06:
    - Token-derived gamification summary identity.
    - Added `POST /api/v1/gamification/xp-awards` with idempotent `eventId`.
    - Added in-memory XP/streak/badge update rules.
  - Epic 08:
    - Added admin payment APIs (`GET/PATCH /api/v1/admin/payments`).
    - Added admin KPI API (`GET /api/v1/admin/kpis`).
    - Added admin audit-log API (`GET /api/v1/admin/audit-logs`).
    - Added admin content decision mutation (`PATCH /api/v1/admin/content/review-queue/{contentId}`).
    - Enforced ADMIN/REVIEWER role checks in services.
  - Tests:
    - Rewrote/expanded `GamificationControllerIT` and `AdminControllerIT`.
- Technical method used:
  - In-memory domain state with deterministic seeds.
  - Service-layer role guard and business validation.
  - API-first integration test validation.
- Commands run:
  - `mvn -q "-Dtest=com.vsign.backend.gamification.GamificationControllerIT,com.vsign.backend.admin.AdminControllerIT" test`
  - `mvn -q "-Dtest=AuthControllerIT,ProfileControllerIT,DictionaryIT" test`
  - `mvn -q "-Dtest=com.vsign.backend.learning.LearningWorkflowIT,com.vsign.backend.assessment.AssessmentControllerIT" test`
- Verification result:
  - All above command groups passed.
- Deferred items:
  - Epic 06/08 persistence migrations and database-backed repositories are intentionally deferred by user direction.

---

## Template (Copy for next entries)

### YYYY-MM-DD HH:mm

- Scope:
- What changed:
- Technical method used:
- Commands run:
- Verification result:
- Deferred items:

---

## 2026-05-23 - Restart From Source Docs

- Scope:
  - Restart backend implementation from architecture standards and full user stories.
- What changed:
  - Added `docs/backend-implementation-checkpoints/PLAN-backend-restart-2026-05-23.md`.
  - Defined Sprint BE-1 through BE-5 with executable acceptance commands.
- Technical method used:
  - Checkpoint-driven rebuild without `git restore` or repository reset.
  - Existing integration tests are treated as executable API contract.
- Commands run:
  - `rg` over backend architecture doc and user story doc.
  - `rg --files src/main/java src/test/java src/main/resources docs/backend-implementation-checkpoints`
  - `git status --short`
- Verification result:
  - Planning complete.
  - Code implementation starts at Sprint BE-1: Foundation + Authentication/Profile.
- Deferred items:
  - Dictionary, Learning, Assessment, Gamification, Monetization, Admin.

---

## 2026-05-24 13:30 +07:00 - Backend Restart BE-1 Through BE-5

- Scope:
  - Implemented backend restart slices BE-1 through BE-5 from the restart plan.
  - Reviewed FE-first `../openapi.json` before finalizing compatibility decisions.
- What changed:
  - Recreated Spring Boot common foundation: success envelope, centralized error handling, validation errors, JWT filter/service, security configuration, password encoder, H2 test profile, and Flyway migration compatibility.
  - Recreated auth/profile, dictionary, learning, assessment, gamification, monetization, and admin API modules.
  - Added FE compatibility aliases for auth/profile `displayName` and payment-order `planType`.
  - Recreated admin APIs for users, content review queue, payment status override, KPI dashboard, and audit logs.
  - Cleaned duplicate Maven dependencies for `flyway-core` and `postgresql` from `pom.xml`.
- Technical method used:
  - Feature-slice rebuild using existing integration tests as executable contract.
  - In-memory deterministic state for non-auth domains while backend waits for final FE contract/OpenAPI reconciliation.
  - JPA/Flyway kept active for user foundation and migration verification.
- Commands run:
  - `rg -n "admin|subscription|dictionary|plans" openapi.json`
  - `mvn -q "-Dtest=AuthControllerIT,ProfileControllerIT,BootSmokeTest,FlywayMigrationTest" test`
  - `mvn -q "-Dtest=DictionaryIT,LearningWorkflowIT" test`
  - `mvn -q "-Dtest=AssessmentControllerIT" test`
  - `mvn -q "-Dtest=GamificationControllerIT,SubscriptionControllerIT" test`
  - `mvn -q "-Dtest=AdminControllerIT" test`
  - `mvn -q test`
- Verification result:
  - Targeted BE-1 through BE-5 test groups passed.
  - Full Maven test suite passed after POM cleanup.
  - Normal sandbox Maven attempts still fail when Maven needs dependency resolution; escalated Maven runs pass.
- Deferred items:
  - Reconcile FE `GET /subscription/plans` contract (`data` array) with current backend integration-test shape (`data.plans`).
  - Reconcile FE dictionary contract (`items/total`, string difficulty) with current backend integration-test shape (`content/totalElements`, numeric difficulty).
  - Replace in-memory non-auth domains with DB migrations, repositories, and service-level transactions after FE contract freeze.
  - Generate backend OpenAPI after final FE/BE response-shape reconciliation.

---

## 2026-05-24 13:44 +07:00 - Retest APIs + FE Contract Reconciliation

- Scope:
  - Retested all implemented API integration tests.
  - Continued FE-first reconciliation against `../openapi.json`.
- What changed:
  - `GET /api/v1/subscription/plans` now returns `data` as an array for FE compatibility.
  - Dictionary API now exposes FE fields `items`, `total`, `id`, `word`, and string `difficulty`.
  - Dictionary keeps non-conflicting legacy metadata (`content`, `totalElements`, `totalPages`, `keyword`, `entryId`, `difficultyLevel`) during transition.
  - Added `GET /api/v1/me/subscription`.
  - Added `GET /api/v1/me/payments`.
  - Updated `DictionaryIT` and `SubscriptionControllerIT` to validate FE-first response shapes.
- Technical method used:
  - Contract-first controller/service DTO adjustment.
  - Backward-compatible fields retained where they do not conflict with FE schema types.
  - Integration tests updated to cover FE paths and response shapes.
- Commands run:
  - `mvn -q test`
  - `mvn -q "-Dtest=DictionaryIT,SubscriptionControllerIT" test`
  - `mvn -q test`
- Verification result:
  - Initial full retest passed before changes.
  - Targeted Dictionary/Monetization test suite passed after changes.
  - Final full Maven test suite passed: 68 tests, 0 failures, 0 errors.
  - Normal sandbox Maven attempts still fail when Maven needs dependency resolution; escalated Maven runs pass.
- Deferred items:
  - Generate backend OpenAPI output from Springdoc.
  - Replace in-memory dictionary, learning, assessment, gamification, monetization, and admin data with DB-backed repositories after FE contract freeze.
  - Tighten payment history ownership once authenticated payment creation is finalized.

---

## 2026-05-24 13:56 +07:00 - OpenAPI Contract + Dictionary DB Slice

- Scope:
  - Generated backend OpenAPI contract.
  - Started persistence hardening by replacing dictionary in-memory data with DB-backed reads.
- What changed:
  - Added `OpenApiContractIT`.
  - Generated `openapi-backend.json` from `/api/v1/api-docs`.
  - Added Flyway migration `V3__dictionary_entries.sql`.
  - Added `DictionaryEntryEntity` and `DictionaryEntryRepository`.
  - Updated `DictionaryService` to read published dictionary entries from JPA while preserving FE-compatible response fields.
  - Updated `FlywayMigrationTest` to verify V1-V3 and six published dictionary seed entries.
- Technical method used:
  - Contract-first verification against FE `../openapi.json`.
  - DB-backed read model through Flyway + Spring Data JPA.
  - Existing controller contract retained; only the dictionary data source changed.
- Commands run:
  - `mvn -q "-Dtest=FlywayMigrationTest,DictionaryIT,OpenApiContractIT" test`
  - `mvn -q test`
- Verification result:
  - Targeted migration/dictionary/OpenAPI contract tests passed.
  - Full Maven test suite passed.
  - Normal sandbox Maven still fails when dependency resolution needs network access; escalated Maven runs pass.
- Deferred items:
  - Move Learning Catalog, Assessment, Gamification, Monetization, and Admin slices from in-memory state to DB-backed repositories.
  - Real API integration inside `v-sign-fe` is not started yet; backend contract/persistence hardening continues first.

---

## 2026-05-24 14:13 +07:00 - Learning Catalog DB Slice + Label Taxonomy

- Scope:
  - Replaced Learning Catalog/practice data source with DB-backed Flyway/JPA.
  - Built an initial vocabulary taxonomy from `D:\raw_videos\archive\Dataset\Labels\label.csv`.
- What changed:
  - Added `V4__learning_catalog.sql`.
  - Added Learning persistence entities/repositories for units, chapters, lessons, practice items, rubric rows, and lesson progress.
  - Updated `LearningWorkflowService` to read units/chapters/lessons/practice items/progress from JPA.
  - Seeded 8 units, 16 chapters, 20 lessons, and 80 practice items:
    - `beginner`: common daily vocabulary, greeting, family, food, basic routines.
    - `intermediate`: education, places, services, emotions, travel, culture.
    - `advanced`: health, technology, legal, finance/business, workplace.
  - Kept `video_url`/`thumbnail_url` nullable because production lesson videos are not linked yet.
  - Preserved raw dataset mapping through `practice_items.source_video_file` for later S3/CDN upload mapping.
  - Updated `LearningWorkflowIT` and `FlywayMigrationTest` for the new taxonomy and V4 migration.
- Technical method used:
  - DB-backed read model with Spring Data JPA.
  - Public API response shape preserved for FE/OpenAPI compatibility.
  - Progress remains user-keyed; unauthenticated flow uses `anonymous`, authenticated flow can later use JWT principal email.
- Commands run:
  - `Get-Content D:\raw_videos\archive\Dataset\Labels\label.csv`
  - `Import-Csv D:\raw_videos\archive\Dataset\Labels\label.csv`
  - `mvn -q "-Dtest=FlywayMigrationTest,LearningWorkflowIT,OpenApiContractIT" test`
  - `mvn -q test`
- Verification result:
  - Targeted Flyway/Learning/OpenAPI tests passed.
  - Full Maven test suite passed: 69 tests, 0 failures, 0 errors.
- Deferred items:
  - Build video attachment workflow: raw dataset file -> uploaded object -> S3/CDN URL -> update `video_url`/asset mapping.
  - Replace Assessment/Quiz, Gamification, Monetization, and Admin in-memory state with DB-backed repositories.
  - Real API integration inside `v-sign-fe` is still not started.

---

## 2026-05-24 14:31 +07:00 - Expanded Learning Vocabulary Seed + Video Pipeline Decision

- Scope:
  - Expanded learning taxonomy after product review that 3 units was too thin.
  - Documented recommended approach for putting lesson/dataset videos online and attaching URLs back to backend records.
- What changed:
  - Expanded `V4__learning_catalog.sql` from 3 units to 8 units.
  - Expanded seed data from 40 to 80 published practice items.
  - Added units for time/numbers, emotions/description, travel/transport, work/career, and culture/nature.
  - Updated `FlywayMigrationTest` and `LearningWorkflowIT` expected catalog counts.
  - Added recommended video attachment architecture to `PLAN-backend-restart-2026-05-23.md`.
- Technical method used:
  - Continued using `label.csv` as the vocabulary source.
  - Preserved current public API response shape.
  - Kept lesson `video_url` nullable and retained `practice_items.source_video_file` for later batch upload mapping.
- Commands run:
  - `Import-Csv D:\raw_videos\archive\Dataset\Labels\label.csv`
  - `mvn -q "-Dtest=FlywayMigrationTest,LearningWorkflowIT,OpenApiContractIT" test`
  - `mvn -q test`
- Verification result:
  - Targeted Flyway/Learning/OpenAPI tests passed.
  - Full Maven test suite passed: 69 tests, 0 failures, 0 errors.
- Deferred items:
  - Implement batch uploader: raw dataset file -> S3/CloudFront object -> DB update by `source_video_file`.
  - Add media metadata table/fields (`storage_key`, checksum, file size, duration) when upload pipeline starts.
  - Real API integration inside `v-sign-fe` is still not started.

---

## 2026-05-24 14:43 +07:00 - Assessment/Quiz DB Slice

- Scope:
  - Replaced Assessment and Quiz attempt in-memory state with DB-backed Flyway/JPA.
  - Left video upload/URL population out of scope per product direction; `video_url` remains nullable for later DB update.
- What changed:
  - Added `V5__assessment_quiz.sql`.
  - Added Assessment persistence entities/repositories for assessments, questions, options, submissions, and submission answers.
  - Added Quiz persistence entities/repositories for lesson quizzes, questions, options, attempts, and attempt answers.
  - Updated `AssessmentService` to read definitions from DB and persist submissions.
  - Updated `QuizAttemptService` to create, submit, score, and review attempts from DB.
  - Updated `FlywayMigrationTest` to verify V1-V5 plus assessment/quiz seed counts.
- Technical method used:
  - Contract-preserving persistence migration.
  - Seeded fixed regression attempts used by `AssessmentControllerIT`.
  - Kept current legacy lesson quiz alias `lesson-greetings` while also seeding `lesson-greetings-1` for catalog alignment.
- Commands run:
  - `mvn -q "-Dtest=FlywayMigrationTest,AssessmentControllerIT,OpenApiContractIT" test`
  - `mvn -q test`
- Verification result:
  - Targeted Flyway/Assessment/OpenAPI tests passed.
  - Full Maven test suite passed: 69 tests, 0 failures, 0 errors.
- Deferred items:
  - Move Gamification XP/streak/leaderboard state to DB-backed repositories.
  - Move Monetization/Payment mock state to DB-backed repositories.
  - Real API integration inside `v-sign-fe` is still not started.

---

## 2026-05-24 14:50 +07:00 - Gamification DB Slice

- Scope:
  - Replaced Gamification summary, leaderboard, and XP award in-memory state with DB-backed Flyway/JPA.
  - Left video upload/URL population out of scope per product direction; `video_url` stays nullable for later DB update.
- What changed:
  - Added `V6__gamification.sql`.
  - Added Gamification persistence entities/repositories for profiles, badges, and XP award idempotency records.
  - Updated `GamificationService` to read profiles/badges from DB and persist XP awards.
  - Kept leaderboard ranking, current-user row, and XP duplicate behavior compatible with existing API tests and FE/OpenAPI response shapes.
  - Updated `FlywayMigrationTest` to verify V1-V6 plus seeded gamification profile count.
- Technical method used:
  - Contract-preserving persistence migration.
  - Persisted idempotency through unique `(user_id, event_id)` XP award records.
  - New users are lazily provisioned into `gamification_profiles` from the authenticated email.
- Commands run:
  - `mvn -q "-Dtest=FlywayMigrationTest,GamificationControllerIT,OpenApiContractIT" test`
  - `mvn -q test`
- Verification result:
  - Targeted Flyway/Gamification/OpenAPI tests passed.
  - Full Maven test suite passed.
- Deferred items:
  - Move Monetization/Payment mock state to DB-backed repositories.
  - Move Admin mock state to DB-backed repositories after monetization persistence is stable.
  - User will upload official videos and populate DB URLs separately.
  - Real API integration inside `v-sign-fe` is still not started.

---

## 2026-05-24 14:58 +07:00 - Monetization/Payment DB Slice

- Scope:
  - Replaced subscription plans, checkout intents, user subscription summary, and payment orders with DB-backed Flyway/JPA.
  - Kept official video upload/URL population out of scope; user will populate DB URLs separately.
- What changed:
  - Added `V7__monetization.sql`.
  - Added Monetization persistence entities/repositories for subscription plans, checkout intents, user subscriptions, and payment orders.
  - Updated `SubscriptionService` to load legacy/active plan lists from DB, persist checkout intents, and read/lazily create user subscription summaries.
  - Updated `PaymentService` to persist created orders and read status/history from `payment_orders`.
  - Updated `FlywayMigrationTest` to verify V1-V7 plus subscription plan/payment order seed counts.
- Technical method used:
  - Contract-preserving persistence migration.
  - Public response DTOs and endpoint paths were kept unchanged for FE/OpenAPI compatibility.
  - Payment history ownership remains broad until authenticated payment order creation is finalized.
- Commands run:
  - `mvn -q "-Dtest=FlywayMigrationTest,SubscriptionControllerIT,OpenApiContractIT" test`
  - `mvn -q test`
- Verification result:
  - Targeted Flyway/Monetization/OpenAPI tests passed.
  - Full Maven test suite passed.
- Deferred items:
  - Move Admin users/content review/audit/KPI state to DB-backed repositories.
  - Tighten payment history ownership after payment order creation is tied to authenticated users.
  - Real API integration inside `v-sign-fe` is still not started.

---

## 2026-05-24 15:05 +07:00 - Admin DB Slice

- Scope:
  - Replaced Admin user list, content review queue, audit log, payment override, and KPI state with DB-backed repositories.
- What changed:
  - Added `V8__admin_state.sql`.
  - Added Admin persistence entities/repositories for admin user accounts, review queue items, and audit logs.
  - Updated `AdminUserService`, `AdminContentReviewService`, and `AdminAuditService` to read/write DB state.
  - Updated `AdminPaymentService` to read/update `payment_orders` from the Monetization slice and compute KPI revenue from persisted payment rows.
  - Updated `FlywayMigrationTest` to verify V1-V8 plus admin seed counts.
- Technical method used:
  - Contract-preserving persistence migration.
  - Payment records now have one persisted source of truth through `payment_orders`.
- Commands run:
  - `mvn -q "-Dtest=FlywayMigrationTest,AdminControllerIT,OpenApiContractIT" test`
  - `mvn -q test`
- Verification result:
  - Targeted Flyway/Admin/OpenAPI tests passed.
  - Full Maven test suite passed.
- Deferred items:
  - Official video URL population remains outside this session; user will upload videos and update DB URLs later.
  - Real API integration inside `v-sign-fe` is still not started.

---

## 2026-05-24 15:09 +07:00 - Signature Attempt Privacy Log

- Scope:
  - Added DB-backed logging for signature workflow attempts while preserving the privacy rule.
- What changed:
  - Added `V9__signature_attempt_logs.sql`.
  - Added `SignatureAttemptLogEntity` and `SignatureAttemptLogRepository`.
  - Updated `LearningWorkflowService` to persist attempt ID, user key, practice item, duration, score, feedback codes, and SHA-256 hash of `signatureVector`.
  - Raw video is not accepted or stored. Raw signature vector is not stored.
  - Premium lesson lock now reads active subscription state from `user_subscriptions` when a JWT principal exists.
  - Updated `FlywayMigrationTest` to verify V1-V9 and the signature attempt log table.
- Technical method used:
  - Privacy-safe metadata persistence.
  - Contract-preserving response DTOs.
- Commands run:
  - `mvn -q "-Dtest=FlywayMigrationTest,LearningWorkflowIT,OpenApiContractIT" test`
  - `mvn -q test`
- Verification result:
  - Targeted Flyway/Learning/OpenAPI tests passed.
  - Full Maven test suite passed.
- Deferred items:
  - Review `openapi-backend.json` with FE implementation before starting real FE API integration.
  - User will upload official videos and populate lesson/practice video URLs directly in DB.

---

## 2026-05-24 22:30 +07:00 - Real AI Attempt Integration

- Scope:
  - Connected the product backend to the real AI attempt flow from `V-Sign-AI-Build` while preserving privacy constraints.
  - Spring Boot does not receive or store raw camera frames/video. FE sends temporary frames directly to FastAPI AI; Spring Boot stores prediction metadata only.
- What changed:
  - Added AI metadata fields to `SubmitSignatureAttemptRequest`, `SignatureAttemptResponse`, and `SignatureAttemptLogEntity`.
  - Added `V10__signature_attempt_ai_metadata.sql` for prediction metadata columns.
  - Added `V11__ai_model_practice_items.sql` to seed practice items for the current AI model labels: `CA_PHE`, `DA`, `DEN`, `NONG`, `SUA`, `TRA`.
  - Updated `LearningWorkflowService` to score real AI predictions from label/confidence/correctness and keep legacy submit behavior for non-AI attempts.
  - Added integration test coverage for AI prediction metadata submission.
- Technical method used:
  - FE-to-FastAPI direct inference path; BE receives only result metadata.
  - Confidence threshold is currently `0.70` in FE practice flow and BE status logic.
  - Attempt log stores normalized target/predicted gloss, confidence, correctness, processed frame counts, hands-detected counts, inference time, and hashed signature vector.
- Commands run:
  - `mvn.cmd -q -Dtest=LearningWorkflowIT test`
  - `mvn.cmd -q test`
- Verification result:
  - `LearningWorkflowIT` passed.
  - Full Maven test suite passed.
- Deferred items:
  - Manual end-to-end camera test with FE, Spring Boot, and FastAPI running concurrently.
  - Retrain/expand AI model beyond the current 6-class drink vocabulary before mapping broad lesson content to recognition.
  - User will upload official videos and populate video URLs separately.

---

## 2026-05-24 22:41 +07:00 - Supabase Flyway Startup Fix

- Scope:
  - Fixed backend startup blockers found when running Spring Boot against the live Supabase PostgreSQL database.
- What changed:
  - Restored immutable `V1__init_schema.sql` content to the applied Flyway checksum by reverting the equivalent type alias change from `timestamp with time zone` back to `timestamptz`.
  - Updated `V10__signature_attempt_ai_metadata.sql` from PostgreSQL-invalid `double` to `double precision` for `confidence` and `inference_ms`.
- Technical method used:
  - Do not edit already-applied migrations except to restore their original immutable content.
  - Directly fix `V10` because it failed before successful application and was not an applied migration yet.
- Commands run:
  - `git diff -- src/main/resources/db/migration/V1__init_schema.sql`
  - `rg -n "\bdouble\b|auto_increment|datetime|tinyint|longtext|mediumtext|json\b|enum\(" src/main/resources/db/migration`
  - `mvn.cmd -q -Dtest=FlywayMigrationTest test`
- Verification result:
  - Source scan found PostgreSQL-incompatible `double` only in `V10`.
  - Local Maven validation could not run inside sandbox because dependency/network access was denied.
- Deferred items:
  - Re-run `mvn.cmd spring-boot:run` in the user terminal to confirm Supabase migration V10/V11 apply and backend starts on port 8080.

---

## 2026-05-25 - Private S3 + CloudFront OAC Media Architecture

- Scope:
  - Locked official video delivery architecture to private S3 origin served only through CloudFront OAC.
  - Backend and DB will store CloudFront URLs, never direct S3 URLs.
- What changed:
  - Added `V12__practice_item_video_url.sql` to support `practice_items.video_url`.
  - Exposed practice item `label`, `sourceVideoFile`, and `videoUrl` in practice item summary/detail API responses.
  - Added CloudFormation template `infra/cloudformation/private-media-cloudfront-oac.yaml`.
  - Added media scripts:
    - `scripts/media/deploy-private-media-stack.ps1`
    - `scripts/media/sync-videos-to-s3.ps1`
    - `scripts/media/generate-video-url-backfill-sql.ps1`
  - Added runbook `docs/media/private-s3-cloudfront-oac-video-runbook.md`.
- Technical method used:
  - S3 Block Public Access remains enabled.
  - CloudFront OAC uses SigV4 request signing to read from S3.
  - Generated SQL backfills CloudFront URLs into `practice_items.video_url`, `learning_lessons.video_url`, and `dictionary_entries.video_url`.
- Verification result:
  - PowerShell scripts pass syntax checks.
  - `generate-video-url-backfill-sql.ps1` dry-run generated CloudFront URL SQL successfully.
  - `mvn.cmd -q "-Dtest=FlywayMigrationTest,LearningWorkflowIT" test` passed with 12 migrations.
  - AWS deployment not run because local AWS CLI credentials currently return `InvalidClientTokenId`.
- Deferred items:
  - User must configure AWS CLI credentials, then deploy stack and run upload/backfill scripts.
  - Add CloudFront signed URL/cookie later if premium video authorization must be enforced at CDN level.

### Deployment note

- 2026-05-25:
  - First CloudFormation deploy failed at `MediaDistribution`.
  - AWS returned `AccessDenied`: `Your account must be verified before you can add new CloudFront resources`.
  - This is an AWS account verification/billing activation blocker, not a template bug.
  - Runbook updated with AWS Support verification steps and rollback stack cleanup commands.
  - Runbook also includes AWS Console manual setup steps for private S3, CloudFront OAC, bucket policy, URL verification, and DB SQL backfill.

---

## 2026-05-25 - Cloudflare R2 MVP Media Decision

- Scope:
  - Switched MVP video hosting target from AWS CloudFront/S3 to Cloudflare R2.
- Reason:
  - AWS Free Tier has about 2 months left.
  - AWS CloudFront distribution creation is currently blocked by account verification.
  - Current video dataset is about 2.7 GB, which fits Cloudflare R2 free tier storage.
- What changed:
  - Added `docs/media/cloudflare-r2-video-runbook.md`.
  - Added `scripts/media/sync-videos-to-r2.ps1` for bulk upload because Cloudflare Dashboard upload is limited to 100 files.
  - Updated `scripts/media/generate-video-url-backfill-sql.ps1` to use generic `-MediaBaseUrl`, while keeping `-CloudFrontDomain` as a backward-compatible alias.
  - Added `V13__dictionary_video_variants.sql` to preserve regional variants from filename suffixes.
  - Updated FE remediation tracking Wave 6 to use R2 for MVP media hosting.
- Technical method used:
  - Public R2 custom domain or `r2.dev` URL is stored directly in DB `video_url` fields.
  - Filename suffix parsing: `B` = `BAC`, `T` = `TRUNG`, `N` = `NAM`, otherwise `TOAN_QUOC`.
  - `dictionary_entries.video_url` remains a default representative video for current FE, while `dictionary_entry_video_variants` stores all regional variants for future UI selection.
  - FE remains unchanged because it already renders backend `videoUrl`.
- Deferred items:
  - If premium video access must be enforced, add R2 private access behind Cloudflare Worker or BE-generated short-lived signed URLs later.

### Verification note

- 2026-05-25:
  - Region-aware SQL dry-run generated successfully with `-DefaultRegionCode BAC`.
  - `mvn.cmd -q "-Dtest=FlywayMigrationTest" test` passed and validated 13 Flyway migrations.
- 2026-05-26:
  - User completed R2 upload and enabled public delivery at `https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev`.
  - Generated `scripts/media/generated-video-url-backfill.sql` using:
    - `-MediaBaseUrl https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev`
    - `-Prefix videos`
    - `-LabelCsvPath D:\raw_videos\archive\Dataset\Labels\label.csv`
    - `-DefaultRegionCode BAC`
  - Verified sample object `videos/W00489.mp4` returns `HTTP 200`, `Content-Type: video/mp4`, `Accept-Ranges: bytes`, and immutable public cache headers.
  - SQL output includes updates for `practice_items`, `learning_lessons`, `dictionary_entries`, and inserts/updates for `dictionary_entry_video_variants`.
  - Next action: run generated SQL in Supabase SQL Editor, then verify API `videoUrl` fields and FE video playback.
- 2026-05-26:
  - First Supabase SQL run failed with `relation "practice_items" does not exist`.
  - Hardened `generate-video-url-backfill-sql.ps1` with `-DbSchema public`, schema-qualified table names, and `to_regclass('public.<table>')` guards.
  - Regenerated `generated-video-url-backfill.sql`; `practice_items` and `learning_lessons` blocks now skip safely if the table/column is absent from the target DB.
- 2026-05-26:
  - Second Supabase SQL run failed with `relation "public.dictionary_entries" does not exist`.
  - Hardened dictionary upsert and variant insert blocks with `to_regclass('public.dictionary_entries')` guards and regenerated the SQL.
  - Current conclusion: target Supabase database/schema likely has not run the V-Sign Flyway migrations yet, so backfill SQL may now run but will no-op until required tables exist.
- 2026-05-26:
  - User confirmed `public` schema returns no V-Sign tables.
  - Backend configuration uses `DB_SCHEMA=v-sign_schema`, so generated video backfill SQL must target that schema instead of `public`.
  - Updated generator to quote schema/table identifiers safely for schema names containing `-`.
  - Regenerated `generated-video-url-backfill.sql` with `-DbSchema v-sign_schema`; output now uses `"v-sign_schema"."dictionary_entries"` etc.
- 2026-05-26:
  - Backfill SQL run reached dictionary upsert but failed with duplicate primary key `dictionary_entries_pkey (id)=(1)`.
  - Root cause: V3 seeded `dictionary_entries` with explicit IDs while the identity sequence was still at its initial value.
  - Updated generator to call `setval(pg_get_serial_sequence(...), max(id) + 1, false)` before inserting new dictionary rows.
  - Regenerated `generated-video-url-backfill.sql` for `v-sign_schema`.
- 2026-05-26:
  - Supabase SQL run failed with `relation "tmp_vsign_dictionary_video_candidates" does not exist`.
  - Removed `ON COMMIT DROP` from the temporary table and added explicit `drop table if exists tmp_vsign_dictionary_video_candidates` before/after use.
  - Regenerated `generated-video-url-backfill.sql` for `v-sign_schema`.
- 2026-05-26:
  - User confirmed dictionary video backfill insert completed.
  - Verified from source CSV that `label.csv` has 4,362 video rows and 3,314 unique labels, so roughly 3,314 `dictionary_entries` rows is expected.
  - Next verification: DB counts for video URLs/variants, backend API `videoUrl` fields, and FE playback.

---

## 2026-05-26 - Learning Catalog Expansion From Dictionary Videos

- Scope:
  - Addressed the course catalog having too few lesson vocabulary items after dictionary video backfill.
- What changed:
  - Added `scripts/media/generated-curated-unit-vocabulary-expansion.sql`.
  - This script keeps the original visible units/chapters and adds 20 dictionary-backed lessons per existing curated chapter, so the first course pages no longer show only a few lessons.
  - Added `scripts/media/generated-learning-catalog-expansion.sql`.
  - The script reads `"v-sign_schema"."dictionary_entries"` rows with `video_url`, then upserts generated course packs:
    - 120 lessons per generated unit.
    - 30 lessons per generated chapter.
    - 1 `practice_items` row per generated lesson.
    - Rubric rows for hand shape, palm orientation, and movement path.
  - Generated lesson/practice IDs use dictionary IDs (`lesson-dict-<id>`, `practice-dict-<id>`) so the script is rerunnable.
  - First 4 generated packs are free; later generated packs are marked premium at chapter/lesson level to preserve freemium behavior.
- FE change paired with this:
  - Dictionary video modal was expanded from a small `max-w-lg` modal to a viewport-sized video dialog.
  - Lesson study video width was expanded from `max-w-2xl` to `max-w-5xl`, with `object-contain` to avoid cropping sign-language frames.
- Verification:
  - `npm.cmd run build` passed.
  - `npm.cmd run test` passed.
  - Existing Vite large chunk warning remains and is still tracked separately.
- Next action:
  - Run `scripts/media/generated-curated-unit-vocabulary-expansion.sql` in Supabase SQL Editor first.
  - Optionally run `scripts/media/generated-learning-catalog-expansion.sql` after that if the product should expose the full dictionary as additional generated packs.
  - Verify generated counts:
    - `learning_lessons` where `lesson_id like 'lesson-curated-ext-%'`.
    - `learning_units` where `unit_id like 'unit-dict-pack-%'`.
    - `learning_lessons` where `lesson_id like 'lesson-dict-%'`.
    - `practice_items` where `practice_item_id like 'practice-dict-%'`.

---

## 2026-05-26 - Course Loading Performance Fix

- Problem:
  - After expanding lesson vocabulary, the FE Courses screen became slow because `VocabularyPack` loaded the full catalog tree on entry:
    - all units,
    - all chapters for every unit,
    - all lessons for every chapter.
- What changed:
  - Refactored `VocabularyPack` to lazy-load catalog layers:
    - initial Courses screen calls only `learningApi.listUnits()`;
    - opening a unit calls `learningApi.listChapters(unitId)`;
    - opening a chapter calls `learningApi.listLessons(chapterId)`.
  - Unit cards now display chapter count before lesson data is loaded, avoiding fake `0/0` lesson progress.
  - Added `V14__learning_catalog_performance_indexes.sql`:
    - `learning_units(is_published, order_index)`;
    - `learning_chapters(unit_id, is_published, order_index)`;
    - `learning_lessons(chapter_id, is_published, order_index)`;
    - `practice_items(lesson_id, is_published, order_index)`;
    - `lesson_progress(user_key, lesson_id)`.
- Verification:
  - FE `npm.cmd run build` passed.
  - FE `npm.cmd run test` passed.
  - BE `mvn.cmd -q "-Dtest=FlywayMigrationTest" test` passed after updating expected applied migrations from 13 to 14.
- Expected impact:
  - Initial Courses load should no longer scale with total lesson count.
  - With 3k+ dictionary lessons, first render should depend mainly on the `/units` response.

---

## 2026-05-26 - Test Actor Accounts

- Scope:
  - Added deterministic login accounts for testing each main actor.
- What changed:
  - Added `V15__test_actor_accounts.sql`.
  - Added `CONTENT_REVIEWER` to `reference_roles`.
  - Seeded login users, premium subscription state, gamification profiles, and admin directory rows.
- Shared password:
  - `Vsign@123`
- Test accounts:
  - `learner.basic@vsign.test` - BASIC learner, role `USER`.
  - `learner.premium@vsign.test` - PREMIUM learner, role `USER`, active yearly subscription.
  - `admin@vsign.test` - admin staff, role `ADMIN`.
  - `superadmin@vsign.test` - platform owner/admin, role `SUPER_ADMIN`.
  - `reviewer@vsign.test` - content reviewer, role `CONTENT_REVIEWER`.
  - `inactive@vsign.test` - disabled learner account for login/access error testing.
- Verification:
  - FE was not involved.
  - Backend test execution was blocked in sandbox because Maven needed network access; the escalation run was not approved in the latest attempt.

---

## 2026-05-26 - Sidebar Logo Layout Polish

- Scope:
  - Frontend-only visual polish for the desktop app shell.
- What changed:
  - Updated `v-sign-fe/src/components/AppSidebar.tsx`.
  - Moved the V-Sign logo out of the rounded sidebar rail/card.
  - Enlarged the logo and kept the rounded rail dedicated to navigation/footer content.
- Verification:
  - FE `npm.cmd run build` passed.
  - FE `npm.cmd run test` passed.

---

## 2026-05-27 - Curated Greeting + Self-Introduction Units

- Scope:
  - Added two backend learning units from the requested vocabulary groups while keeping catalog data in Flyway migrations.
- What changed:
  - Added `V18__introductory_greetings_self_intro_units.sql`.
  - Created `unit-intro-greetings` with 4 beginner lessons.
  - Created `unit-self-introduction` with 7 beginner lessons.
  - Added matching `practice_items`, `practice_item_rubrics`, `lesson_quizzes`, `quiz_questions`, and `quiz_options` for the new lessons.
  - Updated `FlywayMigrationTest` expected counts:
    - published learning units: `10`;
    - published practice items: `97`;
    - published lesson quizzes: `14`;
    - applied Flyway versions: `18`.
- Dictionary/video mapping notes:
  - Rule: use exact labels first; use only semantically correct same-context equivalents; do not seed misleading lessons when no proper video exists.
  - `Xin chào` -> `chào` (`W00489.mp4`) because no exact `xin chào` label exists, and `chào` preserves greeting context.
  - `Cám ơn/Cảm ơn` -> not seeded because no exact or context-correct thank-you equivalent exists in the current dataset.
  - `Xin lỗi` -> exact `xin lỗi` (`W03990.mp4`).
  - `Tạm biệt` -> exact `tạm biệt` (`W03075.mp4`).
  - `Hẹn gặp lại` -> `tạm biệt` (`W03075.mp4`) because both are farewell/closing-context expressions; `hẹn` was rejected as too broad.
  - `Tên` -> `tên riêng` (`W03130N.mp4`) and `tên là gì?` (`W03129N.mp4`) for self-introduction/name context.
  - `Tuổi` / `Tôi tuổi` -> not seeded because current candidates are childhood/young-age concepts, not a general age/self-introduction sign.
  - `Sống` / `Tôi sống ở` -> `sinh sống` (`W02957.mp4`) plus `địa chỉ` (`D0001N.mp4`) for place-of-living context.
  - `Gặp` -> `gặp gỡ` (`W01395.mp4`).
  - `Tôi tên ...` -> covered by `tên riêng` and `tên là gì?`; no exact full-sentence sign exists.
  - `Rất vui được gặp bạn` -> split into valid components `vui mừng` (`W03889B.mp4`) and `bạn` (`W00110B.mp4`); no exact full-sentence sign exists.
- Verification:
  - `mvn.cmd -q "-Dtest=FlywayMigrationTest" test` passed.
  - `mvn.cmd -q test` passed.
