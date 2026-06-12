# V-Sign Baseline Cost And Performance Notes

Date: 2026-06-12

This document records the current pre-deploy baseline for the backend + AI runtime before the first AWS deployment. It is intentionally split between static facts confirmed from source and runtime measurements that still require a running container or EC2 host.

## Confirmed Model Contract

- AI endpoint used by production flow: `POST /predict-landmarks`.
- Frontend extraction: MediaPipe Holistic in the browser.
- Uploaded prediction payload: `sequence: number[][]`.
- Frame feature size: `258`.
- Feature order:
  - Pose: `33 * (x, y, z, visibility) = 132`.
  - Left hand: `21 * (x, y, z) = 63`.
  - Right hand: `21 * (x, y, z) = 63`.
- Face landmarks are not included.
- Server-side model preprocessing still calls `prepare_sequence()`.
- Target model frames: `60`.
- Velocity features are appended server-side, so model input becomes `60 x 516`.
- Max accepted landmark frames in AI service: `120`.
- Max AI JSON request body: `512 KiB`.
- AI prediction timeout: `10s`.
- AI prediction concurrency: `1` by default.

Source references:
- `v-sign-be-ai/model_v2.py`
- `v-sign-be-ai/api_server.py`
- `D:\v-sign-fe\src\services\holisticLandmarkExtractor.ts`

## Confirmed Label Scope

The deployed AI model has 35 labels in `v-sign-be-ai/models/classes.npy`:

`anhhai`, `anhhai_BT`, `banhmi`, `banhmi_NT`, `bo`, `buncha`, `bundau`, `bunmam`, `bunngang`, `bunoc`, `buon`, `ca_phe`, `chao`, `chi`, `chi_BT`, `com`, `congai`, `contrai`, `da`, `den`, `emgai`, `emtrai`, `emtrai_NT`, `hoangso`, `me`, `noigian`, `nong`, `pho`, `pho_NT`, `sua`, `thuongyeu`, `thuongyeu_BT`, `tra`, `vuive`, `vuive_BT`.

The current learning MVP uses the family, emotion, and daily food groups from `V19__mvp_ai_units_family_emotions_food.sql`.

## Static Payload Baseline

Approximate payload shape:

- Minimum accepted attempt: `5 x 258` numbers.
- Typical current FE capture target: 2-3 seconds at adaptive FPS, then server resamples to 60 frames.
- Maximum accepted attempt: `120 x 258 = 30,960` numbers.

Compact JSON payload size using representative 5-decimal landmark values:

- `24` frames: `49,645` bytes.
- `60` frames: `124,021` bytes.
- `120` frames: `247,982` bytes.

The AI body limit is currently `512 KiB`, which is intended to keep landmark-only JSON requests small and reject accidental raw image/video uploads.

## Runtime Baseline Pending

These values are not recorded yet because they need a repeatable running environment:

- AI latency p50/p95 over 20-50 prediction requests.
- AI container RAM idle/peak.
- Backend container RAM idle/peak.

Recommended commands after containers are running:

```powershell
docker stats --no-stream
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs --tail 100 ai
docker compose -f docker-compose.prod.yml logs --tail 100 backend
```

Recommended request timing method:

1. Capture one real landmark payload from the frontend dev flow.
2. Save it outside source control as a local-only JSON file.
3. Send 20-50 requests to backend `/api/v1/signature-workflows/predict-landmarks`.
4. Record p50, p95, error rate, and container RAM before and after the run.

## Deployment Sizing Assumption

Chosen initial server target:

- AWS EC2 `c6i.large`.
- 2 vCPU, 4 GiB RAM.
- Ubuntu 24.04 LTS.
- 30 GiB gp3 root volume.
- 4 GiB swap.
- Backend + AI + Caddy on one host.
- Frontend on Vercel.

Initial resource direction:

- Backend: 1 CPU, 1 GiB memory limit, JVM `-Xmx768m`.
- AI: 2 CPU, 2 GiB memory limit.
- Caddy: 0.25 CPU, 256 MiB memory limit.

## Verification Already Run

```powershell
mvn.cmd -q "-Dtest=LearningWorkflowIT,GamificationControllerIT,SubscriptionControllerIT,HealthControllerIT,FlywayMigrationTest" test
mvn.cmd -q test
python -m py_compile api_server.py
npm.cmd run lint
npm.cmd run test -- aiRecognition
npm.cmd run build
docker compose --env-file .env.deploy.example -f docker-compose.prod.yml config
powershell -ExecutionPolicy Bypass -File v-sign-be\scripts\scan-predeploy-markers.ps1
```

The compose config command completed successfully. Local Docker emitted an access warning for `C:\Users\trand\.docker\config.json`, but compose still rendered the production configuration.
