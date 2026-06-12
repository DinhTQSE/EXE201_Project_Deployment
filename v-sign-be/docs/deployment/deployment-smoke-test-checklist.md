# Deployment Smoke Test Checklist

Run this checklist after every staging or production deploy.

## Services

- Backend container is running.
- AI container is running.
- Frontend/Caddy container is running.
- CPU and memory limits are applied in Docker Compose or the hosting platform.
- Running containers use the expected GHCR image tag from `/opt/vsign/.env.deploy`.
- The server did not build images locally during deploy.

## Health And Version

```powershell
curl https://<domain>/api/v1/health
curl https://<domain>/api/v1/version
curl https://<domain>/ai/health
curl https://<domain>/ai/version
```

Expected:

- Backend returns `status=UP`.
- AI returns `status=healthy`, `model_loaded=true`, and expected `model_version`.
- No endpoint exposes secrets.

## Auth

- Register a new basic user.
- Login with the new user.
- `GET /api/v1/me` succeeds with bearer token.
- Protected endpoints reject unauthenticated requests.

## Learning Flow

- Open a free lesson.
- Start or resume progress.
- Complete quiz with passing answers.
- Submit an AI signature attempt.
- Complete lesson through `POST /api/v1/lessons/{lessonId}/complete`.
- Direct progress update to `COMPLETED` is rejected.

## AI

- Browser camera permission allowed: AI attempt reaches `/ai/predict-landmarks`.
- Browser camera permission denied: UI shows a useful error.
- Network inspector shows no base64 image/video payload in AI requests.

## Media

- Course and dictionary videos return HTTP 200 or 206.
- `Content-Type` is `video/mp4`.
- Missing video state does not crash the page.
- Public R2 URLs are acceptable for the current release.

## Monetization

- Plan list loads publicly.
- Payment order creation requires authentication.
- A user cannot read another user's payment transaction status.
- Real payment activation remains deferred unless a signed gateway webhook is enabled.

## Rollback Readiness

- Latest DB backup path is recorded.
- Previous GHCR image tags are recorded.
- Rollback command sequence in `github-actions-ghcr-caddy-runbook.md` is known to the deploy owner.
