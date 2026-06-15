# V-Sign System And Deployment Guide

Last updated: 2026-06-15

This document is for developers joining the project. It explains the current repository split, runtime architecture, deployment flow, environment ownership, and the rules that must not be broken while adding features.

Do not put real secrets in this file.

---

## 1. Repository Layout

The project is intentionally split into separate repositories/directories.

Server repository:

```text
D:\V-sign_EXE101_Project
```

Contains:

```text
v-sign-be/                    Spring Boot backend
v-sign-be-ai/                 Python AI service
.github/workflows/deploy.yml  Backend + AI CI/CD
docker-compose.prod.yml       EC2 production runtime
Caddyfile                     Public HTTPS reverse proxy
AGENTS.md                     Agent/developer guardrails
AWS_C6A_DEPLOY_PLAN.md        Operator deploy runbook
PRE_DEPLOY_HOLISTIC_REFACTOR_PLAN.md
PAYOS_GOOGLE_ADMIN_COMPLETION_PLAN.md
PROJECT_SYSTEM_AND_DEPLOYMENT_GUIDE.md
```

Frontend repository:

```text
D:\v-sign-fe
```

Contains the Vite/React application deployed on Vercel.

Do not move the frontend back into the server repository. If a task touches frontend code, work in `D:\v-sign-fe`.

---

## 2. Current Production Topology

Production shape:

```text
User browser
  -> Vercel frontend: https://v-sign.vercel.app
  -> Backend API: https://apivsignvn.social/api/v1/*
  -> Caddy on EC2
  -> Spring backend container
  -> private Docker network
  -> Python AI container
```

Current infrastructure:

```text
AWS region: ap-southeast-1
EC2 type: c6a.large
OS: Amazon Linux 2023 x86_64
Root EBS: 30 GB gp3
Swap: 4 GB
Deploy path: /opt/vsign
EC2 SSH user: ec2-user
Elastic IP: 18.136.251.56
Backend API domain: apivsignvn.social
Frontend domain: https://v-sign.vercel.app
```

Only Caddy is public. Backend and AI are private containers.

Public ports:

```text
80   Caddy HTTP challenge / redirect path
443  Caddy HTTPS
22   SSH deploy/admin access
```

Do not open:

```text
8080  backend
8000  AI service
5432  database
```

---

## 3. Runtime Services

Production Docker Compose runs three services:

```text
backend  -> ghcr.io/<owner>/vsign-backend:<sha-or-latest>
ai       -> ghcr.io/<owner>/vsign-ai:<sha-or-latest>
caddy    -> caddy:2-alpine
```

Backend:

- Spring Boot.
- Runs with `SPRING_PROFILES_ACTIVE=prod`.
- Servlet prefix is `/V-sign`.
- Container listens on `8080`.
- Health check: `/V-sign/api/v1/health`.
- Calls AI through `http://ai:8000`.

AI:

- Python service.
- Container listens on `8000`.
- Exposed only inside Docker network.
- Health check: `/health`.

Caddy:

- Public HTTPS entrypoint.
- Routes only `/api/v1/*`.
- Rewrites `/api/v1` to backend servlet path `/V-sign/api/v1`.
- Returns `404` for all other paths.
- Does not expose `/ai`.

Current Caddy behavior:

```text
/api/v1/* -> backend:8080 with URI rewrite to /V-sign/api/v1/*
/*        -> 404
```

---

## 4. Frontend Deployment

Frontend deploys on Vercel from `D:\v-sign-fe`.

Required Vercel env:

```text
VITE_API_BASE_URL=https://apivsignvn.social/api/v1
```

The frontend uses React Router `BrowserRouter`, so Vercel must have SPA fallback. Keep this file in the FE repo:

```text
D:\v-sign-fe\vercel.json
```

Expected contents:

```json
{
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

Without this rewrite, direct visits such as these will fail with Vercel `404: NOT_FOUND`:

```text
https://v-sign.vercel.app/courses
https://v-sign.vercel.app/ai-recognition
https://v-sign.vercel.app/profile
```

Frontend must call only the backend API. It must not call the AI container or expose an AI service URL.

---

## 5. CI/CD Flow

Server deploy is automated by:

```text
.github/workflows/deploy.yml
```

Trigger:

```text
push to main
manual workflow_dispatch
```

Pipeline:

1. Checkout code.
2. Run targeted backend tests.
3. Run AI syntax check.
4. Build backend Docker image for `linux/amd64`.
5. Build AI Docker image for `linux/amd64`.
6. Push images to GHCR:
   - `ghcr.io/<owner>/vsign-backend:sha-<commit>`
   - `ghcr.io/<owner>/vsign-backend:latest`
   - `ghcr.io/<owner>/vsign-ai:sha-<commit>`
   - `ghcr.io/<owner>/vsign-ai:latest`
7. SSH to EC2.
8. Upload deploy bundle:
   - `docker-compose.prod.yml`
   - `Caddyfile`
   - env example files
9. Write `/opt/vsign/.env.deploy`.
10. Login to GHCR.
11. Pull images.
12. Run Docker Compose up.
13. Prune old images older than 168 hours.

The EC2 server does not build application images.

Rollback uses a previous GHCR `sha-*` tag by changing `IMAGE_TAG` in `/opt/vsign/.env.deploy` or by following `AWS_C6A_DEPLOY_PLAN.md`.

---

## 6. GitHub Secrets

GitHub Actions secrets are deploy transport values only:

```text
SERVER_IP
SERVER_USER
SERVER_SSH_KEY
GHCR_USERNAME
GHCR_TOKEN
APP_DOMAIN
DEPLOY_PATH
```

Current intended values:

```text
SERVER_USER=ec2-user
APP_DOMAIN=apivsignvn.social
DEPLOY_PATH=/opt/vsign
```

Do not put DB/JWT/PayOS/Google/SMTP secrets in GitHub Actions for this deployment model. Production runtime secrets live on the server.

---

## 7. Production Environment Files

Server-only runtime env files:

```text
/opt/vsign/.env.prod
/opt/vsign/.env.ai.prod
/opt/vsign/.env.deploy
```

Committed examples:

```text
.env.prod.example
.env.ai.prod.example
.env.deploy.example
v-sign-be/.env.prod.example
```

Never commit real `.env.prod`, `.env.ai.prod`, `.env.deploy`, `.env`, `*.pem`, or `secretKey.properties`.

Production DB schema:

```text
DB_SCHEMA=vsign_prod
```

Local dev may use:

```text
DB_SCHEMA=v-sign_schema
```

Do not use `public` for production. Do not reuse the local dev schema for production.

---

## 8. AI Recognition Contract

Locked design:

```text
Browser webcam
  -> MediaPipe Holistic in browser
  -> compact landmark sequence
  -> backend authenticated endpoint
  -> private AI service
```

Rules:

- Frontend must not upload webcam images, raw frames, video files, JPEGs, or base64 frames.
- Frontend sends landmark/features only.
- Current feature vector size is exactly `258`.
- Feature order:
  - pose: `33 * (x, y, z, visibility) = 132`
  - left hand: `21 * (x, y, z) = 63`
  - right hand: `21 * (x, y, z) = 63`
- Face landmarks are not part of the current model.
- AI service keeps server-side model preprocessing such as resampling/velocity.

Important files:

```text
D:\v-sign-fe\src\services\holisticLandmarkExtractor.ts
D:\v-sign-fe\src\components\AiCameraPractice.tsx
v-sign-be-ai/api_server.py
v-sign-be-ai/model_v2.py
```

The frontend MediaPipe import must support Vite production bundling. Do not regress the constructor fallback logic in `holisticLandmarkExtractor.ts`.

---

## 9. Backend Rules

Backend is Spring Boot with:

- JWT authentication.
- Flyway migrations.
- `spring.mvc.servlet.path=/V-sign`.
- Production profile disables Swagger/OpenAPI.
- Production profile disables SQL debug.
- Security should remain authenticated-by-default except intended public endpoints.

Protected areas:

- lesson progress/completion
- quiz attempts
- signature workflows
- payments/subscriptions
- profile/me
- gamification
- admin

Lesson completion must remain backend-verified:

- video stage reached
- quiz passed
- AI attempt passed when required
- premium locks enforced server-side
- XP award idempotent
- streak calculated server-side using UTC+7 rules

Do not make frontend optimistic completion the source of truth.

---

## 10. PayOS, Entitlements, Google Login, Password Reset, Admin

Follow:

```text
PAYOS_GOOGLE_ADMIN_COMPLETION_PLAN.md
```

Current business decision:

- Lessons and AI Recognition may remain free until PayOS is fully implemented.
- Do not enforce strict lesson/AI paywalls before the user has a reliable payment and recovery path.
- After PayOS checkout, webhook, reconciliation, and frontend pending states are complete, implement backend-enforced free vs premium gates.

PayOS rules:

- PayOS is the planned real payment provider.
- PayOS return URL is UX only.
- Premium activation must come from verified PayOS webhook or verified PayOS reconciliation.
- Recover missed webhook/server failure with:
  - `PENDING_CONFIRMATION`
  - `PAID_SUBSCRIPTION_PENDING`
  - user sync endpoint
  - admin sync endpoint
  - scheduled reconciliation
  - idempotent subscription activation
- Never ask users to pay again while a recoverable paid/pending order exists.

Entitlement rules after PayOS:

- Backend decides whether a user can access lessons, quiz attempts, AI Recognition, and lesson completion.
- Frontend may render locks/CTAs, but it is not trusted.
- Free limits must be centralized/configurable.
- Direct API calls must not bypass premium limits.

Google login rules:

- New Google users become `USER`.
- Existing admin role may be preserved only when the verified Google email already belongs to an admin account.
- Do not grant admin by frontend flag, localStorage, email domain, or query param.

Password reset rules:

- Implement real email reset.
- Store only hashed reset tokens.
- Tokens are short-lived and single-use.
- Reset request must not leak whether an email exists.

Admin rules:

- Admin UI visibility is frontend UX only.
- Backend role checks are mandatory.
- Admin dashboard visible only to `ADMIN` or `SUPER_ADMIN`.
- User delete means soft deactivate in v1.
- Admin user mutations must write audit logs.
- Prevent self-disable/delete and removing the last `SUPER_ADMIN`.

---

## 11. Local Development

Backend local:

```powershell
cd D:\V-sign_EXE101_Project\v-sign-be
mvn.cmd spring-boot:run
```

AI local:

```powershell
cd D:\V-sign_EXE101_Project\v-sign-be-ai
python api_server.py
```

Frontend local:

```powershell
cd D:\v-sign-fe
npm.cmd run dev
```

Common local env:

```text
Backend API local base: http://localhost:8080/V-sign/api/v1
Frontend dev server: http://localhost:5173
AI service local: http://localhost:8000
```

Frontend `.env.local` should use the project’s local API/proxy convention. Do not commit `.env.local`.

---

## 12. Verification Commands

Backend:

```powershell
cd D:\V-sign_EXE101_Project\v-sign-be
mvn.cmd -q test
mvn.cmd -q "-Dtest=LearningWorkflowIT,GamificationControllerIT,SubscriptionControllerIT,HealthControllerIT,FlywayMigrationTest" test
```

AI:

```powershell
cd D:\V-sign_EXE101_Project\v-sign-be-ai
python -m py_compile api_server.py
```

Server compose:

```powershell
cd D:\V-sign_EXE101_Project
docker compose --env-file .env.deploy.example -f docker-compose.prod.yml config
```

Frontend:

```powershell
cd D:\v-sign-fe
npm.cmd run lint
npm.cmd run test -- aiRecognition
npm.cmd run build
```

Predeploy marker scan:

```powershell
cd D:\V-sign_EXE101_Project
powershell -ExecutionPolicy Bypass -File v-sign-be\scripts\scan-predeploy-markers.ps1
```

---

## 13. Production Smoke Checks

External checks:

```powershell
curl.exe -i https://apivsignvn.social/api/v1/health
curl.exe -i https://apivsignvn.social/api/v1/version
curl.exe -i https://apivsignvn.social/
curl.exe -i https://apivsignvn.social/ai/health
```

Expected:

- `/api/v1/health` returns success.
- `/api/v1/version` returns success if version endpoint is enabled.
- `/` returns 404 from Caddy.
- `/ai/health` returns 404 from Caddy because AI is private.

Server checks:

```bash
cd /opt/vsign
sudo docker compose --env-file .env.deploy -f docker-compose.prod.yml ps
sudo docker compose --env-file .env.deploy -f docker-compose.prod.yml logs --tail=100 backend
sudo docker compose --env-file .env.deploy -f docker-compose.prod.yml logs --tail=100 ai
sudo docker compose --env-file .env.deploy -f docker-compose.prod.yml logs --tail=100 caddy
df -h
free -h
sudo docker system df
```

Frontend checks after Vercel deploy:

```text
https://v-sign.vercel.app/
https://v-sign.vercel.app/courses
https://v-sign.vercel.app/ai-recognition
https://v-sign.vercel.app/profile
```

Direct links should load the React app, not Vercel 404.

---

## 14. Operational Notes

Docker log rotation is already configured:

```text
max-size=10m
max-file=3
```

Deploy workflow prunes old Docker images:

```text
docker image prune -af --filter "until=168h"
```

Do not run broad volume-pruning commands in production unless you understand the impact. Caddy certificate data is stored in Docker volumes:

```text
caddy_data
caddy_config
```

---

## 15. Common Mistakes To Avoid

- Do not expose AI publicly through Caddy.
- Do not add `ports: "8000:8000"` for AI in production.
- Do not add `ports: "8080:8080"` for backend in production.
- Do not make EC2 build backend/AI images.
- Do not put runtime DB/JWT/provider secrets in GitHub Actions.
- Do not switch production DB schema to `public`.
- Do not reuse local `v-sign_schema` for production.
- Do not upload webcam images/base64/video to backend or AI.
- Do not let FE mark lesson completion or premium status as truth.
- Do not activate premium from PayOS return URL alone.
- Do not remove the Vercel SPA rewrite.
- Do not enforce free/premium lesson and AI locks before PayOS recovery is implemented.
- Do not switch to ARM/Graviton without verifying backend and AI images and dependencies for `linux/arm64`.

---

## 16. Main Reference Documents

Read these before major work:

```text
AGENTS.md
PRE_DEPLOY_HOLISTIC_REFACTOR_PLAN.md
AWS_C6A_DEPLOY_PLAN.md
PAYOS_GOOGLE_ADMIN_COMPLETION_PLAN.md
v-sign-be/docs/deployment/github-actions-ghcr-caddy-runbook.md
v-sign-be/docs/deployment/deployment-smoke-test-checklist.md
v-sign-be/docs/ops/baseline-cost-performance.md
```

If older docs conflict with `AGENTS.md`, this file, or the root plan files, follow the newer root-level guidance.
