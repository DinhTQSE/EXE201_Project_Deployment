# V-Sign Agent Operating Guide

This file is for future coding agents working in this project. Read it before making changes. The main goal is to preserve the current architecture and avoid undoing deployment, security, AI, and repository-structure decisions.

## Project Layout

Server repository root:

```text
D:\V-sign_EXE101_Project
```

Important paths:

```text
v-sign-be/       Spring Boot backend
v-sign-be-ai/    Python AI service
.github/         GitHub Actions deploy workflow
docker-compose.prod.yml
Caddyfile
AWS_C6A_DEPLOY_PLAN.md
PRE_DEPLOY_HOLISTIC_REFACTOR_PLAN.md
PAYOS_GOOGLE_ADMIN_COMPLETION_PLAN.md
```

Frontend repository is separate:

```text
D:\v-sign-fe
```

Do not move the frontend back into this server repo. If a request touches frontend code, edit files under `D:\v-sign-fe`.

## Locked Production Architecture

The production shape is:

```text
Vercel frontend
  -> https://api.<domain>/api/v1/*
  -> Caddy on EC2
  -> Spring backend container
  -> private Docker network
  -> AI container
```

Locked decisions:

- Backend + AI deploy to one AWS EC2 server.
- Frontend deploys separately on Vercel.
- Backend and AI communicate over Docker Network.
- AI must not be exposed publicly.
- Caddy exposes only ports `80` and `443`.
- Caddy public route is `/api/v1/*` only.
- Backend port `8080` must not be public.
- AI port `8000` must not be public.
- EC2 pulls Docker images from GHCR. It does not build backend/AI images.
- GitHub Actions builds backend/AI images as `linux/amd64`.
- Selected first production server is AWS EC2 `c6a.large`, Ubuntu 24.04 LTS x86_64, 30 GB gp3, 4 GB swap.
- Do not switch to ARM/Graviton unless backend and AI images and Python AI dependencies are verified for `linux/arm64`.

## Deploy Files

Root deploy files are intentional:

```text
.github/workflows/deploy.yml
docker-compose.prod.yml
Caddyfile
.env.deploy.example
.env.prod.example
.env.ai.prod.example
```

The deploy workflow:

1. Runs backend tests.
2. Runs AI syntax check.
3. Builds backend and AI Docker images.
4. Pushes images to GHCR.
5. SSHes into EC2.
6. Uploads deploy files.
7. Runs Docker Compose pull/up on the server.

Keep Caddy behavior:

```text
/api/v1/* -> backend:8080 with URI rewrite to /V-sign/api/v1/*
all other paths -> 404
```

Do not add a public `/ai` route.

## Environment And Secrets

Never commit real secrets.

Ignored/local files may include:

```text
.env
.env.prod
.env.ai.prod
.env.deploy
*.pem
secretKey.properties
```

Use example files with placeholders only:

```text
.env.prod.example
.env.ai.prod.example
.env.deploy.example
v-sign-be/.env.prod.example
```

Local backend development may use:

```text
DB_SCHEMA=v-sign_schema
```

Production must use a clean dedicated schema:

```text
DB_SCHEMA=vsign_prod
```

Do not use `public` for production. Do not reuse `v-sign_schema` for production.

Production server env lives on EC2:

```text
/opt/vsign/.env.prod
/opt/vsign/.env.ai.prod
/opt/vsign/.env.deploy
```

GitHub secrets are for deploy transport only, not DB secrets:

```text
SERVER_IP
SERVER_USER
SERVER_SSH_KEY
GHCR_USERNAME
GHCR_TOKEN
APP_DOMAIN
DEPLOY_PATH
```

## AI Contract

Client-side MediaPipe Holistic is a locked design decision.

Production prediction flow:

```text
Browser webcam
  -> MediaPipe Holistic in browser
  -> compact landmark sequence
  -> backend authenticated endpoint
  -> private AI service
```

Rules:

- Frontend must not upload raw webcam images, video files, JPEGs, or base64 frames.
- Payload must be landmark/features only.
- Current model input frame size is exactly `258`.
- Feature order:
  - pose: `33 * (x, y, z, visibility) = 132`
  - left hand: `21 * (x, y, z) = 63`
  - right hand: `21 * (x, y, z) = 63`
- Face landmarks are not included for the current model.
- Sequence shape before AI resampling is `[N, 258]`.
- AI service keeps server-side resampling/velocity through existing model preprocessing.

Relevant files:

```text
v-sign-be-ai/api_server.py
v-sign-be-ai/model_v2.py
D:\v-sign-fe\src\services\holisticLandmarkExtractor.ts
D:\v-sign-fe\src\components\AiCameraPractice.tsx
```

## Backend Rules

Backend is Spring Boot with:

- JWT auth.
- Flyway migrations.
- `spring.mvc.servlet.path=/V-sign`.
- Production profile disables Swagger/OpenAPI.
- Production profile disables SQL debug.
- Security should stay authenticated-by-default except intended public read/auth endpoints.

Important protected areas:

- lesson progress/completion
- quiz attempts
- signature workflows
- payments/subscriptions
- profile/me
- gamification
- admin

Lesson completion must remain backend-verified:

- lesson video stage reached
- quiz passed
- AI attempt passed when required
- premium locks enforced server-side
- XP award idempotent
- streak calculated server-side using UTC+7 rules

Do not restore frontend-only optimistic completion as source of truth.

## Payment, Google Login, Password Reset, Admin

Follow:

```text
PAYOS_GOOGLE_ADMIN_COMPLETION_PLAN.md
```

Payment rules:

- PayOS is the real payment provider for the planned production flow.
- FE must not activate premium directly.
- Subscription activation must come from verified PayOS webhook or verified PayOS reconciliation.
- Handle missed webhook/server failure with:
  - `PENDING_CONFIRMATION`
  - `PAID_SUBSCRIPTION_PENDING`
  - user sync endpoint
  - admin sync endpoint
  - scheduled reconciliation
  - idempotent subscription activation
- Do not ask a user to pay again while an existing paid/recoverable order is unresolved.

Google login rules:

- New Google users become `USER`, not admin.
- Existing admin role may be preserved only if the verified Google email already belongs to an admin account in DB.
- Do not grant admin via frontend flag, localStorage, email domain, or query param.

Password reset rules:

- Implement real email reset, not a no-op.
- Store only hashed reset tokens.
- Tokens are short-lived and single-use.
- Reset request must not leak whether an email exists.

Admin rules:

- Admin UI route is frontend UX only; backend role checks remain required.
- Admin dashboard visible only to `ADMIN` or `SUPER_ADMIN`.
- User delete means soft deactivate for v1.
- Admin user mutations must write audit logs.
- Prevent self-disable/delete and prevent removing the last `SUPER_ADMIN`.

## Plan Files

Before major work, read the relevant plan:

```text
PRE_DEPLOY_HOLISTIC_REFACTOR_PLAN.md
AWS_C6A_DEPLOY_PLAN.md
PAYOS_GOOGLE_ADMIN_COMPLETION_PLAN.md
v-sign-be/docs/deployment/github-actions-ghcr-caddy-runbook.md
v-sign-be/docs/deployment/deployment-smoke-test-checklist.md
v-sign-be/docs/ops/baseline-cost-performance.md
```

Do not resurrect old decisions from older docs if they conflict with the root plans above.

## Verification Commands

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

## Git And Editing Safety

- Check `git status --short` before broad edits.
- The worktree may be dirty; do not revert user changes.
- Do not use `git reset --hard` or destructive checkout commands unless explicitly requested.
- Keep edits scoped to the requested feature.
- Use Flyway migrations for DB changes; do not rely on JPA schema auto-update.
- Use placeholders in committed env examples.
- Do not paste real secrets into markdown, code, tests, logs, or final answers.
- For manual code edits, prefer `apply_patch`.

## Common Mistakes To Avoid

- Do not expose AI through Caddy.
- Do not add `ports: "8000:8000"` for AI in production.
- Do not add `ports: "8080:8080"` for backend in production.
- Do not make EC2 build backend/AI images.
- Do not put DB secrets in GitHub Actions secrets for this deployment model.
- Do not switch production DB schema to `public`.
- Do not reuse local `v-sign_schema` for production.
- Do not send webcam images/base64/video to backend or AI.
- Do not let FE mark lesson completion or premium status as truth.
- Do not grant admin role from FE state.
- Do not activate subscription from PayOS return URL alone.

## Preferred Agent Workflow

1. Read this file.
2. Read the relevant plan file.
3. Inspect current code with `rg`.
4. Make minimal scoped changes.
5. Update env examples/docs if runtime config changes.
6. Run the smallest meaningful tests first, then broader tests if risk is high.
7. Report changed files, verification run, and any remaining manual steps.
