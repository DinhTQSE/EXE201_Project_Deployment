# Checkpoint 04: Learning & Signature Workflow

## Goal

Implement learning catalog/progress APIs and signature workflow practice attempts.

## API Contract From Plan

| Endpoint | Method | Auth | Request DTO | Response DTO | Status |
| --- | --- | --- | --- | --- | --- |
| `/api/v1/units` | GET | USER | `UnitSearchRequest` | `UnitListResponse` | Batch 2 accepted |
| `/api/v1/units/{unitId}/chapters` | GET | USER | None | `ChapterListResponse` | Batch 2 accepted |
| `/api/v1/chapters/{chapterId}/lessons` | GET | USER | None | `LessonListResponse` | Batch 2 accepted |
| `/api/v1/lessons/{lessonId}` | GET | USER | None | `LessonDetailResponse` | Batch 2 accepted |
| `/api/v1/lessons/{lessonId}/progress` | PUT | USER | `UpdateProgressRequest` | `ProgressResponse` | Batch 2 accepted |
| `/api/v1/learning/practice-items` | GET | Public/current skeleton | query params | `PracticeItemsPageResponse` | Done skeleton |
| `/api/v1/learning/practice-items/{itemId}` | GET | Public/current skeleton | None | `PracticeItemDetailResponse` | Done skeleton |
| `/api/v1/signature-workflows/attempts` | POST | Public/current skeleton | `SubmitSignatureAttemptRequest` | `SignatureAttemptResponse` | Done skeleton |

## Current Implementation

| File | Status |
| --- | --- |
| `learning/controller/LearningController.java` | Added by subagent |
| `learning/controller/SignatureWorkflowController.java` | Added by subagent |
| `learning/service/LearningWorkflowService.java` | In-memory sample data |
| `learning/dto/*` | Practice and attempt DTOs |
| `learning/LearningWorkflowIT.java` | Subagent reports passing |

## DB/Migration Plan

| Table | Purpose | Status |
| --- | --- | --- |
| `units` | Learning unit metadata | Not started |
| `chapters` | Ordered chapters under units | Not started |
| `lessons` | Lesson detail, media URL metadata, premium flag | Not started |
| `user_lesson_progress` | Completion, checkpoint, phase state | Not started |
| `signature_attempts` | Attempt result, confidence, feedback, media metadata only | Not started |

## Test Gates

- [x] Premium gating contract returns `PREMIUM_REQUIRED` for restricted lessons.
- [x] Progress update is idempotent in current response semantics.
- [x] Lesson detail returns resume checkpoint fields.
- [ ] Signature attempt rejects raw media payloads and stores metadata only.
- [x] Existing skeleton tests are reviewed and rerun by main agent.
- [x] Out-of-range practice paging preserves total page metadata.
- [x] Premium lesson progress updates are blocked in current in-memory gate.

## Review Decision

- [ ] Keep skeleton endpoints as temporary FE unblockers.
- [ ] Replace skeleton with plan endpoints before frontend integration.
- [ ] Require persistence before acceptance.
