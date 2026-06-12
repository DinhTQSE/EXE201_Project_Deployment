# Superpower Orchestrator Traceability Report

## Source Request

# Backend Epic Implementation Plan Request (V-SIGN)

Reference baseline (must follow):
- EXE101_Project_V-Sign_BE/BE-init.md

Primary objective:
- Produce a deterministic backend implementation plan per epic for V-SIGN with Java Spring Boot 3.x architecture, JWT security, Flyway migrations, and centralized exception handling as specified in BE-init.md.

Planning context documents (must be used):
- EXE101_Project_V-Sign_BE/docs/superpowers/working/2026-05-21-vsign-userstories-from-docx.md
- docs/US_Frontend_Verification_Report_v2.md

Mandatory planning requirements:
1. Derive backend epics from user stories and map story-to-epic coverage.
2. Define API contracts per epic (endpoint, method, auth role, request DTO, response DTO, validation/error codes).
3. Verify backend API expectations against frontend verification report findings; call out mismatches, missing fields, and contract changes required for FE alignment.
4. Include database and migration implications per epic.
5. Sequence implementation by dependencies and risk.
6. Include test strategy gates per epic (unit/integration/security/contract).
7. Explicitly list assumptions when project details are missing.

Deliverable orientation:
- planning-brief.md should be directly actionable for implementation planning.
- task-graph.json should support incremental delivery with parallelizable tasks when safe.

## Planning Brief

# Planning Brief

## Planning Intent

# Backend Epic Implementation Plan Request (V-SIGN)

Reference baseline (must follow):
- EXE101_Project_V-Sign_BE/BE-init.md

Primary objective:
- Produce a deterministic backend implementation plan per epic for V-SIGN with Java Spring Boot 3.x architecture, JWT security, Flyway migrations, and centralized exception handling as specified in BE-init.md.

Planning context documents (must be used):
- EXE101_Project_V-Sign_BE/docs/superpowers/working/2026-05-21-vsign-userstories-from-docx.md
- docs/US_Frontend_Verification_Report_v2.md

Mandatory planning requirements:
1. Derive backend epics from user stories and map story-to-epic coverage.
2. Define API contracts per epic (endpoint, method, auth role, request DTO, response DTO, validation/error codes).
3. Verify backend API expectations against frontend verification report findings; call out mismatches, missing fields, and contract changes required for FE alignment.
4. Include database and migration implications per epic.
5. Sequence implementation by dependencies and risk.
6. Include test strategy gates per epic (unit/integration/security/contract).
7. Explicitly list assumptions when project details are missing.

Deliverable orientation:
- planning-brief.md should be directly actionable for implementation planning.
- task-graph.json should support incremental delivery with parallelizable tasks when safe.

## Context Mode

- Mode: fullstack
- Repo Scan Enabled: yes
- Explicit Docs: 3
- Explicit Repos: 2
- Domain Anchors: authentication, user profile, document upload, API contract, PostgreSQL, validation, testing, delivery roadmap, epic/user story alignment

## Knowledge Base Inputs

- writing-plan-principles.md: # Writing Plan Principles - Start by clarifying the user outcome, scope boundary, and definition of done. - Prefer explicit assumptions over hidden assumptions. - Organize plans into concrete phases that can be valida...
- task-decomposition-principles.md: # Task Decomposition Principles - Break the planning problem into a small graph of worker tasks with clear ownership. - Separate evidence gathering, architecture, risk review, and final synthesis. - Pass only the cont...
- risk-audit-checklist.md: # Risk Audit Checklist - Check for missing assumptions, context overload, and ambiguous ownership. - Check for optional inputs becoming accidental prerequisites. - Check whether the merge output can be audited without...
- verification-checklist.md: # Verification Checklist - Verify that generated artifacts exist and are internally consistent. - Verify that worker outputs include their required sections. - Verify that the final plan includes a main-agent audit an...
- backend-planning-guide.md: # Backend Planning Guide - Capture service boundaries, APIs, persistence, validation, and deployment constraints. - Highlight framework/runtime assumptions only when they are explicitly known. - Include integration ri...
- frontend-planning-guide.md: # Frontend Planning Guide - Capture UX flows, component boundaries, state ownership, and testing surfaces. - Distinguish between product requirements and implementation details. - Prefer plans that are incremental and...
- fullstack-planning-guide.md: # Fullstack Planning Guide - Align frontend, backend, and integration work around a single delivery sequence. - Make interface contracts explicit before implementation tasks are assigned. - Reserve cross-cutting valid...
- architecture-planning-guide.md: # Architecture Planning Guide - Identify the runtime boundaries, context boundaries, and ownership boundaries. - Prefer extension of native project systems over parallel frameworks. - Record tradeoffs so the final pla...
- implementation-roadmap-guide.md: # Implementation Roadmap Guide - Sequence work into a thin vertical slice first, then follow with expansion tasks. - Pair each implementation phase with validation criteria. - Keep roadmap steps small enough for agent...

## Knowledge Base Excerpts

### writing-plan-principles.md
# Writing Plan Principles

- Start by clarifying the user outcome, scope boundary, and definition of done.
- Prefer explicit assumptions over hidden assumptions.
- Organize plans into concrete phases that can be validated independently.
- Keep outputs deterministic, reviewable, and concise enough for downstream agents to consume.

### task-decomposition-principles.md
# Task Decomposition Principles

- Break the planning problem into a small graph of worker tasks with clear ownership.
- Separate evidence gathering, architecture, risk review, and final synthesis.
- Pass only the context each worker needs to reduce drift.
- Preserve a stable merge order for deterministic final output.

### risk-audit-checklist.md
# Risk Audit Checklist

- Check for missing assumptions, context overload, and ambiguous ownership.
- Check for optional inputs becoming accidental prerequisites.
- Check whether the merge output can be audited without reading every worker artifact.

### verification-checklist.md
# Verification Checklist

- Verify that generated artifacts exist and are internally consistent.
- Verify that worker outputs include their required sections.
- Verify that the final plan includes a main-agent audit and references the planning brief.

### backend-planning-guide.md
# Backend Planning Guide

- Capture service boundaries, APIs, persistence, validation, and deployment constraints.
- Highlight framework/runtime assumptions only when they are explicitly known.
- Include integration risks, data contracts, and verification gates.

### frontend-planning-guide.md
# Frontend Planning Guide

- Capture UX flows, component boundaries, state ownership, and testing surfaces.
- Distinguish between product requirements and implementation details.
- Prefer plans that are incremental and independently reviewable.

### fullstack-planning-guide.md
# Fullstack Planning Guide

- Align frontend, backend, and integration work around a single delivery sequence.
- Make interface contracts explicit before implementation tasks are assigned.
- Reserve cross-cutting validation for the main-agent audit pass.

### architecture-planning-guide.md
# Architecture Planning Guide

- Identify the runtime boundaries, context boundaries, and ownership boundaries.
- Prefer extension of native project systems over parallel frameworks.
- Record tradeoffs so the final plan is traceable.

### implementation-roadmap-guide.md
# Implementation Roadmap Guide

- Sequence work into a thin vertical slice first, then follow with expansion tasks.
- Pair each implementation phase with validation criteria.
- Keep roadmap steps small enough for agent execution or human review.

## Instruction Excerpts

- - REST API endpoints
- - Input validation
- - Validation rules
- - JWT authentication integration
- - PostgreSQL database
- - JWT authentication and authorization
- - RESTful API design
- - Use PostgreSQL
- - Explain how the Spring Boot application connects to PostgreSQL
- Authentication requirements:
- - Implement JWT-based authentication
- - Include login API

## Relevant User Stories

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
- AC3: Earned badges are displayed in a grid on the profile page.
- US-05 â€” Chá»‰nh sá»­a Profile
- AC2: Given user uploads a new avatar (JPG/PNG â‰¤ 2MB) â†’ image saved to S3, displayed on profile.
- US-06 â€” Äá»•i máº­t kháº©u
- AC4: Given new password < 8 characters â†’ validation error shown.
- US-07 â€” ÄÄƒng xuáº¥t
- US-08 â€” QuÃªn máº­t kháº©u
- EPIC 2: Content Management (Learning)


## Planning Directives

- Use the knowledge base to shape the plan before decomposing worker tasks.
- Preserve instruction, user story, and frontend verification traceability in every downstream output.
- Prefer concrete backend API contracts, validation rules, persistence impacts, and test gates over generic process notes.
- Keep optional docs and repos as supporting inputs rather than prerequisites.
- Produce a deterministic worker graph with explicit ownership and merge order.

## Brief Summary

This planning brief is built from 9 knowledge-base documents: writing-plan-principles.md, task-decomposition-principles.md, risk-audit-checklist.md, verification-checklist.md, backend-planning-guide.md, frontend-planning-guide.md, fullstack-planning-guide.md, architecture-planning-guide.md, implementation-roadmap-guide.md.

## Knowledge Base Files

- writing-plan-principles.md: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_FE\skills\superpower-plan\references\knowledge-base\writing-plan-principles.md
  - Summary: # Writing Plan Principles - Start by clarifying the user outcome, scope boundary, and definition of done. - Prefer explicit assumptions over hidden assumptions. - Organize plans into concrete phases that can be valida...
- task-decomposition-principles.md: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_FE\skills\superpower-plan\references\knowledge-base\task-decomposition-principles.md
  - Summary: # Task Decomposition Principles - Break the planning problem into a small graph of worker tasks with clear ownership. - Separate evidence gathering, architecture, risk review, and final synthesis. - Pass only the cont...
- risk-audit-checklist.md: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_FE\skills\superpower-plan\references\knowledge-base\risk-audit-checklist.md
  - Summary: # Risk Audit Checklist - Check for missing assumptions, context overload, and ambiguous ownership. - Check for optional inputs becoming accidental prerequisites. - Check whether the merge output can be audited without...
- verification-checklist.md: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_FE\skills\superpower-plan\references\knowledge-base\verification-checklist.md
  - Summary: # Verification Checklist - Verify that generated artifacts exist and are internally consistent. - Verify that worker outputs include their required sections. - Verify that the final plan includes a main-agent audit an...
- backend-planning-guide.md: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_FE\skills\superpower-plan\references\knowledge-base\backend-planning-guide.md
  - Summary: # Backend Planning Guide - Capture service boundaries, APIs, persistence, validation, and deployment constraints. - Highlight framework/runtime assumptions only when they are explicitly known. - Include integration ri...
- frontend-planning-guide.md: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_FE\skills\superpower-plan\references\knowledge-base\frontend-planning-guide.md
  - Summary: # Frontend Planning Guide - Capture UX flows, component boundaries, state ownership, and testing surfaces. - Distinguish between product requirements and implementation details. - Prefer plans that are incremental and...
- fullstack-planning-guide.md: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_FE\skills\superpower-plan\references\knowledge-base\fullstack-planning-guide.md
  - Summary: # Fullstack Planning Guide - Align frontend, backend, and integration work around a single delivery sequence. - Make interface contracts explicit before implementation tasks are assigned. - Reserve cross-cutting valid...
- architecture-planning-guide.md: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_FE\skills\superpower-plan\references\knowledge-base\architecture-planning-guide.md
  - Summary: # Architecture Planning Guide - Identify the runtime boundaries, context boundaries, and ownership boundaries. - Prefer extension of native project systems over parallel frameworks. - Record tradeoffs so the final pla...
- implementation-roadmap-guide.md: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_FE\skills\superpower-plan\references\knowledge-base\implementation-roadmap-guide.md
  - Summary: # Implementation Roadmap Guide - Sequence work into a thin vertical slice first, then follow with expansion tasks. - Pair each implementation phase with validation criteria. - Keep roadmap steps small enough for agent...

## Instruction Excerpts

### BE-init.md
- - REST API endpoints
- - Input validation
- - Validation rules
- - JWT authentication integration
- - PostgreSQL database
- - JWT authentication and authorization
- - RESTful API design
- - Use PostgreSQL
- - Explain how the Spring Boot application connects to PostgreSQL
- Authentication requirements:
- - Implement JWT-based authentication
- - Include login API
- - Include register API if needed
- - Include JWT validation filter
- - Include API base URL configuration example
- - Calling a protected API
- - Handling 400 validation errors
- 4. PostgreSQL database design
- 12. JWT authentication filter
- 16. API success response wrapper format
- 17. API error response format
- 18. Validation examples
- 20. Example API flow from frontend to backend
- 21. Testing strategy
- - VALIDATION_FAILED
- VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed", HttpStatus.BAD_REQUEST),
- - ValidationException
- - validationErrors

## User Story Evidence

### 2026-05-21-vsign-userstories-from-docx.md
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
- AC3: Earned badges are displayed in a grid on the profile page.
- US-05 â€” Chá»‰nh sá»­a Profile
- AC2: Given user uploads a new avatar (JPG/PNG â‰¤ 2MB) â†’ image saved to S3, displayed on profile.
- US-06 â€” Äá»•i máº­t kháº©u
- AC4: Given new password < 8 characters â†’ validation error shown.
- US-07 â€” ÄÄƒng xuáº¥t
- US-08 â€” QuÃªn máº­t kháº©u
- EPIC 2: Content Management (Learning)
- US-09 â€” Xem danh sÃ¡ch Unit
- US-10 â€” Xem Chapter trong Unit
- US-11 â€” Hiá»ƒn thá»‹ khÃ³a Chapter Premium
- US-12 â€” Xem danh sÃ¡ch Lesson

### US_Frontend_Verification_Report_v2.md
- ### Epic 1: Authentication & Profile (US-01..US-08)
- | US-01 | Dang ky tai khoan | Signup mode exists in LoginModal, but submit path reuses generic `login()` and does not pass email/password into auth logic. | docs/EXE101_FE_Business_Flows.md (Flow 25; Flow 29; Flow 31); src/components/LoginModal.tsx:12-28; src/components/LoginModal.tsx:56-86 | Mismatch | Frontend auth is demo/session-oriented and does not separate register/login contracts. | Split signup/login handlers, validate fields, call dedicated register API, and show inline API error states. | Clarify AC to require real account creation; if demo-mode is intended, explicitly scope it in US text. | Must | v1.1 | M | Backend auth register API | High |
- | US-02 | Dang nhap | Login UI exists, but submit handler ignores entered email/password and always calls generic session login. | docs/EXE101_FE_Business_Flows.md (Flow 25; Flow 30; Flow 31); src/components/LoginModal.tsx:22-28; src/components/LoginModal.tsx:67-83 | Mismatch | Missing credential-based auth integration in current FE flow. | Implement credential login request, error handling, loading state, and invalid-credential messaging. | Update AC to require credential verification and explicit failure states. | Must | v1.1 | M | Backend auth login API | High |
- | US-03 | Dang nhap Google OAuth | No Google OAuth button or redirect/callback handling in LoginModal or Dashboard auth entrypoints. | docs/EXE101_FE_Business_Flows.md (Flow 25; Flow 31); src/components/LoginModal.tsx:56-94; src/pages/Dashboard.tsx:61-83 | Not Implemented | OAuth provider flow was not added in current auth UI. | Add Google sign-in CTA, OAuth redirect handling, callback state restore, and authenticated session sync. | Move US to v2 if OAuth is deferred; otherwise keep AC with provider success/failure paths. | Should | v2 | M | Google OAuth config + backend callback API | High |
- | US-04 | Xem profile | Dashboard exposes profile tab and renders profile screen route in main content area. | docs/EXE101_FE_Business_Flows.md (Flow 57; Flow 100; Flow 101; Flow 102; Flow 103); src/pages/Dashboard.tsx:165-177; src/pages/Dashboard.tsx:301 | Matched | None; profile entry and render path are present. | Keep current flow; add regression test for tab navigation to profile screen. | Keep AC as-is, optionally add acceptance step for both desktop and mobile nav. | Should | v1.1 | S | None | High |
- | US-05 | Chinh sua profile | Profile feature is reachable from Dashboard; business-flow trace shows edit/input/save interactions in profile view. | docs/EXE101_FE_Business_Flows.md (Flow 57; Flow 94; Flow 95; Flow 96; Flow 97; Flow 98; Flow 99; Flow 100; Flow 101); src/pages/Dashboard.tsx:301 | Matched | None; edit interactions are present in traced frontend flow. | Keep behavior; add form validation and save-feedback smoke test coverage. | Keep AC, optionally specify editable fields (name/avatar/bio) explicitly. | Should | v1.1 | S | None | Medium |
- | US-06 | Doi mat khau | No change-password action is exposed in LoginModal, Onboarding, or Dashboard profile access points. | docs/EXE101_FE_Business_Flows.md (no matching flow for change-password UI); src/components/LoginModal.tsx:56-94; src/pages/Dashboard.tsx:165-301; src/pages/Onboarding.tsx:52-174 | Not Implemented | Password management UX not included in current FE scope. | Add Change Password section in profile/security area with current/new/confirm fields and strength/error handling. | Add AC for password policy, wrong-current-password response, and success confirmation. | Must | v1.1 | M | Backend change-password API | High |
- | US-07 | Dang xuat | Desktop and mobile logout buttons call `handleLogout()` to clear auth state and navigate back to landing. | docs/EXE101_FE_Business_Flows.md (Flow 58; Flow 63); src/pages/Dashboard.tsx:72-73; src/pages/Dashboard.tsx:179-186; src/pages/Dashboard.tsx:248-250 | Matched | None; explicit logout flow is wired in both layouts. | Keep flow; add logout redirect regression check. | Keep AC unchanged. | Should | v1.1 | S | None | High |
- | US-08 | Quen mat khau | Login modal has no forgot-password CTA, reset request form, or recovery route. | docs/EXE101_FE_Business_Flows.md (no matching forgot-password flow); src/components/LoginModal.tsx:56-94 | Not Implemented | Recovery UX was not implemented in auth modal design. | Add "Forgot password" link, reset-request screen, and success/error states. | Add AC for recovery email request, token expiry handling, and reset completion. | Must | v1.1 | M | Backend password-reset APIs + email service | High |
- | US-09 | Xem danh sach unit | Unit cards are rendered in `VocabularyPack` default view with click-through to unit detail view. | docs/EXE101_FE_Business_Flows.md (Flow 145; Flow 146); src/pages/VocabularyPack.tsx:737-746; src/pages/VocabularyPack.tsx:758-783 | Matched | None; unit list screen is fully present. | Keep flow; add unit-list load/render regression check. | Keep AC unchanged. | Should | v1.1 | S | None | High |
- | US-10 | Xem chapter trong unit | Selecting a unit opens chapter timeline list via `ChaptersList`, with per-chapter title/progress and click navigation. | docs/EXE101_FE_Business_Flows.md (Flow 146; Flow 143); src/pages/VocabularyPack.tsx:748-755; src/pages/VocabularyPack.tsx:651-679 | Matched | None; chapter browsing is implemented. | Keep flow; add chapter navigation test for unit transition. | Keep AC unchanged. | Should | v1.1 | S | None | High |
- | US-11 | Hien thi khoa chapter premium | Non-premium users see lock state and PRO badge; clicking locked chapter opens Premium modal. | docs/EXE101_FE_Business_Flows.md (Flow 133; Flow 134; Flow 143); src/pages/VocabularyPack.tsx:658-691; src/pages/VocabularyPack.tsx:676-678; src/pages/VocabularyPack.tsx:724 | Matched | None; premium lock visualization and gating exist. | Keep current UX; add lock-state accessibility label and click telemetry. | Keep AC, optionally specify lock icon + upgrade CTA requirement. | Should | v1.1 | S | None | High |
- | US-12 | Xem danh sach lesson | Chapter detail renders lesson timeline with completion/lock statuses and start/continue actions. | docs/EXE101_FE_Business_Flows.md (Flow 129; Flow 130; Flow 131; Flow 132; Flow 141); src/pages/VocabularyPack.tsx:452-527 | Matched | None; lesson list and statuses are present. | Keep flow; add test for lesson timeline state rendering. | Keep AC unchanged. | Should | v1.1 | S | None | High |
- | US-13 | Xem video bai hoc | Lesson screen supports in-lesson video playback and modal fallback video for mapped lessons. | docs/EXE101_FE_Business_Flows.md (Flow 120; Flow 121; Flow 122; Flow 123; Flow 124; Flow 128); src/pages/VocabularyPack.tsx:317-327; src/pages/VocabularyPack.tsx:349-370; src/pages/VocabularyPack.tsx:421-434 | Matched | None; video access path exists in learn and quiz phases. | Keep flow; add playback fallback/error UI for missing video URLs. | Keep AC; optionally clarify fallback modal behavior. | Should | v1.1 | S | None | High |
- | US-14 | Luu tien do tu dong | Progress is auto-marked only when lesson completes (`completeLesson`), not as granular in-lesson checkpoints. | docs/EXE101_FE_Business_Flows.md (Flow 130; Flow 138; Flow 141); src/pages/VocabularyPack.tsx:391-404; src/pages/VocabularyPack.tsx:447 | Mismatch | Implementation tracks completion milestone, while story wording implies broader auto-save progression. | Add checkpoint persistence (start/in-progress/complete), debounce autosave, and restore last checkpoint. | Refine AC to define required granularity (lesson-level vs step-level autosave). | Should | v1.1 | M | Progress persistence contract in auth/data layer | High |
- | US-15 | Progress bar theo chapter | Chapter cards and lesson context show progress ratios and visual bars derived from completed lessons. | docs/EXE101_FE_Business_Flows.md (Flow 139; Flow 140; Flow 141); src/pages/VocabularyPack.tsx:630-633; src/pages/VocabularyPack.tsx:694-716 | Matched | None; chapter progress indicators are already rendered. | Keep as-is; add edge-case handling for zero-lesson chapters. | Keep AC unchanged. | Should | v1.1 | S | None | High |
- | US-16 | Mo khoa bai hoc ke tiep | Completing a lesson advances to next lesson in sequence and enables continue CTA for first incomplete lesson. | docs/EXE101_FE_Business_Flows.md (Flow 130; Flow 132; Flow 138); src/pages/VocabularyPack.tsx:396-404; src/pages/VocabularyPack.tsx:531-539 | Matched | None; next-lesson unlock and progression are present. | Keep behavior; add regression test for chapter-end transition. | Keep AC unchanged. | Should | v1.1 | S | None | High |
- | US-17 | Redirect paywall khi click chapter premium | FE shows paywall modal in-place instead of redirecting to a separate upgrade page. | docs/EXE101_FE_Business_Flows.md (Flow 133; Flow 134; Flow 143); src/pages/VocabularyPack.tsx:676-678; src/pages/VocabularyPack.tsx:724 | Mismatch | Story wording expects redirect, while current UX intentionally preserves context with modal. | Keep modal flow, add clearer chapter-locked rationale text and direct upgrade CTA in modal. | Update US/AC wording from redirect-page to in-context modal paywall behavior. | Must | v1.1 | S | None | High |
- | US-18 | Resume bai hoc dang do | FE resumes at first incomplete lesson in chapter, but not exact in-lesson step/video timestamp/question state. | docs/EXE101_FE_Business_Flows.md (Flow 132; Flow 139); src/pages/VocabularyPack.tsx:453; src/pages/VocabularyPack.tsx:531-536 | Mismatch | Resume design is chapter-level shortcut, not full session checkpoint restore. | Persist per-lesson state (phase, current question, video timestamp) and restore from saved checkpoint. | Update AC to explicitly require exact-state resume scope. | Should | v1.1 | M | Local persistence model + optional backend sync | High |
- | US-19 | Lam quiz MCQ | MockExam provides timed multi-question MCQ flow with option selection and per-question state. | docs/EXE101_FE_Business_Flows.md (Flow 85; Flow 86; Flow 87; Flow 88; Flow 89; Flow 90; Flow 91); src/pages/MockExam.tsx:29-63; src/pages/MockExam.tsx:170-218 | Matched | None; core MCQ exam behavior is implemented. | Keep flow; add test for answer persistence across question navigation. | Keep AC unchanged. | Should | v1.1 | S | None | High |
- | US-20 | Xem diem sau quiz | Submitted exam shows score, percentage, pass/fail message, and detailed per-question results. | docs/EXE101_FE_Business_Flows.md (Flow 86; Flow 87; Flow 91); src/pages/MockExam.tsx:101-147 | Matched | None; score summary is present post-submit. | Keep behavior; add test for score math correctness. | Keep AC unchanged. | Should | v1.1 | S | None | High |
- | US-21 | Review dap an | Result screen includes correct/incorrect markers, chosen answer, and expected answer for each item. | docs/EXE101_FE_Business_Flows.md (Flow 86; Flow 87; Flow 91); src/pages/MockExam.tsx:116-140 | Matched | None; answer review details are implemented. | Keep behavior; add UX polish for long answer wrapping. | Keep AC unchanged. | Should | v1.1 | S | None | High |
- | US-22 | Lam lai quiz | "Thi lai" action resets timer, answers, question index, and submission state to restart exam. | docs/EXE101_FE_Business_Flows.md (Flow 86; Flow 87); src/pages/MockExam.tsx:67-73; src/pages/MockExam.tsx:143-145 | Matched | None; retake loop is fully wired. | Keep behavior; add confirmation prompt only if product requires it. | Keep AC unchanged. | Should | v1.1 | S | None | High |
- | US-23 | Thi thu co bam gio | Exam starts with fixed 15-minute timer and visibly counts down during test session. | docs/EXE101_FE_Business_Flows.md (Flow 85; Flow 86; Flow 87; Flow 88; Flow 89; Flow 90; Flow 91); src/pages/MockExam.tsx:27; src/pages/MockExam.tsx:37-47; src/pages/MockExam.tsx:158-163 | Matched | None; timed-exam behavior is present. | Keep behavior; add paused-tab resilience test if needed. | Keep AC unchanged. | Should | v1.1 | S | None | High |
- | US-24 | Auto-submit khi het gio | Timer effect auto-submits by setting `submitted=true` when time reaches zero. | docs/EXE101_FE_Business_Flows.md (Flow 85; Flow 91); src/pages/MockExam.tsx:37-41 | Matched | None; auto-submit trigger exists in timer logic. | Keep behavior; add timeout toast so user sees explicit auto-submit reason. | Keep AC, optionally add explicit timeout-message expectation. | Should | v1.1 | S | None | High |
- | US-25 | Dieu huong tu do giua cac cau | User can jump directly via question dots and also move with previous/next controls. | docs/EXE101_FE_Business_Flows.md (Flow 87; Flow 88; Flow 89; Flow 90); src/pages/MockExam.tsx:171-186; src/pages/MockExam.tsx:221-237 | Matched | None; free navigation controls are implemented. | Keep behavior; add keyboard-access support for quick navigation. | Keep AC unchanged. | Should | v1.1 | S | None | High |
- | US-26 | Canh bao cau chua tra loi | FE shows answered count but allows submit without explicit unanswered-question warning/confirmation. | docs/EXE101_FE_Business_Flows.md (Flow 89; Flow 90; Flow 91); src/pages/MockExam.tsx:152; src/pages/MockExam.tsx:165-167; src/pages/MockExam.tsx:238-243 | Mismatch | Only passive counter exists; no pre-submit blocking or warning modal. | Add submit confirmation modal listing unanswered question numbers with "review" and "submit anyway" actions. | Update AC to define warning threshold and confirmation behavior for unanswered items. | Should | v1.1 | S | None | High |
- | US-27 | Luyen tap AI (AI Quiz) | AI-review lessons render camera practice quiz with scan interaction and completion callbacks. | docs/EXE101_FE_Business_Flows.md (Flow 118; Flow 125; Flow 126; Flow 127; Flow 130); src/pages/VocabularyPack.tsx:257-283; src/pages/VocabularyPack.tsx:373-375 | Matched | None; AI practice entry flow exists in lesson pipeline. | Keep behavior; add minimal analytics event for scan start/complete. | Keep AC unchanged. | Should | v1.1 | S | None | High |

## Frontend Verification Evidence

## Input/Output Lineage

Instruction/User Stories -> Planning Brief -> Task Graph -> Worker Outputs -> Final Report -> Audit

- Planning Brief: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\superpowers\runs\2026-05-21-backend-epic-plan\planning-brief.md
- Task Graph: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\superpowers\runs\2026-05-21-backend-epic-plan\task-graph.json
- Final Report: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\superpowers\runs\2026-05-21-backend-epic-plan\final-report.md
- Audit Report: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\superpowers\runs\2026-05-21-backend-epic-plan\audit-report.md
- Run Manifest: C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\superpowers\runs\2026-05-21-backend-epic-plan\run-manifest.json

## Worker Outputs

- evidence: Instruction and user story evidence -> C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\superpowers\runs\2026-05-21-backend-epic-plan\workers\evidence\output.md
- api-contracts: Backend API contract plan -> C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\superpowers\runs\2026-05-21-backend-epic-plan\workers\api-contracts\output.md
- frontend-alignment: Frontend/backend alignment plan -> C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\superpowers\runs\2026-05-21-backend-epic-plan\workers\frontend-alignment\output.md
- roadmap: Delivery roadmap and sequencing -> C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\superpowers\runs\2026-05-21-backend-epic-plan\workers\roadmap\output.md
- risk: Domain risk and validation review -> C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\superpowers\runs\2026-05-21-backend-epic-plan\workers\risk\output.md
- report: Final backend epic plan assembly -> C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\superpowers\runs\2026-05-21-backend-epic-plan\workers\report\output.md
