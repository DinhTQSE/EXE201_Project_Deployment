# Checkpoint 02: Authentication & Profile

## Goal

Implement account registration, login, authenticated profile retrieval, profile update, password change, and password reset flows.

## API Contract

| Endpoint | Method | Auth | Request DTO | Response DTO | Status |
| --- | --- | --- | --- | --- | --- |
| `/api/v1/auth/register` | POST | Public | `RegisterRequest` | `SuccessResponse<AuthResponse>` | Batch 1 accepted |
| `/api/v1/auth/login` | POST | Public | `LoginRequest` | `SuccessResponse<AuthResponse>` | Batch 1 accepted |
| `/api/v1/me` | GET | USER | None | `SuccessResponse<ProfileResponse>` | Batch 1 accepted |
| `/api/v1/me/profile` | PATCH | USER | `UpdateProfileRequest` | `SuccessResponse<ProfileResponse>` | Batch 1 accepted |
| `/api/v1/me/change-password` | POST | USER | `ChangePasswordRequest` | `SuccessResponse<Void>` | Not started |
| `/api/v1/auth/password-reset/request` | POST | Public | `PasswordResetRequest` | `SuccessResponse<Void>` | Not started |
| `/api/v1/auth/password-reset/confirm` | POST | Public | `PasswordResetConfirmRequest` | `SuccessResponse<Void>` | Not started |

## Current Implementation

| File | Status |
| --- | --- |
| `auth/controller/AuthController.java` | Moved to `/api/v1/auth`, returns success envelope |
| `auth/controller/ProfileController.java` | Moved to `/api/v1/me`, returns success envelope |
| `auth/dto/AuthResponse.java` | Updated to include nested `user` object |
| `auth/dto/LoginRequest.java` | Bean validation added |
| `auth/dto/RegisterRequest.java` | Bean validation added |
| `auth/persistence/UserEntity.java` | Started |
| `auth/persistence/UserRepository.java` | Started |
| `auth/service/AuthService.java` | Started migration from in-memory to JPA/BCrypt |
| `common/security/JwtAuthFilter.java` | Protects `/api/v1/me` |
| `common/security/PasswordConfig.java` | BCrypt password encoder added |

## DB/Migration Plan

| Table | Purpose | Status |
| --- | --- | --- |
| `users` | account identity, password hash, profile fields, role, XP/streak fields | Existing V1 |
| `reference_roles` | role seed data | Existing V1/V2 |
| `password_reset_tokens` | reset token hash, expiry, consumed flag | Not started |

## Test Gates

- [x] Register returns token and user DTO.
- [x] Login returns token and user DTO.
- [x] Invalid login returns `INVALID_CREDENTIALS`.
- [x] `/api/v1/me` returns authenticated profile.
- [x] Missing bearer token returns `UNAUTHORIZED`.
- [x] Duplicate email returns `EMAIL_ALREADY_EXISTS`.
- [x] Invalid register payload returns `VALIDATION_ERROR` with field errors.
- [x] Profile response exposes account type, XP, streak, badge placeholder, and subscription placeholder fields.
- [x] Profile patch updates full name and avatar URL with validation.
- [ ] Password change validates current password and password policy.
- [ ] Password reset request does not reveal account existence.

## Review Decision

- [x] Continue JPA persistence now.
- [ ] Keep simple auth only and defer profile/password flows.
- [ ] Require Spring Security filter chain before adding more protected endpoints.
