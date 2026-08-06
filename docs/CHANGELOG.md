# Changelog

## [3.0.8] - 2026-08-06

### 🔒 Isolation des espaces métiers appliquée à TOUTE la chaîne (API → services → Web → Mobile)

**Principe** : le rôle actif devient la seule source d'autorisation. Changer de rôle =
changer d'application — sur le backend, les menus, les routes et les données.

### 🎯 API (fix critique)
- **`JwtAuthenticationFilter`** : les autorités Spring ne proviennent plus de **tous**
  les rôles du JWT mais du **rôle ACTIF uniquement** (claim `role`). `@PreAuthorize`,
  `hasRole()` et `hasAnyRole()` évaluent désormais l'espace métier courant — avant ce
  fix, un multi-rôles (ex. FAISEUR + RESPONSABLE) pouvait appeler les API d'un autre
  métier malgré les contrôles de services. L'**Admin actif** reçoit aussi `ROLE_PASTEUR`
  (super-utilisateurs partageant la vue complète, cohérent avec `isSuperUser()`)

### 🔒 Confidentiel — données des familles
- **`FamilyController`** : garde classe ajoutée (les GET étaient **sans `@PreAuthorize`**
  → n'importe quel utilisateur authentifié, même MEMBRE, pouvait lister/lire toutes les
  familles)
- **`FamilyService.findAll/findById` scopés par rôle actif** : super-utilisateurs = tout ;
  chef = sa famille (`familleGereeId` + `findByChefFamilleId`) ; responsable = familles des
  membres de **ses** départements (via `soul_departments`) ; faiseur = familles de ses
  disciples. 403 en lecture hors espace
- **`DashboardService.getCurrentUserMetrics`** : la branche RESPONSABLE comptait **toutes**
  les familles de l'église → membres de ses départements uniquement

### 🗂️ Web — rôle actif partout dans l'UI
- Routes `/evaluations` et `/events/statistics` ouvertes à ADMIN (l'admin est
  super-utilisateur et voit ces menus)
- Les gates `hasRole(...)` (ensemble des rôles) remplacés par le **rôle actif** :
  EventsPage (vue consolidée), ReportsPage (exports), FamiliesPage (comparer),
  FamilyDetailPage (niveau de risque, changer chef, performance faiseurs)

### 📱 Mobile
- **Garde de routes `hasActiveRole`** (au lieu de `hasAnyRole`) : l'isolation s'applique à
  **toutes** les routes et non plus seulement aux tableaux de bord — un FAISEUR actif
  n'ouvre plus `/users`, `/departments`, `/evaluations`, etc.
- `_routeRoles` aligné sur le web (rapport faiseur sans responsable/chef, documents + chef,
  demande membres sans membre, pastoral-360 + chef, recherche + chef)

### 🧪 Tests
- **Nouveau `SecurityUtilsTest`** (7 tests) : rôle actif lu depuis le JWT, `hasActiveRole`
  ignore les rôles non actifs, `isSuperUser` admin/pasteur uniquement
- **Fix infra H2** : les entités utilisent `columnDefinition = "jsonb"` (production
  PostgreSQL) — H2 ne connaît pas ce type → toute la suite backend échouait à la création
  du schéma. `h2-init.sql` déclare `CREATE DOMAIN IF NOT EXISTS jsonb AS JSON`
  (config test uniquement)

### ✅ Validation
- Backend : **128 tests Maven ✓** (0 échec, dont la suite complète rendue exécutable)
- Frontend : **58 tests vitest ✓**, `tsc` propre
- Mobile : **15 tests widget ✓**, `flutter analyze` sans nouvelle issue

## [3.0.7] - 2026-08-06

### 🧪 Mobile — test de régression de la route `/parallel-followups`
- **Nouveau `mobile/test/parallel_followups_route_test.dart`** (2 tests) utilisant le
  **routeur complet** (`appRouter` réel + singleton `AuthState` authentifié FAISEUR) :
  - **Drawer → route** : redirect du login vers l'espace FAISEUR, ouverture du drawer
    (`ScaffoldState.openDrawer()`), `scrollUntilVisible` vers l'entrée « Suivis
    parallèles » (item ~9/14, hors écran), tap → l'écran se rend (onglets
    « Actifs »/« Tous ») et **pas de page 404**
  - **Navigation directe** : `appRouter.go('/parallel-followups')` après quitté la
    cible (indépendance de l'ordre d'exécution, le singleton `appRouter` conservant
    sa position entre les tests)
- **Régressions** : si le `GoRoute` disparaissait à nouveau, les deux tests échoueraient
  (l'écran `ParallelFollowupsScreen` rend des onglets que la page 404 n'a pas)

### ✅ Validation
- Mobile : **15/15 tests widget ✓** (1 + 9 + 3 + 2), `flutter analyze` propre

## [3.0.6] - 2026-08-06

### 🧪 Mobile — tests de navigation des routes à paramètres
- **Nouveau `mobile/test/parameterized_routes_test.dart`** (3 tests) : navigation
  réelle vers `/souls/:id`, `/souls/:id/pastoral-360` et `/departments/:id/report`
  via un routeur de test, avec **preuve que le paramètre `:id` est transmis**
  (fake ApiService enregistrant les chemins appelés : `/souls/soul-123`,
  `/souls/soul-123/pastoral-360`, `/departments/dept-456/detail|report|kpi`) et
  vérification du rendu réel (nom de l'âme, dossier 360°, rapport du département)
  + assertions `findsNothing` sur les états d'erreur (pas de faux positif)
- **Testabilité** : injection `ApiService?` optionnelle ajoutée à `SoulDetailScreen`,
  `Pastoral360Screen` et `DepartmentReportScreen` (`widget.apiService ?? ApiService()`,
  aucun changement de comportement en production)
- **Détail technique** : maps de réponse typés `Map<String, dynamic>` (évite les échecs
  de cast `Map<dynamic, dynamic>` en test), `initializeDateFormatting('fr_FR')` pour
  intl dans Pastoral360, `indices` vides pour contourner RadarChart en test
- **Lint** : correction d'un warning pré-existant dans `SoulDetailScreen`
  (`dateIntegration` non-nullable → `?? '—'` mort supprimé)

### ✅ Validation
- Mobile : **13/13 tests widget ✓** (1 + 9 + 3), `flutter analyze` propre sur les
  fichiers touchés

## [3.0.5] - 2026-08-06

### 🧭 Mobile — correction de la route morte `/parallel-followups`
- **Bug** : `ParallelFollowupsScreen` était importé dans `app.dart` et le drawer pointait
  vers `/parallel-followups`, mais **aucun `GoRoute` n'était enregistré** → tout lien
  « Suivis parallèles » (vue super-utilisateurs et espace FAISEUR) tombait sur le 404
- **Fix** : ajout du `GoRoute '/parallel-followups'` → `ParallelFollowupsScreen`
  (le garde de rôle `/parallel-followups` = ADMIN/PASTEUR/RESPONSABLE/CHEF/FAISEUR
  existait déjà et s'applique désormais réellement)
- **Audit de routes** (revue) : vérification de chaque clé `_routeRoles` et de chaque
  lien des 5 listes du drawer — `/parallel-followups` était la **seule** route morte ;
  tous les imports d'écrans sont désormais utilisés (warning `unused_import` éliminé)
- **Lint** : refactor du calcul `basePath` du redirect (concaténation → interpolation,
  sémantique identique) — `app.dart` passe à **0 issue**

### ✅ Validation
- Mobile : `flutter analyze` propre sur `app.dart`, 10/10 tests widget ✓

## [3.0.4] - 2026-08-06

### 🧪 Mobile — tests widget du hub Rapports
- **Nouveau `mobile/test/reports_screen_test.dart`** (9 tests) : statistiques de
  complétion affichées (compteur 3/5, mini-stats Soumis/En attente/Taux), cartes de
  navigation faiseur/famille, **isolation par rôle** (le RESPONSABLE voit la carte
  « Rapport du département », le FAISEUR ne la voit pas), navigation réelle au tap
  (placeholders GoRouter vérifiés), sélecteur de département en bottom sheet + accès
  au rapport, état d'erreur qui **préserve la navigation** (parité web), et bouton
  « Réessayer » qui recharge les stats
- **Testabilité** : `ReportsScreen` accepte un `ApiService` optionnel injectable
  (`widget.apiService ?? ApiService()`), aucun changement de comportement en production
- **Conventions** : `AuthState` singleton remis à zéro en `setUp`/`tearDown`, fake
  ApiService retournant de vraies `Response` Dio (aucun réseau, aucune dépendance
  plugin), routeur de test minimal pour `context.go`

### ✅ Validation
- Mobile : **10/10 tests widget ✓** (1 existant + 9 nouveaux), `flutter analyze` propre

## [3.0.3] - 2026-08-06

### 📱 Mobile — écran « Rapport du département » pour le responsable
- **Nouveau `department_report_screen.dart`** : équivalent mobile de la page web
  `/departments/:id/report` — synthèse hebdomadaire (familles, rapports soumis,
  présents, absents, présence globale), sorties/maintenus, **détail par famille**
  (statut soumis/non soumis, présence, présents, absents, sorties, maintenus) et
  carte « Indicateurs de la semaine » (taux de soumission, taux de présence,
  rapports soumis/attendus, faiseurs actifs) avec sélecteur de semaine
  (`showDatePicker`)
- **Route `/departments/:id/report`** ajoutée dans `app.dart` (guard déjà couvert par
  `/departments` = ADMIN/PASTEUR/RESPONSABLE ; les autres rôles restent bloqués)
- **Hub Rapports** : nouvelle carte « Rapport du département » (visible RESPONSABLE +
  super-utilisateurs) qui ouvre un **sélecteur de département** en bottom sheet —
  départements administrés via `/departments/by-responsable/:id` pour le responsable,
  tous les départements pour ADMIN/PASTEUR
- **Robustesse** (point de revue) : les données critiques (détail + rapport) sont
  chargées ensemble, le KPI est best-effort — un échec KPI ne masque plus le rapport

### ✅ Validation
- Mobile : `flutter analyze` sans nouvelle erreur sur les fichiers touchés

## [3.0.2] - 2026-08-06

### 📱 Mobile — écran « Rapports » pour le responsable
- **Nouveau hub `/reports`** (`reports_screen.dart`) : équivalent mobile de la page web
  `/reports` — statistiques de complétion hebdomadaire (`/dashboard/report-completion` :
  soumis, en attente, taux avec anneau de progression) + cartes d'accès au rapport du
  faiseur et au rapport de famille (parité avec le web)
- **Route `/reports`** ouverte aux 5 rôles opérationnels (ADMIN/PASTEUR/RESPONSABLE/
  CHEF_DE_FAMILLE/FAISEUR) ; `/reports/maker` ouvert au RESPONSABLE et CHEF_DE_FAMILLE
  (aligné sur le GET backend et le web)
- **Drawer** : entrée « Rapports » ajoutée à l'espace RESPONSABLE, à la vue
  super-utilisateurs, ainsi qu'aux espaces **FAISEUR** et **CHEF_DE_FAMILLE**
  (parité web : tous les rôles opérationnels ont le hub `/reports`, juste avant leur
  rapport dédié faiseur/famille) ; les cartes de navigation restent accessibles même si le
  chargement des stats échoue (comportement web)

### ✅ Validation
- Mobile : `flutter analyze` sans nouvelle erreur (issues restantes pré-existantes)

## [3.0.1] - 2026-08-06

### 🔒 Scoping des données aligné sur le rôle ACTIF (Search, Map, Member, Soul)

**Principe** : les vérifications de visibilité ne se basent plus sur l'ensemble des rôles
possédés (`getRoles().contains(...)`) mais sur le rôle **actif** (espace métier courant)
via `SecurityUtils.isSuperUser()` / `hasActiveRole(...)` — un multi-rôles ne voit que les
données de l'espace dans lequel il travaille.

- **SoulService** : `findAll`/`filterSouls` scopés par rôle actif (faiseur → ses disciples,
  chef → les âmes de sa famille, responsable → les membres de ses départements via
  `soul_departments` actifs) ; la recherche est scopée aussi
- **Fermeture IDOR** : les filtres explicites (`faiseurId`, `familleId`, `typeDisciple`,
  `statut`) ne sont plus des ancres de confiance pour les non super-utilisateurs — ils sont
  intersectés avec l'espace métier (filtrage en mémoire) ; les requêtes DB directes ne sont
  autorisées qu'aux super-utilisateurs
- **SearchService** : `getAccessibleSoulIds` réécrit — la branche RESPONSABLE fuyait
  **toutes les familles** (elle itérait `familyRepository.findAll()`) → corrigée en membres
  des départements administrés ; résultats utilisateurs réservés aux super-utilisateurs
- **MapService** : points scopés par rôle actif ; correction de la fuite des familles du
  responsable (toutes les familles → familles des membres du département) ; familles du
  chef = union dédupliquée (`familleGereeId` + `findByChefFamilleId`) ; mise à jour des
  coordonnées limitée à super-utilisateur ou responsable actif de l'âme
- **MemberService** : présences scopées, boîte de réception et traitement des demandes
  basés sur le rôle actif ; `verifyDepartmentAccess` exige désormais le mode RESPONSABLE
  actif (un faiseur actif qui possède le rôle RESPONSABLE ne gère plus les présences)
  ; `getMyDashboard` conserve les rôles complets (identité du membre, pas scoping)
- **Tests** : `SoulServiceTest` +3 (super-utilisateur voit tout, faiseur scopé, IDOR non
  élargi, recherche sans accès) ; `MemberPresenceSheetTest` +1 (rôle actif incorrect) et
  mise à jour (utilisateur inconnu refusé en `AccessDeniedException` sans fuite d'existence)

### ✅ Validation
- Backend : **122 tests Maven** ✓ (0 échec), BUILD SUCCESS

## [3.0.0] - 2026-08-06

### 🏢 Restructuration des espaces métiers — chaque rôle = un environnement de travail indépendant

**Principe** : un changement de rôle = un changement complet de contexte métier (dashboard,
menus, raccourcis, statistiques, actions). Plus aucun mélange entre les métiers.

### 🗂️ Web — espaces métiers strictement séparés
- **Nouveau `frontend/src/workspaces.ts`** : config centralisée des espaces (home par rôle,
  menus par sections, métadonnées visuelles). Sidebar réécrite : menus par sections dédiées
  au métier actif + bandeau d'espace métier (desktop & mobile)
- **RESPONSABLE** : logiciel de gestion des départements (HRM église) — pilotage,
  départements, membres, présences/demandes, rapports, alertes, événements. Aucun menu
  discipolat/familles/âmes/faiseurs
- **FAISEUR** : uniquement le discipolat (CRM, disciples, rapports, prières, visites,
  évangélisation, objectifs, suivis, recherche)
- **CHEF DE FAMILLE** : uniquement sa famille (dashboard chef, familles, disciples,
  rapports famille, évaluations, prières, événements, alertes, demandes)
- **MEMBRE** : espace personnel (profil, formations, badges, RDV, messagerie)
- **ADMIN / PASTEUR** : super-utilisateurs, vue complète par sections (permissions exclues pour Pasteur)
- **DashboardGate** : `/dashboard` redirige vers l'espace du rôle actif
  (RESPONSABLE → `/dashboard/responsable`, FAISEUR → `/crm/faiseur`, CHEF →
  `/dashboard/chef-famille`, MEMBRE → `/dashboard/membre`)
- **Routes durcies** : `/crm/faiseur` réservé FAISEUR+super, `/dashboard/chef-famille`
  sans FAISEUR, `/reports/maker` sans chef, création de famille sans responsable
- **Transition de rôle animée** : overlay plein écran (icône + label du nouvel espace) puis
  redirection automatique vers le dashboard du rôle choisi
- **Tests de transition de rôle** (`RoleWorkspaceRouting.test.tsx`, 19 tests) : redirection
  DashboardGate vers chaque espace métier + isolation des routes (faiseur bloqué sur le
  dashboard chef, chef/responsable bloqués sur le CRM faiseur, membre bloqué sur les âmes…)
  + accès super-utilisateurs préservés + routes de données toujours accessibles

### 📱 Mobile (Flutter)
- **Drawer par espace métier** : filtre STRICT par rôle actif (corrige le bug `hasAnyRole`
  qui affichait les menus de tous les rôles), listes dédiées par espace
- **`roleHome()`** : redirection après login/switch vers l'espace du rôle
- **Isolation des espaces dans le routeur** : un responsable/chef/faiseur/membre est ramené
  vers son propre dashboard ; impossible d'ouvrir le dashboard d'un autre métier
- **Bottom-nav sensible au rôle** + durcissement des routes dashboard/CRM

### 🔒 Backend & API
- `SecurityUtils.hasActiveRole(...)` + `SecurityUtils.isSuperUser()` : API d'infrastructure
  pour les contrôles d'espace par rôle actif
- `DashboardController` durci : `/dashboard/chef-famille` sans FAISEUR, `/dashboard/crm-faiseur`
  réservé FAISEUR+super, `/dashboard/responsable` ouvert à ADMIN
- Les données restent scopées côté serveur (services existants) — aucun bouton caché ne
  protège les données, toutes les vérifications sont serveur

### ✅ Validation
- Frontend : 58 tests vitest ✓ (39 existants + 19 transition de rôle), `tsc -b` ✓
- Backend : 117 tests Maven ✓ (BUILD SUCCESS)
- Mobile : `flutter analyze` sans erreur

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
