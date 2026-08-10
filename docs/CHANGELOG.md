# Changelog

## [3.13.0] - 2026-08-10

### 🧪 Tests de régression API — contrôleurs d'administration de la plateforme configurable

**Principe** : les 4 contrôleurs admin (identité, modules, menus, champs personnalisés)
disposent désormais de tests d'intégration API qui exercent la **chaîne de sécurité réelle**
(JWT + `@PreAuthorize`) — un futur changement de règle d'accès ou de contrat de réponse est
détecté immédiatement, côté serveur.

### 🧪 Nouveaux tests (3 fichiers, 39 tests)
- **`SettingsControllerTest`** (10 tests) : `GET /public/settings` sans token → 200 avec
  **uniquement les champs publics** (ni `id`, ni `contactNotes`), `GET /settings` par un
  simple FAISEUR → 200 (la règle est `isAuthenticated()`, pas `hasRole('ADMIN')`), 401 sans
  token, PUT par ADMIN → 200 + délégation au service (payload asserté), PUT par non-ADMIN →
  403 **avec `verify(never)`** (preuve que le blocage est à la couche `@PreAuthorize`),
  couleur hexadécimale invalide → 400 (validation bean), reset ADMIN → 200 / non-ADMIN → 403
- **`PlatformConfigControllerTest`** (17 tests) : menus du rôle actif (FAISEUR) → 200,
  gating avec état d'activation des modules, administration menus/modules réservée à
  l'ADMIN (403 + `verify(never)` sur les tentatives non autorisées), création → 201 avec
  payload asserté, édition → 200, suppression → 204, **réordonnancement `POST /menus/reorder`**
  (ordre + section assertés via le service), bascule d'activation `PUT /modules/{key}`
  (`enabled=false` transmis au service)
- **`CustomFieldControllerTest`** (12 tests) : définitions filtrées par rôle (FAISEUR → 200),
  paramètre `entiteType` manquant → 400, 401 sans token, `definitions/all` réservé ADMIN
  (403 + `verify(never)`), création → 201 (entité + code + type assertés), édition → 200,
  suppression → 204, bundle par entité → 200, **sauvegarde des valeurs → 200 avec le
  payload reçu asserté** (corps transmis au service, pas perdu)

### 🧪 Infrastructure de test partagée
- **Nouveau `TestJwtConfig`** (`common/test`) : `@TestConfiguration` fournissant un
  `JwtTokenProvider` RÉEL (clés RSA générées pour le test), importé par chaque test — la
  chaîne de sécurité réelle (JwtAuthenticationFilter + SecurityConfig) signe et valide les
  tokens comme en production, sans base de données

### ✅ Validation
- Backend : **301 tests Maven ✓** (262 + 39), 0 échec, `BUILD SUCCESS` — aucune régression

## [3.12.0] - 2026-08-10

### 🧪 Tests de régression — pages d'administration de la plateforme configurable

**Principe** : les 4 pages d'administration centralisée (identité, modules, menus,
champs personnalisés) disposent désormais de tests de régression dédiés qui couvrent
le rendu, chaque action CRUD et les chemins d'erreur — pour que la modularité de la
plateforme ne régresse pas silencieusement.

### 🗂️ Nouveaux fichiers de test (4 fichiers, 22 tests)
- **`AdminSettingsPage.test.tsx`** (5 tests) : rendu du formulaire pré-rempli depuis
  `/settings` (identité, couleurs, réseaux sociaux), enregistrement PUT `/settings` avec
  le payload modifié, reset POST `/settings/reset`, modification d'une couleur de base,
  **échec d'enregistrement → toast d'erreur**
- **`PlatformModulesPage.test.tsx`** (5 tests) : modules groupés par section avec badges
  clés uniques et état réel du toggle (`aria-checked`) du module désactivé, toggle PUT
  `/platform/modules/{key}`, création POST via le modal (clé/libellé), suppression DELETE
  avec confirmation, **échec du toggle → toast d'erreur**
- **`PlatformMenusPage.test.tsx`** (6 tests) : menus groupés par section avec badges de
  rôles et de modules, création POST via le modal, toggle PUT, réordonnancement POST
  `/platform/menus/reorder` (assertion du tableau d'ordre échangé), suppression DELETE,
  **échec de création → toast d'erreur**
- **`AdminCustomFieldsPage.test.tsx`** (6 tests) : onglets par entité (Âmes/Utilisateurs)
  avec rechargement des définitions au changement d'onglet, création POST (payload
  entité + code + libellé), édition PUT (modal pré-rempli), suppression DELETE,
  **échec de création → toast d'erreur**

### 🧪 Conventions & robustesse
- Convention `vi.hoisted` + mock du module `@/lib/api` aligné sur les autres tests ;
  `react-hot-toast` mocké pour asserter les messages d'erreur (aucune dépendance DOM
  du vrai toast)
- Assertions précises (badges clés uniques, état `aria-checked`, payloads
  `objectContaining`, ordre du réordonnancement) — pas d'assertions tautologiques
- **Points de revue corrigés** : assertion faible `getAllByText('Transferts').length >= 1`
  remplacée par badge clé unique + état du toggle ; ajout d'un test d'échec par fichier

### ✅ Validation
- Frontend : **115 tests vitest ✓** (93 + 22), `tsc` propre — suite complète verte
  (les 2 tests flaky connus Pastoral360/AuditPage passent en isolation et dans la suite
  complète)

## [3.11.0] - 2026-08-10

### 📱 Mobile — thème dynamique (branding église)

**Principe** : l'application mobile applique désormais la palette de couleurs
configurée dans l'administration web (ChurchSettings). À chaque démarrage, le
client fetch `GET /api/v1/public/settings` et dérive les nuances — sans
authentification, sans dépendance au code.

### 🎨 Palette mutable (`AppColors` réécrit)
- `AppColors.primary`/`primaryLight`/`primaryDark` passent de `static const` à
  `static Color` mutable, initialisés avec le vert Discipolat (fallback)
- `AppColors.applyBranding(Color primary, {Color? accentColor})` : dérive
  `primaryLight` et `primaryDark` via `Color.lerp` (blanc/noir), et met à jour
  `accent`/`accentLight` pour la couleur secondaire
- `gold`/`goldLight` renommés en `accent`/`accentLight` pour cohérence sémantique
- 11 usages `const` dans les écrans (login, 404, rapports, utilisateurs, dashboards,
  transferts) dé-constés pour référencer les variables mutables

### 🧩 Modèle `Branding` + provider
- **`data/models/branding.dart`** : parse `PublicBrandingResponse` (churchName,
  primaryColor, accentColor, fontFamily, etc.) + helper `colorFromHex` (fallback
  typé, format `#RRGGBB`/`#AARRGGBB`)
- **`data/services/providers.dart`** : `brandingProvider` (FutureProvider) fetch
  `/public/settings` via l'ApiService existante ; toute erreur réseau → branding
  par défaut (l'application démarre toujours)

### 🏗️ Application dans `DiscipolatApp` (main.dart)
- `DiscipolatApp` devient `ConsumerStatefulWidget` : watch `brandingProvider`,
  applique `AppColors.applyBranding(...)` dès que le branding arrive, puis
  reconstruit `MaterialApp.router` — le thème, les widgets et la `colorScheme`
  lisent `AppColors.primary` au moment du build, donc la nouvelle couleur
  s'applique à toute l'interface sans latence résiduelle
- `title` de l'application alimenté par `churchName` (fallback `'Discipolat'`)

### 🧪 Tests
- **Nouveau `test/branding_theme_test.dart`** (7 tests) :
  - 3 unit tests `colorFromHex` (format valide, sans dièse, invalide → fallback)
  - 2 unit tests `Branding.fromJson` (contrat backend, valeurs par défaut)
  - 2 unit tests `AppColors.applyBranding` (dérivation des nuances, thème suit)
  - 2 tests widget : override du provider avec couleur personnalisée → vérifie
    `AppColors.primary` mise à jour ; fallback quand le provider échoue
- **widget_test.dart** inchangé (le provider retombe sur le branding par défaut)
- **Suites existantes inchangées** : 34 tests widget ✓, `flutter analyze` sans
  nouvelle issue

## [3.10.0] - 2026-08-10

### 🌱 Données de démonstration migrées vers la plateforme configurable (V39)

**Principe** : les nouvelles tables de configuration (champs personnalisés V38, rôles
V37) sont désormais peuplées avec des données d'exemple cohérentes avec le jeu de démo
existant (V2) — l'admin voit immédiatement comment la plateforme s'adapte, sans devoir
créer chaque configuration à la main.

### 🗄️ Migration `V39__demo_seed_platform_config.sql`
- **Champs personnalisés par défaut** (`custom_field_definitions`, 11 définitions) :
  - `SOUL` — Langue parlée, Profession, Date de naissance, Niveau d'études (sélection),
    Situation familiale (sélection), Talent/don, Observations (textarea, lecture/écriture
    restreintes aux rôles pastoraux)
  - `USER` — Téléphone secondaire, Profession
  - `FAMILY` — Quartier · `DEPARTMENT` — Objectif de l'année
- **Valeurs de démonstration** (`custom_field_values`) : remplies pour les âmes seedées
  (Marie Dupont, Jean Martin, Sophie Bernard, Anne Robert, Claire Durand), les utilisateurs
  seedés (pasteur, responsables, chef — téléphone secondaire, profession), la famille
  Timothée (Quartier) et le département Jeunesse (Objectif de l'année) — le rendu des
  formulaires dynamiques et des bundles est visible dès la connexion
- **Rôles personnalisés exemples** (`platform_roles` non système + `role_permissions`) :
  `SECRETAIRE`, `TRESORIER`, `RESPONSABLE_COMMUNICATION`, `INTERCESSEUR` — chacun avec
  une matrice de permissions prête à l'emploi, visible dans la gestion des rôles
  (PermissionsPage) et modifiable/duplicable sans code
- Idempotent : `ON CONFLICT … DO NOTHING` partout (rejouable sans doublons)

### 🔒 Sécurité — enforcement serveur de `roles_ecriture` + `roles_lecture` / `actif`
- **`CustomFieldService.saveValues`** : le rôle ACTIF doit être autorisé à écrire chaque
  champ (`roles_ecriture` vide = tous ; sinon liste de rôles). Les champs non éditables,
  **illisibles** (cohérence lecture/écriture, défense en profondeur) ou **désactivés**
  (`actif=false`) sont **silencieusement ignorés** : le formulaire principal ne peut pas
  échouer à cause d'une restriction, et un appel direct à l'API
  (`PUT /custom-fields/{type}/{id}`) ne peut pas écrire un champ réservé à d'autres rôles
  (le champ `OBSERVATIONS` de la démo le démontre)
- **Robustesse** : résolution des définitions en **une seule requête** (`findAllById`,
  fin du N+1 par champ) ; clés non-UUID, définitions supprimées entre-temps et définitions
  d'une autre entité sont ignorées sans abandonner la sauvegarde en bloc
- **Web** : `useCustomFieldForm` expose `readOnlyFieldIds` (calculé sur `roles_ecriture` +
  rôle actif) ; le renderer désactive ces champs avec la mention « (lecture seule) », les
  exclut de la validation obligatoire et du payload envoyé — plus aucune perte silencieuse
  de saisie côté interface
- **Tests** : `CustomFieldServiceTest` étendu (11 tests — lecture seule, désactivé, autre
  entité, UUID invalide, batch, valeurs vides) ; `SoulCustomFields.test.tsx` +1 (champ
  lecture seule désactivé et exclu de la validation/sauvegarde) ; tests pré-existants
  `PermissionServiceTest`/`UserServiceTest` corrigés (sémantique permissive documentée +
  mock `findAllById` manquant)
- **Validation réelle** : migration exécutée sur PostgreSQL 16 (Flyway V39 `success=true`)
  + rejeu manuel idempotent vérifié (comptes inchangés : 11 définitions, 23 valeurs, 4 rôles) —
  Backend : **260 tests Maven ✓** · Frontend : **93 tests vitest ✓**, `tsc` propre

## [3.9.0] - 2026-08-07

### 🔎 Journal d'audit — filtres utilisateur + plage de dates, export CSV

**Principe** : le pasteur/administrateur ne navigue plus dans un journal brut — il filtre
par utilisateur, par entité et sur une plage de dates (serveur, paginé), et exporte le
résultat en CSV pour archivage ou analyse externe.

### ⚙️ Backend
- **`AuditLogRepository.findFiltered`** : requête combinée `@Query` — chaque critère est
  optionnel (`utilisateurId`, `entiteType`, `debut`, `fin` en `LocalDateTime`) ; les
  filtres se cumulent (avant : logique exclusive `if/else` entre utilisateur et entité)
- **`AuditService`** : `findFiltered(...)` (délégation) + `exportCsv(...)` — parcours
  paginé de toutes les pages (plafonné à 50 000 lignes), **BOM UTF-8** pour Excel,
  séparateur `;`, champs entre guillemets (échappement des `"`), résolution des **emails**
  des utilisateurs (`UserRepository.findAllById`, une seule requête) et détail
  ancienne/nouvelle valeur inclus dans la colonne Détails
- **`AuditController`** : `GET /api/v1/audit` accepte désormais `debut`/`fin`
  (`@DateTimeFormat(iso = DATE_TIME)`) en plus de `utilisateurId`/`entiteType` ;
  **nouveau `GET /api/v1/audit/export`** (`text/csv`, `Content-Disposition` attachment,
  mêmes filtres) — accès ADMIN/PASTEUR, journal toujours immuable (lecture seule)

### 🗂️ Web
- **`AuditPage`** : nouveau sélecteur « Tous les utilisateurs » (liste `/users?size=200`),
  deux champs date « Du → Au » (validation croisée min/max), reset des filtres
  (« Réinitialiser » visible dès qu'un filtre est actif)
- **Noms réels dans le tableau** : la colonne Utilisateur affiche le nom complet résolu
  via la liste des utilisateurs (fallback email puis id tronqué)
- **Bouton « Exporter CSV »** (en-tête) : téléchargement via blob des journaux filtrés
  (`journal-audit-YYYY-MM-DD.csv`), avec état de chargement

### 🧪 Tests
- **Nouveau `AuditServiceTest`** (5 tests) : délégation des critères combinés, critères
  null, CSV avec BOM/en-tête/emails, fallback UUID sans utilisateur résolu, export vide =
  en-tête seul
- **Nouveau `AuditPage.test.tsx`** (6 tests) : rendu titre/entrées/filtres, nom résolu,
  filtres utilisateur+entité (params API), plage de dates (ISO `debut`/`fin`), reset,
  export CSV avec les filtres appliqués (blob + click simulé)
- Backend : suite Maven ✓ (dont nouveau `AuditServiceTest`) · Frontend : **82 tests
  vitest ✓** (76 + 6), `tsc` propre

## [3.8.0] - 2026-08-07

### 🏠 Page d'accueil glassmorphism + thème clair/sombre de la connexion + redesign Audit

### 🏠 Page d'accueil publique (`/`)
- **Nouvelle `LandingPage`** : héros avec titre dégradé, particules animées, glows, cartes de
  fonctionnalités en verre (glassmorphism), section stats, CTA final et footer verre — fond
  adaptatif clair/sombre avec toggle de thème
- **`HomeGate`** (App.tsx) : `/` affiche la landing si visiteur non connecté, redirige vers
  `/dashboard` si déjà authentifié

### 🌗 Mode clair / sombre des pages d'authentification
- **Nouveau hook partagé `useTheme`** (extrait du pattern Navbar : `localStorage.darkMode` +
  classe `dark` sur `<html>`) — réutilisé par `Navbar` (refactor) et les pages auth
- **`AuthLayout`** : fond, particules et carte verre adaptatifs (`bg-white/80` clair /
  `bg-gray-900/70` sombre) + **toggle Soleil/Lune** en haut à droite (persisté)
- **`LoginPage`**, **`ForgotPasswordPage`**, **`ResetPasswordPage`** : classes adaptatives
  (inputs, labels, textes, divider, carte comptes de démo) — plus rien de codé en dur en sombre

### 📋 Redesign du Journal d'audit (menu Pasteur → Administration)
- **Nouveau design glassmorphism** : en-tête avec badge « Immuable », 4 cartes de statistiques
  (total + créations/modifications/suppressions de la page), barre de filtres (recherche +
  sélecteur d'entité) dans une carte verre
- **Tableau stylé** : badges colorés par catégorie d'action (Création vert / Modification bleu /
  Suppression rouge avec icônes), avatars utilisateur, libellés d'entité en français, état vide
  élégant, pagination avec icônes et compteur
- **Recherche client-side** (le backend `/audit` ne filtre pas par texte : il n'accepte que
  `page`/`size`/`utilisateurId`/`entiteType`) — filtrage sur action, utilisateur, entité, détails

### 🧪 Tests
- **Nouveau `LandingPage.test.tsx`** (6 tests) : rendu du héros, boutons de connexion +
  fonctionnalités, lien `/login`, **bascule du thème clair/sombre sur `<html>`** (landing),
  toggle de l'AuthLayout avec LoginPage, restauration du thème persisté au montage
- Frontend : **76 tests vitest ✓** (70 + 6), `tsc` propre — aucun test existant cassé
  (Navbar refactorée vers `useTheme` sans changement de comportement)

### 🔗 Liens de test en local
- Frontend : `http://localhost:5173` · Backend : `http://localhost:8080` ·
  Swagger : `http://localhost:8080/swagger-ui.html` (ADMIN/PASTEUR) ·
  Health : `http://localhost:8080/actuator/health`
- Comptes de démo (`password123`) : `pasteur@discipolat.com`, `responsable@discipolat.com`,
  `chef@discipolat.com`, `faiseur@discipolat.com`, `membre@discipolat.com`, `admin@discipolat.com`
- Docker : `docker compose up -d` (frontend :3000, API :8081) · Dev : `scripts/start-dev.sh`

## [3.7.6] - 2026-08-07

### 🌐 Ouverture des documents dans un vrai navigateur (url_launcher) sur mobile

**Principe** : fin du SnackBar « Lien: … » — le tap sur une pièce jointe (chips) ou
sur l'icône de téléchargement de l'écran Documents ouvre désormais le document dans le
**navigateur externe** via `url_launcher` (`LaunchMode.externalApplication`). La SnackBar
n'apparaît plus qu'en cas d'échec (URL invalide, aucune app capable de l'ouvrir, plugin
indisponible) avec l'URL affichée pour ne pas laisser l'utilisateur sans retour.

### ⚙️ Mobile
- **Nouvelle dépendance `url_launcher: ^6.3.1`** (+ `url_launcher_platform_interface` en
dev pour les tests)
- **`open_url.dart` (`showUrlLink`)** réécrit : parse l'URI, `launchUrl` en mode
`externalApplication`, gestion d'échec → SnackBar avec l'URL (chemin unique : retour
`false` du lanceur ou exception ramenés à un seul test après `context.mounted`,
point de revue)
- **`documents_screen`** : `_openUrl` délègue au helper partagé (plus de copie du
pattern SnackBar)
- **Android** : `<queries>` ajouté avec `ACTION_VIEW` + schéma `https` (visibilité des
navigateurs sur Android 11+, requise par url_launcher)

### 🧪 Tests
- `member_requests_screen_test` : nouveau fake `_FakeUrlLauncherPlatform` (étend
`UrlLauncherPlatform`, enregistre les URLs, peut simuler un échec) — le test « tap »
vérifie désormais que `launchUrl` est appelé avec la bonne URL **et** qu'aucune SnackBar
n'apparaît ; nouveau test d'échec → SnackBar « Impossible d'ouvrir le lien: … »
- **Hygiène de test (points de revue)** : `canLaunch` non utilisé retiré du fake ;
l'instance statique `UrlLauncherPlatform` est restaurée en `tearDown` (pas de fuite
vers les autres fichiers de test)
- Mobile : **25 tests widget ✓**, `flutter analyze` sans nouvelle issue (seule reste
l'info pré-existante `DropdownButtonFormField value:` sur documents_screen, ligne non touchée)

## [3.7.5] - 2026-08-07

### 🧪 Tests widget mobile — chips de pièces jointes sur l'écran Demandes

**Principe** : l'affichage et l'interaction des chips de pièces jointes (`AttachmentChips`)
sur `member_requests_screen` sont désormais couverts par des tests widget — y compris
le tap qui ouvre le lien du document.

- **`member_requests_screen`** : ajout de l'injection optionnelle `apiService`
  (pattern de testabilité déjà utilisé par SoulDetail/Pastoral360/DepartmentReport,
  aucun changement de comportement en production)
- **Nouveau `mobile/test/member_requests_screen_test.dart`** (4 tests) :
  - chips des pièces jointes affichées sur « Mes demandes » (deux documents nommés) ;
  - aucune chip quand la demande n'a pas de pièces jointes (liste vide / clé absente) ;
  - **tap sur une chip → SnackBar « Lien: … »** avec l'URL du document (`showUrlLink`, pump
    borné pour ne pas attendre la disparition de la SnackBar) ;
  - chips des demandes reçues visibles dans l'onglet « Reçues »

### ✅ Validation
- Mobile : **24 tests widget ✓** (20 + 4 nouveaux), `flutter analyze` sans nouvelle issue

## [3.7.4] - 2026-08-07

### 📄 Pièces jointes affichées sur le rapport du département et le dossier Pastoral 360°

**Principe** : les pièces jointes enregistrées (module Fichiers) sont désormais visibles
aussi là où elles servent de preuve/support : le rapport hebdomadaire du département
(responsable) et le dossier Pastoral 360° (documents des rapports de suivi de l'âme).

### ⚙️ Backend
- **`DepartmentService.getDepartmentReport`** : chaque famille du `statsParFamille` expose
  désormais `piecesJointes` (documents liés au rapport de famille, `FAMILY_REPORT`) —
  liste vide si non soumis/aucune pièce
- **`SoulService.getPastoral360`** : nouvelle section `piecesJointes` du dossier —
  agrège les documents des rapports de suivi **SOUMIS** de l'âme (`MAKER_REPORT`,
  triés par semaine décroissante), avec le contexte `source` (« Rapport du … »)
  — les brouillons ne fuient pas dans le dossier (point de revue)
- **`MakerReportRepository`** : nouvelle méthode `findAllByAmeIdAndSoumisTrueOrderBySemaineDesc`

### 🗂️ Web
- **`DepartmentReportPage`** : nouvelle colonne « Pièces » dans le tableau par famille
  (composant partagé `AttachmentLinks`, liens cliquables)
- **`Pastoral360Page`** : nouvelle carte « Pièces jointes » (compteur + liens cliquables
  vers les documents, contexte « Rapport du … » affiché sous chaque document via
  `AttachmentLinks sourceKey`)

### 📱 Mobile
- **`department_report_screen`** : chips `AttachmentChips` sous les stats de chaque carte
  famille (pièces non vides)
- **`pastoral_360_screen`** : nouvelle carte « PIÈCES JOINTES · N » avec chips cliquables
  (contexte source via `AttachmentChips sourceKey`)

### 🧪 Tests
- Backend : `DepartmentServiceTest` +1 (piecesJointes par famille), `SoulServiceTest` +1
  (agrégation des pièces du dossier) — **227 tests Maven ✓**
- Web : nouveau `DepartmentReportPage.test.tsx` (4 tests : colonne Pièces, liens cliquables,
  famille sans pièce sans lien) ; `Pastoral360Page.test.tsx` +1 (carte Pièces jointes +
  `href`/`target`) — **70 tests vitest ✓**, `tsc` propre
- Mobile : `parameterized_routes_test.dart` enrichi (mock pièces 360 + rapport département,
  assertions chips) — **20 tests widget ✓**, `flutter analyze` sans nouvelle issue

## [3.7.3] - 2026-08-07

### 🔗 Pièces jointes visibles et cliquables sur les écrans de détail/liste (web + mobile)

**Principe** : les pièces jointes enregistrées (module Fichiers) ne restent plus dans les
formulaires — elles sont affichées **en lecture seule avec liens cliquables** sur les
écrans qui les exposent : rapports soumis, événements, demandes membres.

### 🗂️ Web
- **Nouveau composant partagé `AttachmentLinks`** : chips cliquables (nom + trombone,
  `href` vers le document, `target=_blank`) — réutilisé partout
- **`MakerReportPage`** : rapport soumis → liens en lecture seule (le sélecteur ne reste
  actif que sur un rapport non soumis)
- **`FamilyReportPage`** : rapport SOUMIS → liens en lecture seule au lieu du sélecteur
- **`EventsPage`** : nouvelle colonne « Pièces » dans le tableau (liens par événement)
- **`MemberRequestsPage`** : liens sur les cartes de la boîte de réception
- **`MemberDashboardPage`** : « Mes demandes » — le compteur devient les liens réels
- **`TransferDetailPage`** : la liste des pièces devient cliquable (alignée sur le
  composant partagé)

### 📱 Mobile
- **Nouveau helper `showUrlLink`** (même pattern que l'écran Documents : SnackBar avec
  l'URL, sans plugin) — réutilisable
- **Nouveau widget partagé `AttachmentChips`** (chips nom + trombone, tap = lien) —
  utilisé par **`member_requests_screen`** (demandes envoyées et reçues) et
  **`events_list_screen`** (cartes d'événements), sans duplication

### 🔧 Corrections de revue
- **`FamilyReportPage`** : les statuts `VU_PAR_RESPONSABLE` / `VU_PAR_PASTEUR` sont aussi
  des rapports déjà soumis → liens en lecture seule (pas de sélecteur ré-éditable)
- **Mobile** : extraction du widget partagé `AttachmentChips` (le bloc chips était
  copié-collé entre les deux écrans)

### 🧪 Tests
- `EventsPage.test` +1 : la colonne « Pièces » rend des liens cliquables (`href`/`target`) ;
  le test d'édition passe à `getAllByText` (le document apparaît désormais dans la colonne
  ET dans le picker)
- `FamilyReportPage.test` +1 : rapport SOUMIS → lien cliquable + sélecteur absent
- Mobile : `flutter analyze` sans nouvelle issue · Frontend : **65 tests vitest ✓**, `tsc` propre

## [3.7.2] - 2026-08-07

### 🧪 Tests du sélecteur de pièces jointes partagé (web + mobile)

- **Mobile — nouveau `test/attachment_picker_field_test.dart`** (5 tests widget) :
  ouverture du sélecteur avec la liste des documents du module Fichiers (fake ApiService),
  sélection multi + validation → `onChanged` reçoit les ids, décocher retire la pièce,
  **création directe d'un document** (dialogue → POST `/files` avec le contrat réel
  `chemin`/`typeFichier` → ajout automatique à la sélection), et état pré-sélectionné
  (« Modifier les documents »). Le `selected` partagé est muté en place (comme les écrans
  réels : `clear()..addAll(ids)`)
- **Web — nouveau `test/EventsPage.test.tsx`** (3 tests) : section « Pièces jointes » dans
  le formulaire, création d'événement envoyant les `fichierIds` cochés (POST `/events`),
  édition pré-remplie depuis `piecesJointes` (PUT `/events/{id}` avec `fichierIds`)
- **Web — nouveau `test/FamilyReportPage.test.tsx`** (2 tests) : sélecteur visible après
  sélection d'une famille, pré-chargement des pièces du rapport backend et renvoi à la
  soumission (POST `/reports/family-weekly` avec `fichierIds`)
- **Correction du harness mobile** (bug trouvé par le test) : le helper `pumpPicker`
  renvoyait le `Set` par valeur alors que `onChanged` réassignait la variable — les tests
  de sélection échouaient à tort alors que le widget était correct (vérifié par
  instrumentation) ; la mutation en place résout le piège

### 🧪 Validation
- Mobile : **20 tests widget ✓** (15 existants + 5 nouveaux), `flutter analyze` sans issue
  sur les fichiers touchés
- Frontend : **63 tests vitest ✓** (58 + 5 nouveaux), `tsc` propre

## [3.7.1] - 2026-08-07

### 🐛 Fix module Documents — alignement sur le contrat backend réel (`typeFichier`/`chemin`)

**Principe** : la création de document via le module Documents (web + mobile) envoyait
`url`/`typeMime`, des noms que le backend ne connaît pas (`CreateFileRequest` attend
`typeFichier`/`chemin`) → toute création échouait (400). La création directe des pickers
de pièces jointes utilisait déjà les bons noms ; le module Documents est désormais aligné.

- **Web** : types `FileEntity`/`CreateFileRequest` (`url`/`typeMime` → `chemin`/`typeFichier`),
  formulaire `DocumentsPage` (état initial, champ chemin, champ type MIME, validation) et
  lien de téléchargement du tableau (`file.chemin`)
- **Mobile** : `documents_screen` — payload POST `/files` (`chemin`/`typeFichier`),
  contrôleur renommé `_cheminCtrl`, ouverture du lien de téléchargement depuis `chemin`

### 🧪 Validation
- Frontend : **58 tests vitest ✓**, `tsc` propre · Mobile : `flutter analyze` sans nouvelle issue

> ✅ Le « Note contrat » du 3.6.0 est résolue : plus aucun formulaire n'utilise `url`/`typeMime`.

## [3.7.0] - 2026-08-07

### 🔗 Réutilisation du sélecteur de pièces jointes multi-documents (rapports, demandes membres, événements — web + mobile)

**Principe** : fin du sélecteur réservé aux transferts. Un **mécanisme unique de pièces
jointes** (table de liaison générique + sélecteur partagé) est désormais utilisé par
**tous** les formulaires : rapports faiseur/famille, demandes membres, événements — et
tout futur module — au lieu de systèmes parallèles.

### ⚙️ Backend — liaison générique `entity_attachments` (migration V34)
- **Nouvelle table `entity_attachments`** : une seule mécanique pour toutes les entités
  (type + id d'entité + fileId + uploadedBy) — réutilisable sans nouvelle table par module
- **Nouveau `EntityAttachmentService`** (module `files`) : `replace(entityType, entityId,
  fichierIds)` en remplacement complet (delete + relink, comme les transferts) et
  `itemsFor(...)` → `AttachmentItem { id, fileId, nom, url }` exposé en `piecesJointes`
- **Rapports** : `SubmitMakerReportRequest` / `SubmitFamilyReportRequest` + `fichierIds`,
  liaison dans `submit`/`draft`, `piecesJointes` dans `MakerReportResponse`/
  `FamilyReportResponse` (tous les endpoints de lecture)
- **Demandes membres** : `CreateMemberRequest` + `fichierIds`, liaison dans
  `MemberService.createRequest`, `piecesJointes` dans `MemberRequestResponse`
- **Événements** : `CreateEventRequest`/`UpdateEventRequest` + `fichierIds`, liaison dans
  `create`/`update`, `piecesJointes` dans `EventResponse` (y compris le programme hebdo)
- **Tests** : nouveau `EntityAttachmentServiceTest` (4 tests : remplacement complet,
  ordre conservé, liste vide = tout retirer, fichier inexistant refusé) + mise à jour
  de `ReportServiceTest`/`EventServiceTest`/`MemberPresenceSheetTest` (nouveaux
  constructeurs/signatures) — Backend : **225 tests Maven ✓**

### 🗂️ Web
- **`TransferAttachmentPicker` renommé `AttachmentPicker`** (composant partagé générique,
  création directe de document incluse) — les pages transfert (création + détail)
  l'importent sous son nouveau nom, l'ancien composant est supprimé
- **`MakerReportPage`** : section « Pièces jointes » par âme (enrichit `ReportFormData` +
  brouillon localStorage) → `fichierIds` envoyé au POST, pièces existantes pré-chargées
- **`FamilyReportPage`** : picker dans la carte synthèse → `fichierIds` au POST, pièces
  existantes rechargées quand le rapport change
- **`MemberDashboardPage`** : picker dans le formulaire « Suggestions, rendez-vous &
  signalements » → `fichierIds` au POST `/members/me/requests` ; compteur de pièces
  jointes sur « Mes demandes »
- **`EventsPage`** : picker dans les formulaires création/édition → `fichierIds` au
  POST/PUT, pré-remplissage à l'édition depuis `piecesJointes`
- **Types** : `piecesJointes` sur `MakerReport`/`FamilyReport`/`MemberRequest`/`Evenement`
  + `fichierIds` sur les requêtes de création/soumission

### 📱 Mobile
- **Nouveau widget partagé `AttachmentPickerField`** (chargement lazy de `GET /files`,
  dialogue de sélection multi pré-cochée, chips retirables, création directe via
  `document_create_dialog`, bouton « Valider la sélection »)
- **`member_requests_screen`** : picker dans la feuille « Nouvelle demande » →
  `fichierIds` au POST
- **`events_list_screen`** (`_CreateEventSheet`) : picker dans la feuille de création →
  `fichierIds` au POST
- **`maker_report_screen`** : section « Pièces jointes » par âme — `fichierIds` transmis
  via `SyncService.saveReportLocally` (payload en ligne **et** file d'attente hors-ligne,
  aucune perte à la synchronisation)

### 🔧 Corrections de revue
- **Bug « retirer toutes les pièces » (web)** : `FamilyReportPage` et `EventsPage` (update)
  transformaient une liste vide en `undefined` → le backend (`replace` à sémantique
  « null = ne pas toucher ») conservait les anciennes pièces. Envoi du tableau brut
  (`fichierIds: fichierIds`) : liste vide = tout retirer, cohérent avec MakerReportPage
  et les transferts
- **Mobile `maker_report_screen`** : section pièces jointes masquée une fois le rapport
  soumis (cohérence avec les autres champs désactivés)
- **Mobile `member_requests_screen`** : feuille « Nouvelle demande » enveloppée dans un
  `SingleChildScrollView` (évite le débordement avec clavier ouvert + picker)

### 🧪 Validation
- Backend : **225 tests Maven ✓** · Frontend : **58 tests vitest ✓**, `tsc` propre
- Mobile : `flutter analyze` sans nouvelle issue (seuls restent les warnings/info pré-existants)

## [3.6.0] - 2026-08-07

### ➕ Création directe d'un document depuis le formulaire de transfert (web + mobile)

**Principe** : fin de l'aller-retour vers le module Documents — le demandeur crée un
document et le sélectionne en une étape, depuis le sélecteur de pièces jointes de la
demande de transfert (création et édition de brouillon).

### 🗂️ Web
- **`TransferAttachmentPicker`** (composant partagé) : bouton « Créer un document »
  (ouvert ou dans le sélecteur) → formulaire inline (nom, URL/chemin, type MIME,
  catégorie, taille) → POST `/files` avec le **contrat backend réel** (`typeFichier`,
  `chemin`, `categorie`) → le document créé est **ajouté automatiquement à la sélection**
  puis la liste est invalidée (react-query)

### 📱 Mobile
- **Nouveau widget partagé `document_create_dialog`** : dialogue de création (nom, URL,
  catégorie, type MIME, taille) → POST `/files` → retourne l'id du fichier créé
- Intégré au dialogue de sélection des pièces jointes du **create screen** et du
  **détail** : « Créer un document » ajoute le fichier à la sélection courante et
  recharge la liste des documents
- **Accès création même liste vide** (point de revue) : le bouton « Choisir » du create
  screen et le dialogue du détail restent accessibles quand le module Fichiers est vide
  — le bouton « Créer un document » gère ce cas (feedback si nom/URL manquants)

### 🧪 Validation
- Frontend : **58 tests vitest ✓**, `tsc` propre
- Mobile : `flutter analyze` sans nouvelle issue (seuls restent les `info`/warnings pré-existants)

> ℹ️ **Note contrat** : la création directe utilise le contrat backend réel du module
> Fichiers (`typeFichier`, `chemin`, `categorie`) — le module Documents web/mobile
> envoyait encore `url`/`typeMime` (noms non reconnus par le backend) ; **corrigé en
> 3.7.1** (alignement complet sur le contrat).

## [3.5.0] - 2026-08-07

### ✏️ Modification des pièces jointes d'un brouillon de transfert (détail web + mobile)

**Principe** : le demandeur peut désormais ajouter/retirer les pièces jointes d'une demande
en brouillon directement depuis l'écran de détail — le backend (PUT `/transfers/{id}` +
`fichierIds`) existait déjà : il **remplace la liste complète** des liaisons
(`deleteByTransferRequestId` + relink) et refuse toute demande hors BROUILLON.

### 🗂️ Web
- **Nouveau composant partagé `TransferAttachmentPicker`** (`components/shared`) : sélecteur
  multi de documents (module Fichiers) avec pré-sélection, chips retirables et compteur —
  réutilisé par le formulaire de création (refactor de la section inline) et par le détail
- **`TransferDetailPage`** : bouton « Modifier » sur la carte Pièces jointes (visible
  uniquement pour le demandeur d'une demande en BROUILLON) → picker pré-rempli avec les
  pièces actuelles → PUT `/transfers/{id}` (liste complète, vide = tout retirer)

### 📱 Mobile
- **`transfer_detail_screen`** : bouton « Modifier » (brouillon + demandeur) → dialogue de
  sélection multi pré-cochée (documents du module Fichiers) → PUT de remplacement puis
  rechargement ; message clair si aucun document n'existe dans le module

### 🧪 Tests
- `TransferWorkflowServiceTest` +2 : remplacement complet des pièces jointes d'un brouillon
  (delete + relink, ordre conservé) et refus de mise à jour hors BROUILLON (BusinessRule)
- Backend : **221 tests Maven ✓** · Frontend : **58 tests vitest ✓**, `tsc` propre
- Mobile : `flutter analyze` sans nouvelle issue

## [3.4.0] - 2026-08-07

### 📎 Pièces jointes dans le formulaire de demande de transfert (web + mobile)

**Principe** : les pièces jointes ne sont plus réservées au détail — le demandeur sélectionne
directement les documents (module Fichiers) lors de la création d'une demande de transfert.
Le backend acceptait déjà `fichierIds` (`linkFiles` à la création) et le détail les affichait
déjà ; seuls les formulaires de création ne les envoyaient pas.

### 🗂️ Web
- **`TransferCreatePage`** : section « Pièces jointes » — sélecteur multi de documents
  (`GET /files`) avec checkboxes, chips des fichiers sélectionnés (retirable un par un),
  compteur de pièces jointes et `fichierIds` envoyé dans le POST `/transfers`

### 📱 Mobile
- **`transfer_create_screen`** : rangée « Pièces jointes » avec compteur, dialogue de
  sélection multi (checkboxes sur les documents du module Fichiers), liste des fichiers
  attachés (retirables via l'icône ✕) et `fichierIds` envoyé dans la création

### 🧪 Validation
- Frontend : **58 tests vitest ✓**, `tsc` propre
- Mobile : `flutter analyze` sans nouvelle issue (seuls restent les `info` pré-existants)

## [3.3.0] - 2026-08-07

### ⏰ Alerte automatique des délais de traitement dépassés (transferts)

**Principe** : un job planifié surveille les demandes de transfert toujours en attente de
validation après leur `delaiLimite` et alerte le pasteur — sans re-notifier une demande
déjà signalée.

### ⚙️ Backend
- **Nouveau job `ScheduledJobs.checkTransferDelays()`** (cron configurable
  `app.scheduler.transfer-delay-check-cron`, par défaut toutes les heures) :
  - requête `findByStatutInAndDelaiLimiteBefore` sur les demandes
    `EN_ATTENTE_VALIDATION` / `VALIDATION_PARTIELLE` dont le délai est dépassé ;
  - notification `TRANSFERT_DELAI_DEPASSE` (IN_APP) à **tous les pasteurs**
    (`findByRolesContaining(PASTEUR)`, multi-rôles inclus) avec l'id de la demande
    en référence (`TRANSFER`) ;
  - **déduplication** `existsByDestinataireIdAndTypeAndEntiteReferenceIdAndEntiteReferenceType`
    : une seule notification par demande et par pasteur (one-shot, pas de spam à chaque
    passage — choix documenté dans le job) ; une notification qui échoue ne bloque pas
    les autres (try/catch par destinataire, comme `TransferWorkflowService.notifyUser`)
- **Nouveau type** `TypeNotification.TRANSFERT_DELAI_DEPASSE`

### 🗂️ Web & 📱 Mobile
- Type `TypeNotification` frontend complété ; centre de notifications mobile : couleur
  `deepOrange` + icône sablier pour `TRANSFERT_DELAI_DEPASSE`

### 🧪 Tests
- **Nouveau `ScheduledJobsTest`** (6 tests) : aucun retard → rien, retard notifié au
  pasteur, validation partielle en retard, déduplication (pas de re-notification),
  absence de pasteur → rien, N demandes × M pasteurs = N×M notifications
- Backend : **219 tests Maven ✓** · Frontend : **58 tests vitest ✓**, `tsc` propre
- Mobile : `flutter analyze` sans nouvelle issue

## [3.2.0] - 2026-08-07

### 🔗 Pont d'intégration — les transferts existants passent par le moteur de workflow

**Principe** : fin des deux systèmes parallèles. Les anciennes mutations directes
`transferFaiseur`, `reassignChef` et `reassign` (âme) ne modifient plus les données —
elles créent et soumettent une **demande de transfert** au moteur de workflow configurable.
Si le circuit de validation est vide, l'exécution reste automatique et immédiate ; sinon
la demande attend les validations paramétrées par le pasteur puis s'exécute seule.

### ⚙️ Backend
- **Nouveau `TransferBridgeService`** : pont unique vers `TransferWorkflowService`
  (create + submit) pour les 3 opérations héritées :
  - `transferFaiseur` → `FAISEUR_FAMILLE_TRANSFERT` (règle d'exécution `transfererAmes`
    transmise à la demande, fusionnée avec la config au moment de l'exécution)
  - `reassignChef` → `CHEF_FAMILLE_TRANSFERT`
  - `reassignSoul` → `FAISEUR_DISCIPLE_CHANGEMENT`
- **Règles d'exécution par demande** : `CreateTransferRequest.reglesExecution` + colonne
  `regles_execution` sur `transfer_requests` (migration V33) — la demande écrase la config
  pour les clés qu'elle fournit, la config reste la source pour les autres
- **Alignement des rôles initiateurs** : `CHEF_FAMILLE_TRANSFERT` accepte désormais
  `CHEF_DE_FAMILLE` par défaut (l'ancien endpoint `/families/{id}/chief` y était ouvert)
- **Endpoints réécrits** (retour = `TransferResponse`, plus l'entité) :
  `PATCH /users/{id}/transfer`, `PATCH /families/{id}/chief`, `PATCH /souls/{id}/reassign`
  — permissions `@PreAuthorize` conservées, contrôle réel par rôle ACTIF + config du workflow
- **Anti-IDOR préservé** : le pont réapplique le scoping par espace métier
  (`WorkspaceScopeService.canAccessFamily/Soul/Faiseur`) que les anciens `findById`
  scopés garantissaient — un chef ne change que le chef de SA famille, un responsable
  ne réaffecte que les âmes de SES départements (vers un faiseur de son espace)
- **Suppression des mutations directes** : `UserService.transferFaiseur`,
  `FamilyService.reassignChef`, `SoulService.reassign` (l'exécution est désormais
  exclusivement assurée par `TransferExecutor` avec historiques + notifications)

### 🗂️ Web & 📱 Mobile
- `UsersPage` et `FamilyDetailPage` (web) et `users_list_screen` (mobile) : affichent le
  retour du workflow — « Faiseur transféré / Chef mis à jour » si la demande a été exécutée
  immédiatement, sinon « Demande de transfert soumise pour validation »

### 🧪 Tests
- **Nouveau `TransferBridgeServiceTest`** (10 tests) : type/personne/règles corrects pour
  chaque opération, soumission exactement une fois, passage par le workflow, et
  **anti-IDOR** (famille hors espace refusée, âme hors espace refusée, faiseur cible
  hors espace refusé, cas légitimes autorisés)
- `TransferWorkflowServiceTest` étendu (fusion config + règles de la demande) ;
  `SoulServiceTest` aligné (constructeur sans `NotificationService`)
- Backend : **213 tests Maven ✓** · Frontend : **58 tests vitest ✓**, `tsc` propre, build ✓
- Mobile : **15 tests widget ✓**, `flutter analyze` sans nouvelle issue

## [3.1.0] - 2026-08-07

### 🔄 Workflow intelligent et configurable des transferts (migration V32)

**Principe** : les transferts ne sont plus de simples actions techniques — ils suivent un
**workflow métier** entièrement configurable par le pasteur, sécurisé et historisé. Le
nombre d'étapes, les validateurs et les règles évoluent **sans modification de code**.

### ⚙️ Moteur de workflow (backend — module `transfers`)
- **Circuit de validation piloté par la base** : `transfer_workflow_configs`
  (types autorisés, rôles initiateurs, mode SEQUENTIEL/PARALLELE/N_VALIDATIONS_REQUISES,
  nombre requis, délais, notifications auto, modèles de messages, règles d'exécution JSON)
  + `transfer_workflow_steps` (étapes ordonnées, rôles validateurs, caractère requis) —
  **9 types de transfert seedés** par défaut (départements ×3, familles ×3, affectations ×3)
- **Cycle de vie complet** : BROUILLON → SOUMIS → EN_ATTENTE_VALIDATION →
  VALIDATION_PARTIELLE → VALIDE → **EXECUTE** (ou REFUSE / ANNULE), puis ARCHIVE — chaque
  transition est historisée (`transfer_history`, immuable) et auditiée
- **Demande de transfert** (`transfer_requests`) : type, personne concernée, affectations
  actuelle/nouvelle (JSONB), demandeur, justification, priorité, commentaires, délai limite
- **Décisions motivées** (`transfer_decisions`) : approbation, refus, demande
  d'informations, renvoi pour correction — rôles non habilités → 403, double validation
  bloquée
- **Exécution automatique** (`TransferExecutor`) : mise à jour des relations (souls,
  familles, départements, `soul_departments`, `member_departments`, `user_departments`),
  historiques métier (`soul_history`, `family_chief_history`), notifications à toutes les
  personnes concernées — aucune étape manuelle après la validation finale
- **Paramétrage pasteur** (`/api/v1/admin/transfers/workflows`) : CRUD complet des
  configurations et étapes, activation/désactivation, suppression protégée
- **Notifications** sur chaque changement d'état (types `TRANSFERT_*`) ; journal d'audit
  systématique ; visibilité scopée par rôle actif (demandeur, personne concernée, validateur)
- **API** : `GET/POST /api/v1/transfers`, `PUT /{id}`, `POST /{id}/submit|decide|cancel|archive`,
  `GET /{id}/history|decisions`, `GET /transfers/configurations`

### 🗂️ Web
- **`TransfersPage`** (`/transfers`) : liste filtrable (statut/type) avec progression des
  validations, soumission et annulation
- **`TransferDetailPage`** (`/transfers/:id`) : circuit de validation visuel, décisions,
  timeline d'historique, pièces jointes, actions de validation motivées
- **`TransferCreatePage`** (`/transfers/new`) : formulaire dynamique par type de transfert
  (personne + cible selon le type), circuit affiché, soumission immédiate optionnelle
- **`TransferAdminPage`** (`/admin/transfers`, ADMIN/PASTEUR) : éditeur complet du
  workflow (étapes réordonnables, rôles, mode, délais, modèles de messages, règles JSON)
- Navigation : section « Transferts » ajoutée aux espaces concernés

### 📱 Mobile (Flutter)
- Écrans **Transferts** (liste avec filtres + FAB), **détail** (circuit, décisions,
  historique, dialogue de décision motivée), **création** (formulaire dynamique) et
  **administration** du workflow — routes et gardes de rôle ajoutées, entrées du drawer
  par espace métier, types de notification `TRANSFERT_*` colorés dans le centre de notifications

### 🧪 Tests
- **Nouveau `TransferWorkflowServiceTest`** (12 tests) : création/historisation,
  exécution immédiate sans circuit, soumission avec circuit, approbation partielle,
  approbation finale → exécution automatique, refus, renvoi pour correction + re-soumission,
  rôle non habilité refusé, double validation bloquée, annulation, archive
- Backend : **201 tests Maven ✓** · Frontend : **58 tests vitest ✓**, `tsc` propre, build ✓
- Mobile : **15 tests widget ✓**, `flutter analyze` sans nouvelle issue

## [3.0.9] - 2026-08-06

### 🔒 Scoping des rapports et événements par rôle actif (suite de 3.0.8)

**Principe** : mêmes règles d'isolation que pour les familles (3.0.8) appliquées aux
rapports hebdomadaires (faiseur + famille) et aux événements. Les filtres explicites
(`faiseurId`, `familleId`) ne sont plus des ancres de confiance (anti-IDOR).

### 🧩 Nouveau service partagé `WorkspaceScopeService`
- **Une seule source de vérité** pour l'intersection des données avec l'espace métier
  courant : `accessibleSoulIds()`, `accessibleFamilyIds()`, `accessibleFaiseurIds()`,
  `canAccessSoul/Family/Faiseur(...)`, `isSuperUser()` — réutilisé par les rapports, les
  événements et le dashboard (évite la duplication des requêtes de scoping)
- Faiseur → ses disciples ; chef → les âmes de sa famille ; responsable → les membres de
  ses départements (via `soul_departments` actifs) ; super-utilisateurs → tout

### 📊 Rapports (MakerReport / FamilyReport / UrgentAid)
- **`findMakerReports`** : un rôle opérationnel ne voit plus que les rapports de son espace
  (intersection faiseur + âme). Les filtres `faiseurId`/`ameId` d'un autre espace renvoient
  **vide** au lieu de fuiter (IDOR fermé)
- **`findMakerReportById`** : accès refusé (403) hors espace — y compris le check
  « soi-même » pour un faiseur
- **`prefill`** : la liste des disciples d'un faiseur n'est plus exposée hors espace
  (faiseur de l'espace ou soi-même uniquement)
- **`submit`/`draft`** : `verifyCanReportFor` refuse de saisir un rapport pour un faiseur
  hors espace (fin de la confiance aveugle au `faiseurId` du payload)
- **`getUrgentAidRequests`** : un RESPONSABLE ne voit plus les demandes d'aide de toute
  l'église — uniquement celles des faiseurs de ses départements
- **Rapports famille** : `findFamilyReports`/`findFamilyReportsByFamily` scopés par
  familles visibles ; `validate` refuse un rapport hors espace (403)
- **Anti-usurpation `chefFamilleId`** : `submitFamilyReport` refuse qu'un rôle
  opérationnel déclare un autre chef de famille que lui-même (le frontend envoie
  `user.id`, le champ fourni n'est plus une ancre de confiance) ; les
  super-utilisateurs conservent la soumission pour le compte du chef
- **`MakerReportController`** : `GET /reports/maker-weekly/{id}` n'avait **aucun
  `@PreAuthorize`** (tout utilisateur authentifié, même MEMBRE, lisait un rapport par ID) →
  garde ajoutée alignée sur les autres endpoints

### 🎉 Événements
- **`findById`** : un événement lié à une famille hors espace → 403 (les événements
  d'église sans famille restent visibles par tous)
- **`findAll`** : pagination en mémoire après filtrage par espace pour les rôles
  opérationnels (familles accessibles précalculées **une seule fois** — pas de requête
  N par événement) ; `findByFamilleId` renvoie une page vide hors espace (pas de fuite)
- **`create`/`update`/`delete`** : `canManageEvent` exige la visibilité de la famille ET
  (organisateur, ou rôle RESPONSABLE/CHEF/PASTEUR/ADMIN dans une famille accessible) — un
  FAISEUR ne peut plus modifier/supprimer n'importe quel événement
- **Inscriptions** (`register`/`unregister`/`markAttendance`) : passent par `findById`
  scopé — impossible de s'inscrire à un événement d'une famille hors espace

### 📈 Dashboard
- **`getReportCompletion`** : les stats globales de complétion ne sont plus exposées aux 5
  rôles — un rôle opérationnel ne voit que le taux de complétion de **son** espace
  (faiseurs de ses départements / de sa famille selon le rôle actif)

### 🧪 Tests
- **Nouveau `WorkspaceScopeServiceTest`** (8 tests) : scoping par rôle actif (faiseur,
  chef, responsable), helpers `canAccess*` et `isSuperUser`
- **Nouveau `ReportServiceTest`** (10 tests) : super-utilisateur voit tout, faiseur scopé,
  filtre hors espace = vide (anti-IDOR), byId 403 hors espace, préfill/submit/validate
  refusés hors espace, aide urgente scopée par département
- **Nouveau `EventServiceTest`** (8 tests) : événement d'église visible par tous, famille
  hors espace refusée (byId/create/update), findAll filtré, famille visible autorisée,
  suppression par l'organisateur autorisée
- `DashboardServiceTest` mis à jour pour le nouveau constructeur ; `@Builder.Default`
  ajouté sur `SoulDepartment.actif` et `Event.nbInscrits` (bug builder → valeurs par
  défaut perdues)

### ✅ Validation
- Backend : **156 tests Maven ✓** (0 échec)
- Frontend : **58 tests vitest ✓**, `tsc` propre
- Mobile : **15 tests widget ✓**

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
