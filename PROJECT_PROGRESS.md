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
- [x] Frontend web : fonctionnalités bêta (badge, comptes démo conditionnels, widget feedback, panneau admin feedback, mode testeur) — **153 tests vitest ✓**
- [x] V50 committée (`cf8df41`) et **poussée sur GitHub**
- [x] Vérification end-to-end locale du flux bêta complet (API profil `beta` + Postgres dédié)
- [x] Audit sécurité live : isolation départements/familles, RBAC, garde-fous reset — OK
- [x] Test `BetaResetServiceTest` ajouté (3 tests — double garde reset)
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

- `cf8df41` feat: bêta-testing public V50 — feedback testeurs, meta plateforme, comptes démo conditionnels, reset env bêta, services Render bêta (DB/API/frontend isolés)
- Travail non committé actuellement : `BetaResetServiceTest.java` (nouveau) + `docs/CHANGELOG.md` (entrée 3.19.0) + ce fichier

## PROBLÈMES CONNUS / BLOCAGES

- **Déploiement bêta public** : nécessite l'action de l'utilisateur (Sync Blueprint Render + secrets GitHub `RENDER_API_KEY`, `RENDER_BETA_API_SERVICE_ID`). Aucun credential Render présent dans l'environnement de travail.
- Limite plan Free Render : 1 Postgres gratuit/workspace → `discipolat-beta-db` peut nécessiter un plan Starter (~7 $/mois) si `discipolat-db` occupe déjà le slot gratuit.
- Cold start API bêta (~1 min au premier accès) — volontaire (pas de keep-alive bêta, quota 750 h/mois).

## AMÉLIORATIONS IDENTIFIÉES (non bloquantes)

- Mobile (Flutter) : widget feedback + badge bêta (priorité basse — le mobile utilise la même API et fonctionne déjà).
- Affichage de la version d'app dans le panneau admin feedback (déjà stockée côté serveur).

## NEXT ACTION

> **Reprise (à la prochaine session)** : demander à l'utilisateur de lancer le
> **Sync Blueprint Render** (crée discipolat-beta-db, discipolat-beta-api,
> discipolat-beta) puis de fournir les secrets GitHub `RENDER_API_KEY` et
> `RENDER_BETA_API_SERVICE_ID`. Ensuite : pousser le commit V50.1
> (BetaResetServiceTest + CHANGELOG 3.19.0) si ce n'est pas déjà fait, vérifier
> l'URL publique `https://discipolat-beta.onrender.com` (connexion comptes démo,
> feedback, reset), et fournir le rapport final (URL, comptes, rôles, version,
> état des tests). Si l'utilisateur ne peut pas déployer : audit continu du
> produit (zéro bouton mort, responsive, performance) et nouvelles corrections.
