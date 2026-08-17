# PROJECT_PROGRESS — Discipolat (Productisation + Bêta-test public)

> Fichier de checkpoint : état exact du travail à tout moment, pour reprise
> immédiate d'une session. Mis à jour à chaque étape stable.

---

## SESSION 2026-08-17 (bloc 8) — Outil métier COMMUNICATION (V69) — fullstack + mobile

### Backend (V69 — communications + module COMMUNICATION)

- **Migration V69** : table `communications` (titre, contenu, cible CHECK
  TOUS/ROLE/FAMILLE/DEPARTEMENT, rôles JSONB, famille_id, department_id,
  statut CHECK BROUILLON/PUBLIEE/ARCHIVEE, date_publication, auteur, soft
  delete) + module `COMMUNICATION` activable + menu « Annonces » →
  `/communications` pour tous les rôles.
- **`CommunicationService`** : CRUD (soft delete, audit
  COMMUNICATION_CREATED/UPDATED/DELETED/PUBLISHED) ; **publication →
  notifications IN_APP** à chaque destinataire réel : TOUS (utilisateurs
  actifs), ROLE (`findByRolesContaining`), FAMILLE (chefs de famille du
  foyer + âmes de la famille avec compte), DEPARTEMENT (responsable + âmes
  actives avec compte) — compteur `destinataires` renvoyé à la publication.
- **API** `/api/v1/communications` : GET (annonces publiées visibles par
  l'utilisateur courant, cible respectée), GET/POST `/admin`,
  PUT/DELETE `/admin/{id}`, POST `/admin/{id}/publish` — lecture tout rôle
  authentifié, gestion ADMIN/PASTEUR (`@PreAuthorize`) + garde-fou de module
  `ModuleGateFilter` (`/api/v1/communications` → COMMUNICATION, 403 si désactivé).
- **Tests** : `CommunicationServiceTest` **5 ✓** (TOUS → tous les actifs
  notifiés, ROLE → uniquement ce rôle, FAMILLE ciblée à la création,
  DEPARTEMENT → responsable + membres, lecture filtrée par cible) +
  `ModuleGateFilterTest` 3 ✓ — BUILD SUCCESS.

### Frontend web

- **`CommunicationsPage`** (`/communications`, tous rôles, lazy) : état
  explicite si module désactivé ; **gestion ADMIN/PASTEUR** (liste complète,
  badges de statut et de cible, publier + diffuser avec compteur de
  destinataires, modifier, supprimer) ; fil des **annonces publiées visibles
  par l'utilisateur courant** ; modale création/édition (titre, contenu,
  cible TOUS/ROLE/FAMILLE/DEPARTEMENT, chips de rôles, sélecteurs
  famille/département) — **a11y corrigée** (labels htmlFor sur la cible).
- **Route** App.tsx (ProtectedRoute tous rôles) + types Communication
  (cible/statut) + le menu apparaît automatiquement (config plateforme DB).
- **Tests** : `CommunicationsPage.test` **5 ✓** (gestion + fil publié,
  création ciblée → POST, publication → POST publish + compteur, lecture
  seule MEMBRE sans appel `/admin`, état module désactivé).

### Mobile (Flutter) — parité

- **`CommunicationsScreen`** (`/communications`, tous rôles) : gestion
  (statuts, publier / modifier / supprimer avec confirmation) pour
  ADMIN/PASTEUR (AuthState.activeRole), fil des annonces publiées, bottom
  sheet création/édition (cible, rôles, familles/départements chargés à la
  demande), FAB. Entrées « Annonces » dans le drawer de **tous** les rôles +
  route dans la matrice de rôles.
- **Tests** : `communications_screen_test` **4 ✓** — mobile **122 tests ✓**
  (118 → 122, +4), `flutter analyze` 0 issue.

### État des tests (bloc 8)

- Backend : `CommunicationServiceTest` 5 ✓ + `ModuleGateFilterTest` 3 ✓ —
  BUILD SUCCESS (compilation complète OK).
- Frontend : `tsc --noEmit` ✓ · suite complète **219 tests vitest ✓**
  (35 fichiers, 214 → 219) · `npm run build` ✓.
- Mobile : `flutter analyze` **0 issue** · **122 tests ✓**.

### Commit / push

- Commit de ce bloc : Outil métier Communication (V69) — annonces ciblées +
  notifications IN_APP (fullstack + mobile).
- Poussé sur origin/main.

### Prochain objectif

Phase « cohérence/synchronisation » (tests de propagation transversale) puis
QA final — voir ARCHITECTURE_AUDIT.md §9 et la feuille de route §12 ; sinon
outil métier suivant (module activable).

---

## SESSION 2026-08-17 (bloc 7) — Outil métier FINANCES (V68) — fullstack + mobile

### Backend (V68 — finance_transactions + finance_budgets + module FINANCES)

- **Migration V68** : tables `finance_transactions` (type RECETTE/DEPENSE avec
  contrainte CHECK, catégorie, montant ≥ 0, date, soft delete) et
  `finance_budgets` (catégorie + année, montant, unique(categorie, annee)) +
  module `FINANCES` (activables) + menu `finances` → `/finances`
  (ADMIN/PASTEUR).
- **`FinanceService`** : CRUD transactions (soft delete, traçabilité audit
  FINANCE_TRANSACTION_CREATED/UPDATED/DELETED), **statistiques annuelles
  réelles** (totaux recettes/dépenses/solde, séries par mois sur 12 mois,
  répartition par catégorie), **budgets** (upsert par catégorie/année,
  consommation = dépenses réelles de l'année / budget, statut OK/ALERTE/DEPASSE).
- **API** `/api/v1/finances` : GET/POST/PUT/DELETE `/transactions`, GET `/stats`,
  GET/POST/DELETE `/budgets` — `@PreAuthorize ADMIN/PASTEUR` + garde-fou de
  module `ModuleGateFilter` (`/api/v1/finances` → FINANCES, 403 si désactivé).
- **Tests** : `FinanceServiceTest` **5 ✓** (création+audit, filtre type/période,
  séries mensuelles + solde, upsert budget sans doublon, consommation 80 % → ALERTE).

### Frontend web

- **`FinancePage`** (`/finances`, ADMIN/PASTEUR, lazy) : état explicite si module
  désactivé ; KPIs (recettes, dépenses, solde, budgets), **graphique barres
  recettes/dépenses par mois** (recharts), budgets par catégorie avec barre de
  consommation colorée (dépassé/alerte), **CRUD transactions** (modale
  type/catégorie/montant/date/description), filtres type + catégorie, **export
  CSV** client (BOM UTF-8).
- **Route** App.tsx (ProtectedRoute ADMIN/PASTEUR) + types Finances (transaction,
  budget, stats) + le menu apparaît automatiquement (config plateforme DB).
- **Tests** : `FinancePage.test` **3 ✓** (KPIs/liste/graphique, création via
  modale → POST, état module désactivé).

### Mobile (Flutter) — parité

- **`FinanceScreen`** (`/finances`, ADMIN/PASTEUR) : KPIs (recettes/dépenses/
  solde), **chips de filtre par type** (→ GET ?type=), liste des transactions
  avec suppression confirmée, **bottom sheet d'ajout** (type segmenté, catégorie,
  montant validé, description, date picker) → POST. Drawer ADMIN/PASTEUR + route
  dans la matrice de rôles.
- **Tests** : `finance_screen_test` **3 ✓** — mobile **118 tests ✓**, analyze 0 issue.

### État des tests (bloc 7)

- Backend : `FinanceServiceTest` 5 ✓ + `ModuleGateFilterTest` 3 ✓ +
  `PageBuilderServiceTest` 29 ✓ — BUILD SUCCESS.
- Frontend : `tsc --noEmit` ✓ · suite complète **214 tests vitest ✓** (34 fichiers).
- Mobile : `flutter analyze` **0 issue** · **118 tests ✓**.

### Commit / push

- Commit de ce bloc : Outil métier Finances (V68) — transactions, budget, stats.
- Poussé sur origin/main.

### Prochain objectif

Outil métier **Communication** (annonces/campagnes ciblées) et/ou passage à la
phase « cohérence/synchronisation » (tests de propagation) — voir
ARCHITECTURE_AUDIT.md §9 et la feuille de route §12.

---

## SESSION 2026-08-17 (bloc 6) — Page Builder V67 : FICHIERS / TACHES / FORMULAIRE

### Backend (V67 — blocs documents, tâches, formulaire de demande)

- **`PageBuilderService`** : 3 nouveaux types de blocs (`FICHIERS`, `TACHES`,
  `FORMULAIRE`) + **2 nouvelles sources** résolues sur données réelles scopées :
  - FICHIERS : `RECENT_FILES` — les 10 derniers documents (nom, catégorie
    humanisée, type, taille, date) ; scopés par familles accessibles
    (`findTop10ByFamilleIdInAndDeletedFalseOrderByCreatedAtDesc`).
  - TACHES : `TACHES_EN_COURS` — les 10 prochaines tâches ouvertes
    (A_FAIRE/EN_COURS/BLOQUEE) par échéance, avec nom du département résolu
    (pas de N+1) ; scopées par départements accessibles.
  - FORMULAIRE : bloc d'interaction (pas de source serveur) — **soumission
    réelle** côté frontend vers `POST /members/me/requests`.
- **Validation serveur** : FICHIERS/TACHES exigent une source de leur type ;
  FORMULAIRE exige une `cible` (PASTEUR/RESPONSABLE/CHEF_DE_FAMILLE) et un
  `type` (SUGGESTION/RENDEZ_VOUS/SIGNALEMENT) valides.
- **Repos étendus** : `FileEntityRepository` (top-10 récents global + scopé),
  `DepartmentTaskRepository` (top-10 ouverts par échéance global + scopé).
- **Tests** : `PageBuilderServiceTest` **29 ✓** (+4 : documents récents avec
  métadonnées, tâches scopées avec noms de départements, formulaire sans cible
  rejeté, formulaire valide accepté) + `PageBuilderControllerTest` 12 ✓ inchangé.

### Frontend web

- **`PageBlockRenderer`** : `FilesBlock` (liste de documents — icône, nom,
  catégorie · date · taille formatée Ko/Mo), `TasksBlock` (tâches avec
  département, échéance, badge de priorité coloré), `FormBlock` (champ message,
  **envoi réel** vers `/members/me/requests` avec type + destinataire, états
  chargement/erreur/confirmation inline).
- **`PlatformPagesPage`** (éditeur) : palettes FICHIERS/TACHES/FORMULAIRE +
  éditeurs dédiés — FICHIERS/TACHES : titre + source ; FORMULAIRE : titre,
  type de demande, destinataire, texte d'aide, libellé du bouton, message de
  confirmation.
- **Types** : `PageDataSource.type` étendu à FICHIERS/TACHES.
- **Tests** : `CustomPageView.test` **8 ✓** (+2 : rendu FICHIERS/TACHES sur
  données réelles, soumission FORMULAIRE → POST + confirmation),
  `PlatformPagesPage.test` **9 ✓** (+1 ajout bloc FORMULAIRE).

### État des tests (bloc 6)

- Backend : `PageBuilderServiceTest` 29 ✓ + `PageBuilderControllerTest` 12 ✓ —
  BUILD SUCCESS.
- Frontend : `tsc --noEmit` ✓ · suite complète **211 tests vitest ✓** (33 fichiers).
- Mobile : inchangé (supervision pages ADMIN, 115 tests ✓).

### Commit / push

- Commit de ce bloc : Page Builder V67 — documents, tâches, formulaire de demande.
- Poussé sur origin/main.

### Prochain objectif

Outils métiers activables (Finances, Communication) — voir ARCHITECTURE_AUDIT.md
§9 — ou extension du Page Builder (bloc statistiques) puis passage à la phase
« cohérence/synchronisation » (tests de propagation).

---

## SESSION 2026-08-17 (bloc 5) — Page Builder V66 : GRAPHIQUE / CALENDRIER / TIMELINE / CHECKLIST

### Backend (V66 — sources & blocs supplémentaires)

- **`PageBuilderService`** : 4 nouveaux types de blocs (`GRAPHIQUE`, `CALENDRIER`,
  `TIMELINE`, `CHECKLIST`) + **7 nouvelles sources de données** résolues sur données
  réelles et scopées par espace métier :
  - GRAPHIQUE : `SOULS_BY_STATUT` (répartition par statut d'âme), `EVENTS_BY_MONTH`
    (6 prochains mois, mois à trous → 0), `ALERTS_BY_TYPE` (actives par type,
    libellés humanisés), `DEPARTMENTS_BY_STATUT` (répartition par statut d'entité).
  - CALENDRIER : `CALENDAR_EVENTS` (événements des 60 prochains jours : date ISO,
    titre, lieu, type, triés par date).
  - TIMELINE : `SOULS_TIMELINE` (10 dernières âmes créées : date, nom complet, statut).
  - CHECKLIST : bloc local (config `items`, aucune donnée serveur — validation
    « au moins un élément » comme LIENS).
- **Repos étendus** : `AlertRepository.findByStatut` + `findByStatutAndAmeIdIn`
  (répartition des alertes actives, scopée).
- **Validation** : blocs GRAPHIQUE/CALENDRIER/TIMELINE exigent une source connue de
  leur type ; `sourceExists` réaligné sur le type exact (plus de détour TABLEAU/LISTE→KPI).
- **Tests** : `PageBuilderServiceTest` **25 ✓** (25 → 25, +7 : checklist sans items
  rejetée, source graphique inconnue rejetée, camembert âmes par statut,
  barres événements par mois (7 points, mois vide → 0), calendrier (date ISO),
  timeline (nom complet + statut), checklist sans données serveur).

### Frontend web

- **`PageBlockRenderer`** : `ChartBlock` (recharts : camembert `PIE` avec Cell colorés,
  barres `BAR`, courbe `LINE` — palette 7 couleurs), `CalendarBlock` (grille
  mensuelle lundi→dimanche, navigation mois précédent/suivant, événements du mois
  listés sous le calendrier), `TimelineBlock` (timeline verticale avec date/label/
  valeur), `ChecklistBlock` (cases à cocher, **persistance localStorage** par
  page+bloc, barre de progression 0–100 %).
- **`PlatformPagesPage`** (éditeur) : palettes des 4 nouveaux types (boutons d'ajout),
  éditeurs dédiés — GRAPHIQUE : titre + source + **type de graphique**
  (PIE/BAR/LINE) ; CALENDRIER/TIMELINE : titre + source ; CHECKLIST : titre +
  liste d'éléments ajoutables/retirables. Aperçu local intégré (blocs sans données).
- **Types** : `PageDataSource.type` étendu à GRAPHIQUE/CALENDRIER/TIMELINE.
- **Tests** : `PlatformPagesPage.test` **8 ✓** (+1 ajout bloc GRAPHIQUE),
  `CustomPageView.test` **6 ✓** (+1 rendu des 4 nouveaux blocs + interaction
  checklist 0/2 → 1/2 · 50 %). **14 tests frontend pour ce bloc, tous verts.**

### Mobile — inchangé (supervision pages ADMIN déjà couverte en V65)

- `PlatformPagesScreen` inchangé : la liste/publication/suppression couvre les
  nouvelles pages ; le rendu des blocs est web.

### État des tests (bloc 5)

- Backend : `PageBuilderServiceTest` **25 ✓ BUILD SUCCESS**.
- Frontend : `tsc --noEmit` ✓ · vitest 14/14 (CustomPageView + PlatformPagesPage) ✓.
- Mobile : `platform_pages_screen_test` **4 ✓**.

### Commit / push

- Commit de ce bloc : Page Builder V66 — graphiques, calendrier, timeline, checklist.
- Poussé sur origin/main.

### Prochain objectif

Étendre le Page Builder avec les blocs restants (formulaire, fichiers, tâches,
statistiques) et/ou les outils métiers activables (Finances, Communication) — voir
ARCHITECTURE_AUDIT.md §9 et la feuille de route §12.

---

## SESSION 2026-08-17 (bloc 4) — Page Builder complet (V65) — fullstack + mobile

### Backend (V65 `custom_pages`)

- **Migration V65** : table `custom_pages` (key/title/description/slug/layout/blocs JSONB/
  roles JSONB/enabled/published/version/created_by) + menu ADMIN « Pages personnalisées »
  (`LayoutTemplate`, section Administration) + **page d'exemple publiée** « Vue d'ensemble
  de l'église » (KPI + tableaux + liste + liens, tous résolus sur données réelles).
- **`PageBuilderService`** : CRUD complet (clés/slugs uniques, slugification, validation
  des blocs et sources), publication/dépublia ge avec **incrément de version**, versionnage
  systématique dans `config_revisions` (PAGE_CREATED/UPDATED/DELETED/PUBLISHED) + audit.
- **Résolution des blocs sur données RÉELLES** : 13 sources (SOULS_TOTAL/ACTIFS,
  FAMILIES_TOTAL, DEPARTMENTS_TOTAL, EVENTS_UPCOMING, ALERTS_OPEN, TRANSFERS_PENDING,
  USERS_TOTAL (sensible), RECENT_SOULS, UPCOMING_EVENTS, RECENT_ALERTS,
  RECENT_TRANSFERS, DEPARTMENTS_LIST) **scopées par espace métier** (WorkspaceScopeService :
  super-utilisateur → tout, sinon âmes/familles/départements accessibles) — aucun KPI fictif.
- **API** `/api/v1/pages` : CRUD ADMIN (POST/PUT/DELETE/publish/preview/sources/options),
  rendu public `GET /pages/{slug}` authentifié + contrôle RBAC par rôles de la page
  (vide = tous ; super-user débloque les pages PASTEUR).
- **Repos** : `AlertRepository`, `DepartmentRepository`, `EventRepository`, `FamilyRepository`,
  `SoulRepository`, `TransferRequestRepository` étendus (comptages/listes top-10 scopées).
- **Tests** : `PageBuilderServiceTest` 18 ✓ (CRUD, rendu, scoping, source sensible masquée
  pour non-super-user, refus de rôle, page non publiée masquée, liste d'alertes résolue)
  + `PageBuilderControllerTest` 12 ✓ (RBAC réel 401/403, rendu 200, preview).

### Frontend web

- **`PlatformPagesPage`** (`/admin/pages`) : liste (badges Publiée·vX/Brouillon/Désactivée,
  rôles), éditeur en modale — titre/adresse/clé/disposition/description, rôles autorisés
  (chips), **blocs réordonnables** (KPI avec icône+couleur, tableau, liste, texte, liens,
  recherche, images), **aperçu local**, toggle de publication, suppression, historique
  versionné (ConfigRevisionHistory). Erreurs serveur affichées (message `detail`).
- **`CustomPageView`** (`/pages/:slug`) : rendu public des blocs résolus (KPI formatés
  fr-FR, tableaux, listes, texte, liens, recherche → `/search?q=`, images), états
  403/page introuvable dédiés, version affichée.
- **`PageBlockRenderer`** : rendu de chaque type de bloc (zéro JSON brut, aucune donnée
  fictive — les valeurs viennent du serveur).
- **Routes** App.tsx (lazy) + icône `LayoutTemplate` + types `CustomPage`/`ResolvedPage`/
  `ResolvedBlock`/`PageDataSource`.
- **Tests** : `PlatformPagesPage.test` 6 ✓ (liste, création avec bloc KPI, ajout bloc
  tableau, toggle publication, suppression, erreur serveur) + `CustomPageView.test` 5 ✓
  (rendu KPI/tableau réels, KPI sans valeur → « — », page vide, 403, 404).

### Mobile (Flutter) — parité supervision

- **`PlatformPagesScreen`** (`/admin/pages`, ADMIN) : liste des pages (titre, adresse,
  badges Publiée·vX/Brouillon/Désactivée, rôles), **toggle publication**
  (POST /pages/{id}/publish), suppression avec confirmation, pull-to-refresh.
  L'éditeur complet de blocs reste web (interface de configuration avancée) — la
  supervision mobile couvre l'état et la publication.
- **Nav** : entrée « Pages personnalisées » dans le drawer ADMIN (exclue du menu PASTEUR)
  + route `/admin/pages` (matrice de rôles ['ADMIN']).
- **Tests** : `platform_pages_screen_test` 4 ✓ — mobile **115 tests ✓**, analyze 0 issue.

### Validation e2e réelle (navigateur Chrome)

- **`scripts/e2e-browser-pages.js` — 16/16 ✓, 0 erreur console** : login admin → liste
  (page d'exemple présente) → création d'une page (KPI + texte) → publication (badge) →
  rendu public de la nouvelle page → rendu de la page d'exemple avec **KPI réels**
  (1015 âmes, 555 actives, 45 familles — base de dev) → tableaux + liens → login faiseur
  (contexte séparé) → accès page publiée → rendu scopé → **RBAC API 403** (GET /pages et
  /pages/sources par non-ADMIN) → suppression de la page de test (nettoyage).

### État des tests (session, tout vert)

- Backend : **494 tests ✓ BUILD SUCCESS** (454 → 494, +40 : 18 PageBuilderService +
  12 PageBuilderController + 10 ConfigRevision/PlatformConfig en cours de session).
- Frontend : `tsc -b` ✓ · **205 tests vitest ✓** (33 fichiers, 190 → 205) · `npm run build` ✓.
- Mobile : `flutter analyze` **0 issue** · **115 tests ✓** (111 → 115, +4 pages).

### Prochain objectif

Extension de la bibliothèque de blocs du Page Builder (graphiques/formulaires/calendrier/
timeline/checklist) et/ou outils métiers activables (Finances, Communication) — voir
ARCHITECTURE_AUDIT.md §9 et la feuille de route §12.

---

## SESSION 2026-08-17 — AUDIT TRANSVERSAL COMPLET (avant refonte)

### Fait

- **ARCHITECTURE_AUDIT.md créé** (livrable mission 1, documents de référence de la refonte) :
  cartographie complète des modules (46), pages (79), entités, relations, permissions,
  workflows, duplications, bugs, manques, UX, backend, mobile, sécurité + plan de refonte.
- **Baselines vérifiées (toutes vertes)** :
  - Backend : **454 tests ✓ BUILD SUCCESS** (`mvn test`)
  - Frontend : **190 tests vitest ✓** (30 fichiers) + `tsc -b` ✓
  - Mobile : `flutter analyze` **0 issue** ✓ (110 tests)
  - **0 lien mort** : les 49 `href` de navigation (workspaces.ts) ont tous une route dans App.tsx
- **Constat d'audit** : le projet est déjà très avancé — la quasi-totalité des modules sont
  fonctionnels (A) et testés. Aucun bug fonctionnel bloquant détecté à l'audit statique.
  Seul item : warning console React `<linearGradient>` (recharts + React 19, cosmétique).

### Restant (feuille de route refonte)

1. Pasteur — centre de supervision (blocs CRM cliquables, audit exploitable, corbeilles).
2. Admin — centre de configuration (constructeur rôles/menus/pages, champs perso, modules).
3. Espaces métiers différenciés par rôle + changement de rôle complet.
4. Plateforme modulaire : **Page Builder** + outils métiers activables + **versionnage**.
5. Cohérence/synchronisation : tests de propagation transversale.
6. QA final : audit page par page, rôles/permissions/CRUD/sync, responsive, perf, sécurité,
   déploiement, GitHub.

### Commit / push

- Commit : `docs(audit): ARCHITECTURE_AUDIT.md — cartographie complète + baselines vertes`
  (backend 454, frontend 190, mobile analyze 0 issue, 0 lien mort).

### Prochain objectif

Démarrer la phase **Pasteur** (ou la phase **Admin** / **Plateforme modulaire**) en suivant
ARCHITECTURE_AUDIT.md et ce checkpoint ; à la reprise d'une session : `git pull` → lire
PROJECT_PROGRESS.md → reprendre.

---


## SESSION 2026-08-17 (blocs 2 et 3) — Pasteur (audit exploitable) + Plateforme (versionnage)

### Bloc 2 — Pasteur : audit exploitable par action (`aa5ce54`)

- **Backend** : `AuditLogRepository`/`AuditService`/`AuditController` acceptent le filtre
  `action` exact sur `GET /audit` et `/audit/export` (chaque critère optionnel).
- **Frontend** : nouvelle catégorie **TRANSFER** (badge ambre, icône `ArrowLeftRight`,
  KPI « Transferts » cliquable + filtre par type d'action) sur `AuditPage`.
- **Tests** : `AuditServiceTest` 6/6, `AuditPage.test` 7/7 (dont entrée `TRANSFERT_SOUL`).

### Bloc 3 — Plateforme modulaire : versionnage des configurations (`8c2b0ae`)

- **V64 `config_revisions`** : journal append-only (entity_type, entity_key, action,
  payload JSONB, user_id, created_at) + index (entity_type, created_at DESC).
- **Backend** : `ConfigRevision`/`ConfigRevisionRepository`/`ConfigRevisionService`
  (record + list, auteur via SecurityUtils) ; `PlatformConfigService` instrumente chaque
  mutation modules/menus (toggle/création/modification/suppression/réordonnancement) avec
  état avant/après ; `GET /api/v1/platform/revisions?entityType=&page=&size=` (ADMIN).
- **Frontend** : composant `ConfigRevisionHistory` (panneau repliable, timeline, chargement
  à la demande) intégré aux pages `PlatformModulesPage` et `PlatformMenusPage`.
- **Tests** : `ConfigRevisionServiceTest` 4/4, `PlatformConfigServiceTest` 9/9,
  `PlatformConfigControllerTest` 20/20 (dont 403 non-ADMIN), frontend **14/14**,
  `tsc -b` ✓, BUILD SUCCESS.

### Constat au fil des blocs

Plusieurs items « restant » du roadmap étaient déjà implémentés (corbeille des âmes
`/souls/trash` + restauration, blocs CRM cliquables du dashboard Pasteur, dashboards
dédiés, constructeur rôles/menus/champs perso). Les vrais écarts restants (F/B) sont :
**Page Builder**, **outils métiers Finances/Communication** (Formation/Inventaire
déjà couverts par TRAININGS + sous-modules DMS), **workflow builder visuel**,
**moteur de notifications configurable en UI**.

### Commit / push

- `aa5ce54` — Pasteur : audit exploitable par action.
- `8c2b0ae` — Plateforme : versionnage des configurations.
- Poussés sur origin/main.

---

## OBJECTIF GLOBAL

Passer l'application du mode "projet/démo" au mode **produit professionnel**
configurable, puis créer et livrer un **environnement de bêta-test public
(URL publique)** séparé de la production, avec comptes de démonstration,
données fictives réalistes, reset de l'environnement, feedback des testeurs
et monitoring.

## ÉTAT GLOBAL

- [x] Audit initial (backend, frontend web, CI, déploiement)
- [x] Backend : module feedback (V40/V41), meta publique, beta admin/reset — **validé e2e**
- [x] Frontend web : fonctionnalités bêta (badge, comptes démo conditionnels, widget feedback, panneau admin feedback, mode testeur) — **167 tests vitest ✓**
- [x] V50 committée (`cf8df41`) et **poussée sur GitHub**
- [x] Vérification end-to-end locale du flux bêta complet (API profil `beta` + Postgres dédié)
- [x] Audit sécurité live : isolation départements/familles, RBAC, garde-fous reset — OK
- [x] Tests backend ajoutés : `BetaResetServiceTest` (3 — double garde reset) + `FeedbackControllerTest` (9) + `BetaAdminControllerTest` (6) — RBAC des nouveaux endpoints verrouillé
- [x] **Mobile : badge BÊTA + feedback testeur + comptes démo conditionnels** (parité web 3.20.0) — 10 tests widget, **89 tests mobile ✓**, analyze sans issue
- [x] **Audit produit (3.20.1)** : 0 lien mort (51 liens croisés), bug navigation PASTEUR corrigé (menus ADMIN retirés de la sidebar), DataTable optimisée (tri mémoïsé, pagination client opt-in, animations bornées), vitest `testTimeout: 15000` (faux échecs sous charge parallèle), smoke test des 6 rôles OK — **165 tests vitest ✓**
- [x] **Perf (3.20.2)** : chargement par route (code splitting) — bundle initial **1.18 MB → 267 KB (80 KB gzip)**, fallback Suspense, pages chargées à la demande — **167 tests vitest ✓** (commit `fe001eb`)
- [ ] Déploiement bêta Render (services bêta dans render.yaml — **Sync Blueprint à faire par l'utilisateur**, pas de credentials Render dans l'environnement)
- [ ] Vérification finale de l'URL publique + rapport final (bloqué sur le déploiement)

## FONCTIONNALITÉS TERMINÉES (V50 — commit `cf8df41`)

### Backend
- `POST /api/v1/feedback` (soumission authentifiée) · `GET /admin/feedback` + `/stats` (ADMIN/PASTEUR) · `PATCH .../{id}/status` (ADMIN)
- `GET /api/v1/public/meta` (public : appName, version, environment, betaMode, demoAccountsEnabled)
- `GET /api/v1/admin/beta/status` + `POST /api/v1/admin/beta/reset` (ADMIN, double garde : env prod + flag reset-enabled — profil beta uniquement)
- Migrations V40 (`feedbacks`) + V41 (module FEEDBACK + menu admin-feedback)
- `DataInitializer` : comptes démo créés **uniquement** si `seed-demo-accounts=true` (profil beta) — 7 comptes `password123`
- `seed-demo.sql` : 9 utilisateurs écosystème, 4 départements, 4 familles, 10 âmes, rapports, alerte

### Frontend web
- `MetaContext` + `BetaBadge` (badge BÊTA conditionnel : Navbar, Landing, Login)
- `LoginPage` : comptes démo affichés **uniquement si** `demoAccountsEnabled`
- `FeedbackWidget` (bouton flottant + modale, contexte technique auto)
- `AdminFeedbackPage` (`/admin/feedback` : stats, filtres, statuts, panneau reset bêta)
- Bandeau "mode testeur" masquable dans MainLayout

## VÉRIFICATIONS END-TO-END RÉALISÉES (le 2026-08-11, API beta locale :8090 + DB `discipolat_beta`)

- [x] `/api/v1/public/meta` → `{environment: "beta", betaMode: true, demoAccountsEnabled: true}`
- [x] Connexion des **7 comptes démo** (`password123`) : admin, pasteur, responsable, chef, faiseur, membre, paul — HTTP 200
- [x] Changement de rôle multi-rôles (paul : RESPONSABLE → CHEF_DE_FAMILLE) — nouveau JWT correct
- [x] Feedback : POST → GET list → GET stats (reporterEmail résolu)
- [x] Sécurité : MEMBRE → /admin/feedback, /admin/beta/reset, /admin/beta/status = **403** ; sans token = **401**
- [x] Reset bêta : POST /admin/beta/reset → OK (tables tronquées, seed restauré : 4 départements, 4 familles, 10 âmes ; feedbacks **conservés** ; comptes recréés)
- [x] Isolation départements : responsable1 voit uniquement SES départements (Jeunesse + Chorale), **403** sur Adultes
- [x] Isolation familles : chef1 voit uniquement SA famille (Timothée), **403** sur famille Tite
- [x] CORS : preflight `http://localhost:5173` → `Access-Control-Allow-Origin` correct

## BUGS DÉCOUVERTS / CORRIGÉS (pendant la session)

- [x] `AdminFeedbackPage` : badge environnement rendu avec un `className` non-interpolé (template literal manquant) → corrigé (le badge affiche bien beta/autre)
- [x] `main.tsx` : indentation JSX du Toaster/MetaProvider clarifiée (aucun changement de comportement)

## TESTS

- Backend : `mvn verify` ✅ (suite complète) + **nouveau `BetaResetServiceTest`** (3 tests : refus prod, refus flag désactivé, statut) ✅
- Frontend : `npx tsc --noEmit` ✅ · `npx vitest run` **153 tests ✓** · `npm run build` ✅
- Mobile : `flutter analyze` **0 issue** · `flutter test` **79 tests ✓**

## DÉCISIONS ARCHITECTURALES

- Séparation **production / bêta** via des **services Render distincts** (DB séparée, API avec profil `beta`, frontend statique séparé). Aucun accès testeur à la prod.
- Comptes démo visibles **seulement** en environnement bêta (serveur-driven via `/public/meta`).
- Reset bêta **strictement désactivé** hors profil `beta` (double garde : flag + environnement) — testé.
- Feedback : aucune donnée personnelle au-delà de l'email de l'auteur (résolu côté serveur).

## MIGRATIONS EFFECTUÉES

- V40 (`feedbacks`), V41 (module + menu FEEDBACK) — committées dans `cf8df41`.

## DÉPLOIEMENTS EFFECTUÉS

- Aucun nouveau déploiement (pas de credentials Render/GitHub dans l'environnement). Production existante : `https://discipolat.onrender.com` / API `https://discipolat-api.onrender.com`.

## DERNIER COMMIT GIT

- `4fc6583` fix(events): contrainte DB types événement (V62) — types CULTE/ETUDE_BIBLIQUE/VEILLEE/PRIERE créaient un 500 ; onglet Événements département : 13 types (parité)
- `scripts/e2e-browser.js` ajouté (harness Puppeteer + Chrome système, 16 étapes, `puppeteer-core` en devDependency frontend)
- `scripts/seed-volumineux.sql` : seed massif 100 % fictif (voir section ci-dessous)

## SEED VOLUMINEUX (2026-08-15) — base de DEV remplie

- Script **`scripts/seed-volumineux.sql`** : `psql ... -f scripts/seed-volumineux.sql`, ré-exécutable (IDs md5 déterministes + ON CONFLICT), n'efface rien.
- Volume après exécution : **1015 âmes** · **73 users** (12 responsables, 16 chefs, 30 faiseurs nouveaux) · **44 familles** · **16 départements** · **1254 liens âme↔département** (127 dans Audiovisuel) · **86 équipes** · **98 postes** · **164 tâches** · **922 affectations** · **72 événements** · **809 pointages** · **192 inscriptions** · **2002 présences hebdo** · 53 rapports département · 81 rapports famille · checklists · matériel · activité.
- Tous les nouveaux comptes : `password123`. Ex. `responsable10@discipolat.com` (Intercession)… `responsable21@discipolat.com`, `chef10@…`–`chef25@…`, `faiseur10@…`–`faiseur39@…`.
- Validé : API rapide (détail <110 ms, recherche <90 ms), pasteur voit les 16 départements, **E2E navigateur 16/16 ✓** sur la base remplie.

## QA NAVIGATEUR RÉEL (2026-08-15) — E2E module Responsable

- **16/16 étapes ✓, 0 erreur console** : login → rôle Responsable → départements → détail → **Outils** (ex-404, 5 onglets) → Rapport → Gestion/Événements → modale Présences (mark-all + export CSV) → recherche globale (« aya » → Aya Kouassi) → dossier membre (onglet Présences, section événements) → dashboard responsable (carte Présence aux événements)
- **Bug corrigé (V62)** : la contrainte CHECK de V3 n'autorisait que 9 types d'événements alors que le dictionnaire en configure 13 → création d'un événement `CULTE`/`VEILLEE`/… : **500 Internal Server Error**. Migration `V62__events_type_constraint_fix.sql` réaligne la contrainte ; vérifié en réel (création CULTE ✓). `EVENT_TYPES` de l'onglet Événements du département réaligné sur les 13 types.
- Relance : `node scripts/e2e-browser.js` (exige Chrome système + stack locale : API :8080, web :5173)

## PROCESSUS LOCAUX LAISSÉS ACTIFS (session e2e du 2026-08-11)

- API bêta locale : **port 8090** (profil `beta`, DB `discipolat_beta`) — logs `/tmp/beta-api.log`
- Dev server frontend bêta : **port 5173** (`VITE_API_URL=http://localhost:8090`) — logs `/tmp/beta-web.log`
- Base `discipolat_beta` créée dans le Postgres Docker local (5433) — réutilisable pour re-tester le flux
- Arrêt : `pkill -f 'spring-boot:run'` et `pkill -f 'vite.*5173'` (ou les tuer par PID)

## PROBLÈMES CONNUS / BLOCAGES

- **Déploiement bêta public — VÉRIFIÉ le 2026-08-11 : les services bêta n'existent PAS encore sur Render.**
  - `https://discipolat-beta.onrender.com` → 404 instantané ; `https://discipolat-beta-api.onrender.com/api/v1/public/meta` → 404 instantané (pas un cold start). DNS OK (CDN Render). Variantes de noms testées (beta-web, web-beta, beta-backend…) → 404 aussi.
  - **Cause la plus probable** : le Sync Blueprint a échoué sur la limite du plan Free Render (1 base Postgres gratuite par workspace — `discipolat-db` occupe déjà le slot). Voir DEPLOYMENT.md §8.7.
  - **Actions utilisateur** : (1) Dashboard Render → Blueprints → vérifier le statut/erreur du sync ; (2) créer `discipolat-beta-db` manuellement (plan Starter ~7 $/mois recommandé, ou passer `discipolat-db` en payant pour libérer le slot gratuit) ; (3) re-sync (ou créer manuellement `discipolat-beta-api` + `discipolat-beta`) ; (4) fournir les secrets GitHub `RENDER_API_KEY` / `RENDER_BETA_API_SERVICE_ID`.
  - NB : `https://discipolat.onrender.com` (prod) est injoignable depuis cet environnement (timeout réseau sandbox — exemple.com répond 200 en 0,5 s ; prod 000/10 s) : non concluant pour l'état de la prod, rien n'a été modifié côté prod.
- Limite plan Free Render : 1 Postgres gratuit/workspace → `discipolat-beta-db` peut nécessiter un plan Starter (~7 $/mois) si `discipolat-db` occupe déjà le slot gratuit.
- Cold start API bêta (~1 min au premier accès) — volontaire (pas de keep-alive bêta, quota 750 h/mois).

## AMÉLIORATIONS IDENTIFIÉES (non bloquantes)

- ~~Mobile : widget feedback + badge bêta~~ → **FAIT en 3.20.0** (parité web complète).
- Affichage de la version d'app dans le panneau admin feedback (déjà stockée côté serveur).

## NEXT ACTION

> **Reprise (à la prochaine session)** : l'utilisateur doit vérifier le statut du
> **Sync Blueprint Render** (les services bêta n'existent pas — 404 sur les deux
> URL, diagnostic du 2026-08-11 consigné ci-dessus). Causes probables : limite
> Postgres gratuite du workspace (créer discipolat-beta-db manuellement, plan
> Starter ~7 $/mois, ou passer la base de prod en payant) puis re-sync, ou
> workspace/sync non effectué. Une fois les services créés : fournir les secrets
> GitHub `RENDER_API_KEY` / `RENDER_BETA_API_SERVICE_ID`, vérifier l'URL publique
> (connexion comptes démo, feedback, reset), fournir le rapport final.
> En attendant : le flux bêta complet reste vérifié en local (port 8090 +
> DB discipolat_beta — voir « PROCESSUS LOCAUX LAISSÉS ACTIFS »).

---

## SESSION 2026-08-12 (perf + préparation vérification)

- **Perf (3.20.2)** : code splitting par route (React.lazy + Suspense) — bundle
  initial **1.18 MB → 267 KB (80 KB gzip)**. `App.tsx` + test adapté
  (RoleWorkspaceRouting : assertions « Tableau de bord » sous waitFor).
  167 tests vitest ✓, tsc ✓, build ✓. Commit `fe001eb` poussé.
- **Script de vérification** : `scripts/verify-beta.sh` — teste la chaîne
  complète sur l'URL bêta publique dès qu'elle existe (meta, 7 comptes démo,
  switch-role, feedback POST→GET→stats, RBAC 403/401, reset, isolation).
- Choix utilisateur (ask_user) : **il fera lui-même le Sync Blueprint Render**.
- État des URL vérifié : `discipolat-beta.onrender.com` et
  `discipolat-beta-api.onrender.com` → **404 (services inexistants)** ; prod API
  `/actuator/health` → **200 OK**.

## SESSION 2026-08-12 (vérification réelle stack bêta locale)

- **Stack bêta locale relancée** : API profil `beta` sur **:8090** (DB
  `discipolat_beta`, Postgres Docker :5433) + frontend Vite sur **:5173**
  (`VITE_API_URL=http://localhost:8090`).
- **`./scripts/verify-beta.sh http://localhost:8090` → 16/16 checks ✓** :
  - Meta `env=beta` + `demoAccountsEnabled=true` ✓
  - 7 comptes démo connectés (`password123`) ✓
  - Switch de rôle paul RESPONSABLE→CHEF_DE_FAMILLE → nouveau JWT ✓
  - Feedback POST → id créé ; stats admin ✓
  - RBAC : MEMBRE → /admin/feedback et /admin/beta/reset = **403** ; sans token = **401** ✓
  - RESPONSABLE1 → /departments = 200 ✓
  - Reset admin → `status:OK`, **71 tables tronquées**, seed restauré ✓
- **Test navigateur réel (browser-use, Chrome)** sur la stack locale : landing
  avec badge BÊTA ✓, page login avec comptes démo ✓, connexion
  `faiseur@discipolat.com` ✓, dashboard CRM Faiseur ✓, navigation sidebar →
  Rapports ✓, widget feedback flottant → modale (subject/category) ✓,
  **0 erreur console** ✓.
- Contrat API feedback vérifié : `category`+`subject` obligatoires (le script
  initial envoyait `categorie`/`message` → 400 ; corrigé). Rate-limiting login
  (10/min/IP) observé en conditions réelles → script avec cache de tokens.
- **ErrorBoundary des routes lazy** (`components/shared/ErrorBoundary.tsx`) :
  si un chunk échoue à se charger (réseau instable), écran de récupération
  avec bouton « Réessayer » au lieu d'une page blanche.
- `verify-beta.sh` enrichi : vérif liste admin (`GET /admin/feedback`) +
  invariant « le feedback survit au reset » → **18/18 checks ✓**.
- Suite complète revalidée : **167 tests vitest ✓** (tsc ✓, build ✓),
  319 tests backend ✓, 89 tests mobile ✓.
- Commits poussés : `fe001eb` (code splitting), `73879ed` (script+checkpoint),
  `6f113d2` (fix contrat feedback script), `65f53cf` (checkpoint vérif),
  `1b84008` (ErrorBoundary + script enrichi).

## NEXT ACTION (session 2026-08-12)

> L'utilisateur exécute : **Dashboard Render → Blueprints → Sync** (ou crée
> manuellement `discipolat-beta-db` + re-sync). Étapes exactes dans
> `docs/DEPLOYMENT.md` §Bêta (activer l'environnement bêta). Une fois les
> services créés, lancer `./scripts/verify-beta.sh` pour valider la chaîne
> complète, ajouter les secrets GitHub RENDER_API_KEY /
> RENDER_BETA_API_SERVICE_ID, vérifier l'URL publique et rédiger le rapport
> final (comptes, rôles, fonctionnalités, version, tests, problèmes connus).

---

## SESSION 2026-08-14 (suite) — événements de département (V56) + recherche globale

- **V56** : `events.department_id` (index) — événements rattachés à un département,
  création/mise à jour avec departmentId, `GET /events/department/{id}` paginé,
  scoping métier (WorkspaceScopeService.accessibleDepartmentIds), scopeEvents étendu.
- **Recherche globale** : `GET /departments/{id}/search` — membres (LIKE base),
  équipes, postes, tâches, événements, 10 max par catégorie. Frontend : champ
  « Recherche rapide » + panneau de résultats ; onglet Événements (création +
  à venir/passés). Refactor : tous les onglets reçoivent `deptId` par prop
  (plus de parsing de window.location) + labels htmlFor/id (a11y).
- Commit `3727c1e`. Tests : +3 frontend (recherche, événements, création).

## SESSION 2026-08-14 (suite) — paramétrage des alertes (V57) + équipes liées aux événements

- **V57** : table `department_settings` (absence_seuil=2, absence_periode=3,
  inactivite_mois=3, tache_retard_alerte=true) — le moteur d'alertes lit ces
  seuils (plus aucune valeur hardcodée) + **nouvelle règle INACTIVITE** (aucune
  fiche de présence depuis N mois). `GET|PUT /departments/{id}/settings` validé.
- **Équipes temporaires liées à un événement** : `department_teams.event_id`,
  validation « l'événement doit appartenir au département », titre d'événement
  groupé (pas de N+1). Frontend : onglet **Paramètres** (seuils + descriptions),
  sélecteur « Événement lié » dans le formulaire d'équipe (dates pré-remplies).
- Commit `4070e67`. Tests : +4 backend, +2 frontend.

## SESSION 2026-08-14 (suite) — documentation du département (V58) + annonces ciblées

- **V58** : `department_documents` (PROCEDURE/GUIDE/DOCUMENT/FORMULAIRE/COMPTE_RENDU/
  RESSOURCE, statut ACTIF/ARCHIVE, url, created_by) + CRUD `/documents` ;
  `department_announcement_members` — cible **MEMBRES** des annonces (validation
  « le membre doit appartenir au département », filtre annoncesPourMembre étendu).
- Frontend : onglet **Documentation** (KPIs par type, ajout/archive/suppression),
  cible « Certains membres » (sélecteur multi, compteur).
- Commit `f3fcb9b`. Tests : +5 backend, +3 frontend.

## SESSION 2026-08-14 (suite) — statistiques par période + sous-modules DMS (V59)

- **Analytics par période** : `GET /departments/{id}/stats?periode=MOIS|TRIMESTRE|
  SEMESTRE|ANNEE|PERSONNALISEE&debut=&fin=` — présence/tâches/discipline filtrées,
  séries par mois (trimestre si > 24 mois), `nouveauxPeriode`, écho de la période.
  Frontend : sélecteur de période + champs Du/Au. Commit `7d6e0d6`.
- **V59 — sous-modules activables** : DEPT_REPORTS / DEPT_CHECKLISTS /
  DEPT_INVENTORY / DEPT_DOCUMENTS ; ModuleGateFilter segmenté (PREFIXE@@SOUS_MODULE)
  → API 403 si désactivé ; onglets masqués côté web, page Rapport → état explicite.
  Commit `f0df165`.
- **Tests** (fin de session) : backend **420 ✓**, frontend **178 ✓**, build ✓,
  `flutter analyze`/`flutter test` inchangés (aucune modif mobile ce bloc).

## SESSION 2026-08-14 (fin) — audit des 27 sections : comblement des derniers écarts

### Parité mobile des outils (commit `3a929e8`)

- Écran Outils mobile : 2 nouveaux onglets **Documentation** (liste/ajout/suppression
  de documents) et **Paramètres** (seuils d'alertes éditables → PUT /settings),
  chargement robuste si module désactivé (403 → état vide). `flutter analyze` 0
  issue, **101 tests mobile ✓** (dont 2 nouveaux widget tests).

### Rapports modifiables (commit `11de474`)

- **§1 « rapports modifiés »** : `updateReport` + PUT /departments/{id}/reports/saved/{id}
  (contenu, titre, statut BROUILLON/SOUMIS/ARCHIVE, période) + éditeur web (modale)
  avec labels a11y. Tests : +2 backend, +1 frontend.

### État final des suites

- Backend : **422 tests ✓** (0 fail) · Frontend : **179 tests ✓** (tsc ✓, build ✓)
  · Mobile : **101 tests ✓** (analyze 0 issue).
- Migrations : V40→V59. Commits poussés : `3727c1e`, `4070e67`, `f3fcb9b`,
  `7d6e0d6`, `f0df165`, `3a929e8`, `11de474`.

### Audit des 27 sections de la consigne

1. Rapports pro ✓ (12 types, sauvegardés/modifiés/consultés/archivés/exportés CSV) ·
2. Rapport auto ✓ (génération sur données réelles) · 3. Analytics ✓ (périodes
   MOIS/TRIMESTRE/SEMESTRE/ANNEE/PERSONNALISEE) · 4. Alertes ✓ (seuils configurables,
   règle INACTIVITE) · 5. Événements ✓ (département + équipes temporaires liées +
   checklists cible EVENEMENT) · 6. Checklists ✓ · 7. Inventaire ✓ (module
   DEPT_INVENTORY activable/désactivable) · 8. Documentation ✓ (V58) ·
9. Communication ✓ (TOUS/ÉQUIPE/POSTE/MEMBRES) · 10. Recherche globale ✓ ·
11. Import/Export ✓ (CSV membres + rapports ; PDF plateforme existant) ·
12. Audit ✓ (journal d'activité + traçabilité) · 13. Permissions ✓ (tests 403/404
   responsable A vs B, pasteur global) · 14. Pasteur/Admin ✓ (superuser) ·
15. Configuration ✓ (dictionnaires, champs personnalisés, seuils, modules) ·
16. Performance ✓ (N+1 éliminés, chargements groupés, pagination) ·
17. Responsive ✓ (web responsive + parité mobile des outils) · 18. QA e2e ✓
(parcours réels validés sessions précédentes) · 19. Tests de permissions ✓ ·
20. Non-régression ✓ (suites complètes) · 21. Audit UX ✓ (recherche, filtres,
   actions rapides, états vides) · 22. Audit pro ✓ (aucune donnée fictive, toutes
   les stats viennent de la base) · 23. Git/GitHub ✓ (commits propres + push) ·
24. Déploiement ⏸ (bloqué : pas de credentials Render — voir §Bêta) ·
25. Mode autonome ✓ · 26. Checkpoint ✓ (ce fichier) · 27. Critère de fin ✓
(fonctionnel, testé, sécurisé, responsive, performant, modulaire, configurable,
maintenable, documenté).

### Prochaines actions possibles

- Parité mobile des événements de département (onglet Événements + équipes liées)
  et de la recherche globale (l'écran Outils et la gestion mobile couvrent
  déjà rapports/checklists/inventaire/docs/paramètres/objectifs/annonces).
- Déploiement bêta Render (blocage utilisateur, voir « NEXT ACTION » plus haut).

---

## SESSION 2026-08-14 (fin) — QA e2e locale + fix recherche globale (commit `0b17665`)

- **QA de bout en bout sur l'API locale :8080** (parcours testeur réel) :
  création événement département (departmentId) ✓ → GET /events/department/{id} ✓
  → équipe temporaire liée (eventId, dates) ✓ → eventTitre résolu dans /management ✓
  → recherche globale multi-catégories (membre réel + équipe + événement) ✓
  → rappel configurable eventRappelJours=14 puis retour à 1 ✓. Données de test
  nettoyées (équipe + événement supprimés).
- **Bug découvert et corrigé** : une équipe archivée apparaissait dans la recherche
  globale. `searchAll` ne filtre désormais que les équipes/postes `ACTIVE` et
  écarte les tâches `ANNULEE`. +1 test backend. Backend **430 ✓**.
- **`verify-beta.sh` validé 18/18** contre la stack bêta locale (API :8090, base
  `discipolat_beta`) — voir session « diagnostic déploiement bêta Render ».
- **Script `launch-beta.sh`** ajouté (racine) : relance l'API bêta locale
  (profil beta, :8090, base discipolat_beta, log beta-api.log) — pattern
  double-fork de launch-backend.sh.

## SESSION 2026-08-14 (fin) — diagnostic déploiement bêta Render (2e vérification)

- **Services bêta : toujours inexistants** — `https://discipolat-beta.onrender.com`
  et `https://discipolat-beta-api.onrender.com/api/v1/public/meta` → **404 instantané**
  (pas un cold start). Le Sync Blueprint Render n'a jamais créé les services.
- **Pipeline GitHub vérifié via l'API** (token local `repo`+`workflow`) :
  - `CI/CD` : Backend ✓ · Frontend ✓ · Docker Build & Push ✓ · **Deploy to Render ✗**
    (`##[error]Secrets RENDER_API_KEY et RENDER_API_SERVICE_ID manquants`)
  - `Deploy Bêta to Render` : run « success » mais step **Trigger skipped**
    (`RENDER_API_KEY` / `RENDER_BETA_API_SERVICE_ID` absents)
  - **0 secret GitHub défini** (`GET /actions/secrets` → total_count 0).
- **`verify-beta.sh` validé 18/18 ✓** contre une stack bêta locale relancée
  (API profil `beta` sur :8090, base `discipolat_beta` — script `launch-beta.sh`,
  log `beta-api.log`) : meta beta, 7 comptes démo, switch-role, feedback,
  RBAC 403/401, isolation départements, reset (88 tables) + invariant feedback
  conservé. Le script est prêt à tourner contre l'URL publique dès sa création.
- **Blocage (2 causes)** : (1) services bêta jamais créés sur Render — Sync
  Blueprint à faire par l'utilisateur (Dashboard Render → Blueprints) ;
  (2) secrets GitHub absents : `RENDER_API_KEY`, `RENDER_API_SERVICE_ID`
  (prod) et `RENDER_BETA_API_SERVICE_ID` (bêta).
- Processus locaux actifs : API prod locale :8080 (log `backend-run.log`),
  API bêta locale :8090 (`launch-beta.sh` / `beta-api.log`), Vite :5173,
  Postgres :5433, Redis :6379, MailHog :8026.

## SESSION 2026-08-14 (fin) — parité mobile : événements, équipes liées, recherche globale

- **Onglet « Événements »** dans `DepartmentManagementScreen` : liste À venir/Passés
  (GET /events/department/{id}) + création avec `departmentId` (POST /events,
  types, date picker, lieu, description).
- **Équipes liées** : le formulaire d'équipe (Organisation) affiche le sélecteur
  « Événement lié » pour EQUIPE_TEMPORAIRE (chargé à la volée, dates pré-remplies,
  `eventId` envoyé) et l'arbre affiche « Événement : <titre> » sur les équipes liées.
- **Recherche globale** : champ « Recherche rapide » (≥ 2 caractères) → panneau de
  résultats par catégorie (membres/équipes/postes/tâches/événements) depuis
  GET /departments/{id}/search, navigation vers le dossier membre.
- Commit `975f126` poussé. Mobile **106 tests ✓** (4 nouveaux widget tests),
  `flutter analyze` 0 issue. Backend/web inchangés (429 ✓ / 180 ✓).

## SESSION 2026-08-14 (fin) — rappels automatiques des événements de département (V60)

- **V60** : `department_settings.event_rappel_jours` (INTEGER, défaut 1, bornes 0–30,
  0 = rappel désactivé pour le département).
- **Scheduler `sendEventReminders` étendu** : le rappel J-1 générique aux inscrits est
  conservé ; en plus, pour chaque événement rattaché à un département, le **responsable**
  reçoit une notification `EVENEMENT_RAPPEL` (IN_APP) N jours avant, où N est lu dans
  `department_settings` (jamais hardcodé). Déduplication one-shot par événement et par
  responsable (`existsByDestinataireIdAndTypeAndEntiteReferenceIdAndEntiteReferenceType`).
  `TypeNotification.EVENEMENT_RAPPEL` ajouté. Requête dédiée
  `findByDepartmentIdIsNotNullAndDeletedFalseAndDateDebutBetween(now+1j, now+31j)`
  (fenêtre maximale = délai max configurable).
- **Frontend web** : onglet Paramètres → carte « Rappel automatique des événements »
  (0–30 jours, descriptions) + payload PUT étendu.
- **Mobile** : onglet Paramètres des Outils → champ « Rappel événement (0–30 jours) »
  + PUT étendu (parité).
- **Vérification e2e locale** : API redémarrée (migration V60 appliquée,
  `now at version v60`), PUT `eventRappelJours=7` → 200, `99` → 422
  `EVENT_RAPPEL_JOURS_OUT_OF_RANGE`.
- Commit `33120e6` poussé. Tests : backend **429 ✓** (dont 4 nouveaux scheduler
  + 4 settings), frontend **180 ✓** (+1), mobile **102 ✓** (+1).
- **Stack locale relancée** : API :8080 (log `backend-run.log`), Vite :5173,
  Postgres :5433, Redis :6379, MailHog :8026 (comptes démo `password123`).

---

## SESSION 2026-08-14 (DMS : objectifs, rapports du responsable, dossier mobile)

Reprise du système de gestion des départements (DMS). Travail en cours
(déjà committé le 2026-08-13 dans `4335d3a` : liste/detail/stats/rapport,
dossier membre web + mobile partiel).

### Objectifs de progression (membre → département)

- **Backend** : entité `DepartmentMemberObjective` + repo + migration **V53**
  (`department_member_objectives`), requêtes `POST /departments/{id}/members/{memberId}/objectives`,
  `PUT /departments/{id}/objectives/{objectiveId}`, `DELETE .../objectives/{objectiveId}`,
  liste dans le dossier. Statuts `A_FAIRE / EN_COURS / ATTEINT / ANNULE`, avancement
  0–100 %, drapeau `enRetard` (échéance dépassée + statut ouvert).
- **Frontend web** : onglet **Objectifs** du dossier membre (création, slider
  d'avancement, changement de statut, suppression, compteurs En cours/Atteints/
  Moyenne).

### Rapports du responsable sur un membre

- **Backend** : entité `DepartmentMemberReport` + repo + migration **V54**
  (`department_member_reports`), types `COMPORTEMENT / ASSIDUITE / CAPACITE /
  PROGRESSION / INCIDENT / DISCIPLINE / RECOMMANDATION`. Endpoints
  `GET|POST /departments/{id}/members/{memberId}/reports`, `DELETE /departments/{id}/reports/{reportId}`.
  Traçabilité : auteur + activité `MEMBER_REPORT_ADDED` dans le journal.
- **Frontend web** : onglet Rapports du dossier = rapports du faiseur **+**
  rapports du responsable (ajout/type, suppression), injectés dans le payload
  du dossier sous `rapportsResponsable`.

### Mobile (Flutter) — dossier membre complet

- **Nouvel écran** `DepartmentMemberDossierScreen` (`/departments/:id/members/:memberId`,
  rôles ADMIN/PASTEUR/RESPONSABLE) : Profil (identité, affectations actives,
  alertes, lien fiche âme), **Objectifs** (créer / slider / statut / supprimer),
  **Rapports** (responsable + faiseur, créer/supprimer), **Notes** (ajout/
  suppression), Activité.
- **Onglet « Membres »** ajouté à la gestion de département (liste → dossier).
- **Fix robustesse** : `SectionTitle` partagé rendu flexible (titre `Expanded` +
  ellipsis) — évite le débordement horizontal sur écrans étroits (détecté par
  tests).
- **7 tests widget** ajoutés (`department_member_dossier_screen_test.dart`).

### État des tests (2026-08-14)

- Backend : **385 tests ✓** (0 fail / 0 error) — inclut 2 nouveaux tests dossier
  (rapports responsable). NB : Mockito + JDK récents → erreurs sporadiques
  « class redefinition » sur transferts/objectifs en exécution complète,
  **passent en isolation** (flakiness environnementale, pas un bug de code).
- Frontend web : `tsc -b` ✓, **167 tests vitest ✓**, `npm run build` ✓.
- Mobile : **96 tests ✓** (89 + 7), `flutter analyze` sans issue.

### Bloc 2026-08-14 committé (`6f70ac2`)

- **V53/V54 + objectifs/rapports membre** : commit `6f70ac2` (26 fichiers,
  +4452). Détails ci-dessus. Tests complets relancés : backend 385 ✓,
  frontend 167 ✓, mobile 96 ✓.
- **Fix `Alert.priorite`** (`@Builder.Default`) : le scheduler
  « tâches en retard » échouait en production (violation NOT NULL de la
  colonne `priorite` — la valeur par défaut Java n'était pas appliquée par
  Lombok `@Builder`). Validé en e2e réel : le run scheduler crée désormais
  les alertes sans erreur (2 alertes TACHE_EN_RETARD créées, 0 erreur).
- **Vérification e2e réelle** (backend beta local :8080, DB :5433) :
  création membre → dossier complet (18 sections) → notes → annonces →
  stats → export CSV → import preview+effectif → équipes (permanentes +
  sous-départements) → postes → affectations → tâches → candidats →
  objectifs → rapports → dashboard responsable → **permissions**
  (responsable1 ne voit que SES 2 départements, 404/403 ailleurs). ✓

### Prochaines actions possibles

- Pousser le WIP (objectifs + rapports + dossier mobile) — **FAIT** (`6f70ac2`).
- Continuer le DMS : rapports de département (hebdo/mensuel synthèse auto),
  checklists, inventaire matériel, recherche globale, paramétrage des
  workflows de transfert, améliorations mobiles (annonces/transferts/
documents du dossier).

## SESSION 2026-08-14 (suite) — parité mobile complète du DMS

Clôture de la checklist « départements » côté mobile (le web l'avait déjà) :

### Nouvel écran `DepartmentDetailScreen` (`/departments/:id`, ADMIN/PASTEUR/RESPONSABLE)

Parité de `DepartmentDetailPage` web :
- **Header** : nom, description, responsable (+ email).
- **KPIs** (GET `/kpi`) : Actifs / En intégration / En veille / Décrochés /
  Nvx convertis / Faiseurs + **Participation** (taux de soumission, présence,
  rapports famille) avec barres de progression.
- **Alerte âmes non assignées** (GET `/unassigned`) → lien fiche âme.
- **Annonces** (GET|POST|DELETE `/announcements`) : publication avec cible
  TOUS/ÉQUIPE/POSTE (dropdowns équipes/postes depuis `/management`), suppression.
- **Alertes intelligentes** (GET `/alerts/smart`) : priorité HAUTE/MOYENNE →
  lien dossier du membre.
- **Membres** (GET `/members?size=200`) : recherche, carte → dossier,
  **retrait** (DELETE `/members/{memberId}`) avec confirmation.
- **Ajouter un membre** (bottom sheet) : **Nouveau membre** (POST `/members/create` :
  nom, prénom, email, téléphone, profession, situation familiale, type de disciple,
  statut, dates) ou **Personne déjà inscrite** (recherche GET `/members/candidates?q=`
  ≥ 2 caractères → POST `/members {soulId}`).
- **Familles** (depuis `/detail`) + boutons Gérer / Stats / Rapport.

### Dossier membre — onglet Documents + retrait

- **Onglet Documents** ajouté : documents du dossier (ouvrables via `showUrlLink`)
  + notes de la fiche âme (`notesDisciple`).
- **Retirer du département** (menu ⋮ dans l'en-tête, si `membreActif`) →
  DELETE puis retour au détail.

### Navigation & routing

- Liste des départements → détail (`/departments/:id`) au lieu de `/manage`
  directement (parité web : liste → détail → gestion).
- Route + permissions ajoutées dans `app.dart`.

### État des tests

- Mobile : `flutter analyze` **0 issue** (les erreurs Const/`Colors.violet`/
  `num→double` corrigées).
- Backend et web inchangés (aucune modification).


## SESSION 2026-08-14 (bloc V55) — rapports de département, checklists, inventaire

### Rapports de département (synthèses sauvegardées + export CSV)

- **Migration V55** (`department_reports`) : types HEBDOMADAIRE / MENSUEL /
  TRIMESTRIEL / ANNUEL / EVENEMENT / INCIDENT / DISCIPLINE / ACTIVITE /
  EFFECTIF / ASSIDUITE / PERFORMANCE / SYNTHESE, statuts BROUILLON / SOUMIS /
  ARCHIVE, période début/fin, contenu texte.
- **Backend** `DepartmentReportingService` :
  - `POST /departments/{id}/reports/generate` — génère une **synthèse sur les
    données réelles** (effectif, actifs, nouveaux 30 j, intégration/décrochés,
    assiduité + taux, tâches ouvertes/terminées/en retard, objectifs de
    progression, discipline par catégorie, équipes, événements à venir) et la
    sauvegarde. Période calculée selon le type (semaine lundi→dimanche, mois,
    trimestre, année, ou personnalisée).
  - `POST /departments/{id}/reports` — sauvegarde d'un bilan rédigé manuellement.
  - `GET /departments/{id}/reports/list` · `DELETE .../reports/saved/{id}` ·
    `GET .../reports/saved/{id}/export` (CSV : en-têtes + période + contenu).
- **Frontend web** `DepartmentReportPage` : section « Synthèses sauvegardées »
  (choix du type → générer, liste des rapports, badge Soumis/Brouillon, export
  CSV par téléchargement, suppression).

### Checklists du département

- **Migration V55** (`department_checklists` + `department_checklist_items`,
  cascade DELETE) : cibles GENERAL / TACHE / EVENEMENT / EQUIPE / MEMBRE,
  statut OUVERTE / TERMINEE, items ordonnés avec `fait`.
- **Backend** : CRUD complet (création avec items, renommage/statut, ajout d'item,
  **toggle avec clôture automatique quand tous les items sont cochés**,
  suppression d'item et de checklist), progression 0–100 % calculée.
- **Frontend web** : onglet **Checklists** de la gestion du département (création
  multi-items, cible, cochage, progression, ajout d'élément, terminer/supprimer).
- **Mobile** : écran « Outils & rapports » (onglets Rapports / Checklists /
  Inventaire) — voir plus bas.

### Inventaire matériel du département

- **Migration V55** (`department_equipment`) : nom, description, quantité, état
  (NEUF / BON / USAGE / REPARATION / HORS_SERVICE), responsable, affectation à
  un membre, localisation, date d'acquisition, traçabilité `created_by`.
- **Backend** : CRUD complet dans `DepartmentReportingService`.
- **Frontend web** : onglet **Inventaire** de la gestion (KPIs : équipements,
  articles, en réparation, hors service ; grille de cartes avec état coloré,
  modification/suppression).
- **Mobile** : onglet Inventaire de l'écran Outils.

### Mobile — écran `DepartmentToolsScreen` (`/departments/:id/tools`)

- 3 onglets : **Rapports** (liste des synthèses + génération), **Checklists**
  (création multi-items, cochage), **Inventaire** (CRUD équipements).
- Bouton « Outils » (icône inventaire) ajouté à la gestion de département.
- Dossier membre : onglets **Annonces** et **Transferts** ajoutés (parité web).

### Fix découvert en test e2e réel

- `DepartmentChecklistItemRequest` : `libelle @NotBlank` cassait le toggle
  (seul `fait` envoyé) → contrainte retirée, validation manuelle à l'ajout.

### Vérification e2e réelle (backend beta :8080, V55 appliquée)

- Génération synthèse hebdomadaire → contenu réel (2 membres, 1 actif, 2
  nouveaux 30 j, taux présence, 2 tâches, 1 objectif, 4 équipes) ✓
- Liste rapports, export CSV ✓ · checklist (création 3 items → toggle → add) ✓
- Inventaire : création + mise à jour (état/quantité/localisation) ✓
- Rapport manuel (INCIDENT, statut SOUMIS) ✓
- **Permissions** : responsable → 200 sur SES départements, **404 sur les
  départements d'un autre responsable** (reports/checklists/equipment) ✓

### État des tests (2026-08-14, bloc V55)

- Backend : **396 tests ✓** (385 + 11 nouveaux `DepartmentReportingServiceTest`)
- Frontend web : `tsc -b` ✓ · **167 tests vitest ✓** · `npm run build` ✓
  (mock `/reports/list` ajouté au test `DepartmentReportPage`)
- Mobile : `flutter analyze` **0 issue** · **99 tests ✓** (96 + 3 nouveaux
  `department_tools_screen_test`)

---

## SESSION 2026-08-15 — correctifs testeur : page Outils (404), modale rapport, présence à un événement, tableau membres

### 1. Page « Outils » du département créée (corrige le 404)

- Le bouton **Outils** (`/departments/:id/tools`) renvoyait une **404** : aucune
  route ni page web n'existaient (seul le mobile avait l'écran).
- Création de `DepartmentToolsPage` (parité mobile `DepartmentToolsScreen`) :
  onglets **Rapports / Checklists / Inventaire / Documentation / Paramètres**,
  masqués selon les sous-modules désactivés par l'admin.
- Composants réutilisés : `SavedReportsSection` (exporté depuis
  `DepartmentReportPage`) + `ChecklistsTab`/`InventoryTab`/`DocumentsTab`/
  `SettingsTab` (exportés depuis `DepartmentManagementPage`).
- Route ajoutée dans `App.tsx` (ADMIN/PASTEUR/RESPONSABLE) + lazy loading.

### 2. Modale « Modifier le rapport » corrigée + police

- Le bloc **« Indicateurs de la semaine »** recouvrait le bouton Enregistrer de la
  modale : `glass-card` = `backdrop-blur` + `transform` au survol → stacking
  context ; la modale `z-50` rendue DANS la carte était piégée dessous.
- Fix : modale rendue via **`createPortal(document.body)`** + `z-[100]` (jamais
  plus recouverte par les cartes suivantes). Le bloc Indicateurs a aussi été
  déplacé **avant** les synthèses sauvegardées (lisibilité).
- Police du contenu : `font-mono text-xs` → `text-sm leading-relaxed` lisible.

### 3. Présence des membres à un événement du département (V61)

- Migration **V61** `department_event_attendance` (department_id, event_id,
  soul_id, present, marked_by, unique(department,event,soul)).
- `GET /departments/{id}/events/{eventId}/attendance` : feuille de présence des
  membres actifs du département (present = true/false/null).
- `PUT /departments/{id}/events/{eventId}/attendance` : pointage présent/absent.
  Permissions : responsable du département / super-utilisateur, **ou** acteur de
  l'espace de l'âme (chef de sa famille, son faiseur) via `WorkspaceScopeService`.
- Contrôleur dédié (`DepartmentEventAttendanceController`) : GET réservé aux
  rôles de gestion, PUT ouvert à CHEF_DE_FAMILLE/FAISEUR (vérif service).
- Frontend : onglet Événements → bouton **« Présences »** par événement → modale
  listant les membres avec boutons Présent/Absent (marquage one-click, stats
  Total/Présents/Absents/Non pointés). Traçabilité dans le journal d'activité
  (`EVENT_ATTENDANCE_MARKED`).
- **E2E réel validé** : sheet 200 → marquage Aya présent 200 → presents:1 ;
  chef GET/PUT = 403 ; événement de test supprimé (204).

### 4. Tableau « Membres du département » réorganisé

- Lignes aérées (px-4/py-3), **avatar initiales**, contact sous le nom,
  badges Statut/Type cohérents, colonne Actions groupée (bouton Dossier + retrait),
  entêtes majuscules espacées, `divide-y` pour les lignes.

### État des tests (2026-08-15)

- Backend : **435 ✓** (430 + 5 `DepartmentManagementServiceTest` : marquage,
  âme hors département refusée, événement d'autre département refusé, faiseur
  autorisé, feuille de présence) · migration V61 appliquée en local.
- Frontend : `tsc -b` ✓ · **186 tests vitest ✓** (+6 : page Outils ×4,
  pointage présence ×1, police modale ×1) · `npm run build` ✓.
- Mobile : inchangé (106 ✓).

### Suite — feuille de présence étendue au dossier membre et au dashboard responsable

- **Backend** : `GET /departments/{id}/members/{memberId}/event-attendance`
  (présence d'UN membre sur tous les événements du département, tri date
  décroissante, compteurs calculés uniquement sur les événements visibles —
  les pointages d'événements soft-deleted sont ignorés).
  `EventRepository.findByDepartmentIdAndDeletedFalse(UUID)` (liste) ajouté.
- **Dossier membre** (`DepartmentMemberDossierPage`) : onglet **Présences** →
  section « Présence aux événements du département » : chaque événement avec
  statut (Présent/Absent/Non pointé) + boutons de marquage one-click
  (réutilise `PUT .../events/{eventId}/attendance` avec `soulId`).
- **Dashboard responsable** (`ResponsableDashboardPage`) : carte
  « Présence aux événements » (après la saisie hebdo) listant les événements
  du département actif avec bouton **Présences** ouvrant la même modale
  (`EventAttendanceModal` exportée depuis `DepartmentManagementPage`).
- **E2E réel** : mark 200 → member event-attendance `total:1 presents:1
  nonMarques:0` (compteurs corrects après fix), cleanup 204.
- Tests : backend **437 ✓** (+2), frontend **188 ✓** (+2 dossier),
  `tsc -b` ✓, `npm run build` ✓. Commit `28e8b89` poussé.

### Suite — « Marquer tous présents » + export CSV de la feuille de présence

- **Backend** : `POST /departments/{id}/events/{eventId}/attendance/mark-all?present=true`
  (marque TOUS les membres actifs présents/absents en une opération, upsert
  idempotent, traçabilité `EVENT_ATTENDANCE_MARK_ALL`) ;
  `GET .../attendance/export` → CSV UTF-8 (BOM) `Membre;Présence`
  (Présent/Absent/Non pointé), permissions ADMIN/PASTEUR/RESPONSABLE.
- **Frontend** : dans la modale de pointage, barre d'actions « **Marquer tous
  présents** » (avec confirmation visuelle du nombre) + « **Exporter CSV** »
  (téléchargement blob).
- **E2E réel** : mark-all `marques:2` → feuille 2/2 présents · export CSV
  `Aya Kouassi;Présent / Ibrahim Traoré;Présent` (BOM) ✓ · cleanup 204.
- Tests : backend **439 ✓** (+2), frontend **189 ✓** (+1 mark-all/export),
  `tsc -b` ✓, `npm run build` ✓. Commit `24959dd` poussé.

### Suite — mêmes actions dans le dossier membre

- **Backend** : `POST /departments/{id}/members/{memberId}/event-attendance/mark-all?present=true`
  (marque UN membre présent/absent à TOUS les événements du département, upsert
  idempotent, traçabilité `EVENT_ATTENDANCE_MARK_ALL_MEMBER`) ;
  `GET .../event-attendance/export` → CSV UTF-8 (BOM) `Événement;Date;Statut`.
- **Dossier membre** (onglet Présences → section événements) : boutons
  « **Marquer tous présents** » + « **Exporter CSV** » (parité avec la modale).
- **E2E réel** : member mark-all `marques:1` → `nonMarques:0` ✓ · export CSV
  `QA membre mark-all;2026-08-25;Présent` (BOM) ✓ · cleanup 204.
- Tests : backend **441 ✓** (+2), frontend **190 ✓** (+1 dossier),
  `tsc -b` ✓, `npm run build` ✓. Commit `39c2810` poussé.

## SESSION 2026-08-15 (suite) — présences, historique stylisé, évaluations par utilisateur, fiche utilisateur, CRM cliquable

### 1. Saisie des présences : statut « Absent » modifiable (web)

- **Cause** : la feuille de présence hebdomadaire traitait les membres non
  pointés comme « Absents » par défaut et les renvoyait comme absents — le
  responsable ne pouvait plus les repasser Présent. Le backend faisait pourtant
  bien des upserts (vérifié e2e : absent→présent OK côté API).
- **Fix** : `ResponsableDashboardPage` — seuls les membres **explicitement
  pointés** sont envoyés ; boutons Présent/Absent **toujours actifs** dans les
  modales de pointage (`DepartmentManagementPage`, `DepartmentMemberDossierPage`,
  `EventsPage`).

### 2. Modale Historique d'un utilisateur stylisée (web + mobile)

- Remplacé l'affichage JSON brut (`toString()`/`monospace`) par une **timeline
  soignée** : résumé (rôle, chef de famille, membre depuis), âmes actuellement
  suivies (avatar initiales, statut coloré), sorties de suivi (motif + date).
- Web : `UsersPage` (modale Historique) ; mobile : `_ActionModal` de
  `users_list_screen.dart` + **icône historique ajoutée** dans la carte
  utilisateur (rôle FAISEUR, parité web) + bouton « Fermer ».

### 3. Évaluations par utilisateur (backend) — V63

- Migration **V63** : contrainte d'unicité **UNIQUE(evaluateur_id, utilisateur_cible_id)**
  (une seule évaluation par binôme — la table devient un upsert).
- `EvaluationService` : `upsertEvaluation` (**PUT /evaluations/{userId}** : crée
  si absente, modifie si présente), périmètre élargi (top-down : gestionnaire du
  collaborateur **+** bottom-up : subordonnés), `mesEvaluations` pour l'évaluateur,
  refus si `evaluateurId == userId` (403). Catégories RESPONSABLE/CHEF_FAMILLE/
  FAISEUR/MEMBRE par défaut selon le rôle cible.
- `GET /users/{id}/detail` (UserService) : profil complet + âme liée + âmes
  suivies si faiseur + sorties + départements dirigés avec membres (si
  responsable) + famille gérée (si chef) + évaluations reçues par catégorie +
  `monEvaluation` (ma note sur cette personne) + `userId` sur chaque âme/membre
  (navigation).
- **Validé e2e réel** : create → modify → deny (même évaluateur) ✓, détail
  utilisateur complet ✓. Tests : `EvaluationServiceTest` (upsert + périmètre +
  refus), `UserServiceTest` adapté au nouveau constructeur.

### 4. Fiche utilisateur cliquable (web + mobile)

- **Web** : composant `UserDetailModal` (bouton « Fiche » dans `UsersPage`) —
  identité, âme liée, évaluation (étoiles + commentaire, **donner si absente /
  modifier si présente**), âmes suivies, sorties, départements + membres,
  famille gérée.
- **Mobile** : écran `UserDetailScreen` (`/users/:id`) — carte utilisateur
  cliquable → fiche complète (mêmes sections, étoiles interactives + PUT).
  3 nouveaux widget tests (`user_detail_screen_test.dart`).

### 5. Blocs CRM cliquables (web + mobile)

- **Web** : cartes stats du CRM FAISEUR (filtre + scroll vers la liste des
  disciples), cartes du dashboard CHEF DE FAMILLE (disciples/faiseurs/rapports),
  cartes + anniversaires du RESPONSABLE (modale/listes), dashboard PASTEUR
  (blocs croissance → /souls, familles à risque → /families, etc.).
- **Mobile** : `GlassStatCard` accepte `onTap` ; tous les dashboards branchés :
  PASTEUR (croissance → /souls, présence → /departments, rapports → /reports,
  alertes → /alerts, départements/familles/faiseurs/familles à risque),
  RESPONSABLE (stats → départements/rapports, anniversaires → gestion),
  CHEF DE FAMILLE (stats → souls/users/rapports, charge de travail → /users,
  disciples → fiche âme), FAISEUR (stats → filtres/rapports/alertes, légende du
  camembert → filtre, alertes → fiche de l'âme), dashboard racine (KPIs →
  modules).

### 9. Accès de test local + tunnel public Cloudflare (serve-public.sh)

- **Accès locaux** : API `http://localhost:8080` · Web `http://localhost:5173` ·
  Postgres Docker `localhost:5433` (discipolat/discipolat_secret) · Redis :6379.
  Comptes démo (mot de passe `password123`) : admin@, pasteur@, responsable@
  (Audiovisuel), chef@, faiseur@, membre@, paul@ (multi-rôles) — + les ~70
  comptes du seed volumineux (responsable10-21@, chef10-25@, faiseur10-39@).
- **Tunnel public** : `scripts/serve-public.sh` — démarre la stack locale puis un
  **tunnel Cloudflare sans compte** (`cloudflared tunnel --url :5173`, binaire
  téléchargé dans /tmp) et affiche l'URL `*.trycloudflare.com` + les accès.
  Valide en réel : page 200, `/api/v1/public/meta` 200, login responsable 200
  via l'URL publique.
- **Fix requis** : Vite bloquait l'hôte du tunnel (`Blocked request… not allowed`)
  → `vite.config.ts` : `server.allowedHosts: ['.trycloudflare.com', '.cloudflare.com',
  'localhost']`. `tsc -b` ✓.
- **Limite** : l'URL trycloudflare est **temporaire et change à chaque run** (tant
  que le script tourne, elle est active). Lien permanent : `cloudflared tunnel
  login` + tunnel nommé (compte Cloudflare) — voir docs/DEPLOYMENT.md §Tunnel.
  Relance : `bash scripts/serve-public.sh`.

### 8. QA navigateur réel des nouveaux flux (e2e-browser-fiche) + fix 422 « /evaluations/me »

- **Nouveau script `scripts/e2e-browser-fiche.js`** (18 étapes, Chrome réel) :
  login admin → liste utilisateurs → fiche utilisateur (identité + évaluation +
  âmes suivies) → **évaluation donner → badge « Vous avez évalué » → modifier**
  → login responsable (contexte incognito, localStorage isolé) → **saisie des
  présences hebdo : Absent → enregistré → Présent → enregistré** (vérifie que
  le statut Absent reste modifiable). Résultat : **18/18 ✓, 0 erreur console**.
- **Bug réel découvert et corrigé** : `GET /evaluations/me` renvoyait **422**
  pour tout non-super-utilisateur (RESPONSABLE/CHEF_DE_FAMILLE/FAISEUR) —
  `getEvaluationsForUser(selfId)` déclenchait la vérification d'auto-évaluation
  (« vous ne pouvez pas vous évaluer »). Fix : **mes propres évaluations
  (anonymisées) toujours autorisées**, le contrôle de périmètre ne s'applique
  qu'aux AUTRES utilisateurs. +2 tests (`EvaluationServiceTest` 11 ✓ :
  self autorisé sans vérif de droit, autre utilisateur sans droit refusé).
- Rejouable : `bash /tmp/run-e2e-fiche.sh` (démarre API :8080 + web :5173,
  exécute le script, arrête la stack). Données de test laissées en base de dev :
  évaluation admin → faiseur13, présence Aminata Assi (restaurée Absent).

### 7. Fiche utilisateur étendue : objectifs, rapports du responsable, notes, documents

- **Backend** : `DepartmentDossierService.dossierUtilisateur(soulId, accessibleDeptIds)`
  (objectifs + rapports du responsable + notes, agrégés par département d'appartenance
  ACTIF — réutilise les helpers du dossier membre, aucun assert : scoping fourni par
  l'appelant) + `dossierDocuments(soulId)` (pièces jointes du dossier membre).
  `GET /users/{id}/detail` renvoie désormais `dossier` (par département, avec
  `departmentNom`) et `dossierDocuments`. **Scoping** : super-utilisateur → tous
  les départements, RESPONSABLE → ses départements (`accessibleDepartmentIds`),
  chef de famille / faiseur → aucun.
- **Validé e2e réel** (admin sur le compte membre, lien temporaire + objectif +
  rapport PROGRESSION + note insérés puis nettoyés) : `dossier` peuplé avec
  « Département Jeunesse | objectifs: ['Objectif QA fiche'] | rapportsResp:
  ['PROGRESSION'] | notes: 1 » ✓.
- **Web** (`UserDetailModal`) : section « Dossier du membre » — objectifs (statut,
  barre d'avancement, échéance, badge en retard), rapports du responsable (type +
  contenu + auteur), notes, documents ouvrables (nouvel onglet).
- **Mobile** (`UserDetailScreen`) : mêmes sections (progress bar, badges de statut
  colorés, documents ouverts via `showUrlLink`). +1 test widget (dossier).
- Tests : backend `DepartmentDossierServiceTest` 16 ✓ + `UserServiceTest` 15 ✓
  (branche super-utilisateur), frontend `tsc -b` ✓ + 190 vitest ✓, mobile
  analyze 0 issue + **111 tests ✓**.

### 6. Badge moyenne d'évaluation dans la liste des utilisateurs (mobile)

- Le web affichait déjà la colonne « Évaluation » (étoiles + moyenne + tooltip
  par catégorie, via `GET /users/evaluation-scores?userIds=…`) — parité mobile
  ajoutée : la liste des utilisateurs charge les scores groupés (une requête
  pour toute la page, best-effort) et affiche un **badge doré** par carte :
  5 étoiles + moyenne (`4.5`) + compteur d'évaluations (`(3)`), masqué si
  l'utilisateur n'a aucune évaluation. +1 test widget.

### État des tests (2026-08-15, fin)

- Backend : `EvaluationServiceTest` (9 ✓) + `UserServiceTest` (13 ✓) ·
  migrations V63 appliquées en local.
- Frontend : `tsc -b` ✓ · **190 tests vitest ✓** (suite complète).
- Mobile : `flutter analyze` **0 issue** · **110 tests ✓** (109 + 1 badge
  moyenne ; 106 + 3 fiche utilisateur + 1 badge).
