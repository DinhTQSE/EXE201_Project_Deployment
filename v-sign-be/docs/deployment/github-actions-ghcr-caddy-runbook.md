# GitHub Actions, GHCR, And Caddy Deploy Runbook

This runbook prepares the current server repository for a push-to-deploy flow on a small AWS EC2 server.

## Repository Assumption

The server GitHub repository root contains:

- `.github/workflows/deploy.yml`
- `docker-compose.prod.yml`
- `Caddyfile`
- `.env.prod.example`
- `.env.deploy.example`
- `.env.ai.prod.example`
- `v-sign-be`
- `v-sign-be-ai`

The frontend is deployed from its own repository/directory to Vercel. It is not built or served by this server workflow.

## Deploy Flow

1. A developer pushes to `main` in the server repository.
2. GitHub Actions runs backend tests and an AI syntax check.
3. GitHub Actions builds these images and pushes them to GHCR:
   - `ghcr.io/<owner>/vsign-backend:sha-xxxxxxx`
   - `ghcr.io/<owner>/vsign-ai:sha-xxxxxxx`
4. GitHub Actions SSHes into the server.
5. The server receives only deploy files: `docker-compose.prod.yml`, `Caddyfile`, and env examples.
6. The server pulls the new GHCR images and restarts services with Docker Compose.

The server does not build application images. Backend and AI communicate over the Docker network. AI is not exposed publicly through Caddy.

## GitHub Secrets

Configure these repository secrets before the first deploy:

| Secret | Required | Purpose |
| --- | --- | --- |
| `SERVER_IP` | Yes | Public IPv4 address or DNS name of the EC2 instance. |
| `SERVER_USER` | Yes | SSH user, for example `ubuntu`. |
| `SERVER_SSH_KEY` | Yes | Private key that can SSH into the server. |
| `APP_DOMAIN` | Yes | Public API domain handled by Caddy, for example `api.vsign.example.com`. |
| `GHCR_USERNAME` | Yes | GitHub username or machine user used by the server to pull GHCR images. |
| `GHCR_TOKEN` | Yes | GitHub token with `read:packages` permission. |
| `DEPLOY_PATH` | No | Server deploy path. Defaults to `/opt/vsign`. |

The workflow uses `GITHUB_TOKEN` to push images to GHCR. The server uses `GHCR_TOKEN` only to pull images.

## AWS Server Bootstrap

Use Ubuntu on EC2. For a free-tier-sized server, keep memory pressure low and add swap before first deploy.

Open security group ports:

- `22/tcp` from your IP only.
- `80/tcp` from the internet.
- `443/tcp` from the internet.

Install Docker and the Docker Compose plugin:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
```

Add a small swap file:

```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

Create the deploy directory:

```bash
sudo mkdir -p /opt/vsign
sudo chown "$USER:$USER" /opt/vsign
```

Create `/opt/vsign/.env.prod` from root `.env.prod.example` and fill real production values. Do not commit this file.

Backend production values should include:

```bash
DB_SCHEMA=vsign_prod
APP_CORS_ALLOWED_ORIGINS=https://your-vercel-app.vercel.app
AI_SERVICE_BASE_URL=http://ai:8000
AI_SERVICE_PREDICT_TIMEOUT_MS=10000
```

Database schema rules:

- Use a clean production schema such as `vsign_prod`.
- Do not use `public` for production.
- Do not reuse local/dev `v-sign_schema` for production.
- Create the production schema before first deploy:

```sql
create schema if not exists vsign_prod;
```

Optional AI runtime overrides can be placed in `/opt/vsign/.env.ai.prod` using root `.env.ai.prod.example`.

## Frontend On Vercel

Deploy the frontend from its own repository/directory. Configure the frontend production API base URL to the public Caddy API domain, for example:

```bash
VITE_API_BASE_URL=https://api.vsign.example.com/api/v1
```

Use the actual env variable name expected by the frontend project if it differs. The frontend should call the backend API only; it should not call the AI service directly in production.

## DNS

Create an `A` record from `APP_DOMAIN` to the EC2 public IPv4 address. Caddy will request and renew HTTPS certificates automatically after DNS points to the server.

## First Deploy

After secrets, DNS, Docker, and `/opt/vsign/.env.prod` are ready, push to `main`.

GitHub Actions should:

- Run backend and AI checks.
- Push backend and AI images to GHCR.
- Upload deploy files to the server.
- Run `docker compose pull`.
- Run `docker compose up -d --remove-orphans`.

Verify:

```bash
cd /opt/vsign
docker compose --env-file .env.deploy -f docker-compose.prod.yml ps
curl https://<api-domain>/api/v1/health
curl https://<api-domain>/api/v1/version
docker compose --env-file .env.deploy -f docker-compose.prod.yml exec -T ai python -c "import urllib.request; print(urllib.request.urlopen('http://localhost:8000/health', timeout=5).read().decode())"
```

## Caddy Routing

Caddy proxies only:

- `/api/v1/*` to backend `backend:8080` after rewriting to `/V-sign/api/v1/*`.

AI remains private on the Docker network at `http://ai:8000`. The backend calls AI through `AI_SERVICE_BASE_URL=http://ai:8000`.

The Caddyfile applies a request body limit for `/api/v1`. It does not currently provide native request rate limiting. If the app needs stronger abuse protection, add Cloudflare/WAF in front of the server or build Caddy with a rate-limit plugin.

Recommended first production rate-protection path:

- Put the API domain behind Cloudflare proxied DNS after Caddy HTTPS is verified.
- Add a Cloudflare WAF/rate limiting rule for `https://<api-domain>/api/v1/signature-workflows/predict-landmarks`.
- Start with a conservative learner-facing threshold, for example 30 requests per minute per IP, then tighten after observing real usage.
- Add a broader rule for `/api/v1/auth/*` if login/register abuse appears.
- Keep Caddy's request body limit enabled even when Cloudflare is in front.

## Rollback

Each deploy has an immutable image tag like `sha-xxxxxxx`. To roll back manually:

```bash
cd /opt/vsign
sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=sha-previous/' .env.deploy
docker compose --env-file .env.deploy -f docker-compose.prod.yml pull
docker compose --env-file .env.deploy -f docker-compose.prod.yml up -d --remove-orphans
```

The next push to `main` will deploy the new commit tag again.
