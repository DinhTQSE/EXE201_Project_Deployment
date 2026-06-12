# Checkpoint 01: Foundation API Contract

## Goal

Create the backend foundation used by every epic: `/api/v1` routing, success envelope, error envelope, validation handling, and testable API contract behavior.

## Planned Contract

| Area | Decision |
| --- | --- |
| Base path | `/api/v1` |
| Success body | `SuccessResponse<T>` with `success`, `message`, `data`, `timestamp` |
| Error body | `ApiErrorResponse` with `timestamp`, `status`, `error`, `code`, `message`, `path`, `validationErrors` |
| Business errors | `BusinessException(ErrorCode)` |
| Validation | Jakarta Bean Validation and global handler |
| Auth token transport | `Authorization: Bearer <token>` |

## Files

| Type | Path | Status |
| --- | --- | --- |
| Create | `src/main/java/com/vsign/backend/common/response/SuccessResponse.java` | Done |
| Create | `src/main/java/com/vsign/backend/common/exception/ErrorCode.java` | Done |
| Create | `src/main/java/com/vsign/backend/common/exception/BusinessException.java` | Done |
| Create | `src/main/java/com/vsign/backend/common/exception/ApiErrorResponse.java` | Done |
| Modify | `src/main/java/com/vsign/backend/common/exception/ApiExceptionHandler.java` | Done |
| Modify | `pom.xml` | Started: validation, JPA, security crypto dependencies added |

## Review Checklist

- [ ] Error codes match frontend expectations.
- [ ] Error body field names are final.
- [ ] Success envelope is acceptable for all endpoints.
- [ ] Validation response includes field-level errors.
- [ ] No endpoint returns raw DTOs unless intentionally public/static.

## Tests

| Test | Status |
| --- | --- |
| `AuthControllerIT` | Passing in targeted run |
| `ProfileControllerIT` | Passing in targeted run |
| `DictionaryIT` | Passing in targeted run |
| Full `mvn test` | Needs rerun after checkpoint review |

## Next Implementation After Review

1. Add dedicated error-envelope tests.
2. Normalize all current subagent endpoints to use the same error codes.
3. Run full suite and fix migration-count assumptions.
