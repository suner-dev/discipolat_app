# Variables d'environnement — Discipolat Church OS

> Version 2.0 — 2026-09-22 · §72 / G6.8. Toutes les variables ci-dessous sont lues par `backend/src/main/resources/application.yml` (profils `dev|docker|beta|prod|test`). Copier vers `.env` / Render / secrets CI. Voir [DEPLOYMENT.md](DEPLOYMENT.md) · [ARCHITECTURE.md](ARCHITECTURE.md) · [RUNBOOK.md](RUNBOOK.md).

## 1. Base & profils

| Variable | Defaut | Description |
|---|---|---|
| `PORT` | `8080` (Render `10000`) | Port HTTP API |
| `SPRING_PROFILES_ACTIVE` | `dev` (local compose `docker`) | `dev\|docker\|beta\|prod` |
| `APP_ENVIRONMENT` | `dev` | `dev\|docker\|beta\|prod` (badge BETA, garde reset) |
| `APP_VERSION` | `1.0.0` | Version exposee `/public/meta` |
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | `jdbc:postgresql://localhost:5432/discipolat/discipolat/…` | En prod : Internal URL Render ; compose `db:5432` |
| `POSTGRES_DB/USER/PASSWORD` | `discipolat/discipolat/discipolat_secret` | Compose `db` uniquement |
| `REDIS_URL` | `redis://localhost:6379` | Rate limiting Bucket4j + cache tenant-aware |
| `ENCRYPTION_AES_KEY` | (vide) | Base64 32 octets (`openssl rand -base64 32`), chiffrement donnees sensibles + backups |
| `VITE_API_URL` | `http://localhost:8080` | URL API pour le frontend (prod `https://discipolat-api.onrender.com`) |

## 2. Auth / JWT / CORS / mail

| Variable | Defaut | Description |
|---|---|---|
| `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` | (vide, base64) | Cles RSA 2048 (`cat keys/private.pem \| base64 -w0`). Secrets Render |
| `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH` | (vide) | Alternative fichiers (compose `/app/keys/*.pem`) |
| `FRONTEND_URL` | `http://localhost:3000,http://localhost:5173,…ngrok…` | Origines CORS (patterns supportes) |
| `FRONTEND_URL_BASE` | `http://localhost:5173` | URL canonique frontend |
| `MAIL_HOST/PORT/USERNAME/PASSWORD` | `localhost/1025/noreply@discipolat.com/(vide)` | Prod `smtp.mailgun.org:2525` + auth TLS (plan Free bloque 25/465/587) ; dev MailHog |

## 3. Beta-testing

| Variable | Defaut | Description |
|---|---|---|
| `DEMO_ACCOUNTS_ENABLED` | `false` (`true` en docker/beta) | Comptes demo visibles au login |
| `DEMO_SEED_ENABLED` | `false` (`true` en docker/beta) | Seed demo au boot — **jamais en prod** |
| `BETA_RESET_ENABLED` | `false` (`true` en docker/beta) | `POST /admin/beta/reset` — **refuse en prod** (double garde) |

## 4. Paiements (Orange / MTN / M-Pesa)

| Variable | Defaut | Description |
|---|---|---|
| `MOBILEMONEY_ENABLED` | `false` | `true` = appels operateurs reels ; `false` = fallback local (reference generee) |
| `ORANGE_API_KEY/MERCHANT_KEY/CLIENT_SECRET/BASE_URL/WEBHOOK_SECRET/RETURN_URL/NOTIF_URL` | `https://api.orange.com`, `…/giving` | Orange Money |
| `MTN_BASE_URL/SUBSCRIPTION_KEY/CLIENT_ID/CLIENT_SECRET/WEBHOOK_SECRET` | `https://sandbox.momodeveloper.mtn.com` | MTN MoMo |
| `MPESA_BASE_URL/CONSUMER_KEY/CONSUMER_SECRET/SHORT_CODE/PASSKEY/WEBHOOK_SECRET/NOTIF_URL` | `https://sandbox.safaricom.co.ke` | M-Pesa |
| `PAYMENTS_WEBHOOK_SECRET` | (vide → endpoint ferme, 503) | Secret webhook operateur |
| `PAYMENTS_WEBHOOK_POLL_INTERVAL_MS` | `30000` | Poll PENDING (MTN sans webhook) |
| `PAYMENTS_SIMULATE_CONFIRM_DELAY_MS` (+ `_NON_MANAGER_MS`, `_SCAN_INTERVAL_MS`) | `0/0/5000` | Simulation beta/dev — **0 en prod** |

## 5. WhatsApp / IA / rate limiting / schedulers

| Variable | Defaut | Description |
|---|---|---|
| `WHATSAPP_ENABLED` | `false` | WhatsApp Business Cloud (Phone ID + token configures en back-office) |
| `OLLAMA_URL` / `AI_MODEL` | `http://localhost:11434` / `llama3` | IA locale (fallback contextuel si injoignable) |
| `GROQ_API_KEY/GEMINI_API_KEY/MISTRAL_API_KEY/HUGGINGFACE_API_KEY` | (vide) | Providers gratuits optionnels |
| `REDIS_URL` / `…KEY_EXPIRE_MINUTES` | `redis://…` / `10` | Buckets distribues |
| Login/refresh/forgot/reset/activate/change/switch-role capacities | `10/20/3/5/5/5/30` par min/IP | Voir [API.md](API.md) |
| Crons `absence/report-reminder/saturday/dashboard/transfer-delay` | `0 0 */6 * * *` etc. | Schedulers Spring (aucun cron Render requis) |

## 6. Infra / CI (GitHub Secrets)

| Variable | Description |
|---|---|
| `RENDER_API_KEY` / `RENDER_API_SERVICE_ID` | Deploiement Render via CI |
| `RENDER_DB_URL` / `BACKUP_ENCRYPTION_KEY` | Dump mensuel chiffre (`backup-postgres.yml`) |

## 7. Exemple `.env` local

```bash
SPRING_PROFILES_ACTIVE=docker
APP_ENVIRONMENT=docker
POSTGRES_DB=discipolat
POSTGRES_USER=discipolat
POSTGRES_PASSWORD=discipolat_secret
REDIS_URL=redis://redis:6379
JWT_PRIVATE_KEY_PATH=/app/keys/private.pem
JWT_PUBLIC_KEY_PATH=/app/keys/public.pem
MAIL_HOST=mailhog
MAIL_PORT=1025
FRONTEND_URL=*
VITE_API_URL=http://localhost:8080
MOBILEMONEY_ENABLED=false
WHATSAPP_ENABLED=false
```

Generer les cles : `openssl genpkey -algorithm RSA -out keys/private.pem -pkeyopt rsa_keygen_bits:2048 && openssl pkey -in keys/private.pem -pubout -out keys/public.pem` (ou `bash scripts/deploy-setup.sh --keys`).
