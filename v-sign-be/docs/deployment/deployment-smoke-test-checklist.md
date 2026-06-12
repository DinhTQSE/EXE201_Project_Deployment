# Deployment Smoke Test Checklist

Run this checklist after every staging or production deploy.

## Services

- Backend container is running.
- AI container is running.
- Caddy container is running.
- CPU and memory limits are applied in Docker Compose or the hosting platform.
- Running containers use the expected GHCR image tag from `/opt/vsign/.env.deploy`.
- The server did not build images locally during deploy.
- Frontend is deployed separately on Vercel.

## Health And Version

Public API checks:

```powershell
curl https://<api-domain>/api/v1/health
curl https://<api-domain>/api/v1/version
```

Internal AI check from the server:

```bash
cd /opt/vsign
docker compose --env-file .env.deploy -f docker-compose.prod.yml exec -T ai python -c "import urllib.request; print(urllib.request.urlopen('http://localhost:8000/health', timeout=5).read().decode())"
docker compose --env-file .env.deploy -f docker-compose.prod.yml exec -T ai python -c "import urllib.request; print(urllib.request.urlopen('http://localhost:8000/version', timeout=5).read().decode())"
```

Expected:

- Backend returns `status=UP`.
- AI returns `status=healthy`, `model_loaded=true`, and expected `model_version`.
- AI is not reachable from the public internet.
- No endpoint exposes secrets.

## Auth

- Register a new basic user.
- Login with the new user.
- `GET /api/v1/me` succeeds with bearer token.
- Protected endpoints reject unauthenticated requests.

## Learning Flow

- Open the Vercel frontend.
- Confirm the frontend API base URL points to `https://<api-domain>/api/v1`.
- Open a free lesson.
- Start or resume progress.
- Complete quiz with passing answers.
- Submit an AI signature attempt through the backend proxy endpoint.
- Complete lesson through `POST /api/v1/lessons/{lessonId}/complete`.
- Direct progress update to `COMPLETED` is rejected.

## AI

- Browser camera permission allowed: AI attempt sends landmark sequence to backend, not directly to AI.
- Browser camera permission denied: UI shows a useful error.
- Network inspector shows no base64 image/video payload in API requests.
- Backend reaches AI over Docker network at `http://ai:8000`.

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
