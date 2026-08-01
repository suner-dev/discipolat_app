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
| `discipolat-db` | PostgreSQL | Free | Base de données (1 GB) — ⚠️ **expire 30 j après création** |
| `discipolat-redis` | Redis | Free | Cache rate limiting (25 MB, en mémoire) |
| `discipolat-api` | Web Service (Docker) | Free | API Spring Boot |
| `discipolat-web` | **Static Site** (CDN) | Free | Frontend React — jamais endormi, 0 h d'instance |
| `discipolat-cron-absence` | Cron Job | Free | Vérification absences /6h |
| `discipolat-cron-reminder` | Cron Job | Free | Rappel rapports samedi 18h |

> **Note :** Le service Redis est automatiquement provisionné par Render Blueprint.
> La variable `REDIS_URL` est automatiquement injectée dans l'API via le bloc `fromDatabase`.

#### 4. URLs finales

```
Frontend : https://discipolat-web.onrender.com   (Static Site — vérifier l'URL réelle après déploiement)
API      : https://discipolat-api.onrender.com
Swagger  : https://discipolat-api.onrender.com/swagger-ui.html
```

> **⚠️ Après migration du frontend (web service → static site) :** vérifier l'URL réelle du
> static site dans le Dashboard Render (`discipolat-web → Settings`) puis mettre à jour
> `FRONTEND_URL` / `FRONTEND_URL_BASE` sur l'API si elle diffère (ex: domaine personnalisé).
> Une URL CORS incorrecte bloque la connexion (login 403).

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

#### 3. Frontend (Static Site)

1. **New + → Static Site**
   - Name: `discipolat-web`
   - Branch: `main`
   - Root Directory: `frontend`
   - Build Command: `npm ci && npm run build`
   - Publish Directory: `dist`

2. **Variables d'environnement (build)** :

| Variable | Valeur | Secret |
|----------|--------|--------|
| `VITE_API_URL` | `https://discipolat-api.onrender.com` | ❌ |

3. **En-têtes de sécurité + fallback SPA** : configurés dans `render.yaml`
   (`headers` et `routes` → `rewrite /* → /index.html`).

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
| **Docker** | Push sur `main` | Build & Push image **backend** vers **GHCR** |
| **Deploy** | Push sur `main` | Trigger déploiement **API** via API Render (le frontend static site se redéploie automatiquement à chaque push) |
| **Keep-alive** | Toutes les 10 min | Ping de l'API pour éviter le cold start (voir section 9) |

### Secrets GitHub requis

Pour que le déploiement automatique fonctionne, ajouter ces **GitHub Secrets** :

| Secret | Valeur | Où l'obtenir |
|--------|--------|-------------|
| `RENDER_API_KEY` | Clé API Render | Dashboard Render → Account Settings → API Keys |
| `RENDER_API_SERVICE_ID` | ID du service API | Dashboard Render → discipolat-api → Settings → Service ID |

> **Note :** `RENDER_WEB_SERVICE_ID` n'est plus nécessaire — le frontend est un
> Static Site qui se redéploie automatiquement à chaque push sur `main`.

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

## 8.5. Migration du frontend Docker → Static Site

> `runtime` étant **immuable après création** (Blueprint Render), la synchro du Blueprint ne
> convertit **pas en place** l'ancien service web `discipolat-web` (runtime: image) en static site.
>
> **Procédure :**
> 1. Dashboard Render → supprimer l'ancien service web `discipolat-web` (ou le renommer).
>    ⚠️ Si votre service actuel s'appelle **`discipolat`** (URL `discipolat.onrender.com`),
>    recréez le static site **sous ce même nom** pour conserver l'URL, ou ajustez
>    `FRONTEND_URL` en conséquence.
> 2. Re-synchroniser le Blueprint (`Dashboard → Blueprints → Sync`) → Render crée le **Static Site** `discipolat-web` depuis `render.yaml`.
> 3. Vérifier l'URL réelle du static site dans le Dashboard. Pendant la migration,
>    l'API accepte déjà les deux origines CORS (`FRONTEND_URL` contient
>    `discipolat.onrender.com` **et** `discipolat-web.onrender.com`) → aucun blocage login.
>    Retirer l'ancienne origine une fois la migration terminée (voir section 4).

---

## 9. Performance — Éviter le cold start Render

### Problème

Les Web Services du plan **Free** de Render s'endorment après **15 minutes sans trafic**.
Au premier chargement suivant, Render doit réveiller l'instance (boot JVM) :
**~1 minute d'attente** avant que l'application ne réponde.

### Architecture anti-cold-start (2 piliers)

#### 1. Frontend = Static Site (CDN mondial)

Le frontend React est un **Static Site** Render (`runtime: static` dans `render.yaml`) :
- servi par le **CDN mondial** de Render → chargement **instantané** et rapide partout ;
- **ne s'endort jamais** et ne consomme **aucune heure d'instance** ;
- se redéploie automatiquement à chaque push sur `main`.

→ C'est le pilier principal : l'ouverture de l'appli est instantanée, toujours.

#### 2. API = maintenue éveillée par GitHub Actions

Le workflow `.github/workflows/keep-alive.yml` ping l'API **toutes les 10 minutes** :

| Cible | URL pingée |
|-------|-----------|
| API | `https://discipolat-api.onrender.com/actuator/health` |

- Intervalle **10 min < 15 min** (seuil de sommeil) → l'API reste **éveillée**.
- **Coût : zéro** — le dépôt est public, donc les minutes GitHub Actions sont illimitées.
- Un ping en échec (après 3 tentatives) marque le run comme **failed** dans l'onglet Actions →
  sert d'alerte gratuite si l'API tombe.
- ⚠️ Les crons GitHub Actions peuvent être retardés de quelques minutes : un écart > 15 min
  est rare mais possible → cold start occasionnel de l'API. Acceptable (démarrage ~1 min).

> **⚠️ Quota Render (plan free) : 750 h/mois PAR WORKSPACE** (pas par service).
> Une instance éveillée 24/7 ≈ 720-744 h/mois. On ne garde donc éveillée **QUE l'API**
> (~720 h ≤ 750 h) : c'est la seule configuration qui tient dans le quota. Garder
> 2 services éveillés (~1464 h) aurait **suspendu toute l'appli à mi-mois**.

#### 3. ⚠️ Limites du plan Free à connaître

- **PostgreSQL Free : expire 30 jours après sa création** → après expiration, 14 j de
  grâce pour passer en payant, sinon **suppression définitive des données**.
  → Pour une appli durable, prévoir un plan payant sur la DB.
- **Redis Free : en mémoire uniquement** → données perdues à chaque redémarrage
  (acceptable : buckets de rate limiting, se reconstruisent seuls).
- **Quota : 750 h/mois/workspace** → si dépassé, Render suspend TOUS les services gratuits
  jusqu'au 1er du mois suivant.
- **Cron jobs : le plan `free` n'existe pas pour les cron jobs** (Blueprint Render) → ils sont
  créés par défaut en **Starter** (~7 $/mois chacun, soit ~14 $/mois pour les 2 crons actuels).
  Point de budget préexistant à connaître.

### Alternative payante (décision produit)

Si l'application monte en charge, passer en plan **Starter** (~7 $/mois/service) :
plus de spin-down, plus de quota, meilleures performances. C'est un choix métier/budget.

---

## 10. Dépannage

| Problème | Cause probable | Solution |
|----------|---------------|----------|
| `401 Unauthorized` sur API | Token JWT expiré ou invalide | Vérifier les clés JWT dans les secrets Render |
| `Connection refused` à la DB | DB pas encore prête ou URL erronée | Vérifier l'Internal Connection String |
| Build Docker échoue | Cache périmé ou dépendances manquantes | Ajouter `"clearCache": "clear"` au trigger de déploiement |
| CORS bloque les requêtes | `FRONTEND_URL` incorrect | Vérifier l'URL exacte du frontend Render |
| Cron jobs ne s'exécutent pas | `INTERNAL_API_KEY` invalide | Vérifier le secret + cohérence API key |
| Page blanche (frontend) | Build non trouvé ou `VITE_API_URL` incorrect | Vérifier les logs Nginx dans le Dashboard |
| Flyway migration échoue | Schéma DB incompatible | Supprimer la table `flyway_schema_history` et relancer (⚠️ données perdues) |
