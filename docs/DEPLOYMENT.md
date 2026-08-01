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

#### 3. Services créés automatiquement

| Service | Type | Plan | Description |
|---------|------|------|-------------|
| `discipolat-db` | PostgreSQL | Free* | Base de données (1 GB) — ⚠️ **expire 30 j après création** — voir §8.6 pour la migration payante (*plan non figé dans `render.yaml`) |
| `discipolat-redis` | Redis | Free | Cache rate limiting (25 MB, en mémoire) |
| `discipolat-api` | Web Service (Docker) | Free | API Spring Boot |
| `discipolat-web` | **Static Site** (CDN) | Free | Frontend React — jamais endormi, 0 h d'instance |

> **Note :** plus de cron jobs Render depuis 2.1.3 — le plan `free` n'existe pas pour
> les crons (~14 $/mois). Les tâches (absences 6h, rappels samedi 18h) sont exécutées
> par le **scheduler interne Spring** (`ScheduledJobs.java`), fiable grâce au keep-alive
> qui maintient l'API éveillée 24/7.

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

#### 4. Tâches planifiées (aucun cron job Render requis)

Les tâches périodiques (vérification des absences toutes les 6h, rappels de rapports
le samedi à 18h) sont exécutées **en interne par le scheduler Spring**
(`@EnableScheduling` + `ScheduledJobs.java`) : aucun cron job Render payant nécessaire.

> Les anciens crons Render (`discipolat-cron-*`, ~14 $/mois) ont été **supprimés en
> 2.1.3** : ils appelaient des endpoints `/api/v1/internal/*` qui n'existaient pas
> dans le code (404) — ils ne faisaient rien. Le scheduler interne couvre tout,
> et le keep-alive (section 9) garantit que l'API reste éveillée pour l'exécuter.

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
# Backup manuel (base locale docker-compose)
docker exec discipolat-db pg_dump -U discipolat discipolat > backup_$(date +%Y%m%d).sql
```

> ⚠️ **Base Render en plan Free : AUCUN backup automatique.** Les bases gratuites
> ne supportent aucun backup (ni automatique ni manuel côté Render). Le seul moyen
> de sauvegarder une base Free est un export `pg_dump` **externe** depuis votre
> machine (voir section 8.6). Les backups quotidiens automatiques n'existent que
> sur les plans **payants** (rétention selon le plan).

### Restauration

```bash
# Restauration locale
cat backup.sql | docker exec -i discipolat-db psql -U discipolat discipolat

# Restauration Render (plans payants uniquement)
# Dashboard Render → discipolat-db → Backups → Restore

# Restauration d'un export pg_dump dans une base Render
# (voir section 8.6, étape 4)
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

## 8.6. Migration de la base PostgreSQL Free → payante (expiration 30 jours)

> **⚠️ Rappel critique :** une base Render **Free** expire **30 jours après sa création**.
> Passé ce délai : 14 jours de grâce pour passer en payant, sinon **suppression
> définitive des données** (et la base est inaccessible pendant la grâce tant
> qu'elle n'est pas passée en payant). Les bases Free n'ont **aucun backup**.

### Étape 0 — Vérifier la date d'expiration

1. **Dashboard Render** → onglet **Databases** → ouvrir `discipolat-db` (page Info).
2. La page Info affiche l'âge de la base ; Render envoie aussi des **emails de
   notification** à l'approche de l'expiration et du début de la grâce.
3. Si la base est déjà expirée (bannière visible), passer directement à l'Étape 2 :
   il reste exactement **14 jours** pour l'upgrade.

### Option A — Upgrade en place vers un plan payant (recommandé)

**Avantages :** aucune perte de données, **l'URL de connexion ne change PAS**
(les identifiants et la config sont conservés) → l'API continue de fonctionner
sans rien modifier.

**Inconvénients :** la base est **indisponible quelques minutes** pendant le
changement ; **impossible de revenir en Free ensuite** ; coût mensuel.

**Procédure exacte dans le Dashboard :**

1. **Dashboard Render** (https://dashboard.render.com) → onglet **Databases**.
2. Cliquer sur **`discipolat-db`** pour ouvrir sa page **Info**.
3. Descendre jusqu'à la section **PostgreSQL Instance**.
4. Cliquer **Update**.
5. Dans **Plan Options**, sélectionner le nouveau type d'instance **payant** :
   - **Starter** (~7 $/mois) — suffisant pour cette application (départ : 0.25 CPU,
     256 MB RAM, 1 GB disque, backups) ;
   - **Basic / Standard / Pro** si besoin de plus de ressources
     (voir https://render.com/pricing).
6. Cliquer **Save Changes**.
7. Attendre la fin du changement (quelques minutes d'indisponibilité).

> ✅ **URL/credentials inchangés** : `SPRING_DATASOURCE_URL`, `USERNAME`,
> `PASSWORD` sont injectés par le Blueprint (`fromDatabase` → `connectionString`)
> et restent identiques. Seule l'instance est redimensionnée.
>
> 📝 **Pensez à `render.yaml`** : le champ `plan` de `discipolat-db` a été **omis**
> pour éviter tout conflit de re-sync Blueprint (une base payante ne peut pas
> repasser en `free`). Sur une base existante, Render conserve le type d'instance
> actuel → aucun coût forcé, aucun échec de synchro.

### Option B — Export pg_dump (filet de sécurité, sans payer)

À faire **AVANT le 30ᵉ jour**. Utile comme filet de sécurité même si vous
choisissez l'Option A.

1. **Dashboard Render** → **Databases** → `discipolat-db` → section **Connect**.
2. Copier la chaîne **External Database URL** (⚠️ PAS l'Internal URL : celle-ci
   n'est accessible que depuis le réseau privé Render, pas depuis votre machine).
3. Depuis votre machine (avec `pg_dump` installé, PostgreSQL 16) :

```bash
pg_dump "postgresql://<user>:<password>@<host>:5432/discipolat" \
  --no-owner --no-privileges > discipolat_backup_$(date +%Y%m%d).sql
```

4. Vérifier le fichier : `head -5 discipolat_backup_$(date +%Y%m%d).sql`
   (doit commencer par `-- PostgreSQL database dump`).
5. **Stocker le fichier hors de Render** (GitHub private, machine, NAS…).

> 💡 Automatisation possible : un workflow GitHub Actions mensuel qui dump la base
> via `pg_dump` (avec l'External URL en secret) et pousse le fichier en artifact.

### Étape 2 — Restaurer dans une nouvelle base (seulement si Option B)

1. **Dashboard Render** → **New + → PostgreSQL** → créer `discipolat-db-v2`
   (plan payant recommandé, même région `Frankfurt`).
2. Copier sa **External Database URL**.
3. Restaurer le dump **dans une base vide** (⚠️ jamais sur une base contenant déjà
   des données) :

```bash
psql "postgresql://<user>:<password>@<host>:5432/discipolat" < discipolat_backup_YYYYMMDD.sql
```

4. **Brancher l'API sur la nouvelle base** :
   - **Dashboard Render** → `discipolat-api` → **Environment** → mettre à jour
     `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` avec les valeurs de
     `discipolat-db-v2` → **Save** → **Deploy**.
   - **OU** modifier `render.yaml` (`fromDatabase` → `discipolat-db-v2`) puis
     re-synchroniser le Blueprint.
5. Vérifier : `GET https://discipolat-api.onrender.com/actuator/health` → `"status":"UP"`
   et un login fonctionnel.

> ⚠️ **Flyway** : le schéma restauré doit correspondre aux migrations existantes.
> `baseline-on-migrate: true` est déjà configuré → pas d'intervention attendue.

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
| Tâches planifiées (absences, rappels) non exécutées | Scheduler Spring inactif | Vérifier les logs API (`ScheduledJobs`) + que le keep-alive tourne (API éveillée) |
| Page blanche (frontend) | Build non trouvé ou `VITE_API_URL` incorrect | Vérifier les logs Nginx dans le Dashboard |
| Flyway migration échoue | Schéma DB incompatible | Supprimer la table `flyway_schema_history` et relancer (⚠️ données perdues) |
