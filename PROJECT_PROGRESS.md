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

## NEXT ACTION (session 2026-08-12)

> L'utilisateur exécute : **Dashboard Render → Blueprints → Sync** (ou crée
> manuellement `discipolat-beta-db` + re-sync). Étapes exactes dans
> `docs/DEPLOYMENT.md` §Bêta (activer l'environnement bêta). Une fois les
> services créés, lancer `./scripts/verify-beta.sh` pour valider la chaîne
> complète, ajouter les secrets GitHub RENDER_API_KEY /
> RENDER_BETA_API_SERVICE_ID, vérifier l'URL publique et rédiger le rapport
> final (comptes, rôles, fonctionnalités, version, tests, problèmes connus).
