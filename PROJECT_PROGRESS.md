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
- [x] Backend : module feedback (V40/V41), meta publique, beta admin/reset (WIP validé : compile)
- [~] Frontend web : fonctionnalités bêta (badge, comptes démo conditionnels, widget feedback, panneau admin feedback, mode testeur)
- [ ] Déploiement bêta Render (services bêta dans render.yaml — sync Blueprint à faire par l'utilisateur)
- [ ] Vérification finale de l'URL publique + rapport final

## FONCTIONNALITÉS TERMINÉES

### Backend (fichiers untracked, compile OK — à committer)
- `POST /api/v1/feedback` (soumission authentifiée, catégorie/priorité/sujet/description + contexte navigateur/OS/appareil/page)
- `GET /api/v1/admin/feedback` + `/stats` + `PATCH .../{id}/status` (ADMIN/PASTEUR)
- `GET /api/v1/public/meta` (appName, version, environment, betaMode, demoAccountsEnabled — public)
- `GET /api/v1/admin/beta/status` + `POST /api/v1/admin/beta/reset` (ADMIN, activé uniquement par le profil `beta`)
- Migration V40 : table `feedbacks` ; V41 : module `FEEDBACK` + menu `admin-feedback`
- `application.yml` : `app.environment`, `app.version`, `app.beta-testing.*`, profil `beta` (demo accounts + reset actifs)
- `DataInitializer` : comptes démo existants (admin, pasteur, responsable, chef, faiseur, membre, paul) — mot de passe `password123`

### Frontend web (nouveau)
- [x] `MetaContext` : fetch `/public/meta` (badge bêta, comptes démo conditionnels)
- [x] `BetaBadge` : badge BÊTA (Navbar, Landing, Login)
- [x] LoginPage : comptes de démo affichés **uniquement si** `demoAccountsEnabled` (env bêta), liste complétée (Admin, Paul multi-rôles)
- [x] `FeedbackWidget` : bouton flottant + modale de soumission (contexte auto : page, navigateur, OS, appareil)
- [x] `AdminFeedbackPage` : `/admin/feedback` (stats + liste + changement de statut)
- [x] Bandeau "mode testeur" dans MainLayout (env bêta uniquement, masquable)
- [x] Route `/admin/feedback` + carte dans l'Admin Dashboard

## FONCTIONNALITÉS EN COURS / RESTANTES

- [ ] Déploiement bêta : ajouter les services `discipolat-beta-db`, `discipolat-beta-api`, `discipolat-beta` (static site) dans `render.yaml` → l'utilisateur doit lancer **Sync Blueprint** dans Render (⚠️ limite free tier : 1 Postgres gratuit/workspace — voir DEPLOYMENT.md §Bêta)
- [ ] Optionnel : workflow `deploy-beta.yml` (trigger API bêta via secrets `RENDER_BETA_API_SERVICE_ID`)
- [ ] Mobile (Flutter) : widget feedback + badge bêta (priorité basse — le mobile utilise la même API)
- [ ] Vérification finale end-to-end sur l'URL publique bêta

## BUGS DÉCOUVERTS / CORRIGÉS

- [x] LoginPage affichait les comptes de démo **dans tous les environnements** (données de test mélangées à la prod) → désormais conditionné au flag serveur `demoAccountsEnabled` (profil bêta uniquement)
- [x] Liste des comptes démo incomplète (manquaient Admin + Paul multi-rôles)

## TESTS

- Backend : `mvn -DskipTests compile` ✅ (BUILD ok). Suite complète : à lancer via CI (`mvn verify`).
- Frontend : `npx tsc --noEmit` + `npx vitest run` + `npm run build` → à valider après les changements.

## DÉCISIONS ARCHITECTURALES

- Séparation **production / bêta** via des **services Render distincts** (DB séparée, API avec profil `beta`, frontend statique séparé). Aucun accès testeur à la prod.
- Comptes démo visibles **seulement** en environnement bêta (serveur-driven via `/public/meta`).
- Reset bêta **strictement désactivé** hors profil `beta` (double garde : flag + environnement).
- Feedback : aucune donnée personnelle au-delà de l'email de l'auteur (résolu côté serveur).

## MIGRATIONS EFFECTUÉES

- V40 (`feedbacks`), V41 (module + menu FEEDBACK) — **non encore committées** (fichiers untracked).

## DÉPLOIEMENTS EFFECTUÉS

- Aucun nouveau déploiement (pas de credentials Render/GitHub dans l'environnement). Production existante : `https://discipolat.onrender.com` / API `https://discipolat-api.onrender.com`.

## DERNIER COMMIT GIT

- `647f4ad` fix: CI - le job Deploy to Render échouait silencieusement (V49)
- Prochain commit : backend bêta + frontend bêta + render.yaml + docs (V50)

## NEXT ACTION

> Committer la V50 (backend bêta + frontend bêta + render.yaml services bêta + docs),
> pousser sur GitHub, puis demander à l'utilisateur de lancer le **Sync Blueprint Render**
> et de fournir les secrets `RENDER_BETA_API_SERVICE_ID` / `RENDER_API_KEY`
> pour déployer et vérifier l'URL publique bêta.
