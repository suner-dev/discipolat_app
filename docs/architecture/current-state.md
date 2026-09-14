# ARCHITECTURE ACTUELLE — Discipolat (État des lieux G0.1)

> Inventaire complet du code existant avant toute modification Church OS v1.
> Date : 2026-09-14

---

## 1. BACKEND — Spring Boot 3.x / Java 21 / PostgreSQL / Flyway

### 1.1 Structure des modules (`backend/src/main/java/com/discipolat/modules/`)

**126 modules** détectés. Principaux domaines :

| Domaine | Modules (exemples) | Nombre |
|---|---|---|
| **Core / Platform** | `tenants`, `users`, `authentication`, `auth`, `platform`, `admin` | 6 |
| **People / Discipleship** | `souls`, `families`, `departments`, `discipleshipPath`, `spiritualJourney`, `mentoring`, `makerTracking`, `cercle` | 12 |
| **Events / Operations** | `events`, `calendar`, `appointments`, `eventChecklist`, `gantt`, `programs`, `streaming` | 9 |
| **Assets / Inventory** | `inventory`, `files` | 2 |
| **Finance / Payments** | `finances`, `payments`, `tontine`, `currency` | 4 |
| **Communication** | `notifications`, `messages`, `broadcast`, `announcements`, `communications`, `groupMessages`, `whatsapp`, `ussd`, `voicenotifications` | 9 |
| **AI / Analytics** | `ai`, `aiPredictions`, `aiVisitNotes`, `intelligence`, `predictions`, `engagementAnalytics`, `executiveInsights`, `growthProjection`, `loadPrediction`, `neighborhoodHealth`, `departmentKpi`, `kpiNarrative`, `usageAnalytics` | 13 |
| **Content / Media** | `sermon`, `bibleReading`, `prayerJournal`, `spiritualJournal`, `documents`, `community` | 6 |
| **Workflows / Automation** | `workflow`, `automations`, `transfers`, `followUpRequests`, `leaveRequests`, `adminRequests` | 6 |
| **Admin / Config** | `customfields`, `forms`, `search`, `imports`, `exports`, `backups`, `dataMigration`, `integrations`, `webhooks`, `compliance`, `gdpr` | 11 |
| **Health / Care** | `health`, `visits`, `encouragements`, `prayers`, `pastoral` (via souls) | 6 |
| **Other** | `badges`, `rewards`, `objectives`, `personalObjectives`, `evaluations`, `surveys`, `testimonials`, `referrals`, `volunteers`, `tickets`, `marketplace`, `skillMatching`, `reverseMentoring`, `succession`, `prophetic`, `compliance`, `faceRec`, `passport`, `demo`, `publicapi` | 26 |

> **Total modules métier : ~126** (dont ~100 modules "feature" créés récemment phases 1-10)

### 1.2 Infrastructure multi-tenant existante

| Composant | Fichier | Description |
|---|---|---|
| **TenantContext** | `common/multitenancy/TenantContext.java` | ThreadLocal tenantId |
| **TenantFilter** | `common/multitenancy/TenantFilter.java` | Hibernate Filter `tenantFilter` |
| **TenantInterceptor** | `common/multitenancy/TenantInterceptor.java` | Extrait tenantId du JWT |
| **CurrentTenantResolver** | `common/multitenancy/CurrentTenantResolver.java` | Résolution contexte authentifié |
| **TenantAwareRepository** | `common/multitenancy/TenantAwareRepository.java` | Interface repo scopée (findByIdAndTenantId) |
| **Default Tenant** | `00000000-0000-0000-0000-000000000001` | Fallback jobs/scheduled |

### 1.3 Entités core tenant-aware (échantillon)

| Entité | Table | tenant_id | Isolation |
|---|---|---|---|
| `User` | `users` | NOT NULL | Hibernate Filter |
| `Soul` | `souls` | NOT NULL | Hibernate Filter |
| `Family` | `families` | NOT NULL | Hibernate Filter |
| `Department` | `departments` | NOT NULL | Hibernate Filter |
| `Event` | `events` | NOT NULL | Hibernate Filter |
| `Tenant` | `tenants` | N/A | Table racine |
| *Tous modules* | *85+ tables* | NOT NULL | Hibernate Filter |

### 1.4 Sécurité & Auth

| Composant | Détails |
|---|---|
| **JWT** | RS256, Access 15min / Refresh 7j, `tenantId` dans claims |
| **Rôles** | `ADMIN`, `PASTEUR`, `RESPONSABLE`, `CHEF_DE_FAMILLE`, `FAISEUR`, `MEMBRE` (6 rôles fixes) |
| **Permissions** | Basiques, pas de scope fin (GLOBAL/TENANT/DEPARTMENT/FAMILY/OWN) |
| **Rate Limiting** | Présent (`RateLimitFilter`) |
| **CORS** | Configuré |

---

## 2. BASE DE DONNÉES — PostgreSQL / Flyway

### 2.1 Migrations Flyway : **136 migrations** (V1 → V136)

| Phase | Migrations | Description |
|---|---|---|
| **Initiales** | V1-V9 | Schéma de base, prayers, events, files, users, roles |
| **Core métier** | V10-V69 | Families, departments, souls, transfers, finances, custom fields, departments mgmt |
| **Multi-tenant** | V70 | **Majeur** : ajout `tenant_id` sur 85+ tables, index composites, Hibernate Filter |
| **Post-MT** | V71-V136 | Modules feature récents (whatsapp, workflow, network, ai, health, etc.) |

**Migrations critiques** :
- `V70__add_multitenancy.sql` (31 KB) — ajout tenant_id partout
- `V135__create_multi_tenant_core_tables.sql` (20 KB) — `tenant_memberships`, `organization_nodes`, `roles`, `permissions`, `saas_plans`, `tenant_features`
- `V136__add_membership_scope_and_role_fk.sql` — scope sur memberships

### 2.2 Indexes multi-tenant (V70)
- `idx_{table}_tenant` sur chaque table
- `idx_{table}_tenant_{col}` pour requêtes fréquentes
- `uk_users_tenant_email` unique (tenant_id, email)

---

## 3. FRONTEND — React 19 / Vite / TypeScript / Tailwind / TanStack Query / Zustand

### 3.1 Structure (`frontend/src/`)

| Dossier | Contenu |
|---|---|
| `api/` | Client API (Axios/TanStack Query), interceptors |
| `components/` | 21+ sous-dossiers : UI primitives, forms, tables, charts, layouts |
| `contexts/` | `AuthContext`, `TenantContext`, `PlatformContext`, `SettingsContext`, `MetaContext` |
| `guards/` | `RouteGuards`, `RequireAuth`, `RequirePermission`, `RequireScope`, `RequireFeature` |
| `hooks/` | Hooks métier (useAuth, useTenant, usePermissions, etc.) |
| `i18n/` | `fr.json`, `en.json` — i18n prêt |
| `layouts/` | `MainLayout`, `AuthLayout` |
| `pages/` | 11+ sous-dossiers : admin, dashboard, departments, families, events, etc. |
| `stores/` | Zustand stores (auth, tenant, ui, notifications) |
| `types/` | Types TypeScript partagés |
| `utils/` | Helpers (format, validation, permissions) |

### 3.2 Écrans principaux (pages/)

| Section | Écrans |
|---|---|
| **Auth** | Login, Register, MagicLink, TenantSelector, Onboarding |
| **Dashboard** | Global, Department, Family, Personal |
| **Administration** | SuperAdmin (tenants, plans, users), TenantAdmin (org, members, settings, branding, modules) |
| **Métier** | Departments (CRUD, members, assets, events), Families (visits, meetings), Events, Finances, Prayers, Discipleship |
| **Configuration** | Branding, CustomFields, Workflows, Roles/Permissions, Modules |

### 3.3 Multi-tenant Frontend

| Composant | Fichier | Statut |
|---|---|---|
| `TenantContext` | `contexts/TenantContext.tsx` | Partiellement intégré |
| `TenantSwitcher` | `components/layout/TenantSwitcher.tsx` | Basique |
| `OrganizationSwitcher` | `components/layout/OrgSwitcher.tsx` | Existant |
| `RouteGuards` | `guards/RouteGuards.tsx` | RequireScope/RequireFeature basiques |

---

## 4. MOBILE — Flutter 3.24 / Riverpod / Drift (SQLite) / Dio

### 4.1 Structure (`mobile/lib/`)

| Dossier | Contenu |
|---|---|
| `api/` | Dio client, interceptors, endpoints |
| `core/` | Config, DI, errors, network, storage, tenant_config |
| `data/local/drift/` | Schéma SQLite offline, DAOs, migrations |
| `features/` | 41+ features : auth, dashboard, departments, families, events, finances, prayers, etc. |
| `l10n/` | `app_en.arb`, `app_fr.arb` — i18n prêt |
| `models/` | Modèles partagés (User, Tenant, Department, etc.) |
| `presentation/` | Widgets réutilisables |
| `shared/` | Constants, extensions, utils |
| `tenant_config.dart` | Configuration tenant active |

### 4.2 Offline / Multi-tenant Mobile

| Composant | Fichier | Statut |
|---|---|---|
| `TenantSession` | `core/tenant/tenant_session.dart` | Existant |
| `Drift Schema` | `data/local/drift/app_database.dart` | Tables isolées par tenant |
| `Sync Queue` | `data/local/sync_queue.dart` | Partiel |
| `Organization Switcher` | `features/organization/` | Existant |

---

## 5. INFRASTRUCTURE & CI/CD

### 5.1 Docker / Déploiement

| Fichier | Description |
|---|---|
| `docker-compose.yml` | Stack local : postgres, redis, backend, frontend, nginx |
| `frontend/Dockerfile` | Build multi-stage Node → Nginx |
| `backend/Dockerfile` | (à vérifier) |
| `infra/nginx/nginx.conf` | Reverse proxy, SSL, WebSocket |
| `render.yaml` | Déploiement Render.com (backend, frontend, DB, Redis) |

### 5.2 GitHub Actions Workflows

| Workflow | Fichier | Description |
|---|---|---|
| **CI** | `ci.yml` | Build + test backend/frontend/mobile |
| **CI/CD** | `ci-cd.yml` | Déploiement staging/production |
| **Deploy Beta** | `deploy-beta.yml` | Déploiement environnement beta |
| **Backup PG** | `backup-postgres.yml` | Sauvegarde quotidienne PostgreSQL |
| **Keep Alive** | `keep-alive.yml` | Ping services Render |

### 5.3 Scripts de lancement

| Script | Description |
|---|---|
| `start-local.sh` | **Canonique** : backend :8080, frontend :5173, DB migration |
| `start-backend.sh` / `start-backend-daemon.sh` | Backend seul |
| `start-services.sh` | Tous services (DB, Redis, Backend, Frontend) |
| `launch-backend.sh` | Lancement backend production |
| `launch-beta.sh` | Lancement beta |
| `restart-backend.sh` | Redémarrage backend |
| `setup-keys.sh` | Génération clés JWT RS256 |

---

## 6. DOCUMENTATION EXISTANTE (`docs/`)

| Fichier | Description |
|---|---|
| `CHURCH_OS_COMMERCIALISATION_MASTER_PROMPT.md` | **Contrat maître** (ce document source) |
| `À faire.md` | Chantier multi-tenant §27-72 + erreurs compilation |
| `PRMPT.txt` | Maître-contrat 43 sections (source PRMPT.pdf) |
| `tenant.md` | Prompt maître multi-tenant (82 sections) |
| `MULTI_TENANT_ARCHITECTURE_AUDIT.md` | Audit architecture (27 risques identifiés) |
| `FINAL_AUDIT_REPORT.md` | Audit final complet |
| `FINAL_VALIDATION.md` | Validation finale |
| `DATABASE.md` | Schéma base de données |
| `DEPLOYMENT.md` | Guide déploiement |
| `API.md` | Documentation API |
| `GUIDE_UTILISATEUR.md` | Guide utilisateur |
| `CHANGELOG.md` | Historique versions |
| `WEB_MOBILE_PARITY.md` | Parité web/mobile |
| `DATA_SYNC_AUDIT.md` | Audit synchronisation données |
| `ROLE_BASED_VISIBILITY.md` | Visibilité par rôle |
| `RAPORT_33_FONCTIONNALITES.md` | 33 fonctionnalités |
| `DISCIPOLAT_20_FONCTIONNALITES_ROADMAP_AGENT.md` | Roadmap 20 features |
| `DISCIPOLAT_MASTER_DEVELOPMENT_PROMPT.md` | Prompt développement maître |

---

## 7. RAPPORTS (`reports/`)

| Fichier | Description |
|---|---|
| `COMMERCIALIZATION_AUDIT.md` | Audit commercialisation |
| `FULLSTACK_FIXES_REPORT.md` | Corrections fullstack |
| `PROJECT_PROGRESS.md` | Progression projet |
| `30_REVOLUTIONARY_FEATURES.md` | 30 features révolutionnaires |

---

## 8. CLÉS & SECRETS (non commités — .gitignore)

| Fichier | Description |
|---|---|
| `.env` | Variables d'environnement locales |
| `keys/private.pem` / `keys/public.pem` | Clés JWT RS256 |
| `*.log` / `*.tmp` | Logs et fichiers temporaires |

---

## 9. RÉSUMÉ QUANTITATIF

| Métrique | Valeur |
|---|---|
| **Modules Backend** | 126 |
| **Tables DB (Flyway)** | ~85 tables tenant-aware + 50+ tables core |
| **Migrations Flyway** | 136 (V1-V136) |
| **Écrans Frontend (pages/)** | 50+ |
| **Features Mobile** | 41+ |
| **Workflows CI/CD** | 5 |
| **Scripts lancement** | 7 |
| **Documents docs/** | 20+ |
| **Rapports reports/** | 4 |
| **Rôles système** | 6 (fixes) |
| **Langues i18n** | FR (défaut), EN (fallback) |

---

## 10. POINTS D'ATTENTION IDENTIFIÉS (d'après audit)

1. **Pas de SUPER_ADMIN** plateforme (risque CRITIQUE)
2. **User = Single Tenant** direct (pas de TenantMembership) — CRITIQUE
3. **Default Tenant Fallback** écritures silencieuses cross-tenant possibles — CRITIQUE
4. **Repositories `findById` sans tenant check** — 80+ repos exposés — CRITIQUE
5. **Cache Redis non isolé** par tenant — HAUTE
6. **WebSocket rooms non isolées** — HAUTE
7. **Fichiers stockés sans préfixe tenant** — HAUTE
8. **RBAC basique seulement** (6 rôles, pas de scope fin) — HAUTE
9. **Pas de Feature Flags** par tenant — HAUTE
10. **Pas de Plans/Quotas SaaS** modélisés — HAUTE

> Voir `docs/MULTI_TENANT_ARCHITECTURE_AUDIT.md` pour liste complète (27 risques).