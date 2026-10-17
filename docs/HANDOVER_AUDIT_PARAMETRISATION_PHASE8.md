---
# HANDOVER — Audit profond de la paramétrisation par église (Phase 8)

> Rédigé le 2026-10-17 sur la branche `audit/parametrisation-deep` (base `develop`
> @ `75800a04`). Objectif : ré-auditer `docs/PARAMETRISATION_AUDIT.md` sans
> reprendre ce qui est déjà fait, corriger ce qui ne l'est pas, et **ne rien
> laisser derrière soi qui puisse régresser silencieusement**.
>
> **TL;DR honnête** : le chantier des phases 0→7 est de bonne qualité et ses
> affirmations tiennent à la vérification (détails §2). Cinq défauts réels ont
> été trouvés et corrigés (§3), dont **un bouton-mort jamais détecté**. Le vrai
> manque n'était pas du code : c'est l'**absence de ratchet CI** (§4).

---

## 1. Ce qui a été vérifié SANS être touché (conclusions factuelles)

Ces contrôles ont été refaits depuis zéro (parseurs indépendants, pas de
réutilisation des rapports des phases précédentes). Ils sont **verts** :

| Contrôle | Résultat |
|---|---|
| R7 backend : `@PreAuthorize` contenant `hasRole(`/`hasAnyRole(` | **0** (616 `@authz.can` en production ; la seule occurrence est un commentaire dans `JwtAuthenticationFilter`) |
| Clés de permission consommées par le web **hors catalogue servi** | **0** (registre `ROUTE_PERMISSIONS` : 130 clés ; `App.tsx` : 146 clés) |
| Clés consommées par le mobile hors catalogue | **0** (`routePermissions` : 77 clés + 9 `canServed`) |
| Parité de matrice `ROUTE_PERMISSIONS` ≡ `ROUTE_ROLES` | **0 divergence / 173 routes** |
| Parité de matrice des gardes intra-pages (`can`/`canAny`) ≡ littéral legacy | **0 divergence / 29 gardes** |
| Parité `canServed` (mobile) ≡ littéral legacy | **0 divergence / 7 gardes** |
| Miroir mobile ↔ web | **79 routes communes, 0 divergence de clés** (les 12 restantes sont des écrans propres au mobile) |
| Miroir servi (`GET /me/permissions`) + invalidation temps réel | en place : `PermissionResolver` + `useRealtimeBus` (`PermissionsChanged` → `refreshContext` + toast) ; mobile `permissionsEpoch` + re-pull au switch de rôle |

Conclusion : **ne pas refaire** la conversion des gardes backend, le registre de
routes, ni les 29 gardes intra-pages. Elles sont correctes et prouvées.

Deux vérifications qui avaient produit un faux soupçon, et leur résolution — utile
pour ne pas les refaire :

- **« 10 clés de garde sans rôle dans la matrice »** (`PERMISSION_ASSIGN`,
  `ROLE_UPDATE`, `ORG_NODE_*`…) : **faux positif de mon parseur**. V135/V136
  accordent ces paires par `JOIN permissions ON …` (et `TENANT_ADMIN` en est
  explicitement **exclu** par V136 §2 — choix métier antérieur, pas une
  régression). Un rôle `TENANT_OWNER` les détient bien. Aucun correctif.
- **« 311 clés du catalogue jamais consommées »** : normal, ce sont des gardes
  **serveur** (`EVENTS_EXECUTE_DELETE`, `AUDIT_READ`…). Le client n'a pas à les
  connaître. Aucun correctif.

---

## 2. Défauts réels trouvés et corrigés

| # | Gravité | Défaut | Correctif |
|---|---|---|---|
| D1 | **Bloquant (bouton-mort)** | `frontend/src/pages/DressCodePage.tsx` consommait `hasPermission("DRESS_CODE_CREATE"/"_UPDATE"/"_ARCHIVE")` — **trois clés qui n'ont jamais existé au catalogue**. `hasPermission` est un match strict sur `GET /me/permissions` : les boutons « Nouveau Dress Code », « Modifier », « Archiver » **n'ont jamais pu être rendus, pour aucun rôle**. Défaut né avec le module G3.4 (`7444f6fa`), jamais vu par un test. | Rebranché sur les clés réellement gardées par `DressCodeController` : `DRESSCODE_EXECUTE` (create/update/archive) et `DRESSCODE_EXECUTE_DELETE` (nouveau bouton **Supprimer** branché sur `DELETE /dress-codes/{id}`, qui n'était consommé par aucune UI). Matrice V258 inchangée ⇒ parité exacte avec l'ancien littéral serveur. |
| D2 | Moyen | 6 gardes d'UI **encore figées** sur le rôle actif, donc non paramétrables : `MemberDashboardPage.canManage`, `ResponsableDashboardPage.canManage`, `ChefFamilleDashboardPage.canManage`, `DashboardPage` blocs « Vue Responsable » et « Mes âmes », `MemberRequestsPage` vue « Toute l'église ». | `useServedAccess().can(<CLÉ>, legacy)` + **V270** : 7 clés UI, chaque matrice = le littéral remplacé, au rôle près. |
| D3 | Moyen | `frontend/src/pages/PeopleDirectoryPage.tsx` : `canRegister = user?.role === "ADMIN" \|\| "PASTEUR"` — **conversion manquée**, la page consumes déjà `/people`. | `can("PEOPLE_EXECUTE", …)` — la clé du garde serveur `POST /people/register`. **Trouvé par le ratchet phase 8 à sa première exécution.** |
| D4 | Moyen | Mobile `users_list_screen.dart` : `hardDelete` était la **seule action de la rangée sans garde** — visible parce que le rôle de la *ligne* était ADMIN/PASTEUR/RESPONSABLE, pas parce que l'*acteur* avait le droit. Un PASTEUR voyait un bouton rouge qui partait en 403 (`USERS_EXECUTE_HARDDELETE` = {ADMIN}). | `canServed('USERS_EXECUTE_HARDDELETE', legacy: hasActiveRole(['ADMIN']))`. |
| D5 | Faible | Mobile `reports_screen._loadDepartments` : portée « tous les départements » figée sur `hasActiveRole(['ADMIN','PASTEUR'])` alors que la garde voisine était déjà servie-first. Réemployer `DEPARTMENTS_EXECUTE` aurait **élargi à RESPONSABLE** (il verrait les départements des autres) ⇒ clé dédiée en V270. | `canServed('UI_REPORTS_DEPARTMENTS_GLOBAL_SCOPE_ACCESS', …)`. |

Correctif de cohérence associé : `useServedAccess` exigeait `permissions.length > 0`
alors que `ProtectedRoute` (App.tsx:358) et `AuthState.canServed` exigent seulement
« contexte chargé ». Le fichier prétendait une sémantique « strictement identique »
qui ne l'était pas. Aligné sur les deux autres (fail-closed partout).

---

## 3. Le vrai manque : aucun ratchet ne pouvait voir ces défauts

Le backend est gelé par la règle d'architecture **R7** (`plafond|R7|0`), le web a
`routeAccessPermissions.test.ts` (registre ↔ catalogue). Mais rien ne vérifiait :

- qu'une clé **consommée** par le client **existe** — d'où D1, invisible depuis
  des mois ;
- que la matrice d'une clé **équivaut** au littéral qu'elle remplace — un
  raccourci aurait élargi ou retiré un accès sans que personne ne le voie ;
- qu'une **nouvelle** garde à littéral de rôle n'apparaît pas.

### Gates ajoutés (reproductibles en CI, sans fixture à maintenir)

- **`frontend/src/__tests__/parametrisationGate.test.ts`** (8 tests) +
  `helpers/servedCatalog.ts` (parseur échappe-aware des migrations) +
  `helpers/sourceScan.ts` (neutralisation des commentaires).
  - (A) toute clé consommée (`hasPermission`, `can`, `canAny`, registre) existe
    dans le catalogue **lu dans `backend/.../db/migration` à l'exécution** ;
  - (B1) parité de matrice `ROUTE_PERMISSIONS` ≡ `ROUTE_ROLES` (173 routes) ;
  - (B2) parité de matrice de chaque garde `can`/`canAny` ≡ son `legacy` ;
  - (C) liste blanche des 6 sites résiduels assumés + **plafond chiffré
    committé** (`RESIDUAL_BASELINE = 6`), plus une garde anti-périmption.
- **`mobile/test/served_permissions_parity_test.dart`** (5 tests) : mêmes règles
  (A)/(B) côté mobile + ratchet chiffré du résiduel (`= 1`).

Les deux ratchets lisent les **migrations** et non un JSON figé : c'est
délibéré, un fixture se désynchronise au premier `INSERT` oublié — précisément
la panne qu'ils évitent.

**Preuve qu'ils mordent** (contrôle négatif effectué pendant ce chantier) :
réintroduire `hasPermission("DRESS_CODE_CREATE")` → le test (A) échoue avec
`DRESS_CODE_CREATE — src/pages/DressCodePage.tsx:42` ; ajouter
`const canSecret = activeRole === 'PASTEUR'` → le test (C) échoue en nommant le
site. Restauré, tout repasse au vert.

---

## 4. Pièges rencontrés (à ne pas re-découvrir)

1. **Apostrophe SQL dans un libellé de `INSERT … VALUES`** : le générateur de
   seed H2 (`gen_h2_seed.py`) et `TestPermissionMatrixSeedGuardTest` analysent les
   tuples avec `\(\s*'([A-Z0-9_]+)'\s*,\s*'([^']+)'…\)`. Un libellé contenant
   `''` (apostrophe échappée) **casse la reconnaissance** : la clé disparaît
   silencieusement du seed H2, les tests d'intégration verdissent sur des gardes
   `@authz.can` non satisfaits. J'ai d'abord écrit `'Vue « Toute l''église » …'`
   et la clé a disparu du seed — détecté parce que le compte d'occurrences ne
   collait pas. **Règle : pas d'apostrophe dans ces libellés** (documenté en tête
   de V270). Les parseurs des nouveaux tests, eux, sont échappe-aware.
2. **V264 est pris par l'agent concurrent** (labels d'accents du catalogue) :
   cette branche utilise **V270** pour éviter toute collision de version.
   `EXPECTED_MIN_VERSION = 263` dans `FlywayMigrationChainPostgreSqlTest` n'a pas
   besoin d'être remonté (assertion `>=` + `migrate()` applique toute la chaîne,
   donc une erreur SQL en V270 fait bien échouer le gate) — mais le constant et
   le `@DisplayName` affiche « V1→V263 » sont désormais en retard d'affichage.
3. **Un nouveau test de seed impose d'étendre deux listes** : la liste `FILES`
   de `gen_h2_seed.py` (hors repo) **et** le tableau de migrations de
   `TestPermissionMatrixSeedGuardTest`. Oublier le second → le test compare un
   seed plus riche que les migrations et tombe en rouge.
4. **`UserRole` (web) ne modélise que les 6 rôles d'église** —
   `TENANT_ADMIN`/`TENANT_OWNER` en sont absents, alors que la matrice serveur
   les accorde. Comparer `activeRole === 'TENANT_ADMIN'` ne compile pas
   (`TS2367`). Élargir l'union aurait un rayon d'action large (maps
   `Record<UserRole, …>`, `switch` exhaustifs) : **non fait ici**, à traiter
   séparément. `DressCodePage` compare donc sur la forme chaîne.
5. **Détecteur de littéraux de rôle et commentaires** : sans neutralisation des
   commentaires, le ratchet lit `@authz.can('X')` cité dans une phrase comme une
   clé consommée. C'est ce qui rendait la première version du test bruyante.

---

## 5. Gate / commandes

```bash
# web
cd frontend && npx tsc --noEmit
cd frontend && npx vitest run --no-file-parallelism      # 1032/1032 (1024 + 8 ratchet)
cd frontend && npx vitest run src/__tests__/parametrisationGate.test.ts

# mobile
cd mobile && flutter analyze
cd mobile && flutter test test/served_permissions_parity_test.dart   # 5/5
cd mobile && flutter test                                             # 679/679

# backend (seed H2 ↔ migrations : OBLIGATOIRE après toute nouvelle matrice)
cd backend && mvn -o -Dtest=TestPermissionMatrixSeedGuardTest test
cd backend && mvn -o -Dtest=FlywayMigrationChainPostgreSqlTest test   # chaîne PG16 réelle
```

Régénérer le seed après une nouvelle matrice :
`python3 .e2e-tmp/param/p4/gen_h2_seed.py` (ajouter la migration à `FILES`).

---

## 6. Reste à faire (hors de cette branche, par ordre de valeur)

1. **Gates manquants sur les autres surfaces du dépôt** : les ratchets (A)/(B)
   cover web + mobile. Le **seed H2 et le générateur de `import.sql`** gagneraient
   à être remplacés par le parseur échappe-aware (cf. §4.1) — aujourd'hui un
   libellé à apostrophe reste un piège.
2. **Élaguer la dette de rôles dupliquée** : le catalogue mélange des clés
   historiques (`MEMBER_READ`, `EVENT_*` de V135/V136) et des clés
   paramétrables (`MEMBERS_EXECUTE*` de V258) pour le même domaine. Les
   premières ne sont gardées nulle part en `@authz.can`. Un audit de tables
   (rôle×permission) permettrait de choisir une seule nomenclature.
3. **Élargir `UserRole`** côté web/mobile aux rôles réellement servis
   (`TENANT_*`, `CHURCH_*`, rôles personnalisés) — cf. §4.4.
4. **Modulariser `DressCodePage`** (i18n, error/toast, confirmation de
   suppression) : la page est restée hors des vagues de qualité de
   `PLAN_RIGUEUR_QUALITE_ET_FONCTIONNALITE` (chaînes FR en dur). Ce n'était pas
   l'objet de ce chantier, mais c'est le fichier le plus faible du périmètre.
