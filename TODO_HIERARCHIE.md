# TODO_HIERARCHIE — Hiérarchie complète des églises et des rôles

> Mission : Dénomination → Région → District → Église Chef → Sous-église/Annexe → Cellule,
> hiérarchie des rôles (chefs/sujets), `getFullHierarchy`, `getSubordinates`,
> `getLeadershipChain`, `getMembersByDenomination/Region/District/EgliseChef`.
> Règle d'or : **ajouts uniquement, aucune suppression, aucune régression.**

État des lieux vérifié dans le code (commit du 2026-10-21).

---

## [FAIT] — Ce qui existe déjà (à conserver, ne pas réécrire)

### Backend
- **Hiérarchie inter-organisations (dénomination)** : `Tenant.kind`
  (`DENOMINATION`, `MEGA_ASSOCIATION`, `CHURCH`… — `TenantKind.java`),
  colonnes `parent_tenant_id` / `root_tenant_id` (migrations V222/V223,
  SPEC_ORGANISATION_DENOMINATION_V2). Transfert de membres inter-églises :
  `TenantTransferService`, traçabilité sur `tenant_memberships` (V223).
- **Arborescence organisationnelle intra-tenant** : `organization_nodes`
  (V135/V145/V147) avec `parent_id`, `path` matérialisé, `level`, `type`
  (`ROOT_CHURCH`, `CAMPUS`, `SUB_CHURCH`, `ASSEMBLY`, `REGION`, `DISTRICT`,
  `DEPARTMENT`, `GROUP`), `responsible_id` (responsable de nœud).
  Service `OrganizationHierarchyService` : `getTree`, `getDescendants`,
  `getAncestors`, `getTreeView`, `createCampus`, move/copy/bulk.
  Endpoints : `/api/v1/tenant/organization/tree|nodes/{id}/children|aggregate|team`,
  `/api/v1/org` (OrganizationHierarchyController).
- **Niveaux configurables par dénomination** : table `organization_levels`
  (V224) + `OrganizationLevelService` (`ensureDefaults` : Région → District →
  Campus → Groupe) + controller `/api/v1/tenant/organization/levels`
  (SPEC_ORGANISATION_MODULABLE_V3 §A).
- **Rattachement membre ↔ nœud (3 mécanismes)** :
  1. `tenant_memberships.scope_type/scope_id` (V136) — portée REGION,
     CHURCH, SUB_CHURCH, CAMPUS, DEPARTMENT… ;
  2. `member_role_assignments` (V226) — capacité × nœud multi-branches ;
  3. `organization_nodes.responsible_id` — signal autoritaire du dirigeant.
- **Relations personnelles chef/sujet déclarées** : `member_relations` (V231,
  `MemberRelation` from→to typé PASTEUR/SUPERIEUR/RESPONSABLE/MENTOR/PARRAIN,
  paramétrable par église), `MemberRelationService.listMembersOf` (sujets
  **directs**, paginé), notifications, endpoints `/api/v1/relations*`.
- **Agrégat « hiérarchie complète d'un membre »** : `UserHierarchyService.getHierarchy`
  (branches développées jusqu'à la racine avec le responsable de chaque
  niveau, ascendants unifiés org+déclaratif, suivi pastoral) —
  endpoints `GET /api/v1/hierarchy/me` et `GET /api/v1/hierarchy/users/{userId}`.
- **Rôles** : `Role` (globaux + par tenant, `priority`), `RoleTitle` (V225),
  résolution `AuthorizationService` (permission accordée si rôle porté sur le
  nœud **ou un ancêtre**, descendance via `path`).

### Frontend web
- `components/relations/HierarchyTree.tsx` (arbre générique),
  `MyHierarchyCard.tsx`, `MyRelationsCard.tsx`, modale fiche membre
  (`UserDetailModalHierarchy.test.tsx`), `OrganizationBrowserPage.tsx`,
  `TenantOrganizationPage.tsx`, `admin/OrganizationLevelsPage.tsx`,
  `admin/OrganizationNodeDetailPage.tsx`, hooks `useOrganizationV3.ts`,
  `useOrgNodes.ts` + tests vitest associés.

### Mobile (Flutter)
- `presentation/widgets/hierarchy_card.dart` (arbre),
  `presentation/screens/profile/my_relations_card.dart`,
  `presentation/screens/tenant/organizations_screen.dart`,
  modèles `data/models/member_relation.dart`, `organization_level.dart`,
  `organization_v3_models.dart`, services `relation_service.dart`,
  `organization_v3_api.dart` + tests associés.

---

## [À FAIRE] — Écarts avec la demande

1. **Cellule manquante dans l'arborescence** : l'enum `OrganizationNodeType`
   n'a pas de type `CELLULE` (et la contrainte CHECK de V135 limite `type` à
   8 valeurs) ; `ensureDefaults` ne crée pas de niveau « Cellule ». La
   dénomination → région → district → église chef → sous-église existe, le
   dernier étage (cellule) aussi via `GROUP`, mais sans sémantique explicite.
2. **Colonnes rapides sur `users`** : aucun champ `cellule_id`, `eglise_id`,
   `district_id`, `leader_id` (le rattachement passe par memberships/
   assignments ; le chef direct « dur » n'existe que déclarativement via
   `member_relations`).
3. **`getSubordinates(leaderId)` directs + INDIRECTS** : seule la liste
   **directe** existe (`/relations/me/members` + `responsible_id`). Pas de
   parcours récursif (BFS anti-cycle) des sujets transitifs.
4. **`getLeadershipChain(memberId)` dédié** : la chaîne ascendante existe
   *dans l'agrégat* (branches/chaine) mais pas en tant que fonction/endpoint
   autonome « chef direct → chef du chef → … → sommet », fusionnant
   `users.leader_id`, responsabilités de nœuds et relations déclarées.
5. **`getFullHierarchy(userId)` typé par créneaux** : l'agrégat renvoie des
   branches génériques ; pas de vue `{ denomination, region, district,
   egliseChef, sousEglise, cellule }` (slots sémantiques + tenants parent
   pour la dénomination).
6. **`getMembersByDenomination / Region / District / EgliseChef`** :
   **aucun endpoint** de liste nominative de membres par échelle
   (descendants de nœud ∪ memberships ∪ assignments ∪ colonnes rapides).
   (Note : la spec V2 §D7 limite par défaut le dénominationnel aux agrégats —
   la nouvelle lecture sera gardée par permission explicite.)
7. **HIÉRARCHIE DES RÔLES** : `roles.priority` (liste plate) existe ; pas de
   `parent_role_id` pour déclarer « un rôle est sous un autre »
   (subordonné/hiérarchie de rôles propre au modèle de demande).
8. **FE** : pas de vue créneaux « ma hiérarchie complète » (dénomination →
   cellule), pas de chaîne de leadership ni de liste des sujets indirects ;
   pas de filtres membres par Région/District/Église chef branchés sur les
   nouveaux endpoints.
9. **MOBILE** : mêmes manques côté Flutter (écrans adaptés).

---

## [PLAN] — Implémentation (100 % additif)

### BE
1. **Migration `V273__hierarchy_cellule_type.sql`** : élargir la CHECK
   `organization_nodes.type` avec `'CELLULE'` (drop+add de la contrainte,
   données inchangées) ; commentaire de sémantique.
2. **Migration `V274__users_hierarchy_columns.sql`** :
   `ALTER TABLE users ADD COLUMN IF NOT EXISTS leader_id UUID NULL,
   cellule_id UUID, eglise_id UUID, district_id UUID, region_id UUID` +
   index ; FK optionnelles (ON DELETE SET NULL) — aucune colonne existante
   touchée.
3. **Entités (ajouts de champs uniquement)** : `User` + `leaderId`,
   `celluleId`, `egliseId`, `districtId`, `regionId` ;
   `OrganizationNodeType.CELLULE` ; `Role` + `parentRoleId` nullable
   (migration `V275__roles_parent_role_id.sql`).
4. **`MemberHierarchyService` (nouveau, module relations)** :
   - `getFullHierarchy(tenantId, userId)` → slots
     `{denomination, region, district, egliseChef, sousEglise, cellule}` en
     remontant la chaîne de nœuds (colonnes rapides, sinon memberships/
     assignments/responsable) + `fullChain` ordonnée ;
   - `getLeadershipChain(tenantId, memberId)` → chef direct → … → sommet
     (fusion `leader_id` > `responsible_id` des nœuds > `member_relations`,
     anti-cycle) ;
   - `getSubordinates(tenantId, leaderId, recursive)` → directs (leader_id,
     nœuds dont il est responsable, relations entrantes) + transitifs (BFS) ;
   - `getMembersByRegion/District/EgliseChef(tenantId, nodeId)` → descendants
     via `path` ∪ rattachements aux nœuds (memberships scopeId, assignments
     nodeId, users.*_id) ;
   - `getMembersByDenomination(denominationTenantId)` → réseau de tenants
     (racine + enfants) avec garde de permission nominative ;
   - écriture : `assignLeader`, `attachMemberToCell` (posent colonnes rapides
     ET garantissent la cohérence avec l'arbre — les deux restent vrais).
5. **Endpoints (nouveau controller `/api/v1/hierarchy/…`, additif par
   rapport à `HierarchyController`)** :
   `GET /users/{id}/full`, `GET /users/{id}/leadership-chain`,
   `GET /users/{id}/subordinates?recursive=`, `PUT /users/{id}/leader`,
   `PUT /users/{id}/attachment`, `GET /regions/{nodeId}/members`,
   `GET /districts/{nodeId}/members`, `GET /churches/{nodeId}/members`,
   `GET /denominations/{tenantId}/members` — gardes `@authz` alignées sur
   l'existant (RELATIONS_EXECUTE / fiche membre).
6. **Tests** : `MemberHierarchyServiceTest` (chaînes, cycles, récursion,
   isolation tenant), extensions `RelationApiSecurityTest`, non-régression :
   `mvn -o test` complet (les 31 tests UserHierarchyService/MemberRelation
   existants doivent passer sans modification).

### FE
7. `hooks/useMemberHierarchy.ts` (nouveaux endpoints) ;
   composant `MemberHierarchyPanel` (créneaux + chaîne de leadership +
   sujets directs/indirects) monté **en plus** dans la fiche membre et la
   page profil ; filtres « Région / District / Église chef » sur
   l'annuaire des membres ; organigramme chefs/sujets (arbre existant
   `HierarchyTree` réutilisé, données enrichis). Tests vitest.

### MOBILE
8. `relation_service.dart` / nouveau `member_hierarchy_service.dart` :
   mêmes appels ; écran « Ma hiérarchie » (slots + chaîne), carte « mes
   sujets » (directs/indirects) dans le profil, filtres par échelle dans
   les organizations screens. Widgets tests + `flutter analyze`.

### Livraison
9. Cocher le présent fichier au fur et à mesure ; push sur `main` à chaque
   étape validée.
