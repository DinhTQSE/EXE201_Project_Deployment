# V-Sign PayOS, Google Login, And Admin Dashboard Completion Plan

Created: 2026-06-13

Purpose: complete the remaining production features without changing the current deploy architecture.

Scope:
- Real payment flow through PayOS.
- Google login.
- Password reset by email.
- Admin dashboard visible only to admin accounts.
- Basic admin operations: list/edit/delete users, system metrics, user usage and activity time.

---

## 1. Architecture Constraints

Current production shape stays:

```text
Vercel frontend
  -> https://api.<domain>/api/v1/*
  -> Caddy on EC2 c6a.large
  -> Spring backend container
  -> private Docker network
  -> AI container
```

Rules:
- FE remains in separate repo: `D:\v-sign-fe`.
- Server repo remains: `D:\V-sign_EXE101_Project`.
- Backend and AI are deployed by GitHub Actions to GHCR.
- EC2 pulls backend/AI images from GHCR.
- All production secrets live in `/opt/vsign/.env.prod` on EC2, not in Git.
- Vercel only stores public frontend env values.
- PayOS webhook is a public backend endpoint but must be signature-verified.
- Google OAuth callback is a public backend endpoint but must validate state and Google response.
- Admin UI is rendered only for authenticated `ADMIN` or `SUPER_ADMIN` users.

---

## 2. Current Source State

Confirmed from current source:

- Backend payment is currently placeholder/contract-ready:
  - `PaymentService.createOrder()` generates synthetic transaction IDs, QR URLs, and deep links.
  - `PaymentController` exposes authenticated `POST /api/v1/payments/orders`.
  - `PaymentService.status()` and `/me/payments` are scoped by authenticated user.
  - Real webhook activation is not implemented.
- Backend admin API partially exists:
  - `GET /api/v1/admin/users`
  - `GET /api/v1/admin/kpis`
  - `GET/PATCH /api/v1/admin/payments`
  - `GET /api/v1/admin/audit-logs`
  - Current user list reads `admin_user_accounts`, not the real `users` table.
- Backend security already protects `/api/v1/admin/**` by role.
- Backend password reset request currently exists as a no-op and must be implemented for production.
- Frontend Google button exists but only shows a placeholder error.
- Frontend has no real admin dashboard route.
- Frontend `AuthContext` already stores user role from backend auth response.
- Premium modal still has simulated/contract-ready payment UX.

---

## 3. Required Env Contract

Use the other project's `.env` only as a naming reference. Do not copy its secret values.

### Backend `/opt/vsign/.env.prod`

Keep existing:

```properties
DB_URL=
DB_USER=
DB_PASSWORD=
DB_SCHEMA=vsign_prod

JWT_SECRET=
JWT_EXPIRATION_MS=150000000
JWT_REFRESH_EXPIRATION_MS=1512000000

APP_CORS_ALLOWED_ORIGINS=https://<vercel-domain>
AI_SERVICE_BASE_URL=http://ai:8000
AI_SERVICE_PREDICT_TIMEOUT_MS=10000
```

Add PayOS:

```properties
PAYOS_CLIENT_ID=
PAYOS_API_KEY=
PAYOS_CHECKSUM_KEY=
PAYOS_RETURN_URL=https://<vercel-domain>/payment/success
PAYOS_CANCEL_URL=https://<vercel-domain>/payment/cancel
PAYOS_WEBHOOK_URL=https://api.<domain>/api/v1/payments/payos/webhook
```

Add Google OAuth:

```properties
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=https://api.<domain>/api/v1/auth/google/callback
FRONTEND_OAUTH_SUCCESS_URL=https://<vercel-domain>/oauth/success
FRONTEND_OAUTH_FAILURE_URL=https://<vercel-domain>/oauth/failure
OAUTH_STATE_SECRET=
```

Add SMTP/password reset:

```properties
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_FROM=
FRONTEND_PASSWORD_RESET_URL=https://<vercel-domain>/reset-password
PASSWORD_RESET_TOKEN_EXPIRATION_MINUTES=15
```

### Frontend Vercel Env

Keep:

```text
VITE_API_BASE_URL=https://api.<domain>/api/v1
```

Only add `VITE_GOOGLE_CLIENT_ID` if the frontend uses Google Identity Services directly. If using backend OAuth redirect flow, FE can start login by navigating to:

```text
https://api.<domain>/api/v1/auth/google/start
```

Recommended for this project: backend OAuth redirect flow, because it matches the env sample with `GOOGLE_CLIENT_SECRET` and success/failure URLs.

---

## 4. Phase 0 - Design Decisions To Lock

- [ ] Use PayOS as the only real payment provider for first production release.
- [ ] Keep old `MOMO`/`ZALOPAY` labels out of production UI after PayOS is active.
- [ ] Payment amount must always come from backend `subscription_plans`, not FE.
- [ ] Payment success must be driven by verified PayOS webhook, not FE button click.
- [ ] PayOS return URL is only a UX signal; it must never activate premium by itself.
- [ ] If PayOS payment succeeds but webhook/subscription update fails, the system must recover through reconciliation without requiring the user to pay again.
- [ ] Payment status may stay `PENDING_CONFIRMATION` while backend verifies PayOS state and applies subscription.
- [ ] Google login creates new users as `USER` only.
- [ ] Google login may preserve existing `ADMIN`/`SUPER_ADMIN` role only when the verified Google email matches an existing admin user.
- [ ] Do not grant admin by Google email domain, frontend flag, localStorage, or query param.
- [ ] Password reset tokens must be random, hashed in DB, single-use, and short-lived.
- [ ] User delete in admin dashboard should be soft-delete/deactivate in v1 to preserve learning/payment/audit history.
- [ ] Hard delete is not part of this admin dashboard release; v1 delete means deactivate.

Acceptance:
- Feature rules are written down before implementation starts.
- No frontend-only trust is used for payment, role, or subscription state.

---

## 5. Phase 1 - Backend Configuration And Schema

### Config Tasks

- [ ] Add PayOS properties to `application.properties`.
- [ ] Add Google OAuth properties to `application.properties`.
- [ ] Add SMTP/password reset properties to `application.properties`.
- [ ] Keep secrets env-only in `application-prod.properties`.
- [ ] Update root `.env.prod.example`.
- [ ] Update `v-sign-be/.env.prod.example`.
- [ ] Update `AWS_C6A_DEPLOY_PLAN.md` server env section after implementation.

### Migration Tasks

Add a new Flyway migration after the latest migration.

Payment:
- [ ] Add PayOS order fields if missing:
  - `payos_order_code`
  - `checkout_url`
  - `payment_link_id`
  - `provider_payload_hash`
  - `last_provider_status`
  - `last_provider_sync_at`
  - `sync_attempts`
  - `sync_error`
  - `paid_at`
  - `canceled_at`
  - `expired_at`
  - `subscription_activated_at`
- [ ] Add indexes:
  - `payment_orders(provider, provider_transaction_id)`
  - `payment_orders(payos_order_code)`
  - `payment_orders(user_email, created_at desc)`
- [ ] Add `payment_webhook_events` table:
  - `id`
  - `provider`
  - `event_key`
  - `signature`
  - `payload_hash`
  - `processed_at`
  - `status`
  - `error_message`
- [ ] Add `payment_reconciliation_runs` table:
  - `id`
  - `started_at`
  - `finished_at`
  - `checked_count`
  - `fixed_count`
  - `failed_count`
  - `error_message`
- [ ] Add `payment_subscription_events` table for idempotent subscription activation:
  - `id`
  - `transaction_id`
  - `user_email`
  - `plan_id`
  - `event_type`
  - `idempotency_key`
  - `created_at`

Google auth:
- [ ] Add user auth fields if missing:
  - `auth_provider`
  - `google_subject`
  - `email_verified`
  - `last_login_at`
  - `last_seen_at`
- [ ] Add unique index on `google_subject` when not null.

Usage/admin metrics:
- [ ] Add `user_activity_events`:
  - `id`
  - `user_email`
  - `event_type`
  - `metadata_json`
  - `created_at`
- [ ] Add `user_usage_daily` aggregate table:
  - `user_email`
  - `activity_date`
  - `active_seconds`
  - `lesson_completions`
  - `ai_attempts`
  - `quiz_attempts`
  - `last_seen_at`
- [ ] Ensure `admin_audit_logs` continues to record admin edits/deletes/payment overrides.

Password reset:
- [ ] Add `password_reset_tokens` table:
  - `id`
  - `email`
  - `token_hash`
  - `expires_at`
  - `consumed_at`
  - `created_at`
  - `request_ip_hash`
- [ ] Add index on `email, created_at desc`.
- [ ] Add unique index on `token_hash`.

Acceptance:
- Clean `vsign_prod` migration works from `V1`.
- Existing local `v-sign_schema` migration path is understood before applying new migration.
- No production secret is committed.

---

## 6. Phase 2 - PayOS Backend Integration

### Backend API Shape

Keep authenticated:

```text
POST /api/v1/payments/orders
GET  /api/v1/payments/{transactionId}
GET  /api/v1/me/payments
GET  /api/v1/me/subscription
```

Add public but signature-verified:

```text
POST /api/v1/payments/payos/webhook
```

Add authenticated user recovery endpoint:

```text
POST /api/v1/payments/{transactionId}/sync
```

Add admin support endpoint:

```text
POST /api/v1/admin/payments/{transactionId}/sync
```

### Implementation Tasks

- [ ] Create `PayosProperties`.
- [ ] Create `PayosClient`.
- [ ] Create PayOS DTOs for create-payment-link response and webhook payload.
- [ ] Replace synthetic QR/deep-link generation in `PaymentService.createOrder()`.
- [ ] Generate stable numeric PayOS `orderCode`.
- [ ] Persist PayOS `orderCode`, payment link ID, checkout URL, QR code, amount, and expiry.
- [ ] Enforce backend plan amount from `subscription_plans`.
- [ ] Reject client-supplied amount if it does not match plan price.
- [ ] Set provider to `PAYOS` for real production orders.
- [ ] Add webhook endpoint.
- [ ] Verify PayOS webhook signature with `PAYOS_CHECKSUM_KEY`.
- [ ] Store webhook event id/hash before processing.
- [ ] Make webhook processing idempotent.
- [ ] On paid webhook:
  - mark order `PAID`
  - set `paid_at`
  - activate or extend `user_subscriptions`
  - set account type to premium only if current business rules require it
  - write admin/audit/payment event
- [ ] On canceled/expired webhook:
  - mark order `CANCELED` or `EXPIRED`
  - keep subscription unchanged
- [ ] Keep manual admin override but label it as exceptional support action.
- [ ] Add error-safe logging: log transaction/order codes, never API key/checksum.

### Reliability And Reconciliation Tasks

These tasks cover the risk where a user pays successfully, but subscription is not updated because the server is down, the webhook is delayed, PayOS retry fails, DB commit fails, or deployment restarts during processing.

- [ ] Store local order as `PENDING` before redirecting user to PayOS.
- [ ] Treat PayOS webhook as one input, not the only recovery path.
- [ ] Make subscription activation idempotent by `transaction_id` or `payment_subscription_events.idempotency_key`.
- [ ] Process payment status update and subscription activation in one DB transaction when possible.
- [ ] If payment status update succeeds but subscription activation fails, mark order `PAID_SUBSCRIPTION_PENDING`.
- [ ] Add user-owned `POST /api/v1/payments/{transactionId}/sync` that queries PayOS and attempts recovery.
- [ ] Add admin `POST /api/v1/admin/payments/{transactionId}/sync` for support recovery.
- [ ] Add scheduled reconciliation job:
  - check recent `PENDING`, `PENDING_CONFIRMATION`, and `PAID_SUBSCRIPTION_PENDING` PayOS orders
  - query PayOS payment status
  - update local payment status
  - activate missing subscription if PayOS says paid
  - record run summary in `payment_reconciliation_runs`
- [ ] Add startup-safe behavior: reconciliation job may run after container restart and must be idempotent.
- [ ] Add retry/backoff for PayOS status query failures.
- [ ] Keep failed sync error in `payment_orders.sync_error`.
- [ ] Expose `PENDING_CONFIRMATION` to FE when PayOS has not been confirmed yet.
- [ ] Expose `PAID_SUBSCRIPTION_PENDING` to FE/admin if payment is paid but subscription activation still needs recovery.
- [ ] Add admin dashboard alert/list for orders that are paid but subscription is pending.
- [ ] Never ask the user to pay again while an order is `PENDING_CONFIRMATION` or `PAID_SUBSCRIPTION_PENDING`.
- [ ] Add support-safe manual action: "Recheck PayOS" before any manual premium activation.

### Security Tasks

- [ ] Permit unauthenticated `POST /api/v1/payments/payos/webhook` in `SecurityConfig`.
- [ ] Require signature verification before processing webhook body.
- [ ] Add Caddy body-size limit coverage under existing `/api/v1/*` proxy.
- [ ] Do not activate premium from FE return URL alone.
- [ ] Do not trust FE `status=success`.

### Tests

- [ ] Create order uses plan price, not FE amount.
- [ ] Unauthenticated create order is rejected.
- [ ] User cannot read another user's payment.
- [ ] Valid PayOS webhook marks order paid.
- [ ] Duplicate webhook is idempotent.
- [ ] Invalid signature is rejected and does not change order.
- [ ] Paid webhook activates subscription.
- [ ] Paid webhook followed by DB/subscription failure leaves order recoverable as `PAID_SUBSCRIPTION_PENDING`.
- [ ] User sync endpoint repairs a paid order when webhook was missed.
- [ ] Scheduled reconciliation repairs paid orders after simulated server downtime.
- [ ] Duplicate reconciliation runs do not activate the same subscription twice.
- [ ] PayOS query failure stores sync error and keeps the order retryable.
- [ ] Canceled/expired webhook does not activate subscription.
- [ ] Admin manual override still requires reason and audit log.

Acceptance:
- PayOS checkout URL is real.
- User becomes premium only after verified webhook.
- User also becomes premium after verified PayOS status reconciliation if webhook was missed.
- Payment status and history reflect backend truth.
- A successful PayOS payment cannot be permanently stuck without admin visibility and a recovery path.

---

## 7. Phase 3 - Google Login Backend

Recommended flow: backend OAuth authorization-code redirect.

Public endpoints:

```text
GET  /api/v1/auth/google/start
GET  /api/v1/auth/google/callback
POST /api/v1/auth/oauth/exchange
```

Flow:

```text
FE "Continue with Google"
  -> navigate to /api/v1/auth/google/start
  -> backend redirects to Google
  -> Google redirects to /api/v1/auth/google/callback
  -> backend validates state, exchanges code, gets Google user info
  -> backend creates short-lived one-time login code
  -> backend redirects to FRONTEND_OAUTH_SUCCESS_URL?code=<one-time-code>
  -> FE calls POST /api/v1/auth/oauth/exchange
  -> backend returns existing AuthResponse with JWT + user role/account type
```

Implementation tasks:

- [ ] Add Google OAuth properties.
- [ ] Add signed/encrypted OAuth state service.
- [ ] Store short-lived OAuth login codes server-side:
  - code
  - email
  - expires_at
  - consumed_at
- [ ] Exchange authorization code with Google token endpoint.
- [ ] Validate:
  - issuer
  - audience/client id
  - expiration
  - email verified
  - state
- [ ] Upsert user by verified email:
  - if user exists, keep existing role/account type
  - if user does not exist, create active `USER` with `BASIC`
  - store `google_subject`, `auth_provider`, avatar, full name, last login
- [ ] Return the existing `AuthResponse` shape so FE auth integration stays simple.
- [ ] Existing password login remains available.
- [ ] If a password user logs in with matching verified Google email, link Google subject to that account.
- [ ] If Google email is unverified, reject login.

Security tasks:

- [ ] Do not put JWT access token in query string.
- [ ] Use one-time exchange code instead.
- [ ] Expire exchange code quickly, recommended 1-5 minutes.
- [ ] Consume exchange code exactly once.
- [ ] Do not create admin accounts from Google login automatically.
- [ ] Existing admin role must come only from DB/admin process.

Tests:

- [ ] Start endpoint returns/redirects to Google with valid state.
- [ ] Callback rejects invalid state.
- [ ] Callback rejects unverified email.
- [ ] New Google user gets role `USER`.
- [ ] Existing admin email keeps `ADMIN` role after Google login.
- [ ] Exchange code can be used once only.
- [ ] Expired exchange code is rejected.

Acceptance:
- Google button logs users in.
- Auth token and role are issued by backend only.
- Admin visibility still depends on backend role.

---

## 8. Phase 4 - Password Reset Email Flow

Current source state:
- `AuthService.requestPasswordReset()` exists but intentionally does nothing.
- Login UI already has a forgot-password mode.

Backend endpoints:

```text
POST /api/v1/auth/password-reset/request
POST /api/v1/auth/password-reset/confirm
```

Backend tasks:

- [ ] Add `PasswordResetTokenEntity` and repository.
- [ ] Generate cryptographically random reset token.
- [ ] Store only token hash, never raw token.
- [ ] Token expiry comes from `PASSWORD_RESET_TOKEN_EXPIRATION_MINUTES`.
- [ ] Send reset email through SMTP config.
- [ ] Email link points to:

```text
https://<vercel-domain>/reset-password?token=<raw-token>
```

- [ ] Request endpoint must always return generic success to avoid account enumeration.
- [ ] Confirm endpoint validates token hash, expiry, and `consumed_at`.
- [ ] Confirm endpoint updates password hash.
- [ ] Confirm endpoint consumes token exactly once.
- [ ] Confirm endpoint invalidates other active reset tokens for same email.
- [ ] Add request rate limit by email/IP.
- [ ] Add audit/security event for reset requested and reset completed.
- [ ] Keep password login working after reset.
- [ ] For Google-only accounts with no password hash, allow setting a password through reset only after email ownership is proven.

Frontend tasks:

- [ ] Wire forgot-password form in `LoginModal.tsx` to backend request endpoint.
- [ ] Add `/reset-password` route.
- [ ] Add reset password page with token from query string.
- [ ] Validate password strength client-side for UX, but enforce on backend.
- [ ] Show generic success after request regardless of whether email exists.
- [ ] After successful reset, route user back to login.

Email tasks:

- [ ] Use `MAIL_FROM` as sender.
- [ ] Keep email content minimal: app name, reset button/link, expiry warning.
- [ ] Do not include old password or any sensitive account data.

Tests:

- [ ] Reset request always returns generic success.
- [ ] Existing user receives token through mail sender abstraction.
- [ ] Unknown email does not reveal account existence.
- [ ] Expired token is rejected.
- [ ] Consumed token is rejected.
- [ ] Token can be used once.
- [ ] Password is updated and old password no longer works.
- [ ] Rate limit blocks abuse.

Acceptance:
- Password reset works in local/dev and production env.
- No raw reset token is stored.
- No account enumeration leak.

---

## 9. Phase 5 - Admin Backend Completion

### User Management

Current `GET /api/v1/admin/users` should be moved from mock/admin projection data to real `users` data.

Required endpoints:

```text
GET    /api/v1/admin/users?search=&role=&status=&page=&size=
GET    /api/v1/admin/users/{userId}
PATCH  /api/v1/admin/users/{userId}
DELETE /api/v1/admin/users/{userId}
GET    /api/v1/admin/users/{userId}/activity
```

Behavior:

- [ ] `GET /admin/users` reads real `users`.
- [ ] Filter by email/name/role/status/account type.
- [ ] Paginate and sort by created/last seen.
- [ ] `PATCH` allows:
  - display name
  - active/disabled state
  - account type when allowed
  - role only for `SUPER_ADMIN`
- [ ] `DELETE` soft-deactivates user in v1.
- [ ] Prevent an admin from disabling/deleting themself.
- [ ] Prevent deleting/demoting the last `SUPER_ADMIN`.
- [ ] Write audit log for every mutation.

### Metrics

Expand admin metrics:

```text
GET /api/v1/admin/metrics/overview?from=&to=
GET /api/v1/admin/metrics/usage?from=&to=&granularity=daily
```

Metrics to expose:

- [ ] Total users.
- [ ] New users in range.
- [ ] Active users in range.
- [ ] Premium users.
- [ ] Revenue from paid PayOS orders.
- [ ] Successful payment count.
- [ ] Lesson completions.
- [ ] Quiz attempts.
- [ ] AI attempts.
- [ ] AI success rate.
- [ ] Average active time per active user.
- [ ] Top active users.

### User Activity Time

Implementation options:

- Primary: FE heartbeat while logged in.
- Secondary: derive activity events from backend actions.

Recommended v1:

```text
POST /api/v1/me/activity/heartbeat
```

Rules:

- [ ] Authenticated only.
- [ ] FE sends heartbeat every 60 seconds while app tab is active.
- [ ] Backend caps counted seconds to avoid inflated usage.
- [ ] Backend writes daily aggregate to `user_usage_daily`.
- [ ] Backend updates `users.last_seen_at`.

Acceptance:
- Admin metrics come from real app tables.
- Admin user operations affect real users.
- Admin mutations are audited.

---

## 10. Phase 6 - Frontend Payment UX

Files likely involved:

- `D:\v-sign-fe\src\components\PremiumModal.tsx`
- `D:\v-sign-fe\src\services\vsignApi.ts`
- `D:\v-sign-fe\src\contexts\AuthContext.tsx`
- New payment result pages under `D:\v-sign-fe\src\pages`

Tasks:

- [ ] Replace provider choices `MOMO`/`ZALOPAY` with PayOS.
- [ ] `createOrder()` calls backend and receives PayOS `checkoutUrl`.
- [ ] Open PayOS checkout URL in same tab or controlled popup.
- [ ] Add route `/payment/success`.
- [ ] Add route `/payment/cancel`.
- [ ] Success page does not grant premium directly.
- [ ] Success page polls `GET /payments/{transactionId}` or refreshes `/me/subscription`.
- [ ] If webhook has not arrived, show `PENDING_CONFIRMATION` state and retry.
- [ ] Add a "Recheck payment" action that calls `POST /payments/{transactionId}/sync`.
- [ ] If payment is paid but subscription is still being repaired, show `PAID_SUBSCRIPTION_PENDING` and keep polling.
- [ ] Do not show "pay again" while the current order is still recoverable.
- [ ] Show support message with transaction ID if recovery keeps failing after configured retries.
- [ ] Cancel page shows safe retry option.
- [ ] Remove simulated success/failure buttons from production.
- [ ] Refresh `/me/subscription` and `/me/payments` after payment returns.

Acceptance:
- FE cannot fake payment success.
- Premium activates only after backend subscription reports active.
- Payment history matches backend.
- Paid users have a clear pending/recovery state if subscription update is delayed.

---

## 11. Phase 7 - Frontend Google Login

Files likely involved:

- `D:\v-sign-fe\src\components\LoginModal.tsx`
- `D:\v-sign-fe\src\contexts\AuthContext.tsx`
- `D:\v-sign-fe\src\services\vsignApi.ts`
- `D:\v-sign-fe\src\App.tsx`
- New pages:
  - `D:\v-sign-fe\src\pages\OAuthSuccess.tsx`
  - `D:\v-sign-fe\src\pages\OAuthFailure.tsx`

Tasks:

- [ ] Replace placeholder Google button handler.
- [ ] On click, redirect browser to `${VITE_API_BASE_URL}/auth/google/start`.
- [ ] Add `/oauth/success` route.
- [ ] Success page reads one-time `code`.
- [ ] Success page calls `POST /auth/oauth/exchange`.
- [ ] Store returned JWT through existing AuthContext.
- [ ] Route user:
  - admin -> `/admin`
  - normal user -> `/home` or onboarding if new
- [ ] Add `/oauth/failure` route with safe retry.

Acceptance:
- Google login works on Vercel.
- JWT is not exposed in URL.
- Role is taken from backend response only.

---

## 12. Phase 8 - Frontend Admin Dashboard

Admin route:

```text
/admin
```

Visibility rules:

- [ ] Admin sidebar/nav item appears only when `profile.role` is `ADMIN` or `SUPER_ADMIN`.
- [ ] Direct route access checks role in React route guard.
- [ ] Backend still enforces role; frontend guard is only UX.
- [ ] Non-admin users see redirect or 404, not admin layout.

Required admin views:

- [ ] Overview:
  - users
  - active users
  - premium users
  - revenue
  - payment success count
  - lesson completions
  - AI attempts/success rate
- [ ] Users:
  - table
  - search/filter
  - edit display name/status/account type
  - soft delete/deactivate
  - user detail drawer
- [ ] User activity:
  - last login
  - last seen
  - active time
  - completed lessons
  - AI attempts
  - payments
- [ ] Payments:
  - PayOS order list
  - status
  - amount
  - user
  - stuck `PENDING_CONFIRMATION` / `PAID_SUBSCRIPTION_PENDING` filter
  - "Recheck PayOS" action
  - manual override only if needed and reason required
- [ ] Audit logs:
  - actor
  - action
  - target
  - reason
  - timestamp

Design direction:

- Operational dashboard, not a marketing page.
- Dense tables, filters, tabs, and small KPI panels.
- Use existing UI primitives and existing app layout where possible.
- Avoid card-heavy decorative layout.

Acceptance:
- Admin sees dashboard after login.
- Normal user cannot see nav item or route.
- Backend returns 403 if normal user calls admin API manually.

---

## 13. Phase 9 - Deploy Plan Updates

Update these files after implementation:

- [ ] Root `.env.prod.example`.
- [ ] `v-sign-be/.env.prod.example`.
- [ ] `AWS_C6A_DEPLOY_PLAN.md`.
- [ ] `PRE_DEPLOY_HOLISTIC_REFACTOR_PLAN.md` session log or follow-up section.
- [ ] `v-sign-be/docs/deployment/deployment-smoke-test-checklist.md`.
- [ ] `v-sign-be/docs/deployment/github-actions-ghcr-caddy-runbook.md`.

Server `/opt/vsign/.env.prod` must include:

- [ ] PayOS values.
- [ ] Google OAuth values.
- [ ] SMTP/password reset values.
- [ ] Existing DB/JWT/CORS/AI values.

Vercel env must include:

- [ ] `VITE_API_BASE_URL=https://api.<domain>/api/v1`.

Google Console config:

- [ ] Authorized JavaScript origin:

```text
https://<vercel-domain>
```

- [ ] Authorized redirect URI:

```text
https://api.<domain>/api/v1/auth/google/callback
```

PayOS dashboard config:

- [ ] Return URL:

```text
https://<vercel-domain>/payment/success
```

- [ ] Cancel URL:

```text
https://<vercel-domain>/payment/cancel
```

- [ ] Webhook URL:

```text
https://api.<domain>/api/v1/payments/payos/webhook
```

Acceptance:
- A push to `main` still deploys backend + AI automatically.
- No extra public service is required.
- No PayOS/Google/SMTP secret is committed.

---

## 14. Phase 10 - Test Matrix

Backend:

- [ ] `mvn.cmd -q test`
- [ ] PayOS create order tests.
- [ ] PayOS webhook valid/invalid/duplicate tests.
- [ ] PayOS missed-webhook reconciliation tests.
- [ ] PayOS paid-but-subscription-pending recovery tests.
- [ ] Subscription activation tests.
- [ ] Google OAuth callback/exchange tests.
- [ ] Password reset request/confirm/rate-limit tests.
- [ ] Admin role authorization tests.
- [ ] Admin user edit/delete audit tests.
- [ ] Admin metrics tests.
- [ ] Activity heartbeat aggregation tests.

Frontend:

- [ ] `npm.cmd run lint`
- [ ] `npm.cmd run test -- aiRecognition`
- [ ] Add admin route tests if test setup supports it.
- [ ] Add payment success/cancel page tests if test setup supports it.
- [ ] Add reset-password page tests if test setup supports it.
- [ ] `npm.cmd run build`

Manual browser:

- [ ] Password login still works.
- [ ] Password reset email request works and reset link changes password.
- [ ] Google login works.
- [ ] New Google user becomes normal user.
- [ ] Existing admin Google login can access `/admin`.
- [ ] Normal user cannot access `/admin`.
- [ ] PayOS checkout opens.
- [ ] Payment success waits for backend subscription.
- [ ] Payment success stays pending if webhook is delayed, then updates after sync/retry.
- [ ] Payment cancel does not activate premium.
- [ ] Admin can list users.
- [ ] Admin can edit/deactivate user.
- [ ] Admin metrics show real values.

Production smoke:

- [ ] `GET https://api.<domain>/api/v1/health`.
- [ ] PayOS webhook receives public POST and verifies signature.
- [ ] PayOS reconciliation can repair a paid order if webhook was missed.
- [ ] Google callback URL is reachable.
- [ ] SMTP password reset works with production env.
- [ ] Vercel frontend uses production API URL.

---

## 15. Recommended Implementation Order

1. Add env examples and backend properties for PayOS, Google OAuth, and SMTP/password reset.
2. Add database migration for PayOS, webhook events, reconciliation, Google auth, password reset, and usage tracking.
3. Implement PayOS backend client and create-payment-link flow.
4. Implement PayOS webhook with signature verification and idempotency.
5. Implement subscription activation from verified PayOS webhook.
6. Implement PayOS reconciliation: user sync, admin sync, scheduled repair job, and paid-but-subscription-pending state.
7. Update frontend Premium modal and payment success/cancel/pending pages.
8. Implement password reset backend and frontend flow.
9. Implement Google OAuth backend flow.
10. Update frontend Google login and OAuth success/failure pages.
11. Replace admin user service with real `users` table data.
12. Add admin edit/deactivate endpoints and audit logs.
13. Add user activity heartbeat and metrics aggregation.
14. Build admin dashboard UI.
15. Update deploy runbooks and smoke checklist.
16. Run full backend/AI/frontend verification.
17. Deploy through existing GitHub Actions + GHCR + Caddy flow.

---

## 16. Release Gates

Gate 1 - Local complete:
- [ ] Backend tests pass.
- [ ] Frontend build passes.
- [ ] No secrets in Git.
- [ ] `.env.prod.example` documents all required variables.

Gate 2 - Staging/prod dry run:
- [ ] PayOS sandbox or controlled low-value payment works.
- [ ] Simulated missed webhook is repaired by sync/reconciliation.
- [ ] Password reset works through production-like SMTP.
- [ ] Google login works on real Vercel domain.
- [ ] Admin route is hidden from normal users.
- [ ] Admin APIs return 403 for normal users.

Gate 3 - Production:
- [ ] PayOS webhook verified and idempotent.
- [ ] Subscription activation is correct from both webhook and reconciliation.
- [ ] Paid orders cannot remain invisible to admin if subscription activation fails.
- [ ] Password reset is active and protected from account enumeration.
- [ ] Admin user mutations are audited.
- [ ] Rollback plan still works by GHCR SHA.

---

## 17. External Docs To Check During Implementation

- PayOS developer docs: `https://payos.vn/docs/`
- Google Identity Services / OAuth docs: `https://developers.google.com/identity`
- Google ID token verification docs: `https://developers.google.com/identity/gsi/web/guides/verify-google-id-token`

Use the current provider docs during implementation because payment/webhook fields can change.
