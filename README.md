# Discipolat — Church OS

**Plateforme multi-tenant de gestion d'église** : un produit, trois clients
(API REST, web, mobile) et une isolation stricte — chaque église (tenant) ne
voit JAMAIS les données d'une autre (prouvé bout-en-bout par
`TenantModuleIsolationEndToEndHttpTest`, bloquant en CI).

- 🌍 **6 langues** : FR (référence), EN, PT, ES, SW, **AR (RTL)** — web
  (`frontend/src/i18n/`) et mobile (`mobile/lib/l10n/intl_*.arb`).
- 🏛️ **Rôles** : ADMIN, PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE, FAISEUR,
  MEMBRE (+ Super Admin plateforme, RBAC détaillé : [docs/RBAC.md](docs/RBAC.md)).
- 📚 Documentation : [docs/](docs/) · état factuel : [STATUS.md](STATUS.md) ·
  guide utilisateur : [docs/GUIDE_UTILISATEUR.md](docs/GUIDE_UTILISATEUR.md).

## Architecture

```
  Navigateur (React 19 + PWA)        Mobile (Flutter 3, Riverpod/Drift)
            │                                        │
            ▼                                        ▼
   Render Static Site (CDN)      ┌────────────────────────────────────┐
   ou Nginx (docker-compose)     │        API Spring Boot 3.4.7       │
            └────────────────────►   JWT RSA + filtre tenant (Hibernate│
                                   @Filter + TenantAware repository)   │
                                   SMTP mail / webhooks opérateurs ◄────┤
                                   scheduler interne (ScheduledJobs)   │
                                   └───────┬───────────────┬──────────┘
                                           ▼               ▼
                                   PostgreSQL 16        Redis 7
                                   (Flyway ; isolation : schéma partagé +   (Bucket4j : rate
                                    tenant_id ; scaling → docs/SCALING.md)   limiting, caches)

  CI/CD GitHub Actions : ci.yml (bloquant : mvn verify, tsc+vitest, flutter)
  · security.yml (gitleaks, dependency-review, npm audit, bandit)
  · e2e.yml (Playwright, e2/) · perf.yml (k6, porte P95 < 2 s)
  · keep-alive.yml (API éveillée 24/7 sur plan gratuit)
```

### Stack technique

| Couche | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 3.4.7, Spring Security (JWT RSA), Spring Data JPA/Hibernate 6, Flyway (159 migrations), springdoc (`/api-docs`), JUnit 5 + Mockito (~1 660 tests) |
| Frontend | React 19, Vite, TypeScript strict, TailwindCSS, Vitest, PWA (manifest + service worker) |
| Mobile | Flutter 3.35+, Riverpod, Drift (base locale), GoRouter, notifications FCM |
| Data & infra | PostgreSQL 16, Redis 7, Docker Compose, Render (prod/bêta), GitHub Actions, GHCR |

## Prérequis

| Outil | Version | Vérifié par |
|---|---|---|
| Java | **21** (Temurin) | `backend/pom.xml` → `java.version` |
| Node.js | **22 LTS** (22.15.1 en build Render) | `render.yaml` → `NODE_VERSION` |
| Flutter | **≥ 3.35.6** | `mobile/pubspec.yaml` → `flutter:` |
| Docker | 24+ (Compose v2) | `docker-compose.yml` |

## Démarrage local

### Voie 1 — tout Docker (recommandée)

```bash
cp .env.example .env          # puis définir au minimum ENCRYPTION_AES_KEY
                              # (32 octets en base64 : openssl rand -base64 32)
bash setup-keys.sh            # clés JWT RSA dans keys/ + .env alimenté
docker compose up -d --build  # db, redis, api, mailhog, web, nginx
```

| Service | URL locale |
|---|---|
| Web (Nginx) | http://localhost:3000/ |
| API | http://localhost:8081/ (interne 8080) — Swagger : `/swagger-ui.html` |
| MailHog (emails fictifs) | http://localhost:8026/ |
| PostgreSQL | localhost:**5433** (`discipolat`/`discipolat_secret`) |
| Redis | localhost:6379 |
| Pont public optionnel | `docker logs discipolat-cloudflared` (trycloudflare) |
| Monitoring optionnel | `docker compose --profile monitoring up -d` → Grafana :3001, Prometheus :9090 |

### Voie 2 — backend natif + frontend Vite

```bash
docker compose up -d db redis
cd backend && mvn -B package -DskipTests && cd ..
bash start-local.sh           # backend :8080 (profil beta, seeds démo) + Vite :5173
```

## Comptes de démonstration (dév/bêta uniquement)

Semés par `DataInitializer` quand `app.beta-testing.seed-demo-accounts=true`
(profils `dev`/`beta`/`docker` locaux — **jamais en production**, garde
explicite dans le code). Mot de passe commun : `password123`.

| Compte | Rôles réels (semés) |
|---|---|
| `admin@discipolat.com` | ADMIN + PASTEUR (active : ADMIN) |
| `pasteur@discipolat.com` | PASTEUR |
| `responsable@discipolat.com` | RESPONSABLE + FAISEUR |
| `chef@discipolat.com` | FAISEUR + CHEF_DE_FAMILLE |
| `faiseur@discipolat.com` | FAISEUR |
| `membre@discipolat.com` | MEMBRE |
| `paul@discipolat.com` | multi-rôles : RESPONSABLE + CHEF_DE_FAMILLE + FAISEUR |

D'autres identités de scénario existent dans les seeds Flyway
(`V2__seed_data.sql`) ; la liste ci-dessus est celle des comptes de test
exposés par `start-local.sh`.

## Variables d'environnement

Référence complète et commentée : [.env.example](.env.example) et
[docs/ENV_TEMPLATE.md](docs/ENV_TEMPLATE.md). Le strict minimum pour démarrer :

| Variable | Rôle | Obligatoire ? |
|---|---|---|
| `POSTGRES_DB/USER/PASSWORD` + `SPRING_DATASOURCE_*` | connexion base | ✅ |
| `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` (base64) ou `*_PATH` | signature des jetons | ✅ |
| `ENCRYPTION_AES_KEY` | chiffrement au repos AES-256-GCM (`CryptoService`, 32 octets base64) | ✅ |
| `REDIS_URL` | rate limiting distribué (dégrade proprement sinon) | recommandé |
| `FRONTEND_URL` / `FRONTEND_URL_BASE` | allowlist CORS / liens email | ✅ en prod |
| `MAIL_HOST/PORT/USERNAME/PASSWORD` | SMTP (Render free : port 2525) | optionnel (emails désactivés sinon) |
| `MOBILEMONEY_ENABLED`, `MTN_*`, `ORANGE_*`, `MPESA_*`… | passerelles opérateurs réelles | optionnel (503 honnête sinon) |
| `PAYMENTS_WEBHOOK_SECRET` | signature webhooks opérateurs | si paiements actifs |

## Architecture mondiale

Multi-tenant partagé, sharding/routing, partitionnement, devises ISO-4217 et
neutralité géographique : **[docs/SCALING.md](docs/SCALING.md)** et
[docs/MULTI_TENANT_ARCHITECTURE.md](docs/MULTI_TENANT_ARCHITECTURE.md).

## Documentation

| Document | Contenu |
|---|---|
| [docs/API.md](docs/API.md) | référence des endpoints (générée : `bash scripts/generate-api-docs.sh`) |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | déploiement Render/Docker, secrets, rotation JWT, rollback |
| [docs/RUNBOOK.md](docs/RUNBOOK.md) | exploitation : incidents symptôme → diagnostic → résolution |
| [docs/GUIDE_UTILISATEUR.md](docs/GUIDE_UTILISATEUR.md) | prise en main par rôle |
| [docs/SECURITY.md](docs/SECURITY.md) · [docs/RBAC.md](docs/RBAC.md) | modèle de sécurité et autorisations |
| [e2/README.md](e2/README.md) | suite E2E Playwright (s'active à la première spec) |

## Contribuer

Branche `main` protégée par la CI bloquante ; conventions et périmètres
d'agents : [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) ·
[AGENT_ORCHESTRATION.md](AGENT_ORCHESTRATION.md).
