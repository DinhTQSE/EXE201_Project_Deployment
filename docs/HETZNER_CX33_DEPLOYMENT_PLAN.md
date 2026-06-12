# V-Sign Hetzner CX33 Deployment Plan

Last updated: 2026-05-26

## 1. Decision

V-Sign sẽ deploy backend và AI trên Hetzner Cloud.

Chosen server:

```text
Provider: Hetzner Cloud
Plan: CX33
Monthly cost target: 6.99 EUR/month
CPU: 4 vCPU
RAM: 8 GB
Disk: 80 GB
OS: Ubuntu 24.04 LTS
```

Existing external services:

- Frontend: Vercel.
- Database: Supabase PostgreSQL.
- Backend: Spring Boot container.
- AI: FastAPI container.
- Reverse proxy: Caddy container.

Important cost note:

- 6.99 EUR/month is the target server cost.
- Actual bill may include VAT, IPv4, backup/snapshot, and bandwidth overage depending on Hetzner account and region.
- Do not enable paid backup by default for student budget. Use manual snapshots before risky releases.

## 2. Target Architecture

```text
User Browser
   |
   | app.yourdomain.com
   v
Vercel Frontend
   |
   | HTTPS API calls
   v
api.yourdomain.com
   |
   v
Caddy Reverse Proxy on Hetzner CX33
   |                                  |
   | /V-sign/api/v1/*                 | /ai/*
   v                                  v
Spring Boot Backend                   FastAPI AI Service
Docker container                      Docker container
port 8080 internal                    port 8000 internal
   |
   | JDBC over SSL
   v
Supabase PostgreSQL
```

Deployment model:

- One Hetzner VM.
- One Docker Compose project.
- Three runtime containers:
  - `vsign-caddy`
  - `vsign-backend`
  - `vsign-ai`
- Backend and AI are isolated containers on an internal Docker network.
- Only Caddy exposes public ports `80` and `443`.

## 3. Domain Plan

Recommended domains:

```text
app.yourdomain.com -> Vercel
api.yourdomain.com -> Hetzner CX33 public IP
```

Vercel environment variables:

```env
VITE_USE_BACKEND=true
VITE_API_BASE_URL=https://api.yourdomain.com/V-sign/api/v1
VITE_AI_BASE_URL=https://api.yourdomain.com/ai
```

Backend public routes:

```text
https://api.yourdomain.com/V-sign/api/v1/auth/login
https://api.yourdomain.com/V-sign/api/v1/dictionary
https://api.yourdomain.com/V-sign/api/v1/units
```

AI public routes:

```text
https://api.yourdomain.com/ai/health
https://api.yourdomain.com/ai/classes
https://api.yourdomain.com/ai/predict
```

## 4. Production Runtime Budget

CX33 has 8 GB RAM. Reserve memory deliberately:

| Component | Target RAM | Hard cap |
| --- | ---: | ---: |
| OS + Docker | 800 MB | 1.2 GB |
| Caddy | 64-128 MB | 256 MB |
| Spring Boot backend | 500-900 MB | 1 GB |
| AI FastAPI current Torch path | 1.5-3 GB | 3.5 GB |
| Buffer/cache/swap headroom | 1.5-2 GB | N/A |

Initial Docker limits:

```yaml
backend:
  mem_limit: 1g
  cpus: "1.25"

ai:
  mem_limit: 3500m
  cpus: "2.50"

caddy:
  mem_limit: 256m
  cpus: "0.25"
```

Backend JVM:

```env
JAVA_TOOL_OPTIONS=-Xms256m -Xmx768m -XX:+UseContainerSupport
```

AI optimization target later:

- Move MediaPipe landmark extraction to frontend.
- Export PyTorch model to ONNX.
- Replace Torch runtime with ONNX Runtime.
- Reduce AI hard cap from `3500m` to `1000m-1500m`.

## 5. Repository Files To Add

Required files:

```text
v-sign-be/Dockerfile
V-Sign-AI-Build/v-sign-be-ai/Dockerfile
infra/hetzner/docker-compose.prod.yml
infra/hetzner/Caddyfile
infra/hetzner/.env.prod.example
.github/workflows/deploy-hetzner.yml
```

Optional later:

```text
scripts/deploy/healthcheck.sh
scripts/deploy/rollback.sh
scripts/ai/export_to_onnx.py
scripts/ai/benchmark_inference.py
```

## 6. Server Bootstrap

### 6.1 Create Server

Hetzner Cloud Console:

```text
Location: Singapore if available and stable for your account; otherwise Germany/Finland
Image: Ubuntu 24.04 LTS
Type: CX33
SSH key: required
IPv4: enabled
Backups: disabled initially to save cost
```

Server label:

```text
vsign-prod-01
```

### 6.2 First Login

```bash
ssh root@<SERVER_IP>
```

Create deploy user:

```bash
adduser deploy
usermod -aG sudo deploy
mkdir -p /home/deploy/.ssh
cp /root/.ssh/authorized_keys /home/deploy/.ssh/authorized_keys
chown -R deploy:deploy /home/deploy/.ssh
chmod 700 /home/deploy/.ssh
chmod 600 /home/deploy/.ssh/authorized_keys
```

Reconnect:

```bash
ssh deploy@<SERVER_IP>
```

### 6.3 Basic Packages

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y ca-certificates curl git ufw htop unzip jq
```

### 6.4 Firewall

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

Do not expose:

```text
8080
8000
5432
```

### 6.5 Install Docker

Install Docker Engine using the official Docker repository for Ubuntu.

After install:

```bash
sudo usermod -aG docker deploy
newgrp docker
docker version
docker compose version
```

### 6.6 App Directory

```bash
sudo mkdir -p /opt/vsign
sudo mkdir -p /opt/vsign/releases
sudo mkdir -p /opt/vsign/logs
sudo chown -R deploy:deploy /opt/vsign
```

Expected server layout:

```text
/opt/vsign/
  docker-compose.prod.yml
  Caddyfile
  .env.prod
  .env.deploy
  releases/
  logs/
```

## 7. Environment Variables

### 7.1 Production Env On Server

Create:

```bash
nano /opt/vsign/.env.prod
chmod 600 /opt/vsign/.env.prod
```

Content:

```env
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

DB_URL=jdbc:postgresql://<supabase-pooler-host>:5432/postgres?prepareThreshold=0&sslmode=require
DB_USER=postgres.<project-ref>
DB_PASSWORD=<rotated-secret>
DB_SCHEMA=v-sign_schema

JWT_SECRET=<rotated-long-secret>
JWT_EXPIRATION_MS=150000000
JWT_REFRESH_EXPIRATION_MS=1512000000

JAVA_TOOL_OPTIONS=-Xms256m -Xmx768m -XX:+UseContainerSupport

AI_ENABLED=true
AI_MAX_FRAMES_PER_REQUEST=60
AI_MAX_REQUESTS_PER_MINUTE=30
```

Create deploy tag env:

```bash
nano /opt/vsign/.env.deploy
chmod 600 /opt/vsign/.env.deploy
```

Content:

```env
BACKEND_IMAGE=ghcr.io/<owner>/vsign-backend:initial
AI_IMAGE=ghcr.io/<owner>/vsign-ai:initial
```

### 7.2 GitHub Secrets

Repository secrets:

```text
HETZNER_HOST=<SERVER_IP>
HETZNER_USER=deploy
HETZNER_SSH_PRIVATE_KEY=<private-key-for-github-actions>
GHCR_TOKEN=<token-with-package-write-read>
```

Optional environment secrets:

```text
PRODUCTION_DOMAIN=api.yourdomain.com
```

### 7.3 Vercel Env

Production:

```env
VITE_USE_BACKEND=true
VITE_API_BASE_URL=https://api.yourdomain.com/V-sign/api/v1
VITE_AI_BASE_URL=https://api.yourdomain.com/ai
```

## 8. Security Cleanup Before First Deploy

Must complete before public deployment:

- [ ] Rotate Supabase database password.
- [ ] Rotate JWT secret.
- [ ] Remove committed secret files:
  - `v-sign-be/src/main/resources/secretKey.properties`
  - `v-sign-be/src/main/resources/secretKey-test.properties`
- [ ] Add secret files to `.gitignore`.
- [ ] Confirm `application.properties` only references env vars.
- [ ] Move test seed migration out of production Flyway path:
  - `V15__test_actor_accounts.sql`
- [ ] Disable production SQL debug logs.

Recommended `.gitignore` additions:

```gitignore
secretKey.properties
secretKey-test.properties
.env
.env.*
!.env.example
```

## 9. Docker Plan

### 9.1 Backend Dockerfile

Location:

```text
v-sign-be/Dockerfile
```

Design:

- Multi-stage build.
- Maven + JDK 21 build stage.
- JRE 21 runtime stage.
- Run as non-root user if possible.
- Expose `8080`.

Runtime command:

```bash
java $JAVA_TOOL_OPTIONS -jar /app/app.jar
```

### 9.2 AI Dockerfile

Location:

```text
V-Sign-AI-Build/v-sign-be-ai/Dockerfile
```

Design:

- Python 3.11 slim base.
- Install OS packages required by OpenCV/MediaPipe.
- Install `requirements.txt`.
- Copy `api_server.py`, `feature_engineering.py`, `models/`.
- Expose `8000`.

Runtime command:

```bash
python -m uvicorn api_server:app --host 0.0.0.0 --port 8000
```

### 9.3 Docker Compose

Location:

```text
infra/hetzner/docker-compose.prod.yml
```

Service design:

```yaml
services:
  caddy:
    image: caddy:2
    container_name: vsign-caddy
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    depends_on:
      - backend
      - ai
    mem_limit: 256m
    cpus: "0.25"

  backend:
    image: ${BACKEND_IMAGE}
    container_name: vsign-backend
    restart: unless-stopped
    env_file:
      - .env.prod
    expose:
      - "8080"
    mem_limit: 1g
    cpus: "1.25"

  ai:
    image: ${AI_IMAGE}
    container_name: vsign-ai
    restart: unless-stopped
    expose:
      - "8000"
    mem_limit: 3500m
    cpus: "2.50"

volumes:
  caddy_data:
  caddy_config:
```

## 10. Caddy Reverse Proxy

Location:

```text
infra/hetzner/Caddyfile
```

Initial config:

```caddyfile
api.yourdomain.com {
    encode zstd gzip

    header {
        Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        Referrer-Policy "no-referrer-when-downgrade"
    }

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

Later improvements:

- Add rate limiting via plugin or move to Nginx/Traefik if Caddy plugin management becomes annoying.
- Lower `/ai/*` body limit after frontend sends landmarks instead of images.

## 11. Backend Production Changes

Create:

```text
v-sign-be/src/main/resources/application-prod.properties
```

Recommended properties:

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

Health endpoint:

```text
GET /V-sign/actuator/health
```

CORS:

- Allow Vercel production domain.
- Allow local dev domain for development only.
- Do not use wildcard with credentials.

## 12. AI Production Changes

Before first deployment:

- [ ] Confirm `/health` works in container.
- [ ] Confirm `/classes` loads model.
- [ ] Confirm `/predict` handles bad payload safely.
- [ ] Add max frames validation.
- [ ] Add request timing log.
- [ ] Add simple version endpoint or include version in `/health`.

Recommended AI health:

```json
{
  "status": "healthy",
  "model_loaded": true,
  "device": "cpu",
  "num_classes": 6,
  "version": "git-sha"
}
```

Near-term optimization:

- Reduce frame capture defaults in frontend.
- Add cooldown.
- Add rate limit.

Medium-term optimization:

- Add `/predict-landmarks`.
- Move MediaPipe extraction to frontend.
- Export model to ONNX.
- Replace Torch runtime with ONNX Runtime.

## 13. CI/CD Plan

### 13.1 Pull Request CI

Triggers:

```text
pull_request
```

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

Important:

- Backend tests currently need fixing around Flyway/H2 vs PostgreSQL syntax.
- Prefer Testcontainers PostgreSQL before making `mvn test` a required production gate.

### 13.2 Production Deploy Workflow

Trigger:

```text
push to main
```

Steps:

1. Checkout.
2. Build backend image if `v-sign-be/**` changed.
3. Build AI image if `V-Sign-AI-Build/v-sign-be-ai/**` changed.
4. Push images to GHCR:

```text
ghcr.io/<owner>/vsign-backend:<git-sha>
ghcr.io/<owner>/vsign-ai:<git-sha>
```

5. SSH into Hetzner server.
6. Backup previous deploy tag:

```bash
cp /opt/vsign/.env.deploy /opt/vsign/releases/previous.env
```

7. Update `/opt/vsign/.env.deploy`.
8. Pull and recreate containers:

```bash
cd /opt/vsign
docker compose --env-file .env.deploy -f docker-compose.prod.yml pull
docker compose --env-file .env.deploy -f docker-compose.prod.yml up -d
```

9. Health check.
10. Rollback if health check fails.

### 13.3 Health Check Commands

```bash
curl -fsS https://api.yourdomain.com/health
curl -fsS https://api.yourdomain.com/V-sign/actuator/health
curl -fsS https://api.yourdomain.com/ai/health
```

### 13.4 Manual Rollback

```bash
cd /opt/vsign
cp releases/previous.env .env.deploy
docker compose --env-file .env.deploy -f docker-compose.prod.yml pull
docker compose --env-file .env.deploy -f docker-compose.prod.yml up -d
```

Warning:

- App rollback does not rollback Supabase schema migrations.
- For destructive DB migrations, create manual backup/snapshot first.

## 14. DNS Setup

DNS records:

```text
app.yourdomain.com CNAME cname.vercel-dns.com
api.yourdomain.com A <HETZNER_IPV4>
```

If using IPv6:

```text
api.yourdomain.com AAAA <HETZNER_IPV6>
```

Validation:

```bash
nslookup api.yourdomain.com
curl -I https://api.yourdomain.com/health
```

## 15. Supabase Setup

Before deploy:

- [ ] Rotate DB password.
- [ ] Confirm connection string.
- [ ] Confirm schema: `v-sign_schema`.
- [ ] Confirm SSL mode.
- [ ] Check Supabase connection limit.
- [ ] Backup before first production Flyway migration.

Backend env:

```env
DB_URL=jdbc:postgresql://<supabase-pooler-host>:5432/postgres?prepareThreshold=0&sslmode=require
DB_USER=postgres.<project-ref>
DB_PASSWORD=<secret>
DB_SCHEMA=v-sign_schema
```

Use small pool:

```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
```

## 16. Logging and Monitoring

### 16.1 Basic Commands

```bash
cd /opt/vsign
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f ai
docker compose -f docker-compose.prod.yml logs -f caddy
docker stats
htop
df -h
free -h
```

### 16.2 Log Rotation

Configure Docker daemon log rotation:

```bash
sudo nano /etc/docker/daemon.json
```

```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "20m",
    "max-file": "5"
  }
}
```

Restart Docker:

```bash
sudo systemctl restart docker
```

### 16.3 Alerts

Minimum alert targets:

- API health down.
- AI health down.
- Disk usage > 80%.
- RAM usage > 85%.
- Container restart loop.

Student-friendly tools:

- UptimeRobot free monitor for `/health`.
- Hetzner metrics.
- Manual `docker stats` during demo.

## 17. Cost Controls

Keep monthly cost low:

- Do not enable automatic Hetzner backups initially.
- Use manual snapshots only before risky deployments.
- Keep only recent Docker images.
- Add Docker log rotation.
- Avoid running staging server 24/7.
- Do not run database on VPS because Supabase already handles it.
- Do not expose AI for anonymous unlimited traffic.

Monthly cleanup:

```bash
docker image prune -f
docker builder prune -f
df -h
```

## 18. AI Abuse and Load Protection

Before public demo:

- [ ] Limit request body size.
- [ ] Limit max frames per request.
- [ ] Add frontend cooldown.
- [ ] Add backend/user-level usage tracking later.
- [ ] Add IP rate limit at proxy or AI service.
- [ ] Return graceful error if AI busy.

Target UX:

- If AI is warming up: show "AI dang khoi dong".
- If AI is unavailable: allow user to continue learning without AI.
- If too many requests: show cooldown timer.

## 19. Deployment Milestones

### Milestone 1 - Server Ready

- [ ] CX33 created.
- [ ] SSH deploy user works.
- [ ] Firewall configured.
- [ ] Docker installed.
- [ ] `/opt/vsign` created.
- [ ] DNS points to server.

### Milestone 2 - App Container Ready

- [ ] Backend Dockerfile builds.
- [ ] AI Dockerfile builds.
- [ ] Compose starts all containers locally.
- [ ] Compose starts all containers on server.
- [ ] Caddy obtains HTTPS cert.

### Milestone 3 - Production Smoke Test

- [ ] `GET /health` returns `ok`.
- [ ] `GET /V-sign/actuator/health` returns up.
- [ ] `GET /ai/health` returns healthy.
- [ ] FE login works from Vercel.
- [ ] Dictionary loads from backend.
- [ ] AI predict works with test payload.
- [ ] Supabase data persists.

### Milestone 4 - CI/CD

- [ ] GHCR push works.
- [ ] GitHub Actions SSH deploy works.
- [ ] New backend image deploys.
- [ ] New AI image deploys.
- [ ] Rollback procedure tested.

### Milestone 5 - Production Hardening

- [ ] Secrets removed from Git.
- [ ] Supabase password rotated.
- [ ] JWT secret rotated.
- [ ] CORS restricted.
- [ ] Resource limits applied.
- [ ] Log rotation applied.
- [ ] Uptime monitor enabled.

## 20. First Deploy Checklist

Do not deploy publicly until all required items are done.

Required:

- [ ] No real secrets in repo.
- [ ] Supabase password rotated.
- [ ] JWT secret rotated.
- [ ] `V15__test_actor_accounts.sql` not applied to production accidentally.
- [ ] Backend production profile created.
- [ ] Backend CORS configured for Vercel domain.
- [ ] Backend health endpoint exists.
- [ ] AI health endpoint works in container.
- [ ] Docker Compose resource limits set.
- [ ] Caddy HTTPS works.
- [ ] Vercel env points to `api.yourdomain.com`.
- [ ] Smoke test passes.

Recommended:

- [ ] Manual Supabase backup before first migration.
- [ ] UptimeRobot monitor.
- [ ] Rollback tested once.
- [ ] AI request cooldown.
- [ ] Docker log rotation.

## 21. Recommended Execution Order

1. Security cleanup in repo.
2. Add backend production profile and health endpoint.
3. Add Dockerfile for backend.
4. Add Dockerfile for AI.
5. Add Compose + Caddy files.
6. Test Compose locally.
7. Provision Hetzner CX33.
8. Configure server, firewall, Docker, env.
9. Point DNS.
10. Manual first deploy.
11. Configure GitHub Actions.
12. Test automated deploy.
13. Configure Vercel env.
14. Run end-to-end smoke test.
15. Enable basic monitoring.

## 22. Post-Deploy Optimization Roadmap

After first stable deployment:

1. Measure AI memory and latency under real demo traffic.
2. Reduce frontend frame count/size.
3. Add AI rate limit.
4. Move MediaPipe landmark extraction to frontend.
5. Export model to ONNX.
6. Quantize model if accuracy is acceptable.
7. Reduce AI container memory limit.
8. Re-evaluate whether CX23 is enough later.

## 23. Go/No-Go Criteria

Go live when:

- Backend health is stable for 24 hours.
- AI health is stable for 24 hours.
- RAM usage stays below 75% during demo load.
- Disk usage stays below 60%.
- FE can login, load dictionary, load lessons, and call AI.
- Rollback command has been tested.

No-go if:

- Backend test/migration state is unclear.
- Production DB may receive test accounts accidentally.
- Secrets remain in Git.
- AI can be spammed anonymously without limit.
- Server OOMs during local stress test.
