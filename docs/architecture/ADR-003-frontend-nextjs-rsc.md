# ADR-003 — Frontend : Next.js App Router / RSC pour la vitrine & le SEO ; tranches verticales pour l'app

> **Statut** : proposé. **Date** : 2026-10-09. **Contexte** : `frontend/` = React + TypeScript + Vite,
> ~261 pages, 6 locales dont RTL, design system partiel, landings publiques `/e/:slug` fraîchement
> livrées (église d'abord). Cette ADR **réconcilie** une tension explicite avec
> [frontend-target-architecture.md](frontend-target-architecture.md) — voir §Réconciliation.

---

## Décision

**Deux surfaces, deux régimes, un seul design system :**

1. **Surface publique (vitrine, landing `/e/:slug`, marketing, tarification, onboarding pré-auth)** →
   **Next.js App Router / React Server Components**, rendu **SSR/edge** pour le **SEO**, le TTFB et les
   Core Web Vitals. C'est là que l'acquisition organique (funnel « mon église est trouvable ») se
   gagne, donc là que le ROI de valorisation est maximal.
2. **Application authentifiée (CRM disciples, finances, plateau, admin)** → on **garde le SPA** et on
   l'amène vers les **tranches verticales** (features/entities/shared) déjà spécifiées dans
   `frontend-target-architecture.md` §1-§13, **sans réécriture**. Le SSR n'y est pas justifié (pas de
   SEO, TTFB non critique, sessions longues).

Le **contrat** (OpenAPI généré) et le **design system à tokens** sont **partagés** entre les deux
surfaces : une seule source de composants, deux shells de rendu.

## Réconciliation avec le document FE existant (honnêteté)

`frontend-target-architecture.md` §15 **refuse** « Next.js / SSR pour l'application » et le §16 ajoute,
entre parenthèses, que **« le SSR serait justifié pour la vitrine publique si l'acquisition devient une
priorité »**. Cette ADR ne contredit pas le doc : elle **active précisément cette condition**.

- Ce qui **reste vrai** du doc et qu'on ne change pas : l'app authentifiée n'a **pas** besoin de SSR ;
  une réécriture globale est **à proscrire** ; les tranches verticales + frontières ESLint sont le vrai
  sujet de l'app.
- Ce que **l'arbitrage utilisateur ajoute** : la vitrine et les landing `/e/:slug` **deviennent** un
  objectif d'acquisition → donc SSR/RSC **pour ces surfaces uniquement**.
- **Conséquence de vérité** : introduire Next.js à côté de Vite = **cohabitation de deux builds**.
  C'est un coût réel (§coûts) qu'on n'accepte que si le SEO public est une priorité business. Sinon,
  la solution la moins chère reste Vite + **pré-rendu statique** des landings (voir Option 1).

## Options

| Option | Descriptif | Coût | SEO | Recommandation |
|---|---|---|---|---|
| **1. Vite + pré-rendu (SSG) des landings** | on garde Vite, on pré-rend `/{public}` en statique + `<meta>`/`og:` (déjà posés) | faible | bon pour pages fixes | si le SEO est un besoin **modeste** |
| **2. Next.js pour la vitrine, SPA pour l'app** (cette ADR) | shell Next/RSC public + SPA authentifié inchangé de régime | moyen | **excellent**, dynamic + ISR | **recommandé** si acquisition = priorité |
| **3. Next.js full (tout en App Router)** | on migre les 240 pages protégées en RSC | **élevé** | n'apporte rien sur le protégé | **refusé** (coûte sans bénéfice, cf. doc §15) |

## Contraintes d'implémentation (non négociables)

- **Un seul design system** : les composants `shared/ui` consommaient déjà des tokens ; ils deviennent
  le kit utilisé par **les deux** shells. Zéro duplication de `Button`/`Card`/`EmptyState`.
- **Marque scopée par tenant** : `applyScopedBranding` (ajoutée en T2.4) devient le point d'entrée de
  thématisation SSR (injectée dans le `<head>` au rendu) — attention R4 (ne jamais fuiter la palette
  d'un tenant dans un rendu partagé : cache par `slug`).
- **Contrat first** : les clients API des deux shells sont **générés** depuis le même `openapi.json`
  (`orval`/`openapi-typescript`) — une seule source de vérité de types.
- **Budgets perf en CI** : échec si LCP public > seuil ou si le bundle initial de l'app progresse de
  > 5 % entre PRs.

## Comment vérifier la décision

- Les landing `/e/:slug` rendent leur contenu **sans JS** (crawl vérifié par Lighthouse/`curl`),
  `og:*` + `noindex` là où requis (R2 du plan église), et **pas** de fuite de branding inter-tenant
  même en cache partagé (test dédié).
- L'app authentifiée reste **exactement** dans son régime SPA ; aucun composant dupliqué entre shells
  (test de graphe d'imports).

*Reproductible : `grep -rn "applyScopedBranding" frontend/src` ; `find frontend/src/pages -name '*.tsx' | wc -l`
(≈ 261 pages) ; présence actuelle de `og:`/`noindex` dans `ChurchLandingPage.tsx`.*
