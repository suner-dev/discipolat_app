# Déploiement — Discipolat

## Architecture de déploiement

```
                     ┌──────────────┐
                     │   GitHub      │
                     │  Repository   │
                     └──────┬───────┘
                            │ push (main)
                    ┌───────┴───────┐
                    │  GitHub Actions│
                    │    CI/CD       │
                    └───┬───┬───┬───┘
                        │   │   │
               ┌────────┘   │   └────────┐
               ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ Backend  │ │ Frontend │ │ Render   │
        │ Tests    │ │ Build    │ │ Deploy   │
        └──────────┘ └──────────┘ └────┬─────┘
                                       │
                          ┌────────────┼────────────┐
                          ▼            ▼            ▼
                   ┌──────────┐ ┌──────────┐ ┌──────────┐
                   │ Render   │ │ Render   │ │ Render   │
                   │ API      │ │ Web      │ │ DB       │
                   │ (Docker) │ │ (Docker) │ │(Postgres)│
                   └──────────┘ └──────────┘ └──────────┘
```

---

## 1. Prérequis

- **Docker** & **Docker Compose** (dev local)
- **OpenSSL** (génération des clés JWT)
- **GitHub** compte + dépôt
- **Render** compte (https://render.com)

---

## 2. Déploiement local (Développement)

```bash
# 1. Cloner le dépôt
git clone <url-du-depot>
cd discipolat

# 2. Générer les clés JWT (une seule fois)
chmod +x setup-keys.sh && ./setup-keys.sh

# 3. Démarrer avec Docker Compose
docker compose up -d

# 4. Vérifier l'état
docker compose ps
docker compose logs -f
```

### Accès locaux

| Service | URL |
|---------|-----|
| **Application frontend** | http://localhost:3000 |
| **API Backend** | http://localhost:8081 |
| **Swagger UI** | http://localhost:8081/swagger-ui.html |
| **OpenAPI JSON** | http://localhost:8081/api-docs |
| **Actuator Health** | http://localhost:8081/actuator/health |
| **PostgreSQL** | localhost:5433 (user: `discipolat`, db: `discipolat`) |

### Services Docker Compose

```
discipolat-net (bridge)
├── db:5432         → PostgreSQL 16
├── redis:6379      → Redis 7 (rate limiting distribué)
├── api:8080        → Spring Boot API
├── mailhog:1025    → SMTP de développement
├── web:80          → React SPA (Nginx)
└── nginx:80        → Reverse proxy (entrée)
```

### Comptes de démonstration (seed data)

| Rôle | Email | Mot de passe |
|------|-------|-------------|
| **Admin** (ADMIN + PASTEUR) | admin@discipolat.com | password123 |
| **Pasteur** | pasteur@discipolat.com | password123 |
| **Responsable** (RESPONSABLE + FAISEUR) | responsable@discipolat.com | password123 |
| **Chef de famille** (FAISEUR + CHEF_DE_FAMILLE) | chef@discipolat.com | password123 |
| **Faiseur** | faiseur@discipolat.com | password123 |
| **Membre** | membre@discipolat.com | password123 |
| **Multi-rôles** (RESPONSABLE + CHEF_DE_FAMILLE + FAISEUR) | paul@discipolat.com | password123 |

---

## 3. Redis — Rate limiting distribué

L'application utilise **Redis 7** comme backend distribué pour le rate limiting (Bucket4j ProxyManager).

### Configuration

```yaml
app:
  rate-limiting:
    redis-url: ${REDIS_URL:redis://localhost:6379}    # URL de connexion Redis
    redis-key-expire-minutes: 10                        # Expiration des clés inactives
```

### Clés Redis

Les buckets sont stockés avec le pattern : `rl:{endpoint}:{ip}`

```
rl:login:192.168.1.1
rl:refresh:10.0.0.1
rl:forgot-password:172.16.0.1
...
```

Chaque clé expire automatiquement après 10 minutes d'inactivité (politique `allkeys-lru`).

---

## 4. Déploiement Render (Production)

### Option A — Render Blueprint (Auto) ⭐ Recommandé

Render Blueprint utilise `render.yaml` à la racine du dépôt pour créer automatiquement tous les services.

#### 1. Préparer les clés JWT

```bash
# Générer les clés et les encoder en base64
openssl genpkey -algorithm RSA -out keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl pkey -in keys/private.pem -pubout -out keys/public.pem

# Encoder pour Render
cat keys/private.pem | base64 -w0
cat keys/public.pem | base64 -w0
```

#### 2. Connecter le dépôt à Render

1. Aller sur https://dashboard.render.com/
2. Cliquer **"New +" → "Blueprint"**
3. Connecter votre dépôt GitHub `suner-dev/discipolat_app`
4. Render détecte automatiquement `render.yaml`
5. Remplir les **secrets** suivants dans le Dashboard Render :
   - `JWT_PRIVATE_KEY` → clé privée base64
   - `JWT_PUBLIC_KEY` → clé publique base64
   - `INTERNAL_API_KEY` → une clé API forte (ex: `openssl rand -hex 32`)

#### 3. Services créés automatiquement

| Service | Type | Plan | Description |
|---------|------|------|-------------|
| `discipolat-db` | PostgreSQL | Free | Base de données (1 GB) |
| `discipolat-redis` | Redis | Free | Cache rate limiting (25 MB) |
| `discipolat-api` | Web Service (Docker) | Free | API Spring Boot |
| `discipolat-web` | Web Service (Docker) | Free | Frontend React + Nginx |
| `discipolat-cron-absence` | Cron Job | Free | Vérification absences /6h |
| `discipolat-cron-reminder` | Cron Job | Free | Rappel rapports samedi 18h |

> **Note :** Le service Redis est automatiquement provisionné par Render Blueprint.
> La variable `REDIS_URL` est automatiquement injectée dans l'API via le bloc `fromDatabase`.

#### 4. URLs finales

```
Frontend : https://discipolat.onrender.com
API      : https://discipolat-api.onrender.com
Swagger  : https://discipolat-api.onrender.com/swagger-ui.html
```

---

### Option B — Configuration manuelle

Si vous préférez configurer chaque service un par un :

#### 1. PostgreSQL

1. **New + → PostgreSQL**
   - Name: `discipolat-db`
   - Database: `discipolat`
   - User: `discipolat`
   - Region: `Frankfurt (EU)`
   - Plan: **Free**
2. Noter l'**Internal Connection String** → servira pour l'API

#### 2. Backend API (Docker)

1. **New + → Web Service**
   - Source: **Deploy from GitHub** → sélectionner le dépôt
   - Name: `discipolat-api`
   - Runtime: **Docker**
   - Branch: `main`
   - Plan: **Free**
   - Region: `Frankfurt (EU)`
   - Root Directory: `backend`
   - Dockerfile Path: `backend/Dockerfile`
   - Health Check Path: `/actuator/health`

2. **Variables d'environnement obligatoires** :

| Variable | Valeur | Secret |
|----------|--------|--------|
| `SPRING_PROFILES_ACTIVE` | `prod` | ❌ |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<db-internal-url>/discipolat` | ❌ |
| `SPRING_DATASOURCE_USERNAME` | `discipolat` | ❌ |
| `SPRING_DATASOURCE_PASSWORD` | (mot de passe de la DB) | ✅ |
| `JWT_PRIVATE_KEY` | (clé privée RSA en base64) | ✅ |
| `JWT_PUBLIC_KEY` | (clé publique RSA en base64) | ✅ |
| `FRONTEND_URL` | `https://discipolat.onrender.com` | ❌ |
| `FRONTEND_URL_BASE` | `https://discipolat.onrender.com` | ❌ |
| `SERVER_PORT` | `10000` | ❌ |

#### 3. Frontend (Docker)

1. **New + → Web Service**
   - Name: `discipolat-web`
   - Runtime: **Docker**
   - Branch: `main`
   - Plan: **Free**
   - Region: `Frankfurt (EU)`
   - Root Directory: `frontend`
   - Dockerfile Path: `frontend/Dockerfile`

2. **Variables d'environnement** :

| Variable | Valeur | Secret |
|----------|--------|--------|
| `VITE_API_URL` | `https://discipolat-api.onrender.com` | ❌ |

#### 4. Cron Jobs

##### Vérification des absences (toutes les 6h)
1. **New + → Cron Job**
   - Name: `discipolat-cron-absence`
   - Source: **Deploy from GitHub**
   - Branch: `main`
   - Command: `curl -s --max-time 30 -X POST https://discipolat-api.onrender.com/api/v1/internal/check-absences -H 'Authorization: Bearer $INTERNAL_API_KEY'`
   - Schedule: `0 */6 * * *`
   - Variable secrète : `INTERNAL_API_KEY`

##### Rappel des rapports (samedi 18h)
1. **New + → Cron Job**
   - Name: `discipolat-cron-reminder`
   - Command: `curl -s --max-time 30 -X POST https://discipolat-api.onrender.com/api/v1/internal/send-reminders -H 'Authorization: Bearer $INTERNAL_API_KEY'`
   - Schedule: `0 18 * * SAT`
   - Variable secrète : `INTERNAL_API_KEY`

---

## 4. Pipeline CI/CD (GitHub Actions)

Le pipeline est défini dans `.github/workflows/ci.yml`.

### Workflow

```yaml
on: push → branches: [main, develop]
```

| Job | Déclencheur | Actions |
|-----|-------------|---------|
| **Backend** | Tout push | `mvn verify` avec PostgreSQL de test |
| **Frontend** | Tout push | `npm ci → tsc → vitest → build` |
| **Docker** | Push sur `main` | Build & Push images vers **GHCR** |
| **Deploy** | Push sur `main` | Trigger déploiement Render via API |

### Secrets GitHub requis

Pour que le déploiement automatique fonctionne, ajouter ces **GitHub Secrets** :

| Secret | Valeur | Où l'obtenir |
|--------|--------|-------------|
| `RENDER_API_KEY` | Clé API Render | Dashboard Render → Account Settings → API Keys |
| `RENDER_API_SERVICE_ID` | ID du service API | Dashboard Render → discipolat-api → Settings → Service ID |
| `RENDER_WEB_SERVICE_ID` | ID du service Web | Dashboard Render → discipolat-web → Settings → Service ID |

### Obtention des Service IDs

```bash
# Lister tous les services Render avec leurs IDs
curl -s -H "Authorization: Bearer $RENDER_API_KEY" \
  https://api.render.com/v1/services | \
  jq '.[] | {name: .service.name, id: .service.id}'
```

---

## 5. Déploiement AWS ECS (montée en charge)

Pour le passage à l'échelle (V2+), voici la procédure AWS :

```bash
# 1. Authentifier Docker à ECR
aws ecr get-login-password --region eu-central-1 | \
  docker login --username AWS --password-stdin \
  <account>.dkr.ecr.eu-central-1.amazonaws.com

# 2. Tagguer et pousser les images
docker tag discipolat-api:latest \
  <account>.dkr.ecr.eu-central-1.amazonaws.com/discipolat-api:latest
docker push <account>.dkr.ecr.eu-central-1.amazonaws.com/discipolat-api:latest

docker tag discipolat-web:latest \
  <account>.dkr.ecr.eu-central-1.amazonaws.com/discipolat-web:latest
docker push <account>.dkr.ecr.eu-central-1.amazonaws.com/discipolat-web:latest

# 3. Déployer via ECS Fargate
aws ecs update-service --cluster discipolat-cluster \
  --service discipolat-api --force-new-deployment
aws ecs update-service --cluster discipolat-cluster \
  --service discipolat-web --force-new-deployment
```

---

## 6. Variables d'environnement — Référence complète

| Variable | Description | Local (docker-compose) | Render |
|----------|-------------|----------------------|--------|
| `SPRING_DATASOURCE_URL` | URL JDBC PostgreSQL | `jdbc:postgresql://db:5432/discipolat` | Généré automatiquement |
| `SPRING_DATASOURCE_USERNAME` | Utilisateur DB | `discipolat` | Généré automatiquement |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe DB | `discipolat_secret` | Généré automatiquement |
| `JWT_PRIVATE_KEY` | Clé privée RSA (base64) | Générée par `setup-keys.sh` | **Secret** — à fournir |
| `JWT_PUBLIC_KEY` | Clé publique RSA (base64) | Générée par `setup-keys.sh` | **Secret** — à fournir |
| `JWT_PRIVATE_KEY_PATH` | Chemin fichier clé privée | `keys/private.pem` | Non utilisé (base64) |
| `JWT_PUBLIC_KEY_PATH` | Chemin fichier clé publique | `keys/public.pem` | Non utilisé (base64) |
| `FRONTEND_URL` | URLs autorisées CORS | `http://localhost:3000,http://localhost:5173` | `https://discipolat.onrender.com` |
| `SPRING_PROFILES_ACTIVE` | Profil Spring | `docker` | `prod` |
| `VITE_API_URL` | URL API pour le frontend | `/api` (proxy Nginx) | `https://discipolat-api.onrender.com` |
| `SERVER_PORT` | Port interne | 8080 | 10000 (défaut Render) |
| `MAIL_HOST` | Serveur SMTP | `mailhog` | À configurer |
| `MAIL_PORT` | Port SMTP | `1025` | À configurer |
| `MAIL_USERNAME` | Utilisateur SMTP | — | À configurer |
| `MAIL_PASSWORD` | Mot de passe SMTP | — | **Secret** — à configurer |

---

## 7. Sauvegarde et Restauration

### Backup PostgreSQL

```bash
# Backup manuel
docker exec discipolat-db pg_dump -U discipolat discipolat > backup_$(date +%Y%m%d).sql

# Backup via Render (automatique)
# Render Free plan : backups automatiques quotidiens, rétention 7 jours
```

### Restauration

```bash
# Restauration locale
cat backup.sql | docker exec -i discipolat-db psql -U discipolat discipolat

# Restauration Render
# Dashboard Render → discipolat-db → Backups → Restore
```

---

## 8. Monitoring et santé

### Endpoints de health check

```
GET /actuator/health        → {"status":"UP"}          (public, requis par Render)
GET /actuator/health/liveness|readiness → probes        (public)
GET /actuator/info          → Version, build info       (ADMIN/PASTEUR)
GET /actuator/metrics       → Métriques JVM/DB          (ADMIN/PASTEUR)
GET /actuator/loggers       → Niveaux de log dynamiques (ADMIN/PASTEUR)
```

### Logs en production

```bash
# Render Dashboard
# → discipolat-api → Logs

# Ou via API Render
curl -s -H "Authorization: Bearer $RENDER_API_KEY" \
  "https://api.render.com/v1/services/$SERVICE_ID/logs?limit=50"
```

---

## 9. Dépannage

| Problème | Cause probable | Solution |
|----------|---------------|----------|
| `401 Unauthorized` sur API | Token JWT expiré ou invalide | Vérifier les clés JWT dans les secrets Render |
| `Connection refused` à la DB | DB pas encore prête ou URL erronée | Vérifier l'Internal Connection String |
| Build Docker échoue | Cache périmé ou dépendances manquantes | Ajouter `"clearCache": "clear"` au trigger de déploiement |
| CORS bloque les requêtes | `FRONTEND_URL` incorrect | Vérifier l'URL exacte du frontend Render |
| Cron jobs ne s'exécutent pas | `INTERNAL_API_KEY` invalide | Vérifier le secret + cohérence API key |
| Page blanche (frontend) | Build non trouvé ou `VITE_API_URL` incorrect | Vérifier les logs Nginx dans le Dashboard |
| Flyway migration échoue | Schéma DB incompatible | Supprimer la table `flyway_schema_history` et relancer (⚠️ données perdues) |
