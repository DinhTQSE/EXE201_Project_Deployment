# PayOS Payment Module — Setup Guide & Implementation Map

## Table of Contents
1. [Where the code lives](#where-the-code-lives)
2. [Files modified in existing code](#files-modified-in-existing-code)
3. [Environment variables](#environment-variables)
4. [Local development setup](#local-development-setup)
5. [Production setup (Nginx + HTTPS)](#production-setup-nginx--https)
6. [API endpoints](#api-endpoints)
7. [End-to-end payment flow](#end-to-end-payment-flow)
8. [Seeded tier data](#seeded-tier-data)
9. [Known gaps to address before launch](#known-gaps-to-address-before-launch)

---

## Where the code lives

All new payment code lives in one package:

```
v-sign-be/src/main/java/com/vsign/backend/payment/
│
├── config/
│   └── PayOSConfig.java              # Reads env vars, creates PayOS SDK bean
│
├── controller/
│   ├── PayOSPaymentController.java   # REST: /api/v1/payments/**
│   └── PayOSWebhookController.java   # REST: /api/v1/payments/payos/webhook
│
├── dto/
│   ├── CreatePaymentRequest.java     # { tierId }
│   ├── CreatePaymentResponse.java    # { checkoutUrl, orderCode, ... }
│   ├── PayOSReturnRequest.java       # { orderCode, status, cancel }
│   ├── PayOSReturnResponse.java      # { orderCode, resolvedStatus }
│   ├── PaymentHistoryResponse.java   # one order in history list
│   └── TierResponse.java            # one tier in tier list
│
├── persistence/
│   ├── TierEntity.java               # DB table: tier
│   ├── TierRepository.java
│   ├── UserTierEntity.java           # DB table: user_tier
│   ├── UserTierRepository.java
│   ├── PayOSOrderEntity.java         # DB table: payment_order
│   ├── PayOSOrderRepository.java
│   ├── PayOSTransactionEntity.java   # DB table: payment_transaction
│   ├── PayOSTransactionRepository.java
│   ├── PaymentOrderStatus.java       # enum: PENDING, PAID, CANCELLED, EXPIRED, FAILED
│   └── PaymentTransactionStatus.java # enum: PENDING, SUCCESS, FAILED
│
└── service/
    ├── PayOSPaymentService.java      # checkout creation, return URL handling
    ├── PayOSWebhookService.java      # webhook dispatch, subscription activation
    └── PaymentExpiryScheduler.java   # background job: expires stale orders
```

**Database migration:**
```
v-sign-be/src/main/resources/db/migration/V23__add_payos_payment_module.sql
```
Creates 4 tables and seeds 3 tiers (free / plus / pro).

---

## Files modified in existing code

These existing files were changed — not created from scratch:

| File | What changed |
|---|---|
| `pom.xml` | Added `vn.payos:payos-java:2.0.1` dependency |
| `src/main/resources/application.properties` | Added 5 PayOS property stubs (`payos.*`) |
| `src/main/resources/.gitignore` (v-sign-be root) | Added `secretKey.properties` rule |
| `common/security/SecurityConfig.java` | Added 3 `permitAll` rules for `/tiers` and webhook endpoints |
| `auth/service/AuthService.java` | `register()` now creates a free `UserTierEntity` for every new user |
| `VSignBackendApplication.java` | Added `@EnableScheduling` |

---

## Environment variables

### PayOS credentials (get from payos.vn dashboard)

| Variable | Where to find it | Example |
|---|---|---|
| `PAYOS_CLIENT_ID` | PayOS dashboard → your app | `abc123` |
| `PAYOS_API_KEY` | PayOS dashboard → your app | `sk_live_...` |
| `PAYOS_CHECKSUM_KEY` | PayOS dashboard → your app | `ck_live_...` |

### URL configuration

| Variable | What it is | Example |
|---|---|---|
| `PAYOS_RETURN_URL` | Frontend page PayOS redirects to after payment (any status) | `https://vsign.com/payment/result` |
| `PAYOS_CANCEL_URL` | Frontend page PayOS redirects to when user cancels | `https://vsign.com/payment/cancel` |

> `PAYOS_RETURN_URL` and `PAYOS_CANCEL_URL` are **frontend URLs**, not backend URLs.
> PayOS redirects the user's browser there after they finish on the PayOS payment page.

---

## Local development setup

**Step 1 — Create the secrets file** (this file is git-ignored, never commit it)

Create `v-sign-be/src/main/resources/secretKey.properties`:

```properties
# PayOS credentials from payos.vn dashboard
PAYOS_CLIENT_ID=your-client-id
PAYOS_API_KEY=your-api-key
PAYOS_CHECKSUM_KEY=your-checksum-key

# Frontend URLs (can be localhost for dev)
PAYOS_RETURN_URL=http://localhost:5173/payment/result
PAYOS_CANCEL_URL=http://localhost:5173/payment/cancel
```

**Step 2 — Expose localhost to PayOS via ngrok**

PayOS needs to reach your webhook over public HTTPS. Use ngrok for local testing:

```bash
# Install ngrok: https://ngrok.com/download
ngrok http 8080
# Output: Forwarding https://abc123.ngrok.io -> localhost:8080
```

**Step 3 — Register the webhook URL in the PayOS dashboard**

Go to PayOS dashboard → your app → Webhook URL, enter:

```
https://abc123.ngrok.io/V-sign/api/v1/payments/payos/webhook
```

PayOS will send a `GET` request to verify the URL is alive (the endpoint returns `200 OK`).

**Step 4 — Start the backend normally**

```bash
cd v-sign-be
mvn spring-boot:run
```

---

## Production setup (Nginx + HTTPS)

PayOS **requires HTTPS** for the webhook endpoint. HTTP will not work.

### Prerequisites
- A public domain pointing to your server (e.g. `api.vsign.com`)
- SSL certificate — use Let's Encrypt / Certbot (free)

### Install Certbot (Amazon Linux / Ubuntu)
```bash
# Ubuntu
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d api.vsign.com

# Amazon Linux 2
sudo amazon-linux-extras install epel -y
sudo yum install certbot python-certbot-nginx
sudo certbot --nginx -d api.vsign.com
```

### Nginx server block

Add or edit `/etc/nginx/sites-available/vsign` (Ubuntu) or `/etc/nginx/conf.d/vsign.conf` (Amazon Linux):

```nginx
server {
    listen 443 ssl;
    server_name api.vsign.com;  # replace with your domain

    ssl_certificate     /etc/letsencrypt/live/api.vsign.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.vsign.com/privkey.pem;

    # Increase body size limit for webhook payloads
    client_max_body_size 2M;

    location /V-sign/ {
        proxy_pass         http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;

        # Prevent Nginx from buffering webhook requests
        proxy_request_buffering off;
    }
}

server {
    listen 80;
    server_name api.vsign.com;
    return 301 https://$host$request_uri;
}
```

```bash
sudo nginx -t          # test config
sudo systemctl reload nginx
```

### Register webhook URL in PayOS dashboard

```
https://api.vsign.com/V-sign/api/v1/payments/payos/webhook
```

### Production environment variables (Docker example)

```bash
docker run -d \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://... \
  -e DB_USER=... \
  -e DB_PASSWORD=... \
  -e DB_SCHEMA=public \
  -e JWT_SECRET=... \
  -e JWT_EXPIRATION_MS=86400000 \
  -e JWT_REFRESH_EXPIRATION_MS=604800000 \
  -e PAYOS_CLIENT_ID=... \
  -e PAYOS_API_KEY=... \
  -e PAYOS_CHECKSUM_KEY=... \
  -e PAYOS_RETURN_URL=https://vsign.com/payment/result \
  -e PAYOS_CANCEL_URL=https://vsign.com/payment/cancel \
  vsign-backend:latest
```

---

## API endpoints

**Base path (all requests):** `/V-sign/api/v1/payments`

> All authenticated endpoints require `Authorization: Bearer <jwt>` header.

### `GET /tiers` — public, no auth

Returns available subscription plans.

**Response:**
```json
[
  {
    "tierId": "00000000-0000-0000-0000-000000000001",
    "title": "free",
    "amount": 0,
    "noMonth": 1,
    "limitedToken": 20,
    "isActive": true
  },
  {
    "tierId": "00000000-0000-0000-0000-000000000002",
    "title": "plus",
    "amount": 49000,
    "noMonth": 1,
    "limitedToken": 100,
    "isActive": true
  },
  {
    "tierId": "00000000-0000-0000-0000-000000000003",
    "title": "pro",
    "amount": 99000,
    "noMonth": 1,
    "limitedToken": 999,
    "isActive": true
  }
]
```

---

### `POST /checkout` — requires auth

Creates a PayOS payment link for the selected tier.

**Request:**
```json
{ "tierId": "00000000-0000-0000-0000-000000000002" }
```

**Response:**
```json
{
  "orderId": "3fa85f64-...",
  "orderCode": 582739123,
  "paymentLinkId": "payos-link-id",
  "checkoutUrl": "https://pay.payos.vn/web/abc123",
  "qrCode": "data:image/png;base64,...",
  "amount": 49000,
  "status": "PENDING",
  "expiredAt": "2026-06-17T16:15:00"
}
```

Redirect the user to `checkoutUrl`. The payment link expires in **15 minutes**.

**Error cases:**
- `400` — tierId not found, or tier is free (amount = 0)
- `409` (IllegalStateException) — user already has an active paid subscription

---

### `POST /payos/return` — requires auth

Called by your **frontend** after PayOS redirects back. PayOS appends query parameters to `PAYOS_RETURN_URL`; your frontend reads them and posts here to sync the order status in your database.

**Request** (map from PayOS query params):
```json
{
  "orderCode": 582739123,
  "status": "PAID",
  "cancel": false
}
```

**Response:**
```json
{
  "orderCode": 582739123,
  "resolvedStatus": "PAID",
  "message": "OK"
}
```

> This endpoint only updates order status for the UI. It does **not** activate the subscription.
> Subscription activation happens exclusively via the webhook.

---

### `GET /me` — requires auth

Returns payment history for the logged-in user.

**Response:**
```json
[
  {
    "orderId": "3fa85f64-...",
    "orderCode": 582739123,
    "tierId": "00000000-0000-0000-0000-000000000002",
    "tierTitle": "plus",
    "amount": 49000,
    "status": "PAID",
    "paymentLinkId": "payos-link-id",
    "createdAt": "2026-06-17T15:00:00",
    "paidAt": "2026-06-17T15:03:22",
    "expiredAt": "2026-06-17T15:15:00"
  }
]
```

---

### `GET /payos/webhook` — public

PayOS calls this to verify the webhook URL is alive. Returns `200 OK`.

### `POST /payos/webhook` — public

PayOS calls this when a payment event occurs. The backend verifies the signature using `PAYOS_CHECKSUM_KEY` before processing.

**PayOS webhook codes handled:**
| Code | Meaning | Action |
|---|---|---|
| `00` | Payment successful | Mark order PAID, save transaction, activate subscription |
| `01` | Payment cancelled | Mark order CANCELLED |
| `02` | Payment expired | Mark order EXPIRED |

---

## End-to-end payment flow

```
┌──────────┐                ┌──────────────┐              ┌────────────┐
│ Frontend │                │  V-Sign API  │              │   PayOS    │
└──────────┘                └──────────────┘              └────────────┘
     │                             │                             │
     │  GET /tiers                 │                             │
     │────────────────────────────>│                             │
     │  [free, plus, pro]          │                             │
     │<────────────────────────────│                             │
     │                             │                             │
     │  POST /checkout {tierId}    │                             │
     │────────────────────────────>│                             │
     │                             │  Create payment link        │
     │                             │────────────────────────────>│
     │                             │  { checkoutUrl }            │
     │                             │<────────────────────────────│
     │  { checkoutUrl }            │                             │
     │<────────────────────────────│                             │
     │                             │                             │
     │  Redirect user to checkoutUrl                             │
     │──────────────────────────────────────────────────────────>│
     │                             │                             │
     │                             │     (user pays on PayOS)    │
     │                             │                             │
     │                             │  POST /webhook {code:"00"}  │
     │                             │<────────────────────────────│
     │                             │  Verify signature ✓         │
     │                             │  Activate subscription      │
     │                             │  200 OK                     │
     │                             │────────────────────────────>│
     │                             │                             │
     │  Redirect to PAYOS_RETURN_URL                             │
     │<──────────────────────────────────────────────────────────│
     │                             │                             │
     │  POST /payos/return         │                             │
     │────────────────────────────>│                             │
     │  { resolvedStatus: "PAID" } │                             │
     │<────────────────────────────│                             │
     │                             │                             │
     │  Refresh UI to show new tier│                             │
```

---

## Seeded tier data

These 3 rows are inserted by `V23__add_payos_payment_module.sql` with **fixed UUIDs** (safe to hardcode on the frontend):

| UUID | Title | Price | AI tokens/month |
|---|---|---|---|
| `00000000-0000-0000-0000-000000000001` | free | 0 VND | 20 |
| `00000000-0000-0000-0000-000000000002` | plus | 49,000 VND | 100 |
| `00000000-0000-0000-0000-000000000003` | pro | 99,000 VND | 999 |

Every new user gets the free tier automatically at registration (handled in `AuthService.register()`).

---

## Known gaps to address before launch

### 1. Frontend return page not built yet
When PayOS redirects to `PAYOS_RETURN_URL`, your frontend page needs to:
1. Read query params from the URL: `?orderCode=...&status=PAID&cancel=false`
2. Call `POST /V-sign/api/v1/payments/payos/return` with those values
3. Refresh the user's subscription state

### 2. No "check my current tier" endpoint
The frontend currently has no way to ask "what tier am I on right now?" You probably need a `GET /api/v1/subscriptions/me` endpoint that returns the user's active `UserTierEntity`. This is not implemented yet.

### 3. Paid tier expiry does not re-assign free tier
When a paid subscription expires (handled by `PaymentExpiryScheduler`), the `UserTierEntity` is marked inactive. The user then has no active tier. The code does not auto-create a new free tier record. Decide: either re-assign free tier on expiry, or have the frontend treat "no active tier" as free.

### 4. Rotate secrets before going live
The `secretKey.properties` file on disk contains a real Supabase password and JWT secret. These were flagged by the security reviewer. Rotate both before the first production deploy:
- Go to Supabase → Settings → Database → Reset password
- Generate a new JWT secret (min 64 random characters)
- Update your production environment variables

### 5. Test mode vs live mode
PayOS has a sandbox/test environment. Use test credentials from the PayOS dashboard during development so you are not processing real money. Switch to live credentials only for production.
