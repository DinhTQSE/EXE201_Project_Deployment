# V-Sign Full System Deployment Plan

Last updated: 2026-05-26

## 1. Scope

Tài liệu này là kế hoạch deploy và vận hành **toàn bộ hệ thống V-Sign**, không chỉ riêng backend trên Hetzner.

Các thành phần trong scope:

- Frontend: `v-sign-fe`, deploy bằng Vercel.
- Backend API: `v-sign-be`, Spring Boot, deploy trên Hetzner CX33 bằng Docker.
- AI Inference API: `V-Sign-AI-Build/v-sign-be-ai`, FastAPI, deploy trên Hetzner CX33 bằng Docker.
- Database: Supabase PostgreSQL.
- Reverse proxy/TLS: Caddy hoặc Nginx trên Hetzner.
- DNS/domain.
- Secrets và environment variables.
- CI/CD automation.
- Database migrations.
- Media/static video strategy.
- Monitoring, logs, backup, rollback.

## 2. Final Deployment Decision

### 2.1 Infrastructure Choices

```text
Frontend hosting: Vercel
Backend/AI server: Hetzner Cloud CX33
Database: Supabase PostgreSQL
Container runtime: Docker + Docker Compose
Reverse proxy: Caddy preferred for simple automatic HTTPS
CI/CD: GitHub Actions + Vercel Git integration
Container registry: GitHub Container Registry (GHCR)
```

### 2.2 Hetzner Server

```text
Provider: Hetzner Cloud
Plan: CX33
Budget target: 6.99 EUR/month
CPU: 4 vCPU
RAM: 8 GB
Disk: 80 GB
OS: Ubuntu 24.04 LTS
Server name: vsign-prod-01
```

Hetzner runs only:

- Reverse proxy.
- Backend container.
- AI container.
- Operational tooling.

Hetzner does **not** run:

- Production database.
- Frontend static hosting.
- Long-term object storage, unless explicitly needed later.

## 3. System Architecture

```text
                       +----------------------+
                       |      GitHub Repo      |
                       | FE / BE / AI / Docs   |
                       +----------+-----------+
                                  |
                +-----------------+------------------+
                |                                    |
                v                                    v
        +---------------+                    +----------------+
        | Vercel Build  |                    | GitHub Actions |
        | Frontend CI/CD|                    | BE/AI CI/CD    |
        +-------+-------+                    +-------+--------+
                |                                    |
                v                                    v
        +---------------+                    +----------------+
        | Vercel CDN    |                    | GHCR Images    |
        | app domain    |                    | backend + ai   |
        +-------+-------+                    +-------+--------+
                |                                    |
                | Browser HTTPS                      | SSH deploy
                v                                    v
+-------------------------------+          +--------------------------+
| User Browser                  |          | Hetzner CX33             |
| - React app                   |  HTTPS   | - Docker Compose         |
| - Webcam capture              +--------->| - Caddy reverse proxy    |
| - Calls BE and AI APIs        |          | - Spring Boot backend    |
+-------------------------------+          | - FastAPI AI service     |
                                           +------------+-------------+
                                                        |
                                                        | JDBC SSL
                                                        v
                                           +--------------------------+
                                           | Supabase PostgreSQL      |
                                           | Managed DB + backups     |
                                           +--------------------------+
```

## 4. Domain and Routing

### 4.1 Recommended Domains

```text
app.yourdomain.com -> Vercel frontend
api.yourdomain.com -> Hetzner reverse proxy
```

Optional later:

```text
admin.yourdomain.com -> same Vercel app admin route, if needed
staging.yourdomain.com -> Vercel preview/staging
staging-api.yourdomain.com -> staging backend, if a staging server is added
```

### 4.2 Public Routes

Frontend:

```text
https://app.yourdomain.com
```

Backend:

```text
https://api.yourdomain.com/V-sign/api/v1/*
```

AI:

```text
https://api.yourdomain.com/ai/*
```

Health:

```text
https://api.yourdomain.com/health
https://api.yourdomain.com/V-sign/actuator/health
https://api.yourdomain.com/ai/health
```

### 4.3 DNS Records

```text
app.yourdomain.com CNAME <vercel-target>
api.yourdomain.com A <hetzner-ipv4>
```

If IPv6 is enabled:

```text
api.yourdomain.com AAAA <hetzner-ipv6>
```

## 5. Environment Layout

### 5.1 Production

```text
Frontend production: Vercel production deployment
Backend production: Hetzner CX33 Docker service
AI production: Hetzner CX33 Docker service
Database production: Supabase production schema/database
```

### 5.2 Staging Strategy For Student Budget

To save money, do not run a second server 24/7 initially.

Use:

- Vercel Preview Deployments for frontend PR testing.
- Local Docker Compose for backend/AI integration testing.
- Optional Supabase staging schema if needed.
- Temporary Hetzner snapshot or short-lived test server only before important demo.

Staging can be added later when budget allows.

## 6. Frontend Deployment Plan

### 6.1 Hosting

Vercel remains the source of truth for frontend deployment.

Flow:

```text
PR opened -> Vercel Preview Deployment
PR merged to main -> Vercel Production Deployment
```

### 6.2 Required Vercel Environment Variables

Production:

```env
VITE_USE_BACKEND=true
VITE_API_BASE_URL=https://api.yourdomain.com/V-sign/api/v1
VITE_AI_BASE_URL=https://api.yourdomain.com/ai
```

Preview:

```env
VITE_USE_BACKEND=true
VITE_API_BASE_URL=https://api.yourdomain.com/V-sign/api/v1
VITE_AI_BASE_URL=https://api.yourdomain.com/ai
```

If staging API is added later:

```env
VITE_API_BASE_URL=https://staging-api.yourdomain.com/V-sign/api/v1
VITE_AI_BASE_URL=https://staging-api.yourdomain.com/ai
```

### 6.3 Frontend CI Gate

Required checks:

```bash
cd v-sign-fe
npm ci
npm test
npm run build
```

Known note:

- `npm run build` works locally using `npm.cmd run build`.
- Production bundle currently has a large JS chunk warning. This does not block first deploy but should be optimized later with route-based code splitting.

## 7. Backend Deployment Plan

### 7.1 Runtime

Backend runs as a Docker container on Hetzner:

```text
Service name: backend
Container name: vsign-backend
Internal port: 8080
Public access: only via Caddy
```

### 7.2 Required Backend Env

Server file:

```text
/opt/vsign/.env.prod
```

Backend variables:

```env
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<supabase-host>:5432/postgres?prepareThreshold=0&sslmode=require
DB_USER=postgres.<project-ref>
DB_PASSWORD=<secret>
DB_SCHEMA=v-sign_schema
JWT_SECRET=<secret>
JWT_EXPIRATION_MS=150000000
JWT_REFRESH_EXPIRATION_MS=1512000000
JAVA_TOOL_OPTIONS=-Xms256m -Xmx768m -XX:+UseContainerSupport
```

### 7.3 Backend Production Profile

Add:

```text
v-sign-be/src/main/resources/application-prod.properties
```

Recommended:

```properties
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=INFO
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
server.forward-headers-strategy=framework
```

Add Actuator:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 7.4 Backend Risks Before Production

Must fix or explicitly handle:

- Real secrets currently exist in `secretKey.properties` files.
- `V15__test_actor_accounts.sql` seeds deterministic test accounts and should not run in production.
- Tests currently fail with H2 because PostgreSQL `ON CONFLICT` is not H2-compatible.
- CORS must be restricted to the Vercel domain.

## 8. AI Deployment Plan

### 8.1 Runtime

AI runs as a Docker container on Hetzner:

```text
Service name: ai
Container name: vsign-ai
Internal port: 8000
Public access: only via Caddy under /ai/*
```

### 8.2 Current AI Endpoints

```text
GET /health
GET /classes
POST /predict
```

Public via proxy:

```text
GET /ai/health
GET /ai/classes
POST /ai/predict
```

### 8.3 AI Resource Protection

Initial limits on CX33:

```yaml
mem_limit: 3500m
cpus: "2.50"
```

Required protections:

- Max request body size at reverse proxy.
- Max frames per request in AI API.
- Frontend cooldown between predictions.
- Graceful error if AI is busy/unavailable.

### 8.4 AI Cost Optimization Roadmap

Not required for first deploy, but should be planned:

1. Reduce frontend frame count, FPS, JPEG quality.
2. Move MediaPipe landmark extraction to frontend.
3. Add `/predict-landmarks`.
4. Export model to ONNX.
5. Run ONNX Runtime instead of PyTorch in production.
6. Quantize ONNX model if accuracy remains acceptable.
7. Reduce AI memory limit after optimization.

## 9. Database Deployment Plan

### 9.1 Supabase Role

Supabase remains the production database.

Hetzner does not host PostgreSQL.

Benefits:

- Managed backup options.
- No DB ops burden on VPS.
- Lower risk of losing data if VPS is rebuilt.

### 9.2 Connection

Backend uses env-provided JDBC URL:

```env
DB_URL=jdbc:postgresql://<supabase-pooler-host>:5432/postgres?prepareThreshold=0&sslmode=require
```

Keep Hikari small:

```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
```

### 9.3 Migration Policy

Flyway runs from backend:

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.schemas=${DB_SCHEMA}
```

Rules:

- No test data in production migrations.
- Backup Supabase before large migrations.
- Avoid destructive migrations without rollback SQL.
- Any migration changing existing data must be reviewed manually.

### 9.4 Production Data Safety

Before first production deploy:

- [ ] Rotate leaked Supabase password.
- [ ] Confirm production schema name.
- [ ] Remove/relocate test account seed migration.
- [ ] Take a manual Supabase backup or export before first migration.

## 10. Media and Static Assets

Current repo contains media-related scripts and runbooks for R2/S3.

Initial production approach:

- Keep frontend assets on Vercel.
- Keep database metadata in Supabase.
- Host learning videos/media on object storage, not on Hetzner disk.

Recommended media storage:

```text
Cloudflare R2 or AWS S3 + CDN
```

Reason:

- VPS disk is small and should not be the source of truth for media.
- Object storage is cheaper and safer for large videos.
- CDN improves video delivery.

Short-term if media URLs already point to external storage:

- Do not change.
- Verify all seeded video URLs are HTTPS and accessible.

Before production:

- [ ] Decide R2 or S3 for video assets.
- [ ] Confirm private/public access policy.
- [ ] Confirm DB seed URLs point to production media URLs.
- [ ] Do not store production user uploads on Hetzner local disk.

## 11. Reverse Proxy and TLS

### 11.1 Caddy

Caddy handles:

- HTTPS certificates.
- Gzip/zstd compression.
- Routing to backend and AI.
- Request size limits.
- Security headers.

Example route plan:

```caddyfile
api.yourdomain.com {
    encode zstd gzip

    handle_path /ai/* {
        request_body {
            max_size 12MB
        }
        reverse_proxy ai:8000
    }

    handle /V-sign/* {
        reverse_proxy backend:8080
    }

    respond /health "ok" 200
}
```

### 11.2 CORS

Preferred:

- FE and API are separate domains.
- Backend explicitly allows `https://app.yourdomain.com`.
- AI can allow only frontend origin if browser calls AI directly.

Do not allow wildcard origins with credentials.

## 12. Docker Compose Runtime

Server directory:

```text
/opt/vsign
```

Files:

```text
/opt/vsign/docker-compose.prod.yml
/opt/vsign/Caddyfile
/opt/vsign/.env.prod
/opt/vsign/.env.deploy
/opt/vsign/releases/
```

`.env.deploy`:

```env
BACKEND_IMAGE=ghcr.io/<owner>/vsign-backend:<sha>
AI_IMAGE=ghcr.io/<owner>/vsign-ai:<sha>
```

Deploy command:

```bash
cd /opt/vsign
docker compose --env-file .env.deploy -f docker-compose.prod.yml pull
docker compose --env-file .env.deploy -f docker-compose.prod.yml up -d
```

## 13. CI/CD Automation

### 13.1 Overview

```text
Frontend changes -> Vercel build/deploy
Backend changes  -> GitHub Actions build Docker image -> GHCR -> Hetzner deploy
AI changes       -> GitHub Actions build Docker image -> GHCR -> Hetzner deploy
DB migration     -> included in backend image, reviewed before merge
Docs only        -> no deployment
```

### 13.2 Path-Based CI

Frontend paths:

```text
v-sign-fe/**
```

Backend paths:

```text
v-sign-be/**
```

AI paths:

```text
V-Sign-AI-Build/v-sign-be-ai/**
```

Docs paths:

```text
docs/**
*.md
```

### 13.3 PR Checks

Frontend:

```bash
cd v-sign-fe
npm ci
npm test
npm run build
```

Backend:

```bash
cd v-sign-be
mvn test
```

AI:

```bash
cd V-Sign-AI-Build/v-sign-be-ai
python -m compileall .
```

Until backend H2/Flyway issue is fixed, `mvn test` can run as advisory, but production readiness requires fixing it or moving to Testcontainers PostgreSQL.

### 13.4 Main Branch Deploy

On push to `main`:

1. Detect changed services.
2. Build changed Docker images.
3. Push to GHCR with tag:

```text
<git-sha>
```

4. SSH into Hetzner.
5. Save previous deployment tag.
6. Update `.env.deploy`.
7. Pull and recreate services.
8. Run health checks.
9. Rollback if health check fails.

### 13.5 GitHub Secrets

```text
HETZNER_HOST
HETZNER_USER
HETZNER_SSH_PRIVATE_KEY
GHCR_TOKEN
```

Production environment should require manual approval until the pipeline is trusted.

## 14. Rollback Strategy

### 14.1 App Rollback

Keep previous image tags:

```text
/opt/vsign/releases/previous.env
```

Rollback:

```bash
cd /opt/vsign
cp releases/previous.env .env.deploy
docker compose --env-file .env.deploy -f docker-compose.prod.yml pull
docker compose --env-file .env.deploy -f docker-compose.prod.yml up -d
```

### 14.2 Database Rollback

App rollback does not undo Flyway migrations.

Rules:

- Back up before risky migrations.
- Avoid destructive migrations.
- For destructive changes, prepare manual rollback SQL.
- Keep migrations backward-compatible when possible.

## 15. Server Bootstrap Checklist

On Hetzner:

- [ ] Create CX33 Ubuntu server.
- [ ] Add SSH key.
- [ ] Create `deploy` user.
- [ ] Disable password login if possible.
- [ ] Install Docker Engine and Compose plugin.
- [ ] Configure UFW firewall.
- [ ] Create `/opt/vsign`.
- [ ] Add `.env.prod`.
- [ ] Add `.env.deploy`.
- [ ] Add `docker-compose.prod.yml`.
- [ ] Add `Caddyfile`.
- [ ] Login to GHCR.
- [ ] Point DNS.
- [ ] Start services.

Firewall:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

Do not expose backend/AI ports publicly.

## 16. Observability

### 16.1 Basic Health Checks

```bash
curl -fsS https://api.yourdomain.com/health
curl -fsS https://api.yourdomain.com/V-sign/actuator/health
curl -fsS https://api.yourdomain.com/ai/health
```

### 16.2 Server Commands

```bash
cd /opt/vsign
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f ai
docker compose -f docker-compose.prod.yml logs -f caddy
docker stats
free -h
df -h
htop
```

### 16.3 Minimum Monitoring

Use:

- UptimeRobot for `/health`, backend health, AI health.
- Hetzner Cloud metrics.
- Docker logs during demo.

Alert when:

- API health down.
- AI health down.
- RAM > 85%.
- Disk > 80%.
- Container restarts repeatedly.

### 16.4 Log Rotation

Configure Docker log rotation:

```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "20m",
    "max-file": "5"
  }
}
```

## 17. Backup Strategy

### 17.1 Database

Primary backup is Supabase.

Before first production migration:

- [ ] Create manual backup/export.
- [ ] Confirm restore path is understood.

### 17.2 Server

Student-budget approach:

- No paid continuous Hetzner backup initially.
- Manual snapshot before major infrastructure changes.
- All app code/images are recoverable from GitHub/GHCR.
- `.env.prod` must be backed up securely outside the repo.

### 17.3 Media

Use object storage with its own backup/versioning policy if media becomes critical.

Do not rely on Hetzner local disk for production media source of truth.

## 18. Security Plan

Must do:

- Rotate exposed Supabase password.
- Rotate JWT secret.
- Remove secret files from Git.
- Add secret files to `.gitignore`.
- Restrict CORS.
- Keep only `80/443/22` open.
- Use HTTPS only.
- Do not expose Supabase credentials to frontend.
- Do not expose backend/AI internal ports.

Recommended:

- Disable root SSH after deploy user works.
- Use SSH key only.
- Add basic fail2ban later.
- Keep Docker and OS patched.
- Use least-privilege Supabase credentials if practical.

## 19. Cost Control Plan

Monthly target:

```text
Hetzner CX33: 6.99 EUR/month target
Vercel: free/low tier
Supabase: current plan/free tier if enough
Monitoring: free tier
GHCR: GitHub included quota
```

Avoid:

- Always-on staging server.
- Paid Hetzner backups initially.
- Storing large videos on VPS.
- Unlimited AI endpoint access.
- Oversized Docker logs.

Monthly maintenance:

```bash
docker image prune -f
docker builder prune -f
df -h
free -h
```

## 20. Release Workflow

### 20.1 Normal Feature

```text
branch -> PR -> CI -> review -> merge main -> auto deploy affected service
```

### 20.2 Frontend-Only

```text
Change v-sign-fe -> Vercel Preview -> merge -> Vercel Production
```

### 20.3 Backend-Only

```text
Change v-sign-be -> GitHub Actions build backend image -> deploy backend -> health check
```

### 20.4 AI-Only

```text
Change v-sign-be-ai -> GitHub Actions build AI image -> deploy AI -> health check
```

### 20.5 Migration Change

```text
Migration PR -> manual review -> backup Supabase -> merge -> deploy backend -> verify
```

Migration PRs should not be auto-merged casually.

## 21. Production Readiness Checklist

Repository:

- [ ] Full system deployment plan reviewed.
- [ ] Dockerfiles added.
- [ ] Compose and Caddy config added.
- [ ] GitHub Actions deploy workflow added.
- [ ] Secret files removed from Git.
- [ ] `.gitignore` updated.

Frontend:

- [ ] Vercel production env configured.
- [ ] Vercel preview behavior understood.
- [ ] FE calls production API domain.
- [ ] FE handles AI unavailable state.

Backend:

- [ ] Production profile added.
- [ ] Actuator health endpoint added.
- [ ] CORS restricted.
- [ ] SQL debug disabled in prod.
- [ ] Hikari pool small.
- [ ] Production migrations reviewed.

AI:

- [ ] Docker image builds.
- [ ] `/health` works.
- [ ] `/classes` works.
- [ ] `/predict` has limits.
- [ ] AI has memory/CPU limit.

Supabase:

- [ ] Password rotated.
- [ ] JWT secret rotated.
- [ ] DB backup/export before first migration.
- [ ] Schema confirmed.

Hetzner:

- [ ] CX33 provisioned.
- [ ] Docker installed.
- [ ] Firewall configured.
- [ ] DNS configured.
- [ ] Caddy HTTPS works.
- [ ] Docker log rotation configured.

CI/CD:

- [ ] PR checks work.
- [ ] GHCR push works.
- [ ] SSH deploy works.
- [ ] Health check works.
- [ ] Rollback tested.

## 22. First Production Smoke Test

Run after first deploy:

```text
1. Open https://app.yourdomain.com
2. Register/login test user
3. Load dictionary page
4. Load learning units/chapters/lessons
5. Submit quiz or assessment
6. Open AI practice
7. Call AI predict
8. Verify backend stores signature attempt
9. Check Supabase data
10. Check server RAM/CPU
11. Restart AI container and confirm backend still works
12. Run rollback once in controlled test
```

## 23. Recommended Execution Order

1. Clean secrets and rotate exposed credentials.
2. Fix production migration safety.
3. Add backend prod profile and health endpoint.
4. Add Dockerfile for backend.
5. Add Dockerfile for AI.
6. Add Compose/Caddy infra files.
7. Test Docker Compose locally.
8. Provision Hetzner CX33.
9. Configure DNS, firewall, Docker, `/opt/vsign`.
10. Deploy manually once.
11. Configure Vercel production env.
12. Run full smoke test.
13. Add GitHub Actions deploy automation.
14. Test auto deploy and rollback.
15. Add monitoring and log rotation.

## 24. Post-Launch Optimization

After stable first deploy:

- Reduce FE bundle size with code splitting.
- Reduce AI frame payload.
- Add rate limiting.
- Move MediaPipe extraction to frontend.
- Add `/predict-landmarks`.
- Export model to ONNX.
- Quantize model if accuracy is acceptable.
- Re-evaluate whether CX23 can replace CX33 later.

## 25. Go/No-Go

Go live if:

- No secrets remain in Git.
- FE, BE, AI, DB all pass smoke test.
- Backend and AI health stable.
- RAM usage below 75% under demo load.
- Rollback is tested.
- Supabase migration state is known and backed up.

Do not go live if:

- Test account seed may run in production.
- Backend CORS is wildcard with credentials.
- AI can be spammed without limits.
- Backend tests/migrations are not understood.
- Server OOMs during AI prediction.
