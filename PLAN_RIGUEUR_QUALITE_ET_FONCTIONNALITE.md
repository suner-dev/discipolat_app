# PLAN — Rigueur, Qualité & Fonctionnalité (zéro orphelin)

> Objet : reprise de qualité de **Discipolat** après retours terrain (test par un
> pasteur via tunnel Cloudflare). Ce document est la **référence de travail** :
> liste, phase par phase, tout ce qui doit être audité et corrigé.
> Principes directeurs du product owner :
> **« rien en dur ; tout doit être consommable/clicable ; aucune page ni bouton
> orphelin ; ne pas supprimer ce qui existe — ajouter, corriger, améliorer »**.

## Règles de travail (non négociables)

1. **Branche de travail** : `develop`. `main` ne reçoit que le merge final, après
   validation complète. Pas de push force, pas de skip de hook CI.
2. **Ne pas couper la démo en cours** : tant que le pasteur teste, on **ne rebuild
   pas** et on **ne redémarre pas** les conteneurs `discipolat-*` ni le tunnel
   `discipolat-cloudflared`. Les correctifs sont préparés dans le **source**
   uniquement ; le déploiement (rebuild image web + redémarrage) est une **étape
   finale explicite** (Phase 6) qui attend l'accord du product owner.
3. **Fullstack + mobile** : chaque parcours doit être cohérent côté API (Spring),
   web (React) et mobile (Flutter). Si l'API n'expose pas, on l'ajoute ; si le FE
   consomme un endpoint inexistant, on l'implémente réellement (pas de faux OK).
4. **Zéro codage en dur** : interdiction d'afficher des clés techniques brutes
   (`USER_CREATE`, enum, `role.key`, `entiteType`, codes statut) à l'utilisateur
   final. Humanisation via le **catalogue backend** (labels) ou un **convertisseur
   central partagé**, jamais un `.replace(/_/g,' ')` parsemé.
5. **i18n réel** : FR (référence) + EN + PT + ES + SW + AR (RTL). Toute chaîne
   visible passe par l'i18n ; pas de FR figé dans les composants.
6. **Accessibilité & UX** : tout élément visuel qui prétend être interactif DOIT
   être interactif (link/route/mutation réelle) ou explicitement inactif (état
   grisé + tooltip expliquant pourquoi). Pas de bouton-mort.
7. **Definition of Done** : page = route déclarée + rendu réel + tous boutons
   branchés + états vide/chargement/erreur + test (Vitest / JUnit) + vérification
   manuelle via tunnel quand pertinent.

---

## État des lieux (audit factuel 2026-10-17, lecture seule)

Périmètre audité : `frontend/src` (~267 pages, ~271 routes dans `App.tsx`), backend
`PermissionController`/`PermissionService` + migrations `V37`/`V72`, `index.css`,
`MainLayout.tsx`.

### Cause racine n°1 — Modales déplacées (obligation de scroller)

- `MainLayout.tsx:127` : chaque page est enveloppée dans
  `<div key={pathname} className="page-enter-active animate-fade-in">`.
- `index.css:602` : `.page-enter-active { animation: pageEnter … both; will-change: opacity, transform; }`
  et la keyframe `pageEnter` se termine sur `transform: translate3d(0,0,0)`.
- Conséquence : `will-change: transform` + `transform` persistant (fill `both`)
  transforment ce conteneur en **containing block** pour les descendants
  `position: fixed`. Or les modales `.modal-overlay { position: fixed; inset: 0 }`
  (`index.css:416`) sont alors recadrées sur la **hauteur totale de la page** au lieu
  du viewport → la modale s'affiche « au milieu »/hors écran, il faut scroller.
- **Impact** : systémique — toutes les pages utilisant `.modal-overlay` (`UsersPage`
  création, `PermissionsPage` rôles, etc.).

### Cause racine n°2 — Codes bruts affichés à l'utilisateur

- `PermissionsPage.tsx:361` `{permKey}` → sous-titre `USER_CREATE` sous le libellé.
- `PermissionsPage.tsx:301` `({role.key})` → `(PASTEUR)` etc. en mono.
- `PermissionsPage.tsx:329` `{module}` en `uppercase` → `UTILISATEURS`, `DEPARTEMENTS`.
- Backend `permission_catalog` (`V37`) fournit déjà de bons labels mais **sans
  accents** : `Creer des utilisateurs`, `Gerer les permissions`, `Televerser…`.
- Autres affichages bruts : `TenantAdminRolesPage.tsx:232` `{role.key}` ;
  `UsageAnalyticsPage.tsx:138` `{a.action}` ; `AdminDashboardPage.tsx:258/261`
  `{a.action}`/`{a.entiteType}` ; `VoiceReportsPage.tsx:118` `{a.action}`.

### Cause racine n°3 — Interactions non claires / attendues « partout »

- `PermissionsPage` : les lignes de permission ne sont pas des boutons ; seuls les
  toggles R/W/D (colonnes de droite, hors écran sans scroll horizontal) sont
  cliquables → perception « rien n'est cliquable ».
- Bouton **« Mode R/W/D »** (`PermissionsPage.tsx:283`) : bascule réelle (simple ↔
  granulaire Lecture/Écriture/Suppression + portée) mais **sans légende ni aide**.
- `AdminDashboardPage` : tuiles KPI et « Activité récente » sont des `<div>`
  non cliquables ; seuls les 20 outils + actions rapides sont des `<Link>`.

### Cause racine n°4 — Liens morts statiques (5)

`/ai/family-cohesion`, `/ai/predictions`, `/ai/sermon`, `/contact`, `/swagger-ui.html`.

### Cause racine n°5 — Page orpheline

`pages/ai/AiDashboardPage.tsx` : jamais importé (aucune route/lien).

### Divers — chaînes non-i18n

`PermissionsPage`/`UsersPage` mélangent FR en dur et `tText(...)` ; `tText()` est un
passe-plat FR→? dont la couverture réelle des 6 langues reste à vérifier.

---

## Phases de travail

IDs : `C#` correctif bloquant, `A#` audit/qualité FE, `M#` mobile, `V#` validation.
Phases 1–2 = **bloqueurs vus par le pasteur** (priorité absolue). Phases 3–5 =
**mandat large** (« tout fonctionne, zéro orphelin ») mené de façon outillée.

### PHASE 1 — Bloquants UX vus en démo (haute priorité, faible risque)

- **C1. Corriger le containing block des modales (fix global).**
  (a) Neutraliser la persistance du `transform` sur `.page-enter-active` (retirer
  `will-change: transform` ; faire revenir `transform` à `none` après l'animation).
  (b) Fournir `components/ui/Modal.tsx` rendu via `createPortal(document.body)` +
  verrou de scroll `body` + focus trap + aria ; migrer d'abord `UsersPage` et
  `PermissionsPage`, puis par vagues (A1). Objectif : plus aucune modale ne demande
  de scroller.
- **C2. Retirer les codes bruts de `PermissionsPage`.** Supprimer l'affichage grand
  public de `{permKey}` (L361) et `({role.key})` (L301) ; garder la clé technique
  derrière un mode « debug » admin optionnel (ne pas perdre l'info, la déplacer).
- **C3. Humaniser les labels du catalogue.** Migration SQL ajoutant les accents/
  français correct aux libellés `permission_catalog` (+ traduction si table dédiée,
  sinon repli centralisé FE). Ne pas dépendre d'un `.replace(/_/g,' ')`.
- **C4. Ergonomie des permissions.** Légende « Mode simple / Mode granulaire
  (Lecture–Écriture–Suppression + portée) », infobulles R/W/D, rappel du scroll
  horizontal des colonnes de rôles ; lignes atteignables au clavier.
- **C5. Les 5 liens morts.** Router `/ai/*` vers les vraies pages ; `/contact` vers
  la page légale/contact existante ; `/swagger-ui.html` vers le lien API réel (ou
  page `ApiDocsPage`).
- **C6. Cliquable partout au Centre d'admin.** Tuiles KPI (`Utilisateurs`,
  `Actions récentes`, `Alertes actives`, `Tables DB`) et chaque entrée d'« Activité
  récente » mènent à la page/détail (`/users`, `/audit`, `/alerts`). Les tuiles
  « santé » restent informatives mais clairement étiquetées comme telles.

### PHASE 2 — Convergence « zéro dur / zéro non-i18n » (FE)

- **A1. Inventaire & migration des modales** vers `components/ui/Modal.tsx`
  (portal + scroll lock + focus trap + aria) par vagues de ~15–20 pages.
- **A2. Cartographie liens→routes** (script) ; corriger tout lien dynamique
  ``to={`/x/${id}`}`` vers route inexistante/mal paramétrée.
- **A3. Convertisseur central de codes** (`lib/humanize.ts` + lookup catalogue/
  dictionnaires) appliqué partout où une enum/clé technique est affichée ; supprimer
  les `.replace(/_/g,' ')` (35 occurrences).
- **A4. Audit i18n exhaustif** : FR en dur → clés i18n ; vérifier 6 locales ; RTL AR.
  S'appuyer sur `npm run i18n:audit` et `npm run debt:audit` (déjà en CI).

### PHASE 3 — Consommation réelle du backend (web)

- **A5. Inventaire endpoints API** (`/v3/api-docs`) vs appels `api.get/post/...` FE :
  lister (a) endpoints backend **jamais consommés** et (b) appels FE vers endpoints
  **inexistants/404**. Pour chacun : câbler l'UI ou implémenter l'endpoint.
  Priorité : paiements, workflows, notifications, import/export, organisation,
  rôles/permissions, IA.
- **A6. Parité écran↔action** : sur chaque page, chaque bouton/champ est branché à
  une mutation/query réelle ; états vide/erreur/chargement réels ; pas de `onClick`
  vide ni de TODO-mort.

### PHASE 4 — Mobile (Flutter)

- **M1. Cartographie routes mobile** (`GoRouter`) vs pages ; détecter écrans
  orphelins et boutons morts ; consommer les mêmes endpoints que le web.
- **M2. Parité** : les parcours critiques (pasteur, responsable, chef de famille,
  faiseur, membre) fonctionnent offline/online (Riverpod/Drift), i18n 6 langues.

### PHASE 5 — Qualification & non-régression

- **V0. Tests** : Vitest (composants corrigés), JUnit (endpoints ajoutés), Playwright
  e2 (`e2/`) sur les parcours admin/pasteur ; CI bloquante respectée.
- **V2. Ratchets** : `npm run lint && npm run i18n:audit && npm run debt:audit` au
  vert ; `mvn verify` au vert.

### PHASE 6 — Déploiement (après accord explicite)

- **V1.** Une fois le pasteur terminé : `docker compose build web` (ou rebuild image)
  puis redémarrage ciblé ; re-vérifier app + Swagger via le tunnel ; confirmer les
  liens. **Uniquement sur feu vert du product owner.**

---

## Exécution & assistance

- **Séquencement** : Phase 1 (C1→C6) d'abord, puis A1–A4, puis Phase 3, puis mobile,
  puis qualification. Chaque item coche une DoD vérifiable.
- **Sous-agents experts** (pour accélérer sans survoler) :
  - **Agent A — Design System / Modales** : `Modal.tsx` portal + migration A1 + C1.
  - **Agent B — i18n / Humanisation** : A3 + A4 + C3 (labels catalogue + traductions).
  - **Agent C — Parité API** : A5 (inventaire endpoints/404) + consommations manquantes.
- Le titulaire (lead) garde la main sur `develop`, fusionne les travaux des agents,
  et ne merge sur `main` qu'après V2 au vert.

## Prochain livrable immédiat

Application de la **Phase 1** dans le source (sans redéploiement) : C1 (CSS +
`Modal.tsx` + migration `UsersPage`/`PermissionsPage`), C2, C3 (migration SQL), C4,
C5, C6. Puis rapport de diff + points à valider avant rebuild.
