# V-Sign Pre-Deploy Holistic Refactor Plan

Created: 2026-06-10

Source documents:
- `docs/PRE_DEPLOY_COST_OPTIMIZATION_PLAN.md`
- `PRE_DEPLOY_PRODUCTION_READINESS_PLAN.md`

Decision locked by product/architecture:
- Keep MediaPipe Holistic full body. Do not replace it with Hand Landmarker or hand-only extraction.
- MediaPipe Holistic must run on the client side when possible.
- Frontend must not send raw webcam images, video files, or base64 frames to backend/AI service.
- AI service should receive only the current model's Holistic landmark sequence/features and return prediction metadata.
- Backend stores only prediction metadata: target sign, predicted sign, confidence, correctness, model version, status, timing.

Current source review result:
- AI service currently accepts `POST /predict` with base64 JPEG frames, then runs Python MediaPipe Holistic server-side.
- Current trained model input is `RAW_FEATURE_SIZE=258` per frame, then resampled to 60 frames and velocity is appended.
- Current model uses pose + left hand + right hand only. It does not use face landmarks.
- FE currently captures canvas JPEG frames in `src/services/aiRecognition.ts` and posts `{ frames }` to AI service.
- FE lesson flow currently has `video -> quiz -> ai-practice -> done`, but BE completion is still a generic progress update and does not verify quiz + AI pass.
- Payment/premium production work is deferred for this refactor cycle per latest direction.

---

## 1. Target Architecture

Current high-cost path to refactor:

```text
Browser webcam
  -> capture JPEG/base64 frames
  -> send frames to AI service
  -> server decodes images
  -> server runs MediaPipe/OpenCV
  -> model inference
  -> BE stores metadata
```

Target production path:

```text
Browser webcam
  -> MediaPipe Holistic full body in browser
  -> normalize full-body landmarks
  -> send compact landmark sequence to AI service
  -> AI model classifies landmark sequence
  -> FE sends prediction metadata to BE
  -> BE validates and stores metadata only
```

Required landmark scope:

```text
Current model contract:
- raw feature size per frame: 258
- pose: 33 landmarks * (x, y, z, visibility) = 132 values
- left hand: 21 landmarks * (x, y, z) = 63 values
- right hand: 21 landmarks * (x, y, z) = 63 values
- face landmarks: not used by the current model
- total: 132 + 63 + 63 = 258

Sequence contract:
- minimum frames before prediction: 5
- training/realtime target frames: 60
- missing pose/hand landmarks reuse previous frame values; initial previous values are zero
- EMA smoothing alpha: 0.7
- normalization: subtract pose landmark 0/nose x,y,z from pose/hand x,y,z values
- velocity: appended by model preprocessing, producing 516 values per prepared frame
```

Do not add face landmarks to the production prediction payload unless the model is retrained and `RAW_FEATURE_SIZE` changes. Keep MediaPipe Holistic full body because the source extractor is Holistic, but the payload sent to the current model must stay exactly pose + left hand + right hand in the order above.

---

## 2. Refactor Roadmap

Priority order:

| Phase | Priority | Scope | Goal |
| --- | --- | --- | --- |
| 0 | P0 | Audit and baseline | Know current cost, payload size, latency, mock/security risk |
| 1 | P0 | AI privacy/cost refactor | Move Holistic extraction to browser, send landmarks only |
| 2 | P0 | Learning pipeline hardening | Correct sample video -> quiz -> AI practice -> completion |
| 3 | P0 | Auth/security hardening | Remove dev shortcuts, protect APIs, production profile |
| 4 | P0 | Data/mock cleanup | Remove demo/test/prod-visible mocks and wrong video mappings |
| 5 | Deferred | Premium/payment correctness | Skip for this refactor cycle; keep as a separate monetization track |
| 6 | P1 | Media/CDN readiness | R2 custom domain, CORS, cache, video health check |
| 7 | P1 | Performance polish | Bundle splitting, lazy load Holistic, stable loading/error states |
| 8 | P1 | Deploy operations | Docker/resource limits, Caddy proxy, monitoring, smoke tests, rollback |
| 9 | P1 | CI/CD automation | GitHub Actions builds GHCR images and deploys by server-side pull |

---

## 3. Phase 0 - Audit And Baseline

### Tasks

- [ ] **BASE-01:** Record current AI request payload size for one prediction attempt.
- [ ] **BASE-02:** Record current AI latency p50/p95 with 20-50 prediction requests.
- [ ] **BASE-03:** Record AI service RAM idle/peak and backend RAM idle/peak.
- [ ] **BASE-04:** Confirm exact current AI model input contract: frame images, landmarks, feature vector, sequence length, labels.
- [ ] **BASE-05:** Confirm current trained MVP labels for 3 units: family, emotions, daily foods.
- [ ] **BASE-06:** Run static search for prod-visible mock/demo/test text.
- [ ] **BASE-07:** Create or update `docs/ops/baseline-cost-performance.md`.

### Acceptance

- [ ] Baseline metrics exist before refactor.
- [ ] We know whether current AI model can accept Holistic landmarks directly or needs an adapter/export.
- [ ] Known mocks/security gaps are listed before implementation.

---

## 4. Phase 1 - Client-Side MediaPipe Holistic Full Body

Current source state:
- `V-Sign-AI-Build/v-sign-be-ai/api_server.py` owns MediaPipe Holistic extraction server-side.
- `extract_landmarks_from_frame()` produces pose 132 + left 63 + right 63.
- `normalize_landmarks()` uses pose landmark 0/nose as origin.
- `model_v2.py` validates `RAW_FEATURE_SIZE=258`, resamples to 60 frames, appends velocity, and classifies with branch BiLSTM attention.
- `v-sign-fe/src/services/aiRecognition.ts` still captures JPEG frames and posts to `/predict`.

### FE Tasks

- [x] **AI-FE-01:** Add `src/services/holisticLandmarkExtractor.ts`.
- [x] **AI-FE-02:** Lazy-load MediaPipe Holistic only when user enters the AI practice step.
- [x] **AI-FE-03:** Extract exactly the current model feature order: pose 33*(x,y,z,visibility), left hand 21*(x,y,z), right hand 21*(x,y,z).
- [x] **AI-FE-04:** Do not include face landmarks in the payload for the current model.
- [x] **AI-FE-05:** Match current preprocessing: previous-frame interpolation for missing landmarks, EMA smoothing alpha 0.7, nose-based normalization.
- [x] **AI-FE-06:** Add adaptive sampling config, but keep enough frames for stable 60-frame resampling. Start from 2-3 seconds at 8-10 FPS; benchmark before lowering further.
- [x] **AI-FE-07:** Send compact `sequence: number[][]` where each frame has exactly 258 normalized values.
- [x] **AI-FE-08:** Keep `prepare_sequence()` server-side initially so target-frame resampling and velocity stay identical to current Python model code.
- [x] **AI-FE-09:** Stop webcam tracks and Holistic processing immediately when leaving AI step.
- [x] **AI-FE-10:** Replace current base64-frame request path with landmark-sequence request path.
- [x] **AI-FE-11:** Add a network guard/test to ensure AI requests do not contain `data:image`, `base64`, `jpeg`, or raw frame arrays.
- [ ] **AI-FE-12:** Add unsupported-browser state for devices that cannot run MediaPipe Holistic.
- [ ] **AI-FE-13:** Keep camera preview local only; never upload preview frames.

### AI Service Tasks

- [x] **AI-SVC-01:** Add endpoint `POST /predict-landmarks`.
- [x] **AI-SVC-02:** Validate request shape: `sequence.length >= 5`, every frame has exactly 258 numeric values, finite numeric bounds, optional target label.
- [x] **AI-SVC-03:** Reject payloads that contain raw frames/base64 fields.
- [x] **AI-SVC-04:** Add model version and label set version to prediction response.
- [ ] **AI-SVC-05:** Add request timeout, concurrency limit, and payload size limit.
- [x] **AI-SVC-06:** Ensure logs contain only metadata, never raw landmarks if logs become too large/sensitive.
- [x] **AI-SVC-07:** Reuse existing `prepare_sequence()` and model forward path for `/predict-landmarks`.
- [x] **AI-SVC-08:** Keep old `/predict` only as temporary dev fallback, disabled in production.

### BE Tasks

- [x] **AI-BE-01:** Validate prediction metadata before storing attempt log.
- [ ] **AI-BE-02:** Store model version/label version if columns already exist; otherwise add a small migration.
- [x] **AI-BE-03:** Do not accept raw frame/video fields in any backend AI attempt endpoint.
- [ ] **AI-BE-04:** Rate-limit AI attempt metadata logging per user/session.

### Acceptance

- [ ] Browser DevTools Network shows only landmark/features sent to AI.
- [ ] No raw webcam image/video/base64 leaves frontend.
- [ ] MediaPipe Holistic remains the extraction mechanism.
- [ ] Prediction payload is exactly `[N, 258]` before server-side resampling/velocity.
- [ ] AI success/wrong/low-confidence/service-offline states still work.
- [ ] MVP 3 AI units can complete the full learning flow.

---

## 5. Phase 2 - Learning Pipeline Hardening

Current source state:
- FE `LessonStudyModal` already has four UI steps: `video -> quiz -> ai-practice -> done`.
- FE `QuizPanel` supports both modes depending on option data: prompt video -> choose text, or prompt text -> choose video.
- FE `DynamicQuizPanel` is a local fallback generated from dictionary entries when backend lesson quiz is missing. This is not server-authoritative.
- FE AI target is resolved from lesson title/aliases through `resolveAiPracticeTarget()`. Wrong title/alias mapping can block completion or target the wrong label.
- FE marks completion by calling `learningApi.updateProgress(... status: "COMPLETED")`, then does optimistic local `completeLesson()`.
- BE `updateProgress()` currently accepts client-supplied `completionPct`, `phase`, and `status`; it does not verify video watched, quiz passed, or AI passed.
- BE quiz attempts are created/submitted without user ownership binding in the current service flow.

Required lesson flow after review:

```text
1. Sample video
2. Quiz A: show text, choose correct video
3. Quiz B: show video, choose correct text
4. AI practice with Holistic landmarks
5. Summary and server-side completion
```

### Review Tasks Before Editing

- [x] **LEARN-REVIEW-01:** Inventory current lesson flow in `VocabularyPack.tsx` and confirm which parts can be kept.
- [x] **LEARN-REVIEW-02:** Inventory backend quiz schema/options from migrations and `QuizAttemptService`.
- [x] **LEARN-REVIEW-03:** Confirm every MVP lesson title maps to an AI label in `AI_PRACTICE_TARGETS`.
- [x] **LEARN-REVIEW-04:** Confirm each MVP lesson has backend quiz data with both required question modes where possible.
- [x] **LEARN-REVIEW-05:** Confirm how FE currently updates XP/streak locally through `AuthContext.completeLesson()`.
- [x] **LEARN-REVIEW-06:** Confirm BE has enough persisted data to verify quiz pass + AI pass before completion. If not, add a minimal completion endpoint design first.

### Implementation Tasks After Review

- [ ] **LEARN-FE-01:** Keep existing modal/step structure if possible; refactor internals instead of rebuilding the page.
- [x] **LEARN-FE-02:** Remove or dev-gate `DynamicQuizPanel`; production completion must not depend on local-generated quiz.
- [ ] **LEARN-FE-03:** Use backend quiz options with real dictionary/R2 video URLs.
- [ ] **LEARN-FE-04:** Ensure each lesson includes both quiz modes: text -> video and video -> text.
- [ ] **LEARN-FE-05:** Wrong quiz choices block progression until retry/pass.
- [ ] **LEARN-FE-06:** AI step is required for trained MVP labels.
- [ ] **LEARN-FE-07:** AI bypass is visible only if BE returns `aiSupported=false` for that lesson.
- [x] **LEARN-FE-08:** Remove optimistic completion/XP/streak as the source of truth; hydrate from BE after completion.
- [x] **LEARN-FE-09:** Replace final FE `updateProgress(... COMPLETED)` with `POST /api/v1/lessons/{lessonId}/complete`; local completion may update UI only after BE success.
- [x] **LEARN-FE-10:** Send authenticated token when creating/submitting lesson quiz attempts so BE can bind quiz pass to the learner.
- [x] **LEARN-BE-01:** Add a server-side lesson completion command endpoint or harden progress update to verify required stages.
- [x] **LEARN-BE-02:** Completion verifies: lesson video stage reached, latest quiz attempt passed, latest AI attempt passed for the expected practice item.
- [x] **LEARN-BE-03:** Bind quiz attempts to authenticated user/session before using them for completion.
- [x] **LEARN-BE-03A:** Add `quiz_attempts.user_key` and indexes for verified completion lookup.
- [x] **LEARN-BE-03B:** Reject client attempts to mark progress `COMPLETED` through the generic progress endpoint; completion must use the verified command.
- [x] **LEARN-BE-03C:** Add integration tests for: no-auth completion, missing quiz/AI rejection, direct progress completion rejection, and successful quiz+AI completion.
- [ ] **LEARN-BE-04:** XP award must be idempotent by user + lesson + event.
- [ ] **LEARN-BE-05:** Streak calculation stays server-side UTC+7 and must not reset 7 -> 1 after same-day completion.
- [ ] **LEARN-BE-06:** Premium locks are enforced server-side from active subscription.

### Acceptance

- [ ] Basic user cannot complete premium content by direct route.
- [ ] Premium user sees premium lessons unlocked after login/subscription hydrate.
- [ ] Streak remains correct for continuous learning days.
- [ ] Lesson completion never happens before quiz + AI requirements are satisfied.
- [ ] Current working UI structure is preserved unless source review proves a rewrite is lower risk.

---

## 6. Phase 3 - Auth And Security Hardening

Current source state:
- `SecurityConfig` is authenticated-by-default with only intended public read/login endpoints permitted.
- `SubscriptionService.createDefaultSubscription()` no longer auto-activates premium by email naming.
- `application-prod.properties` disables SQL debug and Swagger/OpenAPI in production.
- Runtime secrets are environment-driven; `secretKey.properties` was removed from `src/main/resources`, while `secretKey-test.properties` contains isolated test JWT values only.

### Tasks

- [x] **SEC-BE-01:** Remove email-based premium shortcut.
- [x] **SEC-BE-02:** Replace broad `permitAll` with authenticated-by-default security.
- [x] **SEC-BE-03:** Permit only true public endpoints: login/register/public catalog if intended.
- [x] **SEC-BE-04:** Protect progress, quiz, signature attempt, payment, subscription, profile, admin endpoints.
- [x] **SEC-BE-05:** Add ownership checks for progress/payment/subscription data.
- [x] **SEC-BE-06:** Add production profile with SQL debug off and env-only secrets.
- [x] **SEC-BE-07:** Protect or disable Swagger in production.
- [x] **SEC-FE-01:** Remove demo account copy from login UI.
- [x] **SEC-FE-02:** Remove local premium/role overrides.
- [x] **SEC-FE-03:** Logout clears local session and calls backend revoke/logout if implemented.

### Acceptance

- [ ] Unauthenticated users cannot submit progress, quiz, AI attempt, payment order.
- [ ] Premium cannot be activated by localStorage/email naming.
- [ ] Swagger is not public in production unless protected.

---

## 7. Phase 4 - Data And Mock Cleanup

Current source state:
- `V15__test_actor_accounts.sql` seeds test actors into normal migration flow.
- `V3__dictionary_entries.sql` still seeds English/demo dictionary rows with `cdn.vsign.test`.
- `V6__gamification.sql`, `V7__monetization.sql`, and `V8__admin_state.sql` still contain demo/test users, payments, review docs.
- `V17`, `V18`, and `V19` still contain `pub-*.r2.dev` media URLs; public R2 URLs are accepted for the current product deployment.
- `LoginModal.tsx` no longer exposes demo local account copy.
- `PremiumModal.tsx` still contains contract-ready payment copy.

### Tasks

- [ ] **DATA-01:** Move test actor account seed out of production migration.
- [ ] **DATA-02:** Remove shared test password accounts from production seed.
- [ ] **DATA-03:** Remove demo payment/admin/review rows from production seed.
- [ ] **DATA-04:** Remove old English demo dictionary entries if still visible.
- [ ] **DATA-05:** Normalize dictionary ID sequence after bulk import.
- [x] **DATA-06:** Create script to scan DB/source for `vsign.test`, `pay.vsign.test`, `cdn.vsign.test`, and `localhost`. Public `r2.dev` media URLs are allowed for the current release.
- [ ] **DATA-07:** Video mapping must be exact semantic match from label/source. If not exact, mark `NEEDS_VIDEO`; do not map to similar-but-wrong words.
- [ ] **DATA-08:** Keep regional variants explicit: B, T, N, BT, NT, or national/default.

### Acceptance

- [ ] Production source has no user-visible mock/demo/test data.
- [ ] Main 3 MVP units use correct videos and correct regional context.
- [ ] Missing videos are explicit, not silently substituted.

---

## 8. Phase 5 - Payment And Premium Correctness - Deferred

Status:
- Deferred for this refactor cycle by current direction.
- Do not implement payment gateway work before AI + learning MVP flow is stable.
- Still keep known production blockers documented so they are not forgotten.

Current source state:
- `PaymentService.createOrder()` still creates synthetic transaction IDs/deep links/QR URLs.
- FE `PremiumModal` still contains payment contract-ready/simulated UX paths.
- `PaymentService.history()` is scoped by authenticated user email.
- `PaymentService.createOrder()` accepts request amount when present instead of always trusting plan price.

### Deferred Tasks

- [ ] **PAY-01:** Add `PaymentGatewayClient` abstraction for MoMo/ZaloPay.
- [ ] **PAY-02:** Backend price comes from `subscription_plans`, not FE amount.
- [x] **PAY-03:** Payment orders require authenticated user.
- [ ] **PAY-04:** Webhook verifies signature, timestamp, amount, transaction ID.
- [ ] **PAY-05:** Webhook is idempotent by provider transaction ID.
- [ ] **PAY-06:** Paid order activates `user_subscriptions`.
- [ ] **PAY-07:** FE refreshes `/me` and `/me/subscription` after payment status changes.
- [ ] **PAY-08:** Remove simulated payment branch from production.

### Acceptance

- [ ] Invalid/duplicate payment webhook does not activate premium twice.
- [ ] Paid user unlocks premium without manual local override.

---

## 9. Phase 6 - Media, R2, CDN

Current source state:
- MVP videos are currently referenced through public `pub-aaf79542519744cfaf424549ccf4588c.r2.dev` URLs in migrations, and this is acceptable for the current product deployment.
- There is no verified custom media domain in source yet; a custom domain is optional post-release hardening, not a blocker.
- Source mappings should preserve regional variants: B, T, N, BT, NT, and national/default.

### Tasks

- [x] **MEDIA-01:** Accept public `pub-*.r2.dev` production media URLs for the current release; custom domain migration is deferred.
- [ ] **MEDIA-02:** Configure R2 CORS only for staging/prod FE domains.
- [ ] **MEDIA-03:** Set cache headers for versioned MP4 files.
- [ ] **MEDIA-04:** Add script to verify every course/dictionary video URL returns 200 or 206 and `video/mp4`.
- [ ] **MEDIA-05:** Add preload only for current lesson video, not entire course.

### Acceptance

- [x] Course video load may use the public R2 media domain for the current release.
- [ ] Slow or missing videos show a useful error state.

---

## 10. Phase 7 - Frontend Performance And UX

Current source state:
- `v-sign-fe/package.json` does not currently include a MediaPipe web dependency.
- AI camera code is currently bundled through normal route/component imports.
- `lovable-tagger` is present as a dev dependency and Vite plugin only in development mode.
- Several UI strings are still mojibake/encoding-corrupted in source output.
- Vite dev proxy maps `/ai` to localhost AI service; production env must override this.

### Tasks

- [ ] **FE-PERF-01:** Lazy-load Dictionary, Assessment, AI camera, Payment modal.
- [ ] **FE-PERF-02:** Lazy-load MediaPipe Holistic only in AI practice.
- [ ] **FE-PERF-03:** Add global error boundary.
- [ ] **FE-PERF-04:** Fix all remaining encoding/mojibake UI strings.
- [ ] **FE-PERF-05:** Use consistent loading skeleton/spinner, no blank page while API loads.
- [ ] **FE-PERF-06:** Verify responsive layout on desktop/tablet/mobile.
- [ ] **FE-PERF-07:** Production env must not reference localhost.

### Acceptance

- [ ] `npm run build` passes.
- [ ] Main routes do not blank-screen on API failure.
- [ ] Initial bundle does not include Holistic unless AI route is used.

---

## 11. Phase 8 - Backend/AI Runtime And Deploy Ops

Current source state:
- Backend has `application-prod.properties` in `src/main/resources`.
- Backend prod profile disables SQL debug and Swagger/OpenAPI, and enables Flyway validation.
- AI service has `requirements.txt` with heavy server-side packages: MediaPipe, OpenCV, Torch, matplotlib, pandas, scikit-learn.
- Backend, frontend, and AI service have production Dockerfiles.
- Production Compose runs backend, AI, and Caddy, with Caddy serving the frontend and reverse-proxying `/api/v1` and `/ai`.
- Production Compose pulls immutable GHCR image tags instead of building application images on the server.

### Tasks

- [x] **OPS-01:** Add/verify backend `application-prod.properties`.
- [x] **OPS-02:** Add backend health endpoint.
- [x] **OPS-03:** Add AI health and version endpoints.
- [x] **OPS-04:** Add Dockerfiles and production Docker Compose.
- [x] **OPS-05:** Add memory/CPU limits so AI cannot take backend down.
- [x] **OPS-06:** Add Caddy reverse proxy body-size limits for `/api/v1` and `/ai`.
- [x] **OPS-07:** Add DB backup/restore runbook.
- [x] **OPS-08:** Add deployment smoke test checklist.
- [ ] **OPS-09:** Add production request-rate protection through Cloudflare/WAF or a Caddy rate-limit plugin if traffic/abuse requires it.

Recommended low-cost JVM guardrail:

```text
JAVA_TOOL_OPTIONS=-Xms256m -Xmx768m -XX:+UseContainerSupport
```

Recommended service resource direction:

```text
backend: 1 CPU, 768 MB to 1 GB RAM
ai: 2 CPU, 1-2 GB RAM depending on model runtime
proxy: 0.25 CPU, 256 MB RAM
```

### Acceptance

- [ ] Backend stays healthy when AI restarts.
- [ ] AI endpoint has request body protection and a documented request-rate mitigation path.
- [ ] Deploy can be rolled back with a known checklist.

---

## 12. Phase 9 - GitHub Actions, GHCR, And Server Pull Deploy

Current source state:
- Root `.github/workflows/deploy.yml` runs tests, builds backend/AI/frontend images, pushes them to GHCR, then SSHes into the server.
- Root `docker-compose.prod.yml` uses `ghcr.io/${GHCR_OWNER}/vsign-*:${IMAGE_TAG}` images.
- Root `Caddyfile` is the production reverse proxy config.
- `v-sign-be/docs/deployment/github-actions-ghcr-caddy-runbook.md` documents AWS bootstrap, GitHub secrets, first deploy, and rollback.

### Tasks

- [x] **CICD-01:** Use GHCR image naming for backend, AI, and frontend/Caddy images.
- [x] **CICD-02:** Add GitHub Actions test/build workflow for backend, frontend, and AI.
- [x] **CICD-03:** Push immutable commit SHA tags and `latest` tags to GHCR.
- [x] **CICD-04:** Add deploy job that SSHes into the server, uploads deploy files, pulls GHCR images, and runs Docker Compose.
- [x] **CICD-05:** Ensure production Compose pulls images from GHCR and does not build application images on the server.
- [x] **CICD-06:** Document required GitHub secrets, server env files, DNS, Docker install, swap, and rollback.
- [ ] **CICD-07:** Provision AWS EC2 instance, security group, Docker, swap, DNS, and `/opt/vsign/.env.prod`.
- [ ] **CICD-08:** Add GitHub repository secrets for the real server.
- [ ] **CICD-09:** Run first server deploy and complete smoke test checklist.

### Acceptance

- [ ] A push to `main` can deploy backend, AI, and frontend to the server without manual build commands.
- [ ] The server only stores deploy config and env files; application code is not required on the server.
- [ ] Rollback can select a previous GHCR `sha-*` tag.
- [ ] Caddy serves HTTPS for the configured `APP_DOMAIN`.

---

## 13. Cost Optimization Rules Specific To Holistic

- [ ] Load Holistic only at AI step.
- [ ] Run Holistic on client; server should not decode images or run MediaPipe for production path.
- [ ] Send only the current 258-value pose/hand feature vector per frame; do not send face landmarks for the current model.
- [ ] Use adaptive FPS: start 6-8 FPS, reduce on low-end devices.
- [ ] Cap sequence length and duration.
- [ ] Quantize/round landmark values before sending if accuracy is stable.
- [ ] Stop camera and animation loops on route change/modal close.
- [ ] Do not continuously predict; predict only after user starts an attempt.
- [ ] Add cooldown between attempts.
- [ ] Limit free-user AI attempts per day/session.
- [ ] Keep a feature flag to disable AI if service is unavailable.

---

## 14. Test Matrix

### Backend

- [ ] `mvn test`
- [ ] Flyway migrate on clean staging DB.
- [ ] Auth: register/login/protected endpoint access.
- [ ] Learning: stage validation, premium lock, completion idempotency.
- [ ] Gamification: XP idempotency, streak UTC+7.
- [ ] Payment: deferred in this refactor cycle; run existing tests only, do not implement real gateway now.
- [ ] AI metadata: success, wrong sign, low confidence, unsupported, no raw data persisted.

### Frontend

- [ ] `npm run lint`
- [ ] `npm run build`
- [ ] Basic user: login -> free lesson -> quiz -> AI -> completion.
- [ ] Premium user: login -> premium lesson unlocked.
- [ ] Dictionary: search/filter/open video/missing video.
- [ ] AI: camera allowed, camera blocked, service offline, low confidence, success.
- [ ] Network assertion: no base64/image/video payload sent to AI/BE.
- [ ] Learning flow assertion: no lesson can reach completion without backend quiz pass + AI pass.

### Media

- [ ] R2/custom domain returns 200/206 for MVP videos.
- [ ] Content type is `video/mp4`.
- [ ] Missing video state is visible and non-crashing.

---

## 15. Release Gates

- [ ] **Gate 1 - Internal QA:** Phase 1-4 P0 tasks done and local tests pass. Phase 5 is explicitly deferred.
- [ ] **Gate 2 - Staging:** Fresh DB migration, staging env, media domain, E2E pass. Payment sandbox only if Phase 5 is pulled back into scope.
- [ ] **Gate 3 - Beta:** Limited real-user test, monitoring enabled, backup verified.
- [ ] **Gate 4 - Production:** No P0/P1 blockers, secrets rotated, rollback plan ready.

---

## 16. Immediate Implementation Order

Recommended next implementation sequence:

1. **LEARN-REVIEW-01 to LEARN-REVIEW-06:** Review current learning flow and confirm what to keep before editing.
2. **AI-FE-01 to AI-FE-13:** Replace base64 frame upload with client-side Holistic 258-feature landmarks.
3. **AI-SVC-01 to AI-SVC-08:** Add `/predict-landmarks` and enforce payload rules.
4. **LEARN-FE-01 to LEARN-BE-06:** Lock correct lesson flow and streak/XP completion.
5. **DATA-06 to DATA-08:** Verify MVP 3-unit videos and regional mapping.
6. **SEC-BE-01 to SEC-FE-03:** Remove auth/security production blockers.
7. **FE-PERF-01 to FE-PERF-05:** Lazy load AI/large routes and fix loading/encoding.
8. **OPS-01 to OPS-09:** Prepare production runtime and smoke checks.
9. **CICD-01 to CICD-09:** Prepare GitHub Actions, GHCR, Caddy, and AWS server pull-deploy automation.

Do not start payment production integration until the learning + AI MVP path is stable, unless premium monetization is required for the first release.

---

## 17. Session Log

- **2026-06-10:** Consolidated cost optimization and production readiness documents into one Holistic-first refactor plan. Important correction: MediaPipe Holistic full body is retained and must run client-side; the production path must send landmarks/features only, not raw frames.
- **2026-06-10 Source review:** Re-reviewed AI/FE/BE source. Corrected landmark scope to the current model contract: 258 values per frame, pose + left hand + right hand, no face landmarks. Marked Phase 5 payment/premium gateway work as deferred. Updated Phase 2 to start with current learning-flow review before implementation because FE already has `video -> quiz -> ai-practice -> done`, while BE completion still lacks quiz/AI verification.
- **2026-06-11 Phase 2 tracking update:** Confirmed current BE has persisted quiz attempts and AI attempt logs, but quiz attempts were not user-bound and `updateProgress()` still allowed FE to set `COMPLETED`. Implementation now focuses on a verified `POST /api/v1/lessons/{lessonId}/complete` endpoint, `quiz_attempts.user_key`, direct-completion blocking in progress update, FE completion call replacement, and BE integration tests.
- **2026-06-11 Phase 1 implementation:** Added client-side `@mediapipe/holistic` extraction in `v-sign-fe/src/services/holisticLandmarkExtractor.ts`, changed FE AI calls to `POST /predict-landmarks` with `[N,258]` landmark sequences only, added a FE network guard test, added AI service `/predict-landmarks` validation/metadata response, and added backend signature-attempt raw payload rejection. Verified with `npm run test -- aiRecognition`, `npm run build`, `python -m py_compile api_server.py`, and `mvn -q -DskipTests compile`.
- **2026-06-11 Phase 2 implementation:** Added verified `POST /api/v1/lessons/{lessonId}/complete`, blocked generic progress `COMPLETED`, added `quiz_attempts.user_key` migration/indexes, bound quiz attempts to auth user, required same-user quiz pass plus same-user AI pass for lesson practice items, dev-gated local dynamic quiz fallback, and changed FE completion to call the verified endpoint only after AI success. Verified with `mvn -q -Dtest=LearningWorkflowIT test`, `mvn -q -DskipTests compile`, `npm run build`, `npm run test -- aiRecognition`, and `python -m py_compile api_server.py`.
- **2026-06-11 Phase 3 partial implementation:** Removed email-name premium auto-activation in `SubscriptionService`, removed demo account copy from `LoginModal`, and stopped hydrating premium/subscription from localStorage on app startup. Full authenticated-by-default `SecurityConfig` tightening remains open because public catalog vs protected workflow route policy needs a dedicated pass. Verified with `mvn -q -Dtest=SubscriptionControllerIT test`, `mvn -q -DskipTests compile`, and `npm run build`.
- **2026-06-11 Phase 3 security tightening pass:** Updated `SecurityConfig` to authenticated-by-default, keeping only auth, dictionary/catalog reads, practice-item reads, plan reads, and assessment reads public. Protected lesson quiz/progress/complete, quiz attempts, signature workflows, payments, checkout, me, gamification, leaderboards, and admin routes. Updated FE payment order creation to send bearer token and aligned affected integration tests with authenticated workflow policy.
- **2026-06-12 verification update:** Verified the Holistic + learning completion + security tightening changes with `mvn -q -Dtest=LearningWorkflowIT test`, `mvn -q -Dtest=SubscriptionControllerIT test`, `mvn -q -DskipTests compile`, `npm run build`, `npm run test -- aiRecognition`, and `python -m py_compile api_server.py`. Remaining security/ops items stay open in the checklist, especially ownership hardening for payment/subscription data, production profile/secrets, Swagger production gating, and deployment runbooks.
- **2026-06-12 ownership hardening:** Completed SEC-BE-05 for monetization paths. Payment order creation now binds `user_email` from the authenticated principal, payment status lookup requires matching transaction id plus owner email, `/me/payments` returns only the current user's orders, and checkout intents use the authenticated principal as owner instead of trusting request `userId`. Added regression coverage for cross-user payment status access and user-scoped payment history. Verified with `mvn -q -Dtest=SubscriptionControllerIT test` and `mvn -q -DskipTests compile`.
- **2026-06-12 production profile hardening:** Completed SEC-BE-06, SEC-BE-07, and OPS-01 for backend config. Removed runtime `secretKey.properties` from source resources, kept only isolated test JWT values in `secretKey-test.properties`, added `application-prod.properties` with SQL debug off, Flyway validation on, and Swagger/OpenAPI disabled. Ran `mvn -q clean compile` to clear stale copied resources and verified with `mvn -q -Dtest=SubscriptionControllerIT test`.
- **2026-06-12 FE logout cleanup:** Completed SEC-FE-03 for the current auth model. Logout clears token, logged-in flag, profile, onboarding, stats, reminder, premium/subscription legacy keys, payment history, and reward state. No backend revoke/logout endpoint exists yet, so no revoke call was added. Verified with `npm run build`.
- **2026-06-12 data marker scanner:** Completed DATA-06 with `v-sign-be/scripts/scan-predeploy-markers.ps1`. The script scans source/config/migrations while excluding build artifacts, docs, dependencies, virtualenvs, and caches; it summarizes marker counts and fails by default unless run with `-NoFail`. Public `r2.dev` media URLs are now allowed for the current release, so the scanner focuses on remaining mock/dev markers such as `vsign.test`, `pay.vsign.test`, `cdn.vsign.test`, `localhost`, and test/demo accounts.
- **2026-06-12 media deployment decision:** Public Cloudflare R2 `pub-*.r2.dev` URLs are accepted for the current product deployment. MEDIA-01 is marked complete for this release scope; custom media domain migration can be revisited later as post-release hardening.
- **2026-06-12 health/version endpoints:** Completed OPS-02 and OPS-03. Backend now exposes public `GET /api/v1/health` and `GET /api/v1/version`, with security allow-list coverage and integration test `HealthControllerIT`. AI service keeps `/health`, now includes API/model/label metadata, and adds `GET /version`. Verified with `mvn -q -Dtest=HealthControllerIT test`, `mvn -q -DskipTests compile`, and `python -m py_compile api_server.py`.
- **2026-06-12 Docker deploy scaffold:** Completed OPS-04, OPS-05, and OPS-06. Added Dockerfiles for backend, frontend, and AI service, `.dockerignore` files, root `docker-compose.prod.yml` with CPU/memory limits, optional server-side `.env.prod`, and reverse proxy rules for `/api/v1` and `/ai`. The initial proxy scaffold was later replaced with Caddy and GHCR pull-deploy automation. Verified compose syntax with `docker compose -f docker-compose.prod.yml config`, plus `mvn -q -DskipTests compile`, `npm run build`, and `python -m py_compile api_server.py`.
- **2026-06-12 deployment runbooks:** Completed OPS-07 and OPS-08 with `v-sign-be/docs/deployment/db-backup-restore-runbook.md` and `v-sign-be/docs/deployment/deployment-smoke-test-checklist.md`. The runbooks cover PostgreSQL backup/restore, rollback readiness, service health/version checks, auth, learning flow, AI landmark requests, media, monetization, and rollback records.
- **2026-06-12 GitHub Actions + GHCR + Caddy deploy automation:** Added root `.github/workflows/deploy.yml` so push to `main` runs tests, builds backend/AI/frontend images, pushes them to GHCR, and deploys through SSH by pulling images on the server. Replaced the frontend proxy image with Caddy, added production `Caddyfile`, changed `docker-compose.prod.yml` to pull GHCR images, added deploy env examples, and documented AWS bootstrap/secrets/rollback in `github-actions-ghcr-caddy-runbook.md`. Server provisioning, real GitHub secrets, and first AWS smoke deploy remain pending.
- **2026-06-12 CI/CD validation:** Moved deploy entry files to root to match the intended GitHub repository structure: `docker-compose.prod.yml`, `Caddyfile`, `.env.deploy.example`, and `.env.ai.prod.example`. Verified Compose config with `docker compose --env-file .env.deploy.example -f docker-compose.prod.yml config`, backend targeted tests with `mvn.cmd -q "-Dtest=LearningWorkflowIT,SubscriptionControllerIT,HealthControllerIT" test`, FE test/build with `npm.cmd run test -- aiRecognition` and `npm.cmd run build`, and AI syntax with `python -m py_compile api_server.py`. FE build still emits the existing large chunk warning tracked under Phase 7.
