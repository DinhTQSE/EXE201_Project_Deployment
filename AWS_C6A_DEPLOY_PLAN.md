# V-Sign AWS c6a.large Operator Deploy Runbook

Created: 2026-06-12
Updated: 2026-06-12

Purpose: this file is a step-by-step guide for the project owner/operator to deploy V-Sign. Codex prepares the repository and can help verify files, but the actual AWS, DNS, GitHub, Vercel, and production secret steps are done by you.

---

## 0. Deployment Ownership Model

Codex has already prepared:

- Backend + AI production Dockerfiles.
- Root `docker-compose.prod.yml`.
- Root `Caddyfile`.
- GitHub Actions workflow at `.github/workflows/deploy.yml`.
- Example env files with placeholders only.
- Predeploy refactor checklist in `PRE_DEPLOY_HOLISTIC_REFACTOR_PLAN.md`.

You will do manually:

- Create the AWS EC2 instance.
- Create DNS records.
- Create production server env files.
- Add GitHub repository secrets.
- Push code to GitHub.
- Connect Vercel frontend to the deployed API.
- Run first production smoke test.

Do not put real production secrets into Git.

---

## 1. Final Target Architecture

```text
Vercel frontend
  -> https://api.<domain>/api/v1/*
  -> Caddy on EC2
  -> backend container
  -> private Docker network
  -> AI container
```

Production rules:

- Only Caddy exposes public ports `80` and `443`.
- Backend port `8080` is private inside Docker.
- AI port `8000` is private inside Docker.
- Frontend calls only the backend API domain.
- Frontend must not call the AI container directly.
- EC2 server does not build application images. It only pulls images from GHCR.

---

## 2. Chosen Server

Use:

```text
AWS region: ap-southeast-1
EC2 instance: c6a.large
Architecture: x86_64 / linux/amd64
vCPU/RAM: 2 vCPU / 4 GB RAM
OS: Amazon Linux 2023 x86_64
Root disk: 30 GB gp3
Swap: 4 GB
Runtime: backend + AI + Caddy
Frontend: Vercel, not EC2
```

Estimated 60-day 24/7 cost:

```text
c6a.large compute: usually higher than us-east-1; verify current ap-southeast-1 On-Demand price in AWS before launch
30 GB gp3 EBS: about 5-8 USD
Public IPv4: about 7-8 USD
Buffer for transfer/tax/small variance: about 15-25 USD
Expected total: likely still near the 160 USD target, but tighter than us-east-1
```

Why `c6a.large`:

- Same x86_64 Docker compatibility as `c6i.large`.
- Cheaper than `c6i.large` for this use case.
- Avoids ARM risk for Python AI dependencies during the first deploy.
- Stable compute instance, not CPU-credit burstable like `t3.medium` or `t4g.medium`.

Do not use these for the first deploy unless the plan is revised:

- ALB.
- NAT Gateway.
- RDS.
- CloudFront in front of the API.
- ARM/Graviton instances such as `t4g.medium`, `c6g.large`, or `c7g.large`.

---

## 3. Accounts And Inputs You Need Before Starting

Prepare these before touching AWS:

- AWS account with billing enabled.
- Domain name or subdomain you can edit.
- GitHub repository containing this server repo.
- GitHub access to repository settings and secrets.
- Vercel access for frontend repo `D:\v-sign-fe`.
- Supabase/PostgreSQL production database credentials.
- A production backend API domain. If you already own a domain, recommended:

```text
api.<your-domain>
```

If you do not own a domain yet, use temporary Elastic-IP hostname mode after allocating the Elastic IP:

```text
<elastic-ip-with-dashes>.sslip.io
```

Example:

```text
52-0-56-137.sslip.io
```

This hostname resolves to the embedded Elastic IP and lets Caddy request a normal public HTTPS certificate. It is temporary and can be replaced by `api.<your-domain>` later.

Keep these values ready:

```text
AWS region = ap-southeast-1
EC2 user = ec2-user
Deploy path = /opt/vsign
Backend API domain = api.<your-domain> or <elastic-ip-with-dashes>.sslip.io
Production DB schema = vsign_prod
```

Current deployment values:

```text
Elastic IP = 18.136.251.56
Backend API domain = apivsignvn.social
Backend API base URL = https://apivsignvn.social/api/v1
```

---

## 4. Local Repository Check Before Push

Run from local PowerShell:

```powershell
cd D:\V-sign_EXE101_Project
git status --short
```

Expected:

- You should understand every modified file.
- Real `.env`, `.env.prod`, `.env.ai.prod`, private keys, and credential files must not be staged.
- Example files such as `.env.prod.example` are safe to commit because they contain placeholders only.

Optional pre-push checks:

```powershell
cd D:\V-sign_EXE101_Project\v-sign-be
mvn.cmd -q test

cd D:\V-sign_EXE101_Project\v-sign-be-ai
python -m py_compile api_server.py

cd D:\V-sign_EXE101_Project
docker compose --env-file .env.deploy.example -f docker-compose.prod.yml config
```

Frontend checks, from the separate frontend repo:

```powershell
cd D:\v-sign-fe
npm.cmd run lint
npm.cmd run test -- aiRecognition
npm.cmd run build
```

If these fail, fix locally before deploy.

---

## 5. Create Production Database Schema

Use your production database SQL editor, for example Supabase SQL editor.

Run:

```sql
create schema if not exists vsign_prod;
```

Production schema policy:

- Local dev may use `v-sign_schema` because it contains current dev data.
- Production must use `vsign_prod`.
- Do not use `public` for production.
- Do not reuse `v-sign_schema` for production.
- Flyway should run from `V1` against `vsign_prod` during the first production deploy.

You will put `DB_SCHEMA=vsign_prod` in the server `/opt/vsign/.env.prod` file later.

---

## 6. Create AWS EC2 Instance

Do this in AWS Console.

1. Go to `EC2`.
2. Select region `ap-southeast-1` (Asia Pacific - Singapore).
3. Click `Launch instance`.
4. Name:

```text
vsign-prod-1
```

5. AMI:

```text
Amazon Linux 2023 AMI, 64-bit x86
```

6. Instance type:

```text
c6a.large
```

7. Key pair:

- Create a new key pair if you do not already have one.
- Type can be RSA or ED25519.
- Download the `.pem` file.
- Store it somewhere private, for example outside the repo.
- Do not commit this key.

8. Network/security group:

Inbound rules:

```text
SSH   TCP 22   Your IP only
HTTP  TCP 80   0.0.0.0/0
HTTPS TCP 443  0.0.0.0/0
```

If you use IPv6, also allow HTTP/HTTPS from `::/0`.

Do not open:

```text
8080
8000
5432
```

9. Storage:

```text
30 GiB gp3 root volume
```

10. Launch instance.

---

## 7. Allocate Elastic IP

Do this in AWS Console.

1. Go to `EC2 > Elastic IPs`.
2. Click `Allocate Elastic IP address`.
3. Allocate in `ap-southeast-1`.
4. Select the new Elastic IP.
5. Click `Actions > Associate Elastic IP address`.
6. Associate it with `vsign-prod-1`.

Record this value:

```text
SERVER_IP=<elastic-ip>
```

This IP is used by:

- DNS `A` record.
- GitHub secret `SERVER_IP`.
- SSH from your local machine.

---

## 8. Choose Temporary API Hostname

If you do not own a domain yet, use the Elastic IP through `sslip.io`.

Example:

```text
Elastic IP: 52.0.56.137
Temporary API hostname: 52-0-56-137.sslip.io
Temporary API base URL: https://52-0-56-137.sslip.io/api/v1
```

Rules:

- Use dashes instead of dots in the IP part.
- Do not include `http://` or `https://` in the GitHub `APP_DOMAIN` secret.
- Use the temporary hostname in `APP_DOMAIN`, not the raw IP, if Vercel frontend needs to call the API.
- Raw `http://<elastic-ip>` is acceptable only for backend smoke testing, not for Vercel production, because Vercel HTTPS pages cannot safely call HTTP APIs.
- Later, when you buy a domain, change `APP_DOMAIN` to `api.<your-domain>`, change Vercel `VITE_API_BASE_URL`, and rerun deploy.

Verify the temporary hostname from local PowerShell:

```powershell
nslookup <elastic-ip-with-dashes>.sslip.io
```

Expected:

```text
<elastic-ip-with-dashes>.sslip.io resolves to <elastic-ip>
```

---

## 9. SSH Into The Server

From local PowerShell:

```powershell
ssh -i C:\path\to\vsign-prod.pem ec2-user@<elastic-ip>
```

If Windows OpenSSH rejects the key because of permissions, run this locally:

```powershell
icacls "C:\path\to\vsign-prod.pem" /inheritance:r
icacls "C:\path\to\vsign-prod.pem" /grant:r "$($env:USERNAME):(R)"
```

Then SSH again:

```powershell
ssh -i C:\path\to\vsign-prod.pem ec2-user@<elastic-ip>
```

Expected result:

```text
ec2-user@ip-...:~$
```

---

## 10. Bootstrap The Server

Run these commands on the EC2 server through SSH.

Update packages:

```bash
sudo dnf update -y
```

Install Docker Engine and Compose plugin.

Amazon Linux 2023 normally already includes `curl-minimal`. Do not install the full `curl` package unless needed, because it can conflict with `curl-minimal`.

```bash
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
```

Allow the `ec2-user` user to run Docker:

```bash
sudo usermod -aG docker "$USER"
exit
```

SSH back in:

```powershell
ssh -i C:\path\to\vsign-prod.pem ec2-user@<elastic-ip>
```

Verify:

```bash
docker version
docker compose version
```

Create 4 GB swap:

```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

Create deploy directory:

```bash
sudo mkdir -p /opt/vsign
sudo chown "$USER:$USER" /opt/vsign
cd /opt/vsign
```

Expected:

- `docker version` works without `sudo`.
- `docker compose version` works.
- `free -h` shows about 4 GB swap.
- `/opt/vsign` is writable by `ec2-user`.

---

## 11. Create Server Env Files

These files are created on the EC2 server. They are not committed to GitHub.

### 11.1 Backend Env

On EC2:

```bash
nano /opt/vsign/.env.prod
```

Paste and fill real production values:

```properties
DB_URL=jdbc:postgresql://<host>:<port>/<database>?prepareThreshold=0
DB_USER=<prod-db-user>
DB_PASSWORD=<prod-db-password>
DB_SCHEMA=vsign_prod

JWT_SECRET=<long-random-production-secret>
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000

APP_CORS_ALLOWED_ORIGINS=https://<your-vercel-domain>
AI_SERVICE_BASE_URL=http://ai:8000
AI_SERVICE_PREDICT_TIMEOUT_MS=10000
APP_PAYMENT_PUBLIC_BASE_URL=https://payments.example.invalid
```

Rules:

- Do not wrap values in quotes unless the app explicitly requires it.
- No spaces around `=`.
- `DB_SCHEMA` must be `vsign_prod`.
- `APP_CORS_ALLOWED_ORIGINS` must include the real Vercel frontend domain.
- Keep `AI_SERVICE_BASE_URL=http://ai:8000`.

Save in nano:

```text
Ctrl+O, Enter, Ctrl+X
```

### 11.2 AI Env

On EC2:

```bash
nano /opt/vsign/.env.ai.prod
```

Paste:

```properties
VSIGN_AI_API_VERSION=2.0.0
VSIGN_AI_MODEL_VERSION=branch-bilstm-attention-v2
VSIGN_AI_LABEL_VERSION=mvp-3-units-20260609
VSIGN_AI_ENABLE_LEGACY_FRAME_PREDICT=false
VSIGN_AI_MAX_LANDMARK_FRAMES=120
VSIGN_AI_MAX_JSON_BODY_BYTES=524288
VSIGN_AI_PREDICT_TIMEOUT_SECONDS=10
VSIGN_AI_MAX_CONCURRENT_PREDICTIONS=1
```

Save.

### 11.3 Confirm Env Files Exist

On EC2:

```bash
ls -la /opt/vsign
```

Expected:

```text
.env.prod
.env.ai.prod
```

Do not print these files in terminal screenshots or commit logs because they contain secrets.

---

## 12. Configure DNS Or Temporary Hostname

If you are using temporary Elastic-IP hostname mode with `sslip.io`, skip custom DNS setup for now and use:

```text
APP_DOMAIN=<elastic-ip-with-dashes>.sslip.io
VITE_API_BASE_URL=https://<elastic-ip-with-dashes>.sslip.io/api/v1
```

If you own a domain, configure your DNS provider:

1. Create an `A` record:

```text
Name: api
Type: A
Value: <elastic-ip>
TTL: Auto or 300
```

2. Wait for propagation.

From local PowerShell:

```powershell
nslookup api.<your-domain>
```

Expected:

```text
api.<your-domain> resolves to <elastic-ip>
```

Cloudflare note:

- For the first deploy, use DNS-only mode if possible.
- After HTTPS works, you can decide whether to enable proxy mode.
- If proxy mode is enabled, keep SSL/TLS mode compatible with Caddy HTTPS.

---

## 13. Configure GitHub Repository

Do this in GitHub web UI for the server repository.

### 13.1 Workflow Permissions

Go to:

```text
Repository > Settings > Actions > General > Workflow permissions
```

Select:

```text
Read and write permissions
```

Reason: the workflow uses `GITHUB_TOKEN` to push Docker images to GHCR.

### 13.2 Add Repository Secrets

Go to:

```text
Repository > Settings > Secrets and variables > Actions > New repository secret
```

Add:

```text
SERVER_IP=<elastic-ip>
SERVER_USER=ec2-user
SERVER_SSH_KEY=<full private key content from the EC2 key pair>
GHCR_USERNAME=<your-github-username>
GHCR_TOKEN=<github-token-that-can-read-ghcr-packages>
APP_DOMAIN=<elastic-ip-with-dashes>.sslip.io
DEPLOY_PATH=/opt/vsign
```

If you already own a domain, use this instead:

```text
APP_DOMAIN=api.<your-domain>
```

Secret notes:

- `SERVER_SSH_KEY` is the private key content, including header and footer.
- Do not use the public key.
- If GHCR packages are private, `GHCR_TOKEN` must be able to pull packages.
- A classic PAT with `read:packages` is usually enough for package pull. If packages are tied to a private repo, also ensure the token has access to that repo.
- DB credentials do not go into GitHub secrets for this setup. They live only in `/opt/vsign/.env.prod` on the server.

### 13.3 Create The GHCR Token

There are two different GitHub tokens in this deploy flow:

```text
GITHUB_TOKEN
GHCR_TOKEN
```

`GITHUB_TOKEN`:

- Automatically exists inside GitHub Actions.
- You do not create it manually.
- The workflow uses it to push backend/AI images to GHCR.
- This is why `.github/workflows/deploy.yml` has:

```yaml
permissions:
  contents: read
  packages: write
```

`GHCR_TOKEN`:

- You create it manually in your GitHub account.
- It is a Personal Access Token used by the EC2 server to pull GHCR images.
- It is stored as a GitHub Actions repository secret named `GHCR_TOKEN`.
- The deploy workflow sends it over SSH only for `docker login ghcr.io` and `docker compose pull`.

Create `GHCR_TOKEN`:

1. Open GitHub.
2. Click your avatar.
3. Go to `Settings`.
4. Go to `Developer settings`.
5. Go to `Personal access tokens`.
6. Choose `Tokens (classic)`.
7. Click `Generate new token` > `Generate new token (classic)`.
8. Name it:

```text
vsign-prod-ghcr-pull
```

9. Expiration:

```text
90 days
```

10. Select scope:

```text
read:packages
```

11. If the repository/package is private and GitHub requires repo access for your account setup, also grant the minimum private repository access needed for this server repository.
12. Generate token.
13. Copy it immediately.
14. Add it to the server repository secret:

```text
GHCR_TOKEN=<copied-token>
```

If you make GHCR packages public, anonymous Docker pull can work. This runbook still keeps `GHCR_TOKEN` because the deploy workflow intentionally performs authenticated `docker login` before pulling images.

---

## 14. Push Server Repo To GitHub

From local PowerShell:

```powershell
cd D:\V-sign_EXE101_Project
git status --short
git add .
git status --short
```

Before commit, verify that no real secret files are staged.

Commit and push:

```powershell
git commit -m "Prepare production deploy pipeline"
git push origin main
```

Expected:

- GitHub Actions starts automatically on `main`.
- Workflow name: `Build And Deploy Server`.

If you do not want to push yet, stop here.

---

## 15. First GitHub Actions Deploy

In GitHub:

1. Open the repository.
2. Go to `Actions`.
3. Open the latest `Build And Deploy Server` run.
4. Watch jobs in order:

```text
Test Server Artifacts
Build And Push GHCR Images
Deploy To Server
```

Expected:

- Backend tests pass.
- AI syntax check passes.
- Backend image is pushed:

```text
ghcr.io/<owner>/vsign-backend:sha-xxxxxxx
ghcr.io/<owner>/vsign-backend:latest
```

- AI image is pushed:

```text
ghcr.io/<owner>/vsign-ai:sha-xxxxxxx
ghcr.io/<owner>/vsign-ai:latest
```

- Deploy job SSHes into EC2.
- Deploy job creates `/opt/vsign/.env.deploy`.
- Deploy job pulls images and starts Compose.

If deploy fails with missing `.env.prod`, SSH into EC2 and create `/opt/vsign/.env.prod`, then rerun the workflow.

---

## 16. Verify Server After Deploy

SSH into EC2:

```powershell
ssh -i C:\path\to\vsign-prod.pem ec2-user@<elastic-ip>
```

Run:

```bash
cd /opt/vsign
docker compose --env-file .env.deploy -f docker-compose.prod.yml ps
```

Expected services:

```text
backend
ai
caddy
```

Expected state:

```text
running or healthy
```

Check logs:

```bash
docker compose --env-file .env.deploy -f docker-compose.prod.yml logs --tail=100 backend
docker compose --env-file .env.deploy -f docker-compose.prod.yml logs --tail=100 ai
docker compose --env-file .env.deploy -f docker-compose.prod.yml logs --tail=100 caddy
```

Check container resource usage:

```bash
docker stats --no-stream
```

---

## 17. Public API Smoke Test

From local PowerShell:

```powershell
curl.exe -i https://<app-domain>/api/v1/health
curl.exe -i https://<app-domain>/api/v1/version
```

Expected:

- HTTP `200`.
- Health endpoint reports backend healthy.
- Version endpoint returns backend metadata.

Root path should not expose the app:

```powershell
curl.exe -i https://<app-domain>/
```

Expected:

```text
404
```

Direct AI access should not exist publicly:

```powershell
curl.exe -i https://<app-domain>/ai/health
```

Expected:

```text
404
```

Internal AI check from EC2:

```bash
cd /opt/vsign
docker compose --env-file .env.deploy -f docker-compose.prod.yml exec backend sh -lc "curl -fsS http://ai:8000/health"
```

Expected:

```text
AI health JSON
```

---

## 18. Configure Vercel Frontend

Do this in the Vercel project connected to frontend repo `D:\v-sign-fe`.

Set environment variable:

```text
VITE_API_BASE_URL=https://<app-domain>/api/v1
```

For temporary Elastic-IP hostname mode:

```text
VITE_API_BASE_URL=https://<elastic-ip-with-dashes>.sslip.io/api/v1
```

Do not set a public AI URL.

Deploy frontend on Vercel.

Expected frontend API behavior:

```text
FE -> https://<app-domain>/api/v1/*
```

Not allowed:

```text
FE -> http://ai:8000/*
FE -> https://<app-domain>/ai/*
FE -> localhost in production
```

---

## 19. End-To-End Smoke Test

Use the deployed Vercel frontend.

Run these checks:

- Register a new user.
- Login.
- Open a free lesson.
- Play the current lesson video.
- Pass text-to-video quiz.
- Pass video-to-text quiz.
- Allow camera.
- Run AI practice.
- Complete the lesson.
- Confirm XP/streak update from backend.
- Open dictionary and play a video.
- Confirm premium lessons stay locked for a basic user.

Browser DevTools network checks:

- Requests go to `https://<app-domain>/api/v1`.
- No request goes to `localhost`.
- No request goes to `/ai`.
- AI practice request sends landmark sequence only.
- No request body contains `data:image`, `base64`, `jpeg`, or raw image frames.

---

## 20. Runtime Metrics To Record

After first successful deploy, record these in `v-sign-be/docs/ops/baseline-cost-performance.md`:

- Backend container idle RAM.
- AI container idle RAM.
- Backend container peak RAM during smoke test.
- AI container peak RAM during AI practice.
- AI prediction latency p50/p95 from 20-50 attempts if possible.

Useful EC2 commands:

```bash
cd /opt/vsign
docker stats --no-stream
docker compose --env-file .env.deploy -f docker-compose.prod.yml logs --tail=200 backend
docker compose --env-file .env.deploy -f docker-compose.prod.yml logs --tail=200 ai
```

---

## 21. Rollback

Use this if the latest deploy is broken and you know a previous good GHCR SHA tag.

SSH into EC2:

```powershell
ssh -i C:\path\to\vsign-prod.pem ec2-user@<elastic-ip>
```

On EC2:

```bash
cd /opt/vsign
nano .env.deploy
```

Set:

```properties
GHCR_OWNER=<github-owner-lowercase>
IMAGE_TAG=sha-previous
APP_DOMAIN=<app-domain>
```

Then:

```bash
docker compose --env-file .env.deploy -f docker-compose.prod.yml pull
docker compose --env-file .env.deploy -f docker-compose.prod.yml up -d --remove-orphans
docker compose --env-file .env.deploy -f docker-compose.prod.yml ps
```

Rollback is valid only if:

- `https://<app-domain>/api/v1/health` returns healthy.
- Internal `http://ai:8000/health` works from backend.
- Caddy serves HTTPS.
- Login and free lesson flow still work.

---

## 22. Common Failure Checks

### GitHub deploy cannot SSH

Check:

- `SERVER_IP` secret is the Elastic IP.
- `SERVER_USER=ec2-user`.
- `SERVER_SSH_KEY` contains the full private key.
- EC2 security group allows SSH from GitHub runner IPs or from anywhere temporarily.
- The instance is running.

### GHCR pull fails on server

Check:

- `GHCR_USERNAME` is correct.
- `GHCR_TOKEN` can read packages.
- GHCR package visibility allows the token to pull.
- Workflow image owner matches `ghcr.io/<owner>/vsign-*`.

### Caddy cannot get HTTPS certificate

Check:

- If using your own domain, DNS `A` record points to the Elastic IP.
- If using temporary mode, `<elastic-ip-with-dashes>.sslip.io` resolves to the Elastic IP.
- Ports `80` and `443` are open in the security group.
- No other process is using ports `80` or `443`.
- `APP_DOMAIN` secret matches the hostname exactly.

### Backend starts but DB migration fails

Check:

- `/opt/vsign/.env.prod` has the correct `DB_URL`, `DB_USER`, `DB_PASSWORD`.
- `DB_SCHEMA=vsign_prod`.
- Production schema exists:

```sql
create schema if not exists vsign_prod;
```

- Database firewall allows the EC2 public IP if your provider requires allowlisting.

### Frontend cannot call API

Check:

- Vercel env `VITE_API_BASE_URL=https://<app-domain>/api/v1`.
- Backend CORS includes the exact Vercel domain in `APP_CORS_ALLOWED_ORIGINS`.
- Redeploy Vercel after changing env.
- Backend health endpoint works publicly.

### AI prediction fails

Check:

- `ai` container is running.
- Backend can reach `http://ai:8000/health`.
- `/opt/vsign/.env.ai.prod` exists.
- AI logs do not show model file loading errors.
- EC2 memory is not exhausted:

```bash
free -h
docker stats --no-stream
```

---

## 23. Hard Boundaries

Do not do these during the first stable deploy:

- Do not expose AI publicly.
- Do not open port `8000`.
- Do not open backend port `8080`.
- Do not deploy frontend on the EC2 server.
- Do not switch to ARM/Graviton until backend and AI images are verified as `linux/arm64`.
- Do not add RDS/ALB/NAT Gateway unless the budget is revised.
- Do not implement production payment gateway in this deploy cycle unless Phase 5 is explicitly reactivated.

---

## 24. First Deploy Completion Criteria

Mark the deployment complete only when all are true:

- EC2 `c6a.large` is running.
- Elastic IP is attached.
- `APP_DOMAIN` resolves to the Elastic IP. This can be `api.<your-domain>` or `<elastic-ip-with-dashes>.sslip.io`.
- `/opt/vsign/.env.prod` exists on EC2.
- `/opt/vsign/.env.ai.prod` exists on EC2.
- GitHub Actions test job passes.
- GHCR backend and AI images are published.
- GitHub Actions deploy job succeeds.
- `backend`, `ai`, and `caddy` containers are running.
- `GET https://<app-domain>/api/v1/health` returns healthy.
- Vercel frontend uses the production API domain.
- Login, free lesson, quiz, AI practice, and completion work.
- Browser network shows no raw image/base64 upload.
