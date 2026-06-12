# Checkpoint 08: Admin Panel

## Goal

Implement admin content, user, payment, KPI, and audit APIs with RBAC and status-transition validation.

## API Contract From Plan

| Endpoint | Method | Auth | Request DTO | Response DTO | Status |
| --- | --- | --- | --- | --- | --- |
| `/api/v1/admin/content/**` | GET/POST/PATCH/DELETE | ADMIN | `AdminContentRequest` | `AdminContentResponse` | Skeleton review queue only |
| `/api/v1/admin/users` | GET | ADMIN | query params | `AdminUserListResponse` | Done skeleton |
| `/api/v1/admin/content/review-queue` | GET | ADMIN/Reviewer | query params | `ReviewQueueResponse` | Done skeleton |
| `/api/v1/admin/payments` | GET/PATCH | ADMIN | `AdminQueryRequest`, `ManualPaymentStatusRequest` | `AdminPageResponse` | Not started |
| `/api/v1/admin/kpis` | GET | ADMIN | date range params | `AdminKpiResponse` | Not started |

## Current Implementation

| File | Status |
| --- | --- |
| `admin/controller/AdminUserController.java` | Added by subagent |
| `admin/controller/AdminContentController.java` | Added by subagent |
| `admin/service/AdminUserService.java` | In-memory user list |
| `admin/service/AdminContentReviewService.java` | In-memory review queue |
| `admin/dto/*` | Admin user and review queue DTOs |
| `admin/AdminControllerIT.java` | Added by subagent |
| `admin/controller/AdminPaymentController.java` | Implemented |
| `admin/controller/AdminKpiController.java` | Implemented |
| `admin/controller/AdminAuditController.java` | Implemented |
| `admin/service/AdminPaymentService.java` | Implemented in-memory payment admin flow |
| `admin/service/AdminAuditService.java` | Implemented in-memory audit trail |

## DB/Migration Plan

| Table | Purpose | Status |
| --- | --- | --- |
| `audit_logs` | Admin action traceability | Not started |
| `media_assets` | Uploaded content metadata and review state | Not started |
| Existing domain tables | Source data for admin views | Not started |

## Test Gates

- [x] ADMIN role enforced through JWT claims.
- [x] Non-admin receives `FORBIDDEN`.
- [x] Pagination and filters validated.
- [x] Manual payment status override requires reason.
- [x] Audit log created for admin mutations.
- [x] Current skeleton tests are reviewed and rerun by main agent.

## New Endpoints Implemented

- `GET /api/v1/admin/payments`
- `PATCH /api/v1/admin/payments/{transactionId}`
- `GET /api/v1/admin/kpis`
- `GET /api/v1/admin/audit-logs`
- `PATCH /api/v1/admin/content/review-queue/{contentId}`

## Endpoint Acceptance Table

| Endpoint | Method | Auth | Request | Response | Error Codes | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `/api/v1/admin/users` | GET | ADMIN | query: `role`, `status` | `AdminUserListResponse` | `UNAUTHORIZED`, `FORBIDDEN` | Implemented |
| `/api/v1/admin/content/review-queue` | GET | CONTENT_REVIEWER or ADMIN | none | `ReviewQueueResponse` | `UNAUTHORIZED`, `FORBIDDEN` | Implemented |
| `/api/v1/admin/content/review-queue/{contentId}` | PATCH | ADMIN | `ReviewDecisionRequest{decision,reason}` | `ReviewQueueItemResponse` | `UNAUTHORIZED`, `FORBIDDEN`, `VALIDATION_ERROR`, `NOT_FOUND` | Implemented |
| `/api/v1/admin/payments` | GET | ADMIN | query: `page`, `size` | `AdminPaymentPageResponse` | `UNAUTHORIZED`, `FORBIDDEN`, `VALIDATION_ERROR` | Implemented |
| `/api/v1/admin/payments/{transactionId}` | PATCH | ADMIN | `ManualPaymentStatusRequest{status,reason}` | `AdminPaymentRecordResponse` | `UNAUTHORIZED`, `FORBIDDEN`, `VALIDATION_ERROR`, `NOT_FOUND` | Implemented |
| `/api/v1/admin/kpis` | GET | ADMIN | query: `fromDate`, `toDate` | `AdminKpiResponse` | `UNAUTHORIZED`, `FORBIDDEN` | Implemented |
| `/api/v1/admin/audit-logs` | GET | ADMIN | none | `List<AdminAuditLogResponse>` | `UNAUTHORIZED`, `FORBIDDEN` | Implemented |

## Verification Evidence (2026-05-22)

- `mvn -q "-Dtest=com.vsign.backend.admin.AdminControllerIT,com.vsign.backend.gamification.GamificationControllerIT" test` -> pass
- `mvn -q "-Dtest=AuthControllerIT,ProfileControllerIT,DictionaryIT" test` -> pass
- `mvn -q "-Dtest=com.vsign.backend.learning.LearningWorkflowIT,com.vsign.backend.assessment.AssessmentControllerIT" test` -> pass

## Review Decision

- [x] Accept admin full in-memory API scope for current phase.
- [ ] Block admin APIs until RBAC is implemented.
- [ ] Prioritize audit log migration before mutations.
