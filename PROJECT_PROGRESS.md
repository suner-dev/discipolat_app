# PROJECT_PROGRESS — Discipolat (Productisation + Bêta-test public)

> Fichier de checkpoint : état exact du travail à tout moment, pour reprise
> immédiate d'une session. Mis à jour à chaque étape stable.

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

- `2956527` docs: checkpoint — diagnostic déploiement bêta Render (services non créés)
- Prochain commit : **3.20.0 mobile** (PlatformMeta + metaProvider + BetaBadge + feedback sheet + comptes démo conditionnels + 10 tests) — non encore poussé

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
