# Checkpoint 05: Assessment

## Goal

Implement quiz/assessment listing, detail, submission, review, and AI quiz metadata-only attempt contracts.

## API Contract From Plan

| Endpoint | Method | Auth | Request DTO | Response DTO | Status |
| --- | --- | --- | --- | --- | --- |
| `/api/v1/lessons/{lessonId}/quiz` | GET | USER | None | `QuizResponse` | Batch 2 accepted |
| `/api/v1/quiz-attempts/{attemptId}/submit` | POST | USER | `SubmitAttemptRequest` | `QuizResultResponse` | Batch 2 accepted |
| `/api/v1/quiz-attempts/{attemptId}/review` | GET | USER | None | `QuizReviewResponse` | Batch 2 accepted |
| `/api/v1/ai-quiz/attempts` | POST | USER/PREMIUM | `AiAttemptRequest` | `AiAttemptResultResponse` | Not started |
| `/api/v1/assessments` | GET | Public/current skeleton | None | `List<AssessmentSummaryResponse>` | Done skeleton |
| `/api/v1/assessments/{id}` | GET | Public/current skeleton | None | `AssessmentDetailResponse` | Done skeleton |
| `/api/v1/assessments/{id}/submissions` | POST | Public/current skeleton | `AssessmentSubmissionRequest` | `AssessmentSubmissionResultResponse` | Done skeleton |

## Current Implementation

| File | Status |
| --- | --- |
| `assessment/controller/AssessmentController.java` | Added by subagent |
| `assessment/service/AssessmentService.java` | In-memory sample data/scoring |
| `assessment/dto/*` | Summary/detail/submission/result DTOs |
| `assessment/AssessmentControllerIT.java` | Added by subagent |

## DB/Migration Plan

| Table | Purpose | Status |
| --- | --- | --- |
| `quizzes` | Quiz metadata linked to lesson | Not started |
| `quiz_questions` | Question prompts and explanations | Not started |
| `quiz_options` | Answer options and correctness | Not started |
| `quiz_attempts` | Attempt ownership, status, score, timing | Not started |
| `quiz_attempt_answers` | Selected answers and correctness | Not started |
| `ai_attempt_logs` | Predicted sign/confidence metadata, no raw media | Not started |

## Test Gates

- [x] Quiz response hides correct answers.
- [x] Submit scores unanswered questions as incorrect.
- [ ] Attempt ownership is enforced.
- [x] Already-submitted attempt returns business error.
- [ ] AI endpoint rejects media bytes and enforces premium/preview quota.
- [x] Current skeleton tests are reviewed and rerun by main agent.
- [x] Repeated lesson quiz fetches issue usable in-memory attempts.
- [x] Submit rejects answer IDs that do not belong to the question.

## Review Decision

- [ ] Accept `/assessments` skeleton as a separate endpoint group.
- [ ] Rename/rework to match planned `/lessons/{lessonId}/quiz` and `/quiz-attempts` contracts.
