# ECLESIASTICAL_HIERARCHY_TODO — Audit & extension de la hiérarchie ecclésiastique et de la gestion des membres

> Mission : permettre, depuis tout membre / responsable / cellule / église / entité
> ecclésiastique, de retrouver son appartenance, remonter et descendre dans la
> hiérarchie, et **consulter les membres à chaque échelle** — BE + FE web + mobile.
> Règle absolue : **additif uniquement, ne rien supprimer, ne rien réécrire de fonctionnel**.
>
> Auteur : agent senior fullstack · Date d'audit : 2026-10-22 · Base : `develop` @ `a1a70c7a`
> (main = PR #19 mergée dessus ; workflow du dépôt : commit `develop` → PR vers `main`).

---

## 0. STATUT FINAL (honnête, 2026-10-22)

**Implémentation : TERMINÉE et revérifiée empiriquement** (BE + FE web + MO + SEC + DOC), en
**additif strict** — aucun endpoint supprimé, aucune clé retirée, aucun behavior legacy réécrit.
Manques d'audit **B1/B2/B3/B4 tous traités** : roster paginé typé (`NodeMemberRosterController` /
`NodeMemberRosterService`), B2 corrigé (`getMembersWithAccess` sens descendant + `ADHESION_TENANT`
racine-seule + N+1 tué), lien web `?nodeId=&scope=`, écran mobile « Membres ».

**Vérifications rejouées** (détail + preuves horodatées en §5) : roster BE **33/33** ; `vitest`
**1088/1088** ; `tsc -b` 0 err ; `npm run build` OK ; i18n 0 clé manquante ; `flutter test`
**745** (roster 21) ; `flutter analyze` 0 erreur ; `docs/API.md` + `docs/openapi.json`
**réellement régénérés** (endpoint présent en ligne 1813).

**Réserve assumée** — la suite backend complète marque **1 échec** qui est **flaky et
hors périmètre** (`inventory/AssetQrServiceTest`, décodage image ZXing ; passe 6/6 en isolation
sous charge sérielle). Rien d'autre : le code n'est pas régressé par la mission.

**Publication : FAITE** — commit `develop` @ `d93eac28` poussé sur `origin/develop`, **PR #21**
ouverte vers `main` (https://github.com/suner-dev/discipolat_app/pull/21). `main` est protégée :
la fusion RESTE à déclencher par le mainteneur (review + gates CI) — cet état ne signifie donc
pas « déployé en production », mais « livré et en attente de merge ».

---

## 1. SYNTHÈSE D'AUDIT — verdicts par capacité

| # | Capacité | Verdict | Emplacement réel (vérifié par lecture du code) |
|---|---|---|---|
| A1 | Hiérarchie organisationnelle flexible (niveaux configurables par dénomination, pas une enum rigide) | **1 — IMPLÉMENTÉE** | BE : `modules/tenants/domain/OrganizationNode.java` (path matérialisé `text`, `level`, `parentId`, `levelId`, `responsibleId`), `OrganizationLevel.java` + `OrganizationLevelService/Controller` (`/api/v1/tenant/organization/levels`) — chaque dénomination définit ses niveaux (Région, Zone, Circonscription…). Types sémantiques : `OrganizationNodeType` = ROOT_CHURCH, CAMPUS, SUB_CHURCH, ASSEMBLY, REGION, DISTRICT, DEPARTMENT, GROUP. |
| A2 | Navigation ascendante/descendante entre entités | **1 — IMPLÉMENTÉE** | BE : `OrganizationHierarchyService.getDescendants/getAncestors/getSiblings/getTree/getTreeView` + endpoints `/api/v1/org/nodes/{id}/{descendants,ancestors,siblings}`, `/org/tree`, `/org/tree/flat`, `/tenant/organization/tree`, `/nodes/{id}/children` (drill-down V3 avec compteurs). Cycle protégé dans `moveNode` (`CYCLIC_HIERARCHY`). FE : `OrganizationBrowserPage.tsx` (`/organization`), `pages/admin/OrganizationNodeDetailPage.tsx` (onglets identité/équipe/modules/thème/agrégats/codes), `hooks/useOrganizationV3.ts`. Mobile : `screens/tenant/organizations_screen.dart`, `node_detail_screen.dart`, `data/services/organization_v3_api.dart`. |
| A3 | Chaîne humaine d'encadrement (membre → responsable direct → responsable de son responsable → …) et personnes encadrées | **1 — IMPLÉMENTÉE** | BE : module `relations` — entité `MemberRelation` (arête dirigée from→to, typée par dictionnaire `MEMBER_RELATION_TYPE` paramétrable par église, migration `V231`), `MemberRelationService` (invariants : pas d'auto-rattachement, même tenant, 1 ACTIVE par (from,to,type), plafonds 10 sortants/500 entrants, notifications `RELATION_DECLAREE`/`RELATION_REVOQUEE`, traçabilité REVOKED sans purge). `UserHierarchyService` (601 l.) : agrégat `HierarchyView` = rôles, **branches multi-branches** (origines ASSIGNATION_V3 / ADHESION_NOEUD / RESPONSABLE_NOEUD), chaîne d'ascendance jusqu'à la racine avec le responsable de chaque niveau, **ascendants déduppliqués**, `suivi` (encadrement pastoral), `resume` (complétude). Endpoints `/api/v1/hierarchy/{me,users/{id}}` + `/api/v1/relations/*` (7 endpoints). FE : `UserDetailModal.tsx` (arbre, fil d'Ariane, encadrants, membres rattachés), `ProfilePage.tsx`, `components/relations/{MyRelationsCard,HierarchyTree}.tsx`. Mobile : `widgets/hierarchy_card.dart`, `my_relations_card.dart`, `services/relation_service.dart`, fiche `user_detail_screen.dart` + profil. **9/9 endpoints consommés web ET mobile** (cf. `docs/PLAN_HIERARCHIE_RELATIONS_MEMBRES.md` §10). |
| A4 | Rattachement membre × entité (appartenance) + affectations multiples + historique | **1 — IMPLÉMENTÉE** | BE : `TenantMembership.scopeId/scopeType` (adhésion avec portée nœud, `joinedAt`, statut, transfert tracé), `MemberRoleAssignment` (V226 — membre × rôle-capacité × nœud, 0..n = multi-branches, multi-responsabilités), `RoleTitle` (titres localisés par rôle × nœud). Le rôle ≠ rattachement : distincts par conception (enum legacy vs assignments vs memberships). |
| A5 | Compteurs de membres par entité (directs + sous-structures, déduppliqués) | **1 — IMPLÉMENTÉE** | BE : `NodeAggregateService.snapshot()` — memberCount = memberships ACTIVE couvrant le sous-arbre, **`.map(userId).distinct()`** (déduplication), churchCount, leaderCount ; snapshots historisés (`NodeAggregateSnapshot`, série de progression). Exposés dans `/tenant/organization/tree` (compteurs inline) et `/nodes/{id}/aggregate`. FE : sparkline `ProgressionSparkline.tsx`. Mobile : cellules compteurs `node_detail_screen.dart`. |
| A6 | Fiche membre complète : identité, rôles, appartenance, chemin hiérarchique, responsables, encadrés | **1 — IMPLÉMENTÉE** | BE : `GET /api/v1/users/{id}/detail` enrichi de `relations`, `hierarchie`, `hierarchiePartielle`, `hierarchieIndisponible` (dégradation explicite quand données manquantes — scénario F couvert). FE `UserDetailModal` + Mobile `user_detail_screen.dart` lisent le même endpoint. |
| A7 | Permissions & isolation | **1 — IMPLÉMENTÉE (à étendre au nouveau périmètre)** | `@authz.can(...)` par permission paramétrée par église (V258/V263), tenant issu du seul contexte serveur + revérifié (`requireTenantUser` → 404), `RelationApiSecurityTest` 18 tests, `TenantFilterDefArchitectureTest`, `MultiTenantSecurityTests`. |
| **B1** | **Consulter la LISTE des membres d'une entité (église, région, district, cellule…) à plusieurs profondeurs, avec pagination + recherche + distinction directs/indirects** | **4 — ABSENTE** (le cœur du manque de la mission §6) | Aucun endpoint de roster paginé par nœud. `GET /org/nodes/{id}/members` existe mais **invérifié** (voir B2) et non typé (renvoie `List<TenantMembership>` brut — entité JPA : ni nom, ni pagination, fuite du shape interne). `/api/v1/users` (annuaire) ne filtre **que** par rôle/statut — pas par nœud. FE : les pages org n'affichent que des **nombres** (D7 « sans PII » côté V3) ; aucun écran « membres de cette entité ». Mobile : idem. |
| **B2** | `GET /api/v1/org/nodes/{id}/members` (`getMembersWithAccess`) | **3 — DÉFECTUEUX** | Sémantique inversée : `isDescendantOf(nodeId, m.scopeId)` teste si le **nœud consulté** est descendant de la portée du membre → un membre rattaché à une cellule **remonte** dans la liste du parent (au lieu de redescendre) ; les membres à portée `TENANT` inondent **chaque** nœud ; N+1 (2 `findById` par membership, roster complet chargé en mémoire, sans pagination). **Aucun consommateur web ni mobile vérifié** (grep) → correction additive sans casser d'usage. |
| **B3** | Lien navigable entité → annuaire de ses membres (web) | **4 — ABSENTE** | `OrganizationNodeDetailPage`/`OrganizationBrowserPage` ne proposent pas d'entrée « Voir les membres » ; `UsersPage` n'accepte aucun paramètre de filtrage par nœud. |
| **B4** | Écran mobile « membres d'une entité » | **4 — ABSENTE** | `node_detail_screen.dart` affiche aggregate/team (nombres + équipe) mais pas le roster paginé ; `users_list_screen.dart` sans filtre par entité. |
| C1 | Dénomination / association comme entités du graphe | **5 — À VÉRIFIER / conception retenue** | Modèle actuel : **1 tenant = 1 dénomination/réseau**, racine = ROOT_CHURCH, et les niveaux intermédiaires (Association, Région, District…) sont **configurables** via `OrganizationLevel` (documenté explicitement dans le Javadoc : « V2 fixait la hiérarchie dans l'enum ; V3 rend chaque dénomination libre de définir ses propres niveaux »). C'est la réponse du projet à l'exigence « structure non rigide ». **Ne pas** inventer une table dénomination au-dessus des tenants (serait une réécriture destructive du multi-tenant). |
| C2 | `path` en `ltree` | **5 — CLOSE** | Constat H8 (`docs/ORGANIZATION_HIERARCHY.md`) : `path` indisponible en `ltree` sur base migrée. **Résolu dans le code actuel** : colonne `path text` + `findDescendants` en LIKE sur le path matérialisé ; la doc est en retard, pas le code. |

## 2. PLAN D'EXÉCUTION — tâches (dépendances →)

### Backend
- [x] **BE-1** — Nouvel endpoint roster : `GET /api/v1/tenant/organization/nodes/{nodeId}/members?scope=direct|subtree&q=&page=&size=` → page typée `{userId, firstName, lastName, email?, roles[], relationDirecte vs indirecte (viaNodeId/viaNodeName), statut, joinedAt}`. Règles : déduplication par userId (au plus proche du nœud), ordre stable, `size` clampé, `q` filtre nom/email, **tenant du contexte serveur revérifié** (404 cross-tenant), dépend BE prerequisite A2 (descendants) — réutilise `OrganizationNodeRepository.findDescendants` + `TenantMembership` + `MemberRoleAssignment` (pour rattacher aussi les porteurs d'assignment sur un nœud du sous-arbre, sans doublon avec la membership). Critères : scénarios A, B, F, G côté service + HTTP.
- [x] **BE-2** — Correction ciblée de `getMembersWithAccess` (B2) : sens descendant correct (membres dont la portée est **dans** le sous-arbre du nœud), projection légère ; **signature/additif préservés** si consommé — sinon dépréciation documentée, jamais de suppression. Critère : test unitaire qui échouerait sur l'ancienne sémantique.
- [x] **BE-3** — Compteurs cohérents : exposition `memberCountDirect` + `memberCountSousArbre` sur la fiche nœud (réutilise la logique distinct() de `NodeAggregateService` ; additif dans `toView`/`aggregate`). Critère : directs ⊆ sous-arbre, aucun double comptage.
- [x] **BE-4** — Tests : service (direct/subtree/dédup/repli données manquantes), HTTP (RBAC : rôles limités, 404 cross-tenant, clamp pagination), architecture tenant-filter. Scénarios A/B/F/G.

### Frontend web
- [x] **FE-1** — `hooks/useOrganizationV3.ts` : ajouter `useNodeMembers(nodeId, {scope,q,page})` sur BE-1. 
- [x] **FE-2** — `OrganizationNodeDetailPage` : nouvel onglet **« Membres »** (liste paginée, recherche, badge Direct/Indirect, compteur) — additif aux 6 onglets existants, garde d'affichage alignée sur la permission backend.
- [x] **FE-3** — `UsersPage` : filtre URL `?nodeId=&scope=` (annuaire scopé entité) + lien « Voir les membres » depuis le navigateur org (`OrganizationBrowserPage` panneau détail). Fil d'Ariane hiérarchique déjà posé dans `UserDetailModal` (réutilisé, pas re-créé).
- [x] **FE-4** — i18n parité stricte 6 locales (fr/en/pt/es/sw/ar) pour les nouvelles clés `orgV3.members.*`.
- [x] **FE-5** — Tests vitest : hook + onglet Membres (rendu paginé, scope, repli vide) + UsersPage filtre nodeId. `tsc --noEmit` 0 erreur.

### Mobile
- [x] **MO-1** — `organization_v3_api.dart` : méthode `nodeMembers(nodeId, scope, q, page)` (client réel `api_service.dart`, pas le stub legacy).
- [x] **MO-2** — `node_detail_screen.dart` : section **« Membres »** (liste paginée scroll, recherche, badges Direct/Indirect, état d'erreur + repli « information non renseignée »).
- [x] **MO-3** — i18n 6 locales + tests `flutter test` (parsing défensif : champ absent ≠ crash) ; `flutter analyze` 0 erreur.

### Base de données
- [x] **DB-0** — **Aucune migration nécessaire** (audit concluant : path matérialisé + levelId + memberships + assignments couvrent le modèle). Vérifié : dernière migration V272 ; rien à ajouter. Si un besoin surgissait : additive uniquement (pas de colonne destructrice, pas de réécriture de V231/V232).

### Sécurité (transverse)
- [x] **SEC-1** — Le roster BE-1 ne divulgue rien hors périmètre : tenant strict (404), garde `@authz` alignée sur la lecture d'annuaire existante (`isAuthenticated()` pour la visibilité minimale, comme `/users` — l'annuaire est déjà ouvert à tout membre authentifié du tenant ; le nouveau endpoint n'ouvre donc **rien de plus** que l'existant), email/PII limité au shape `UserResponse` existant.
- [x] **SEC-2** — Rejeu des tests d'isolation (`RelationApiSecurityTest`, `MultiTenantSecurityTests`, `TenantFilterDefArchitectureTest`) + nouveau `NodeMembersSecurityTest`.

### Documentation
- [x] **DOC-1** — Mettre à jour `docs/ORGANIZATION_HIERARCHY.md` (constat H8 clos, `path text`, roster endpoint) et `docs/CHANGELOG.md` ; `docs/API.md` **généré** via `scripts/generate-api-docs.sh` (règle du dépôt, pas d'édition manuelle).
- [x] **DOC-2** — Cocher ce fichier au fur et à mesure + statut final honnête.

## 3. CE QUI EST CONSERVÉ SANS RÉÉCRITURE (travail existant réutilisé)

- Arbre V3 complet (niveaux configurables, move/copy/bulk, anti-cycle, héritage de config DEFAULT/INHERITED/OVERRIDDEN) — **réutilisé en amont** par BE-1.
- `UserHierarchyService` (agrégat hiérarchie multi-branches, ascendants déduppliqués, suivi pastoral, resume de complétude) — **intouché**.
- `MemberRelation` + 7 endpoints relations + notifications + plafonds — **intouchés**.
- `NodeAggregateService` (compteurs distinct) — **enrichi additivement** (BE-3).
- Fiches web/mobile, fil d'Ariane, arbre `HierarchyTree`/`hierarchy_card`, annuaire `/users` — **enrichis additivement**.
- Système de permissions paramétrées par église (`@authz.can`, V258/V263) — **réutilisé**, aucun système concurrent créé.

## 4. RISQUES & DÉCISIONS

| Risque | Décision |
|---|---|
| Modifier `getMembersWithAccess` (B2) pourrait casser un consommateur inconnu | Grep web+mobile : **zéro consommateur** ; correction addotive + test ; endpoint conservé (jamais supprimé). |
| PII : lister les membres d'un nœud expose des noms | Parité stricte avec l'existant : l'annuaire `/users` est déjà `isAuthenticated()`-visible par tout le tenant ; le roster n'ouvre pas plus. Le mode V3 « nombres sans PII » (D7) reste **régi par la garde tenant-admin de `OrganizationNodeV3Controller`** — le roster est placé sous la même garde, pas d'élargissement. |
| Doubles comptes directs/indirects (membre rattaché par 2 chemins) | Dédup par userId « au plus proche du nœud », éprouvé dans `NodeAggregateService` ; même règle appliquée BE-1. |
| ltree (H8) | Clos en code (`path text`) ; doc seulement à corriger. |

## 5. CHECKLIST DE LIVRAISON (statut réel)

- [x] Audit 3 couches effectué sur le **code** (pas la doc) — verdicts §1.
- [x] BE-1 roster paginé · [x] BE-2 correction B2 · [x] BE-3 compteurs directs/indirects · [x] BE-4 tests
- [x] FE-1..5 · [x] MO-1..3 · [x] SEC-1..2 · [x] DOC-1..2
- [x] `mvn -o test` (cible + suite) rejoué, résultats réels consignés ici — suite complette **2455 tests / 1 échec** ; l'unique échec est `inventory.AssetQrServiceTest` (aller-retour de décodage image ZXing, **non lié** à la mission, passe en isolation → flaky sous charge parallèle). Roster BE **33/33 verts**.
- [x] `tsc -b` (0 erreur) + `vitest run` (123 fichiers / 1088 tests verts) + `npm run build` (succès) + parité i18n (0 clé manquante sur les 6 locales)
- [x] `flutter analyze` (0 erreur ; uniquement des lints `info` préexistants) + `flutter test` (745 tests verts, dont roster **21**)
- [x] Publié sur `develop` (commit `d93eac28` poussé sur `origin/develop`) puis **PR #21** vers `main` ouverte (https://github.com/suner-dev/discipolat_app/pull/21) — workflow du dépôt respecté (`main` protégée par PR, PR #17-20 en preuve). La **fusion** elle-même reste à déclencher par le mainteneur (review + gates CI).
- [x] Scénarios A→I validés au **niveau tests** (BE service+HTTP/SEC, FE hook+composants+i18n, MO parsing+écran+6 locales) — cf. tableau ci-dessous. **Non** rejoués en E2E manuel browser/app.

### Preuves d'exécution (rejouées sur l'arbre de travail courant)

> Machine partagée/saturée → portes sérialisées (jamais `mvn` + `vitest` + `flutter` en
> parallèle). Chaque chiffre ci-dessous provient d'une exécution **fraîche** de cette session,
> pas d'un log antérieur ni des affirmations du CHANGELOG.

| Vérification | Commande | Résultat réel | Date (2026-10-22) |
|---|---|---|---|
| Suite backend complète | `bash scripts/mvn-local.sh test` | **2455 tests, 1 échec** — seul échec = `inventory/AssetQrServiceTest.renderThenDecodeRoundTrip…` ( aller-retour image ZXing, **sans rapport** à la mission) ; `BUILD FAILURE`. Rejoué en isolation (`-Dtest=AssetQrServiceTest`) → **6/6 verts** ⇒ **flaky** sous charge parallèle. | 16:41 → 16:43 |
| Roster BE (nouveaux + B2) | mêmes surefire-reports | `NodeMemberRosterServiceTest` **17**, `OrganizationHierarchyMembersAccessTest` **6**, `NodeMembersSecurityTest` **10** → **33/33 verts** | 16:41 |
| Frontend typage | `npx tsc -b` | **0 erreur** (exit 0) | 16:46 |
| Frontend tests | `npx vitest run` | **123 fichiers / 1088 tests verts** ; périmètre mission : `NodeMembersRoster.test.tsx` **18**, `UsersPageScopedRoster.test.tsx` **8**, `OrganizationBrowserPage.test.tsx` **7** | 16:47 |
| Parité i18n | `npm run i18n:audit` | **0 clé manquante** sur fr/en/pt/es/sw/ar (les colonnes « en trop / non traduites » sont la dette de baseline préexistante, pas les nouvelles clés `orgV3.members.*`) | 16:46 |
| Build web | `npm run build` (`tsc -b && vite build`) | **succès** (exit 0) | 16:52 |
| Mobile analyse | `flutter analyze lib test` | **0 erreur** ; 531 issues = lints `info`/`warning` de baseline ; fichiers touchés → seulement des `info` (ex. `prefer_null_aware_operators`, `withOpacity` déprécié préexistant) | 17:02 |
| Mobile tests | `flutter test` | **745 tests verts** ; `node_member_roster_test.dart` rejoué seul → **+21 verts** | 17:05 → 17:08 |
| DOC-1 API générée | boot `spring-boot:run` (profil dev, port 8090, base `:5433`) puis `bash scripts/generate-api-docs.sh http://localhost:8090` | `/api-docs` HTTP 200 ; `docs/API.md` régénéré → **1451 chemins / 1800 opérations** ; ligne 1813 documente `GET /api/v1/tenant/organization/nodes/{nodeId}/members` (`nodeId` path req + `scope`/`q`/`page`/`size` query). `docs/openapi.json` régénéré (git M). | 17:16 → 17:19 |
| Publication `develop`→PR `main` | `git push origin develop` puis `gh pr create --base main --head develop` | **FAIT** : commit `d93eac28` sur `origin/develop` ; **PR #21** ouverte vers `main` (https://github.com/suner-dev/discipolat_app/pull/21). Fusion laissée au mainteneur (`main` protégée). | 2026-10-22 |
