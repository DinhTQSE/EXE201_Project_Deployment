# GitHub Actions, GHCR, And Caddy Deploy Runbook

This runbook prepares the current project for a push-to-deploy flow on a small AWS EC2 server.

## Repository Assumption

The GitHub repository root contains:

- `.github/workflows/deploy.yml`
- `docker-compose.prod.yml`
- `Caddyfile`
- `.env.prod.example`
- `.env.deploy.example`
- `.env.ai.prod.example`
- `v-sign-be`
- `v-sign-fe`
- `V-Sign-AI-Build/v-sign-be-ai`

The workflow is designed for this monorepo layout. If backend, frontend, and AI are later split into separate repositories, the image build contexts and deploy bundle paths must be adjusted.

## Deploy Flow

1. A developer pushes to `main`.
2. GitHub Actions runs backend, frontend, and AI checks.
3. GitHub Actions builds these images and pushes them to GHCR:
   - `ghcr.io/<owner>/vsign-backend:sha-xxxxxxx`
   - `ghcr.io/<owner>/vsign-ai:sha-xxxxxxx`
   - `ghcr.io/<owner>/vsign-frontend:sha-xxxxxxx`
4. GitHub Actions SSHes into the server.
5. The server receives only deploy files: `docker-compose.prod.yml`, `Caddyfile`, and env examples.
6. The server pulls the new GHCR images and restarts services with Docker Compose.

The server does not build application images.

Unlike the reference HistoryTalk project, V-Sign has a React SPA in this monorepo. The frontend is therefore published as `vsign-frontend`, a Caddy-based image that contains the built `dist` files. The server still deploys by pulling images only.

## GitHub Secrets

Configure these repository secrets before the first deploy:

| Secret | Required | Purpose |
| --- | --- | --- |
| `SERVER_IP` | Yes | Public IPv4 address or DNS name of the EC2 instance. |
| `SERVER_USER` | Yes | SSH user, for example `ubuntu`. |
| `SERVER_SSH_KEY` | Yes | Private key that can SSH into the server. |
| `APP_DOMAIN` | Yes | Public domain handled by Caddy, for example `vsign.example.com`. |
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

Optional AI runtime overrides can be placed in `/opt/vsign/.env.ai.prod` using root `.env.ai.prod.example`.

## DNS

Create an `A` record from `APP_DOMAIN` to the EC2 public IPv4 address. Caddy will request and renew HTTPS certificates automatically after DNS points to the server.

## First Deploy

After secrets, DNS, Docker, and `/opt/vsign/.env.prod` are ready, push to `main`.

GitHub Actions should:

- Run tests and builds.
- Push all three images to GHCR.
- Upload deploy files to the server.
- Run `docker compose pull`.
- Run `docker compose up -d --remove-orphans`.

Verify:

```bash
docker compose --env-file /opt/vsign/.env.deploy -f /opt/vsign/docker-compose.prod.yml ps
curl https://<domain>/api/v1/health
curl https://<domain>/ai/health
```

## Caddy Routing

Caddy serves the built frontend and proxies:

- `/api/v1/*` to backend `backend:8080` after rewriting to `/V-sign/api/v1/*`.
- `/ai/*` to AI service `ai:8000`.

The Caddyfile applies request body limits for API and AI routes. It does not currently provide native request rate limiting. If the app needs stronger abuse protection, add Cloudflare/WAF in front of the server or build Caddy with a rate-limit plugin.

## Rollback

Each deploy has an immutable image tag like `sha-xxxxxxx`. To roll back manually:

```bash
cd /opt/vsign
sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=sha-previous/' .env.deploy
docker compose --env-file .env.deploy -f docker-compose.prod.yml pull
docker compose --env-file .env.deploy -f docker-compose.prod.yml up -d --remove-orphans
```

The next push to `main` will deploy the new commit tag again.
