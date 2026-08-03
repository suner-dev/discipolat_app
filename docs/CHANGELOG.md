# Changelog

## [2.1.8] - 2026-08-03

### 💾 Backup PostgreSQL automatisé (mensuel, GitHub Actions)
- **Nouveau workflow `.github/workflows/backup-postgres.yml`** : dump mensuel de la base
  Render (1er du mois à 02:00 UTC + déclenchement manuel `workflow_dispatch`)
- Export `pg_dump` PostgreSQL 16 via conteneur `postgres:16-alpine`, vérification de
  l'en-tête du dump, puis **chiffrement AES-256** (openssl, `-pbkdf2 -iter 100000`)
  AVANT l'upload — le dépôt étant public, le chiffrement protège les données
  personnelles (âmes, familles, emails)
- Upload en artifact GitHub Actions (rétention 90 jours) + suppression du fichier en
  clair sur le runner ; commande de déchiffrement documentée dans le workflow
- **Secrets GitHub requis** : `RENDER_DB_URL` (External Database URL de `discipolat-db`)
  et `BACKUP_ENCRYPTION_KEY` (`openssl rand -base64 32`)
- DEPLOYMENT.md §8.6 Option B : référence au workflow ajoutée
- ⚠️ Rappel : le backup ne remplace pas l'upgrade — la base Free expire fin août 2026
  (voir 2.1.7/§8.6)

## [2.1.7] - 2026-08-03

### 🗄️ Préparation migration DB Free → payante (expiration 30 j — base créée fin juillet 2026)
- **Nouveau script `scripts/backup-render.sh`** : export `pg_dump` prêt à l'emploi depuis
  la machine locale vers la base Render Free (⚠️ aucun backup automatique sur plan Free)
  — prend l'**External Database URL** en argument ou via `RENDER_DB_URL`, fait `pg_dump
  --no-owner --no-privileges` (dump portable), vérifie l'en-tête PostgreSQL et rappelle
  de stocker le fichier hors de Render
- **Datation de la base** : `discipolat-db` créée avec le Blueprint (render.yaml initial le
  29/07, déploiement Render le 30/07/2026) → **expiration estimée : fin août 2026**
  (vérification exacte : Dashboard → Databases → discipolat-db → Info)
- DEPLOYMENT.md §8.6 Option B : référence au script ajoutée (étapes 3-5)
- **Recommandation** : export `pg_dump` immédiat (filet de sécurité gratuit) + upgrade
  en place Starter (~7 $/mois) AVANT l'expiration — URL de connexion inchangée, aucune
  perte de données, ~quelques minutes d'indisponibilité

## [2.1.6] - 2026-08-01

### 🎯 Static site finalisé — alignement config sur l'URL historique `discipolat.onrender.com`
- **Static site en ligne sous `discipolat.onrender.com`** (nom `discipolat` conservé, URL historique
  préservée après suppression de l'ancien web service Docker — dénouement de la migration §8.5)
- **Fix critique `FRONTEND_URL_BASE`** : pointait encore vers `discipolat-web.onrender.com` (URL
  morte) → les **liens email** (activation de compte, reset password) générés dans `AuthService`
  (`frontendUrl + /activate?token=` et `/reset-password?token=`) étaient brisés en production
- `render.yaml` aligné : nom du static site `discipolat-web` → `discipolat` (correspond au service
  réel créé dans le Dashboard, évite un doublon au prochain Sync Blueprint), `FRONTEND_URL` et
  `FRONTEND_URL_BASE` → `https://discipolat.onrender.com` (origine CORS unique)
- DEPLOYMENT.md mis à jour (table des services, URLs finales, Option B, dénouement §8.5)
- **Action requise côté Dashboard** : Sync Blueprint + redéploiement de l'API pour appliquer
  les nouvelles valeurs CORS/email

## [2.1.5] - 2026-08-01

### 📚 Doc — Migration frontend Static Site : mécanisme exact + checklist anti-piège
- **DEPLOYMENT.md §8.5 enrichie** avec le mécanisme exact du Blueprint Render : les services
  sont associés **par nom, pas par type** (*« Render attempts to apply the Blueprint's
  configuration to that existing service »*) et `runtime`/`type` sont **immuables après
  création** → un Sync ne peut pas convertir un web service Docker en static site
- Séquence d'échec détaillée (matching par nom → runtime immuable → static site jamais créé)
  + symptômes diagnostiquables sans le Dashboard (`404 no-server`, préflight CORS 403)
- **Checklist anti-piège en 6 règles** (ne pas modifier runtime/type, supprimer l'ancien
  service AVANT le Sync, vérifier le statut du Sync, vérifier l'URL réelle, redéployer l'API
  pour le CORS, garder les 2 origines pendant la bascule) + procédure de migration en 7 étapes
  validée en conditions réelles (2.1.1 → 2.1.5)

## [2.1.4] - 2026-08-01

### 📧 Fix SMTP — plan Free Render bloque les ports 25/465/587
- **Découverte critique** : les web services du plan Free Render bloquent le trafic SMTP
  sortant sur les ports **25, 465 et 587** → les emails (création de compte, reset password,
  rappels) échouaient **silencieusement** en production avec `MAIL_PORT=587` (Mailgun)
- **Fix gratuit** : passage au port **2525** (accepté par Mailgun avec STARTTLS, même
  comportement que 587) dans `application.yml` (défaut profil prod) et `render.yaml`
- Documentation : note SMTP explicite dans DEPLOYMENT.md (§6) + tableau env mis à jour
  (MAIL_HOST=`smtp.mailgun.org`, MAIL_PORT=`2525`)
- ⚠️ Ce fix évite d'être **obligé de payer un plan API** (~7 $/mois) juste pour envoyer
  des emails → l'API reste sur le plan Free à 0 $/mois

## [2.1.3] - 2026-08-01

### 💸 Optimisation coûts — Cron jobs Render supprimés (~14 $/mois économisés)
- **Suppression des 2 cron jobs Render** (`discipolat-cron-absence`, `discipolat-cron-reminder`)
  dans `render.yaml` : le plan `free` n'existant pas pour les crons, ils coûtaient ~14 $/mois
- **Preuve de redondance + inutilité** : les tâches (absences /6h, rappels samedi 18h) sont
  déjà exécutées par le **scheduler interne Spring** (`ScheduledJobs.java`,
  `@EnableScheduling`), et les endpoints `/api/v1/internal/check-absences` et
  `/send-reminders` appelés n'existaient **pas** dans le code (404) → ces crons ne faisaient rien
- Le **keep-alive** (2.1.1) maintient l'API éveillée 24/7 → le scheduler Spring tourne de façon fiable
- Nettoyage de la doc et des scripts : suppression de `INTERNAL_API_KEY` (jamais lue par le code)
  et de `RENDER_WEB_SERVICE_ID` (static site en auto-deploy) — DEPLOYMENT.md, ENV_TEMPLATE.md, deploy-setup.sh

## [2.1.2] - 2026-08-01

### 🗄️ Migration PostgreSQL Free → payant (expiration 30 jours)
- Nouvelle section 8.6 dans DEPLOYMENT.md : procédure exacte Dashboard Render pour
  vérifier l'expiration, upgrader en place vers un plan payant (sans perte de données,
  URL de connexion inchangée) ou exporter en `pg_dump` avant expiration + restauration
- Correction section 7 : les bases Render **Free n'ont AUCUN backup automatique**
  (l'affirmation « backups quotidiens, rétention 7 jours » était fausse) — seul un
  export `pg_dump` externe est possible en Free

## [2.1.1] - 2026-08-01

### ⚡ Performance — Anti cold start Render
- **Frontend converti en Static Site Render** (`runtime: static`) : servi via CDN mondial,
  jamais endormi, 0 heure d'instance consommée → ouverture de l'appli instantanée à chaque visite
- **Keep-alive GitHub Actions** (`.github/workflows/keep-alive.yml`) : ping de l'API toutes les
  10 minutes (gratuit, dépôt public) → plus de 30-90 s d'attente au premier chargement
- **Respect du quota Render** : 750 h/mois **par workspace** → seule l'API reste éveillée
  (~720 h ≤ 750 h) ; le frontend static site ne consomme rien
- En-têtes de sécurité (CSP, HSTS, X-Frame-Options…) et fallback SPA `/* → /index.html`
  désormais déclarés dans `render.yaml` (remplacent nginx.conf en production)
- CI : suppression du build/push de l'image Docker frontend + trigger deploy web (auto-deploy)
- Documentation : section « Éviter le cold start Render » + limites du plan Free (Postgres 30 j) dans DEPLOYMENT.md

## [2.1.0] - 2026-07-31

### 🔒 Sécurité & Robustesse
- `/actuator/health` public (requis par le healthcheck Render et docker-compose) ; détails restreints à ADMIN/PASTEUR
- Réponses HTTP propres 400/404/405 dans `GlobalExceptionHandler` (paramètre manquant, ressource introuvable, méthode non supportée)

### 🧪 Tests & CI
- **Fix CI Backend** : le test d'intégration rate-limiting (`PerIpRateLimiterIntegrationTest`) ne dépend plus de Testcontainers — il utilise `REDIS_URL` (fournie en CI) et est skippé automatiquement si Redis est injoignable (`@EnabledIf`)
- IPs de test aléatoires par run : plus de fuite de buckets Redis entre deux `mvn verify` consécutifs sur le même Redis
- Profil `test` sans driver/dialecte H2 hardcodés : compatible H2 (local) et PostgreSQL (override `SPRING_DATASOURCE_URL` en CI)
- **Total : 72 tests backend, 0 skipped** (les 13 tests Redis s'exécutent désormais réellement)

### 📱 Responsive
- Vérification responsive automatisée : 27 pages × 3 tailles d'écran (375 / 768 / 1440 px) = 81 points de contrôle sans débordement horizontal

## [2.0.0] - 2026-07-30

### 🏗️ Architecture
- **Système Multi-Rôles** : un compte, plusieurs rôles, Role Context Switcher
- JWT enrichi avec `roles[]`, `activeRole`, `estChefDeFamille`
- `SecurityUtils.getAllUserRoles()` + `SecurityUtils.getCurrentUserRole()` mis à jour
- `@PreAuthorize` vérifie désormais `hasAnyRole` pour le multi-rôle
- `User.java` : ajout de `Set<UserRole> roles`, `UserRole activeRole`
- DataInitializer seed : 6 profils multi-rôles (admin, pasteur, responsable, chef, faiseur, paul)

### 📊 Dashboards
- **Dashboard Pasteur** : centre de pilotage avec vue globale, croissance, alertes, stats
- **Dashboard Responsable** : vue département (scope unique), événements, rapports, évaluations
- **Dashboard Chef de Famille** : vue famille, faiseurs, disciples, réseau
- **CRM Faiseur** : suivi des disciples avec filtres (Actifs, Intégration, Veille, Décrochés)
- **Dossier Pastoral 360°** : fiche complète avec indices intelligents, timeline, notes privées

### 🔒 Sécurité
- **Rate limiting Bucket4j** : 7 buckets configurables (login, refresh, forgot-password, reset-password, activate, change-password, switch-role)
- HSTS (1 an, subdomains), CSP, X-Frame-Options DENY
- Actuator et Swagger restreints à ADMIN/PASTEUR
- Cache configuré (CacheConfig, CacheMissLogger)

### 🔍 Recherche et analyses
- **Recherche intelligente** : moteur de recherche cross-entity
- **Indices automatiques** : santé spirituelle, fidélité, engagement, participation
- **Alertes intelligentes** : inactifs, absences multiples, isolement

### 🧪 Tests
- 3 nouveaux fichiers de test frontend : `Pastoral360Page.test.tsx` (11 tests), `CrmFaiseurPage.test.tsx` (8 tests), `Sidebar.test.tsx` (7 tests)
- 1 nouveau fichier de test backend : `DashboardServiceTest` (6 tests)
- Correction des tests existants pour le multi-rôle (`JwtTokenProviderTest`, `AuthServiceTest`, `SoulServiceTest`, `TwoFactorServiceTest`)
- **Total : 59 tests backend + 39 tests frontend = 98 tests ✅**

### 🗄️ Base de données
- Migrations V9 à V15 (programmes hebdomadaires, évaluations, discipline, multi-rôle, performances)
- Indexes composites optimisés (53+ indexes)
- Table `user_roles` pour le multi-rôle
- Colonnes `active_role`, `user_id`, `created_at`, `updated_at`

### 📱 Mobile (Flutter)
- Role Switcher complet synchronisé
- Écrans dashboard spécialisés (Pasteur, Responsable, Chef de Famille)
- Écran Évaluations, Recherche, Prières, Événements
- Drawer dynamique avec sélecteur de rôle
- Scrollable dashboards

### 🔧 Technologies ajoutées
- Bucket4j 8.10.1 (rate limiting)
- Micrometer Prometheus (métriques)
- Spring Cache abstrait
- @EnableScheduling + @EnableCaching

## [1.0.0] - 2026-07-15

### Backend
- Authentification JWT RS256 avec refresh token
- RBAC complet (Pasteur, Responsable, Chef de famille, Faiseur)
- CRUD des départements, familles de disciples, âmes
- Reporting hebdomadaire à deux niveaux (faiseur + famille)
- Suivis parallèles
- Alertes automatiques 48h et rappels de rapport
- Dashboard décisionnel avec KPI
- Notifications in-app et email
- Journal d'audit
- Jobs planifiés (vérification absence, rappels)
- Architecture Spring Modulith avec modules indépendants

### Frontend
- Application React 19 avec TypeScript
- Tableau de bord avec graphiques Recharts
- Gestion complète des âmes, familles, départements
- Saisie de rapport hebdomadaire par âme
- Rapport de famille consolidé
- Gestion des suivis parallèles
- Alertes et notifications
- Mode sombre/clair
- Design responsive
- Protection des routes par RBAC

### Infrastructure
- Docker Compose (API + Frontend + DB + Nginx)
- Nginx reverse proxy
- Pipeline CI/CD GitHub Actions
- Script de génération de clés JWT
- Configuration multi-environnements (dev, docker, prod)

### Mobile (Structure)
- Structure Flutter prête pour le développement
- Architecture clean avec séparation des couches
- Modèles de données synchronisés avec le backend

### Documentation
- README complet avec guide de démarrage
- ARCHITECTURE.md détaillée
- API.md avec tous les endpoints
- DATABASE.md avec schéma
- DEPLOYMENT.md avec procédure
- SECURITY.md avec politique de sécurité
- DECISIONS.md avec arbitrages
- CHANGELOG.md
