# Implementation Plan - Google OAuth2 & SMTP Mail Notifications

This document outlines the detailed plan to integrate Google OAuth2 login and SMTP email notifications (including temporary password delivery and double-hashed password reset tokens) in the Spring Boot backend (`v-sign-be`).

---

## 1. Database Schema Migration

We will create a new Flyway migration file:
`v-sign-be/src/main/resources/db/migration/V24__add_google_oauth_and_password_reset.sql`

```sql
-- V24: Google OAuth and Password Reset Support
ALTER TABLE users ADD COLUMN IF NOT EXISTS pwd_reset_token_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS pwd_reset_expiry TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_pwd_reset_token_hash ON users (pwd_reset_token_hash);
```

---

## 2. Dependencies (`pom.xml`)

We will add the standard Spring Mail starter:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
```

---

## 3. Entity Updates (`UserEntity.java`)

We will add the fields to map the new database columns:

```java
    @Column(name = "pwd_reset_token_hash")
    private String pwdResetTokenHash;

    @Column(name = "pwd_reset_expiry")
    private OffsetDateTime pwdResetExpiry;

    // Getters and Setters
```

---

## 4. Email Notification Service (`EmailService.java`)

We will implement an asynchronous service to send emails without blocking HTTP request threads:

*   **Location**: `com.vsign.backend.common.mail.EmailService`
*   **Key annotations**: `@Service`, `@EnableAsync` (enabled on the application entry point), `@Async` on sending methods.
*   **Fallback Strategy**: All mail triggers will be wrapped in `try-catch` blocks to prevent third-party SMTP failures from failing user registration/login.

---

## 5. Google OAuth2 Login Module

### A. Configuration Properties
We will declare the configuration in `application.properties` and `.env.prod.example`:
*   `oauth2.google.client-id`
*   `oauth2.google.client-secret`
*   `oauth2.google.redirect-uri` (e.g. `{baseUrl}/api/v1/auth/google/callback`)
*   `frontend.oauth-success-url` (e.g. `https://v-sign.vercel.app/oauth-redirect`)
*   `frontend.oauth-failure-url` (e.g. `https://v-sign.vercel.app/login?error=oauth_failed`)

### B. Service Implementation (`GoogleOAuthService.java`)
*   Provides `getAuthorizationUrl()` using standard query parameters:
    `https://accounts.google.com/o/oauth2/v2/auth?client_id={clientId}&redirect_uri={redirectUri}&response_type=code&scope=openid%20email%20profile`
*   Exchanges incoming `code` for OAuth tokens via a POST request to Google's endpoint:
    `https://oauth2.googleapis.com/token`
*   Queries Google UserInfo API:
    `https://www.googleapis.com/oauth2/v3/userinfo`
*   Maps User Profile:
    *   Normalize email.
    *   If user exists but is disabled (`!user.isActive()`), throw `ACCOUNT_DISABLED`.
    *   If user does not exist:
        *   Register new user with default role `USER` and `BASIC` tier.
        *   Generate random temporary password: `HT-GOOGLE-<12-chars-uuid>`. Hash with BCrypt.
        *   Seed default `free` subscription tier.
        *   Asynchronously send the temporary password via email.
    *   Return JWT authentication tokens.

### C. Controller Endpoints (`GoogleOAuthController.java`)
*   `GET /api/v1/auth/google/login-url` - Returns URL for frontend redirect.
*   `GET /api/v1/auth/google/callback?code=xyz` - Receives redirect from Google, authenticates/registers the user, and redirects the browser back to `frontend.oauth-success-url` with `accessToken`, `tokenType`, `expiresIn`, and `email` as query parameters.

---

## 6. Password Reset Flow

We will complete the no-op implementation in `AuthService.java` and map endpoints:

### A. Request Reset (`POST /api/v1/auth/password-reset/request`)
*   Accept email, normalize it.
*   If user exists:
    *   Generate a cryptographically secure random 32-byte token (base64url-encoded).
    *   Compute its SHA-256 hash.
    *   Save hash and expiry (`now + 15 minutes`) in DB.
    *   Asynchronously send reset link to user's email: `${frontend.password-reset-url}?token={RAW_TOKEN}`.
*   Always return `200 OK` with a generic message to prevent email enumeration.

### B. Complete Reset (`POST /api/v1/auth/password-reset/complete`)
*   Input body: `token` (raw), `newPassword`, `confirmPassword`.
*   Validate password length/rules and match confirmation.
*   Compute SHA-256 hash of incoming raw `token`.
*   Query user by token hash.
*   Verify if token has not expired.
*   Update password to BCrypt hashed `newPassword`.
*   Clear database token reset columns.

---

## 7. Verification Plan

### Automated Tests
1. Write integration test in `GoogleOAuthControllerIT.java` mocking Google API exchanges.
2. Write integration test in `PasswordResetControllerIT.java` verifying token generation, database hash storage, expiration checks, and successful resetting.

### Manual Verification
1. Initiate Google OAuth via Swagger UI.
2. Trigger password reset request and inspect generated logs and DB entries.
