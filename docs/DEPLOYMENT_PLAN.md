# V-Sign Deployment Plan

Last updated: 2026-05-26

## 1. Deployment Goal

V-Sign hiện tại được triển khai theo mô hình:

- Frontend (`v-sign-fe`): deploy bằng Vercel.
- Backend (`v-sign-be`): Spring Boot 3.3, Java 21, PostgreSQL/Supabase, Flyway.
- AI service (`V-Sign-AI-Build/v-sign-be-ai`): FastAPI/Uvicorn, Python 3.11, Torch/MediaPipe.
- Database: Supabase PostgreSQL managed service.

Mục tiêu của kế hoạch này là thiết kế quy trình deploy tự động để mỗi lần fix bug hoặc cập nhật tính năng có thể:

- Build và test đúng phần thay đổi.
- Deploy tự động sau khi merge vào `main`.
- Có health check sau deploy.
- Có rollback nhanh nếu release lỗi.
- Không để secret trong source code.

## 2. Recommended Architecture

### 2.1 High-Level Topology

```text
User Browser
   |
   | app.yourdomain.com
   v
Vercel
   |
   | API calls
   v
api.yourdomain.com
   |
   v
Reverse Proxy: Caddy/Nginx/Traefik
   |                         |
   | /V-sign/api/v1/*        | /ai/*
   v                         v
Spring Boot Backend          FastAPI AI Service
Docker container             Docker container
   |
   | JDBC
   v
Supabase PostgreSQL
```

### 2.2 Server Model

Deploy backend và AI trên cùng một server được, nhưng nên là **cùng host, tách container**:

- `vsign-backend`: Spring Boot app, port nội bộ `8080`.
- `vsign-ai`: FastAPI app, port nội bộ `8000`.
- `reverse-proxy`: expose public HTTPS `443`.

Không nên gộp BE và AI vào cùng một container vì:

- Backend và AI có runtime khác nhau: Java vs Python.
- AI có dependency nặng hơn: Torch, OpenCV, MediaPipe.
- AI có thể tiêu tốn CPU/RAM cao hơn, cần resource limit riêng.
- Rollback/restart từng service sẽ dễ hơn.

### 2.3 Minimum Server Recommendation

Cho MVP hoặc traffic vừa phải:

- CPU: 4 vCPU.
- RAM: 8 GB.
- Disk: 40-80 GB SSD.
- OS: Ubuntu LTS.
- Runtime: Docker Engine + Docker Compose plugin.

Nếu AI inference chậm hoặc nhiều request đồng thời:

- Tăng lên 8 vCPU / 16 GB RAM.
- Hoặc tách AI sang server riêng.
- Hoặc dùng queue/rate limit cho endpoint `/ai/predict`.

## 3. Domain and Routing

### 3.1 Domains

Đề xuất:

- Frontend: `app.yourdomain.com` trỏ về Vercel.
- API gateway: `api.yourdomain.com` trỏ về VPS/server.

### 3.2 Reverse Proxy Routing

Public routes:

- `https://api.yourdomain.com/V-sign/api/v1/*` -> backend `http://vsign-backend:8080/V-sign/api/v1/*`
- `https://api.yourdomain.com/ai/*` -> AI `http://vsign-ai:8000/*`

Vì backend hiện có cấu hình:

```properties
spring.mvc.servlet.path=/V-sign
server.port=8080
```

nên endpoint backend production sẽ có dạng:

```text
https://api.yourdomain.com/V-sign/api/v1/auth/login
https://api.yourdomain.com/V-sign/api/v1/dictionary
```

AI service hiện có endpoint:

```text
GET /health
GET /classes
POST /predict
```

Sau reverse proxy, FE gọi:

```text
https://api.yourdomain.com/ai/health
https://api.yourdomain.com/ai/classes
https://api.yourdomain.com/ai/predict
```

## 4. Frontend Deployment on Vercel

Frontend vẫn để Vercel tự deploy từ Git.

### 4.1 Vercel Environment Variables

Production:

```env
VITE_USE_BACKEND=true
VITE_API_BASE_URL=https://api.yourdomain.com/V-sign/api/v1
VITE_AI_BASE_URL=https://api.yourdomain.com/ai
```

Preview/Staging nếu có:

```env
VITE_USE_BACKEND=true
VITE_API_BASE_URL=https://staging-api.yourdomain.com/V-sign/api/v1
VITE_AI_BASE_URL=https://staging-api.yourdomain.com/ai
```

### 4.2 Vercel Flow

- Pull request: Vercel tạo Preview Deployment.
- Merge vào `main`: Vercel deploy Production.
- Không cần deploy FE từ server backend.

## 5. Backend and AI Containerization

### 5.1 Backend Docker Image

Cần thêm `v-sign-be/Dockerfile`.

Build logic:

- Build bằng Maven + Java 21.
- Output Spring Boot jar.
- Runtime image chỉ cần JRE 21.
- Expose `8080`.

Runtime env:

```env
DB_URL=
DB_USER=
DB_PASSWORD=
DB_SCHEMA=
JWT_SECRET=
JWT_EXPIRATION_MS=
JWT_REFRESH_EXPIRATION_MS=
SPRING_PROFILES_ACTIVE=prod
```

### 5.2 AI Docker Image

Cần thêm `V-Sign-AI-Build/v-sign-be-ai/Dockerfile`.

Build logic:

- Base Python 3.11.
- Install system libraries cho OpenCV/MediaPipe nếu cần.
- Install `requirements.txt`.
- Copy `api_server.py`, `feature_engineering.py`, `models/`.
- Start bằng Uvicorn:

```bash
python -m uvicorn api_server:app --host 0.0.0.0 --port 8000
```

Runtime env đề xuất:

```env
PYTHONUNBUFFERED=1
```

### 5.3 Docker Compose Production

Cần thêm file `infra/docker-compose.prod.yml` hoặc `docker-compose.prod.yml`.

Services:

- `reverse-proxy`
- `backend`
- `ai`

Backend và AI cùng một Docker network nội bộ. Chỉ reverse proxy expose public port `80/443`.

## 6. Secrets and Security

### 6.1 Critical Current Issue

Hiện tại backend có secret thật trong:

```text
v-sign-be/src/main/resources/secretKey.properties
v-sign-be/src/main/resources/secretKey-test.properties
```

Trước production cần làm ngay:

1. Rotate Supabase database password.
2. Rotate JWT secret.
3. Xóa secret khỏi source code.
4. Thêm rule `.gitignore`:

```gitignore
secretKey.properties
secretKey-test.properties
.env
.env.*
!.env.example
```

5. Dùng environment variables hoặc secret manager.

### 6.2 Server Secret Storage

Trên server production tạo:

```text
/opt/vsign/.env.prod
```

Permission:

```bash
chmod 600 /opt/vsign/.env.prod
```

File này không commit lên Git.

### 6.3 GitHub Secrets

GitHub repository secrets cần có:

```text
SERVER_HOST
SERVER_USER
SSH_PRIVATE_KEY
GHCR_TOKEN
```

Nếu tách staging/prod, dùng GitHub Environments:

- `staging`
- `production`

Production nên bật manual approval.

## 7. Database and Migration Strategy

### 7.1 Supabase Connection

Backend dùng Supabase PostgreSQL qua JDBC.

Với Spring Boot server chạy lâu dài, nên dùng connection pool cẩn thận:

```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
```

Production `DB_URL` nên lấy từ Supabase dashboard. Nếu dùng pooler transaction mode, cần giữ `prepareThreshold=0` như cấu hình hiện tại để tránh vấn đề prepared statements.

Ví dụ:

```env
DB_URL=jdbc:postgresql://<pooler-host>:5432/postgres?prepareThreshold=0&sslmode=require
DB_USER=postgres.<project-ref>
DB_PASSWORD=<secret>
DB_SCHEMA=v-sign_schema
```

### 7.2 Flyway

Backend hiện chạy Flyway khi start:

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.schemas=${DB_SCHEMA}
```

Quy trình production:

1. Backup Supabase trước migration lớn.
2. Deploy backend image mới.
3. Backend start và Flyway apply migration.
4. Health check backend.
5. Nếu fail do migration, rollback app có thể chưa đủ; cần plan rollback DB riêng.

### 7.3 Seed Data Warning

File sau đang seed account test:

```text
v-sign-be/src/main/resources/db/migration/V15__test_actor_accounts.sql
```

Không nên chạy file này trên production DB. Cần chuyển sang một trong các hướng:

- Tách seed test khỏi `db/migration`.
- Dùng Flyway location riêng cho local/staging.
- Dùng profile-specific migration.
- Đưa test accounts vào script chạy thủ công chỉ cho QA.

### 7.4 Test Database

Backend test hiện fail vì H2 không hiểu PostgreSQL `ON CONFLICT` trong migration `V15`.

Khuyến nghị:

- Dùng Testcontainers PostgreSQL cho integration test.
- Hoặc rewrite seed SQL thành cú pháp tương thích H2 và PostgreSQL.

Vì production là PostgreSQL, Testcontainers PostgreSQL là lựa chọn đúng hơn.

## 8. CI/CD Design

### 8.1 Branch Strategy

```text
feature/* -> pull request -> main
fix/*     -> pull request -> main
main      -> production deploy
```

Optional:

```text
develop -> staging deploy
main    -> production deploy
```

### 8.2 Pull Request CI

PR pipeline chạy theo path thay đổi.

Frontend changes:

```bash
cd v-sign-fe
npm ci
npm test
npm run build
```

Backend changes:

```bash
cd v-sign-be
mvn test
```

AI changes:

```bash
cd V-Sign-AI-Build/v-sign-be-ai
python -m venv .venv
pip install -r requirements.txt
python -m compileall .
```

Nên thêm smoke test AI sau này:

```bash
python -c "import api_server; print('ok')"
```

Lưu ý: import `api_server` có thể load dependency nặng. Nếu import gây load model, cần viết health/smoke test nhẹ hơn.

### 8.3 Production Deploy Trigger

Khi merge vào `main`:

1. Checkout source.
2. Detect changed paths.
3. Build Docker image backend nếu `v-sign-be/**` đổi.
4. Build Docker image AI nếu `V-Sign-AI-Build/v-sign-be-ai/**` đổi.
5. Push images lên GHCR:

```text
ghcr.io/<org-or-user>/vsign-backend:<git-sha>
ghcr.io/<org-or-user>/vsign-ai:<git-sha>
```

6. SSH vào server.
7. Update image tag trong `/opt/vsign/.env.deploy`.
8. Chạy:

```bash
cd /opt/vsign
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

9. Health check.
10. Nếu fail, rollback tag trước.

### 8.4 Health Checks

Backend nên thêm Spring Actuator:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Production health endpoint:

```text
GET https://api.yourdomain.com/V-sign/actuator/health
```

AI hiện có:

```text
GET https://api.yourdomain.com/ai/health
```

Post-deploy script:

```bash
curl -fsS https://api.yourdomain.com/V-sign/actuator/health
curl -fsS https://api.yourdomain.com/ai/health
```

### 8.5 Rollback

Mỗi deploy lưu current/previous image tag:

```text
/opt/vsign/releases/current.env
/opt/vsign/releases/previous.env
```

Rollback procedure:

```bash
cd /opt/vsign
cp releases/previous.env .env.deploy
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Quan trọng: rollback app không rollback database migration. Với migration destructive, cần DB rollback plan riêng.

## 9. Server Bootstrap Plan

Thực hiện một lần khi chuẩn bị server.

### 9.1 Install Packages

```bash
sudo apt update
sudo apt install -y ca-certificates curl git ufw
```

Install Docker theo tài liệu chính thức của Docker.

### 9.2 Create App Directory

```bash
sudo mkdir -p /opt/vsign
sudo chown -R deploy:deploy /opt/vsign
```

### 9.3 Firewall

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

Không expose trực tiếp:

- `8080`
- `8000`

### 9.4 Docker Login

```bash
docker login ghcr.io
```

Dùng token có quyền pull package.

### 9.5 Deploy Files on Server

Server cần có:

```text
/opt/vsign/docker-compose.prod.yml
/opt/vsign/Caddyfile hoặc nginx.conf
/opt/vsign/.env.prod
/opt/vsign/.env.deploy
/opt/vsign/releases/
```

## 10. Observability and Operations

### 10.1 Logging

MVP:

```bash
docker compose logs -f backend
docker compose logs -f ai
docker compose logs -f reverse-proxy
```

Production tốt hơn:

- Ship logs tới Grafana Loki, Better Stack, Datadog, hoặc CloudWatch.
- Log JSON cho backend nếu cần query tốt hơn.

### 10.2 Monitoring

Theo dõi tối thiểu:

- CPU server.
- RAM server.
- Disk usage.
- Container restart count.
- Backend response time.
- AI inference latency.
- Supabase connection usage.
- Error rate `4xx/5xx`.

### 10.3 Alerts

Cần alert khi:

- `/V-sign/actuator/health` fail.
- `/ai/health` fail.
- Disk usage > 80%.
- RAM usage > 85%.
- Container restart liên tục.
- Supabase connection gần limit.

## 11. Release Workflow

### 11.1 Normal Feature/Fix

1. Developer tạo branch:

```bash
git checkout -b fix/payment-status
```

2. Commit code.
3. Mở PR vào `main`.
4. CI chạy test/build.
5. Review và merge.
6. Vercel deploy FE nếu FE đổi.
7. GitHub Actions deploy BE/AI nếu BE/AI đổi.
8. Kiểm tra health check và smoke test.

### 11.2 Backend-Only Fix

Trigger:

```text
v-sign-be/**
```

Pipeline:

- Run Maven test.
- Build backend Docker image.
- Push GHCR.
- Deploy only backend service.
- Health check backend.

### 11.3 AI-Only Fix

Trigger:

```text
V-Sign-AI-Build/v-sign-be-ai/**
```

Pipeline:

- Compile/smoke test Python.
- Build AI Docker image.
- Push GHCR.
- Deploy only AI service.
- Health check AI.

### 11.4 Frontend-Only Fix

Trigger:

```text
v-sign-fe/**
```

Pipeline:

- Vercel Preview on PR.
- Vercel Production on merge.
- No server deploy needed.

## 12. Pre-Production Checklist

### 12.1 Repository

- [ ] Add backend Dockerfile.
- [ ] Add AI Dockerfile.
- [ ] Add production Docker Compose file.
- [ ] Add reverse proxy config.
- [ ] Add GitHub Actions workflow.
- [ ] Remove committed secrets.
- [ ] Add `.env.example` for backend and AI.
- [ ] Add `.gitignore` for secret files.

### 12.2 Backend

- [ ] Add Actuator health endpoint.
- [ ] Configure production CORS for Vercel domain.
- [ ] Move test seed accounts out of production migrations.
- [ ] Fix integration tests with Testcontainers PostgreSQL.
- [ ] Disable noisy SQL logs in production:

```properties
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=INFO
```

### 12.3 AI

- [ ] Confirm model files are included in image.
- [ ] Confirm `/health` works after container start.
- [ ] Add resource limits in Compose.
- [ ] Add request size/rate limit at reverse proxy.
- [ ] Decide max request body size for frame uploads.

### 12.4 Supabase

- [ ] Rotate exposed DB password.
- [ ] Confirm DB schema name.
- [ ] Confirm pooler/direct connection choice.
- [ ] Enable SSL mode if required.
- [ ] Backup before first production migration.

### 12.5 Vercel

- [ ] Set production env vars.
- [ ] Set preview env vars if staging exists.
- [ ] Confirm CORS allows Vercel domain.
- [ ] Confirm FE can call backend login endpoint.
- [ ] Confirm FE can call AI health/predict endpoint.

## 13. Recommended Implementation Order

1. Security cleanup:
   - Rotate Supabase password.
   - Rotate JWT secret.
   - Remove secret files from Git.

2. Backend readiness:
   - Add Actuator.
   - Add CORS.
   - Fix or isolate test seed migration.

3. Containerization:
   - Add backend Dockerfile.
   - Add AI Dockerfile.
   - Add Docker Compose production file.

4. Server setup:
   - Provision VPS.
   - Install Docker.
   - Configure reverse proxy and HTTPS.

5. CI/CD:
   - Add GitHub Actions PR CI.
   - Add GitHub Actions production deploy.
   - Add rollback workflow.

6. Production validation:
   - Deploy staging first if possible.
   - Run smoke tests.
   - Deploy production.
   - Monitor logs and metrics.

## 14. External References

- Docker Compose `up`: https://docs.docker.com/reference/cli/docker/compose/up/
- Docker Compose `restart`: https://docs.docker.com/reference/cli/docker/compose/restart/
- GitHub Actions environments and deployments: https://docs.github.com/en/actions/reference/deployments-and-environments
- Vercel environment variables: https://vercel.com/docs/environment-variables
- Supabase database connection strings: https://supabase.com/docs/reference/postgres/connection-strings
