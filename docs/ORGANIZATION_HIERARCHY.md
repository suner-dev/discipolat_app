# Organization Hierarchy — Discipolat Church OS

**Version**: 1.1
**Date**: 2026-10-22
**Statut**: Hiérarchie d'organisation : racines, campus, sous-églises, départements — operationnelle. Le constat **H8 est clos** : la colonne `path` est en `text` (et non `ltree`, indisponible sur une base migrée), et le roster nominatif des membres d'un nœud est exposé (§7.1).

---

## 1. Hierarchy Model

Discipolat uses a **materialized path hierarchy** for organization units. The path is a plain
`text` column (NOT PostgreSQL `ltree` — see « Note H8 » below), and descendants are resolved
either by `LIKE '<parentPath>%'` (`OrganizationNodeRepository.findDescendants`) or, for the
roster, by walking `parentId` in memory (cycle-safe, no SQL operator to keep in sync):

```
TENANT (Root)
    │
    └── ROOT_CHURCH (Level 0) — "Église Centrale"
          │
          ├── CAMPUS (Level 1) — "Campus Nord"
          │       │
          │       ├── MINISTRY (Level 2) — "Ministère Jeunesse"
          │       │       │
          │       │       ├── DEPARTMENT (Level 3) — "Département Louange"
          │       │       │       │
          │       │       │       ├── SUB_DEPARTMENT (Level 4) — "Équipe Chant"
          │       │       │       │
          │       │       │       └── TEAM (Level 5) — "Choriste"
          │       │       │
          │       │       └── CELL (Level 4) — "Cellule Étudiants"
          │       │
          │       └── DEPARTMENT (Level 3) — "Département Enfants"
          │
          └── CAMPUS (Level 1) — "Campus Sud"
                  │
                  └── DEPARTMENT (Level 2) — "Département Accueil"
```

---

## 2. Entity: OrganizationNode

```java
@Entity
@Table(name = "organization_nodes")
public class OrganizationNode {
    @Id UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;
    
    @Column(name = "parent_id")
    UUID parentId;  // null = root
    
    @Column(name = "name", nullable = false)
    String name;
    
    @Column(name = "code", unique = true)
    String code;  // e.g., "CAMPUS_NORD", "DEPT_LOUANGE"
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    OrganizationNodeType type;  // ROOT_CHURCH, CAMPUS, SUB_CHURCH, ASSEMBLY, REGION, DISTRICT, DEPARTMENT, GROUP
    
    String description;
    String status;  // ACTIVE, INACTIVE, ARCHIVED
    
    String icon;    // Lucide icon name
    String color;   // Hex color
    Integer sortOrder;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "config_source")
    ConfigSource configSource;  // DEFAULT, INHERITED, OVERRIDDEN
    
    @Column(name = "resolved_config_json", columnDefinition = "jsonb")
    Map<String, Object> resolvedConfigJson;
    
    // Materialized path for efficient ancestry queries
    // H8 : `ltree` n'est PAS utilisable sur une base migrée (le type d'extension
    // n'existe pas partout, et la colonne doit survivre à un dump/restore).
    // Le path est donc du texte simple, segmenté par des points.
    @Column(name = "path", nullable = false, columnDefinition = "text")
    String path;  // e.g., "root.church.campus_nord.ministry_jeunesse.dept_louange"
    
    @Column(name = "level")
    Integer level;  // 0 = root church, 1 = campus, 2 = ministry, etc.
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
}

enum NodeType {
    CHURCH,           // Root church (1 per tenant)
    CAMPUS,           // Physical campus / site
    MINISTRY,         // Ministry area (Jeunesse, Enfants, Louange...)
    DEPARTMENT,       // Operational department
    SUB_DEPARTMENT,   // Sub-division
    TEAM,             // Working team
    CELL,             // Small group / cell group
    GROUP,            // Generic group
    FAMILY            // Spiritual family
}

enum ConfigSource {
    DEFAULT,      // Platform defaults
    INHERITED,    // From parent
    OVERRIDDEN    // Locally customized
}
```

---

## 3. Path & Level Semantics

| Level | Type | Path Example | Use Case |
|-------|------|--------------|----------|
| 0 | CHURCH | `tenant_root` | Tenant root (always 1) |
| 1 | CAMPUS | `tenant_root.campus_nord` | Physical location |
| 2 | MINISTRY | `tenant_root.campus_nord.ministry_jeunesse` | Ministry area |
| 3 | DEPARTMENT | `...dept_louange` | Operational unit |
| 4 | SUB_DEPARTMENT | `...equipe_chant` | Sub-unit |
| 5 | TEAM | `...choristes` | Working team |
| 4 | CELL | `...cellule_etudiants` | Small group |
| 5 | GROUP | `...groupe_priere` | Generic group |
| 5 | FAMILY | `...famille_martin` | Spiritual family |

---

## 4. Configuration Inheritance

### 4.1 Resolution Chain

```
Platform DEFAULT
    │
    ▼
ROOT_CHURCH (OVERRIDDEN or INHERITED)
    │
    ▼
CAMPUS (OVERRIDDEN or INHERITED)
    │
    ▼
MINISTRY/DEPARTMENT (OVERRIDDEN or INHERITED)
    │
    ▼
TEAM/CELL/FAMILY (OVERRIDDEN or INHERITED)
```

### 4.2 ConfigurationResolver Service

```java
@Service
public class ConfigurationResolver {
    
    // Resolves full config chain for a node
    public Map<String, Object> resolveConfig(UUID nodeId) { ... }
    
    // Gets effective config (with inheritance)
    public Map<String, Object> getEffectiveConfig(UUID nodeId) { ... }
    
    // Invalidates cache on config change
    public void invalidateCache(UUID nodeId) { ... }
    
    // Finds all descendants
    public List<UUID> findDescendantIds(UUID ancestorId) { ... }
}
```

### 4.3 Configurable Properties

| Property | Inheritable | Example Values |
|----------|-------------|----------------|
| `timezone` | ✅ | `Europe/Paris`, `Africa/Douala` |
| `currency` | ✅ | `EUR`, `FCFA`, `USD` |
| `language` | ✅ | `fr`, `en`, `pt`, `es`, `sw`, `ar` |
| `date_format` | ✅ | `DD/MM/YYYY`, `MM/DD/YYYY` |
| `week_start` | ✅ | `MONDAY`, `SUNDAY` |
| `branding` | ❌ | Tenant-level only |
| `modules_enabled` | ✅ | `["events", "finances", "assets"]` |
| `workflow_definitions` | ✅ | Custom approval chains |
| `custom_statuses` | ✅ | Status sets per entity |
| `custom_fields` | ✅ | Field definitions per entity |
| `dress_codes` | ✅ | Dress code templates |

---

## 5. Scope Resolution

### 5.1 Scope Types

```java
public enum ScopeType {
    TENANT,           // Entire tenant (TENANT_OWNER, TENANT_ADMIN)
    REGION,           // Multi-campus region (REGION_ADMIN)
    CHURCH,           // Single church/campus (CHURCH_ADMIN, CAMPUS_PASTOR)
    SUB_CHURCH,       // Sub-church (if applicable)
    DEPARTMENT,       // Department (DEPARTMENT_ADMIN)
    FAMILY,           // Spiritual family (FAMILY_LEADER)
    ASSIGNED,         // Explicitly assigned persons (DISCIPLE_MAKER)
    OWN               // Self only (MEMBER)
}
```

### 5.2 Scope Hierarchy (Parent covers Children)

```
TENANT
    └── REGION
            └── CHURCH / CAMPUS
                    └── SUB_CHURCH
                            └── DEPARTMENT
                                    └── FAMILY
                                            └── ASSIGNED
                                                    └── OWN
```

### 5.3 Scope Coverage Rules

| Assigned Scope | Covers | Example |
|----------------|--------|---------|
| `TENANT` | All | Tenant Admin sees everything |
| `REGION` | Region + children | Region Admin sees all campuses in region |
| `CHURCH` | Church + children | Church Admin sees all departments |
| `DEPARTMENT` | Dept + families + teams | Dept Admin sees teams & cells |
| `FAMILY` | Family + assigned | Family Leader sees family members |
| `ASSIGNED` | Only assigned persons | Disciple Maker sees disciples |
| `OWN` | Self only | Member sees own profile |

### 5.4 Implementation

```java
public class ScopeHierarchy {
    
    private static final Map<ScopeType, Set<ScopeType>> COVERS = Map.of(
        TENANT, Set.of(TENANT, REGION, CHURCH, SUB_CHURCH, DEPARTMENT, FAMILY, ASSIGNED, OWN),
        REGION, Set.of(REGION, CHURCH, SUB_CHURCH, DEPARTMENT, FAMILY, ASSIGNED, OWN),
        CHURCH, Set.of(CHURCH, SUB_CHURCH, DEPARTMENT, FAMILY, ASSIGNED, OWN),
        SUB_CHURCH, Set.of(SUB_CHURCH, DEPARTMENT, FAMILY, ASSIGNED, OWN),
        DEPARTMENT, Set.of(DEPARTMENT, FAMILY, ASSIGNED, OWN),
        FAMILY, Set.of(FAMILY, ASSIGNED, OWN),
        ASSIGNED, Set.of(ASSIGNED, OWN),
        OWN, Set.of(OWN)
    );
    
    public static boolean covers(ScopeType assigned, ScopeType required) {
        return COVERS.getOrDefault(assigned, Set.of()).contains(required);
    }
}
```

---

## 6. Space Mapping

Each `OrganizationNode` can have **one Space** (Department, Family, or Sub-Team):

| Node Type | Space Type | Example |
|-----------|------------|---------|
| DEPARTMENT | `DEPARTMENT` | "Département Louange" |
| SUB_DEPARTMENT | `SUB_TEAM` | "Équipe Chant" |
| TEAM | `SUB_TEAM` | "Choristes" |
| CELL | `SUB_TEAM` | "Cellule Étudiants" |
| GROUP | `SUB_TEAM` | "Groupe Prière" |
| FAMILY | `FAMILY` | "Famille Martin" |

### Space Configuration
```java
@Entity
@Table(name = "spaces")
public class Space {
    @Id UUID id;
    UUID tenantId;
    UUID organizationUnitId;  // Links to OrganizationNode
    
    @Enumerated(EnumType.STRING)
    SpaceType spaceType;  // DEPARTMENT, FAMILY, SUB_TEAM
    
    String templateCode;  // e.g., "DEPT_WORSHIP", "FAMILY_STANDARD"
    String name;
    String code;
    String icon;
    String color;
    
    @Column(name = "configuration_json", columnDefinition = "jsonb")
    Map<String, Object> configurationJson;  // Space-specific overrides
    
    @Enumerated(EnumType.STRING)
    VisiblePeopleScope visiblePeopleScope;  // CAMPUS, CHURCH
}
```

---

## 7. API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/org/units` | Create organization unit |
| `GET` | `/api/org/units` | List units (tree or flat) |
| `GET` | `/api/org/units/{id}` | Get unit details |
| `PUT` | `/api/org/units/{id}` | Update unit |
| `DELETE` | `/api/org/units/{id}` | Archive unit (soft delete) |
| `GET` | `/api/org/units/{id}/descendants` | Get all descendants |
| `GET` | `/api/org/units/{id}/ancestors` | Get ancestry chain |
| `GET` | `/api/org/units/{id}/resolved-config` | Get effective config |
| `PUT` | `/api/org/units/{id}/config-source` | Set config source (DEFAULT/INHERITED/OVERRIDDEN) |
| `PUT` | `/api/org/units/{id}/local-config` | Set local override config |
| `GET` | `/api/org/units/tree` | Full hierarchy tree |
| `POST` | `/api/org/campus` | Create campus (wizard step) |

> Les routes réellement servies en V3 sont sous `/api/v1/tenant/organization/**`
> (`OrganizationNodeV3Controller`, `OrganizationLevelController`, `NodeMemberRosterController`),
> et les routes de navigation sous `/api/v1/org/**` (`OrganizationHierarchyService`).
> `docs/API.md` est **généré** (`scripts/generate-api-docs.sh`) : c'est la liste de référence,
> ce tableau n'en est que la vue d'ensemble.

### 7.1 Roster des membres d'un nœud (« consulter les membres à chaque échelle »)

```text
GET /api/v1/tenant/organization/nodes/{nodeId}/members
      ?scope=DIRECT|SUBTREE   (défaut SUBTREE ; valeur inconnue ⇒ SUBTREE, jamais 500)
      &q=<nom|prénom|email>   (trimé + insensible à la casse côté serveur ; blanc ⇒ non envoyé)
      &page=0&size=20         (size borné — clamp serveur)
→ 200 { nodeId, nodeName, nodeType, scope, content[], page, size,
        totalElements, totalPages, memberCountDirect, memberCountSousArbre }
```

`content[]` (projection `NodeMemberRow`, jamais l'entité JPA) : `userId`, `firstName`,
`lastName`, `email`, `photoUrl`, `statut`, `attachedNodeId`, `attachedNodeName`,
`attachedNodeType`, `direct` (rattaché au nœud consulté vs à une sous-structure),
`origine`, `roles[]`, `viaChaine[]` (fil d'Ariane du nœud consulté jusqu'au point
d'attache), `depuis`.

**Règles de calcul** (`NodeMemberRosterService`, service dédié — l'endpoint historique
`GET /api/v1/org/nodes/{id}/members` est conservé, non supprimé) :

- **Trois origines**, bornées au périmètre : `ADHESION_NOEUD` (membership ACTIVE dont la
  `scopeId` est dans le sous-arbre), `ASSIGNATION_V3` (capacité ACTIVE posée sur un nœud du
  sous-arbre), `RESPONSABLE_NOEUD` (`responsibleId` d'un nœud du sous-arbre).
- **`ADHESION_TENANT` ne déborde plus** : une adhésion de portée `TENANT` ne rattache à
  aucune sous-structure — elle compte **uniquement sous la racine**. C'était le défaut B2
  (un membre « tenant » inondait chaque nœud).
- **Déduplication par `userId`, au plus profond** (le plus près du terrain) ; à profondeur
  égale, le plus ancien. Les `roles[]` sont agrégés **hors arbitrage** : un membre
  « Diacre » ici et « Responsable » dans une sous-cellule affiche ses deux rôles.
- **Compteurs structurels, pas filtrés** : `memberCountDirect` / `memberCountSousArbre`
  décrivent l'entité, donc `q` ne les modifie pas (seuls `totalElements` / `totalPages`
  suivent le filtre). Une recherche de trois lettres ne peut pas faire afficher « 1 » sur
  une entité de 40 membres.
- **Toutes les sources en trois lectures** (`loadSources`) : memberships, assignments,
  libellés de rôles préchargés — le N+1 corrigé par la mission ne revient pas.
- **Garde** : `@PreAuthorize("isAuthenticated()")`, stricte parité avec `GET /api/v1/users`
  (l'annuaire est déjà consultable par tout membre authentifié du tenant). Le roster ne
  fait qu'**appliquer** cette même visibilité à une entité ; il n'ouvre rien de plus.
  `TenantContext.requireTenantId()` + nœud revérifié dans le tenant → **404** cross-tenant,
  jamais 403 (pas d'énumération d'identifiants).

### 7.2 `memberCount` de l'agrégat vs compteurs du roster (ne pas les confondre)

| | `GET /nodes/{id}/aggregate` | `GET /nodes/{id}/members` |
|---|---|---|
| Nature | **instantané persisté** (`NodeAggregateSnapshot`, série de progression) | calcul **à la demande**, sur l'état courant |
| `memberCount` / `memberCountSousArbre` | memberships ACTIVE distinct couvrant le sous-arbre | adhésions + capacités + responsables, dédupliqués |
| `memberCountDirect` | adhésions de portée du nœud, capacités **ici**, et `responsibleId` du nœud | mêmes sources, mais selon l'arbitrage de profondeur du roster |
| Sens à `q` / pagination | insensible (snapshot) | compteurs insensibles ; `totalElements`/`totalPages` sensibles |

Les deux peuvent donc **légitimement diverger** (instantané contre état courant, périmètre
des capacités) : ce n'est pas une incohérence à « réaligner » à la sauvage. La règle tenue
est plus étroite et vérifiée par test : **`direct ⊆ sous-arbre`** et un membre n'est compté
qu'une fois par échelle. L'additif BE-3 (`memberCountDirect` dans `metrics` et `toView`) n'a
modifié aucun shape existant.

### 7.3 Parcours pastoral temporel (« le récit d'un membre »)

`GET /api/v1/hierarchy/me/parcours` (`isAuthenticated`) et
`GET /api/v1/hierarchy/users/{userId}/parcours`
(`@authz.can('RELATIONS_EXECUTE','TENANT',null)`, même garde que la fiche hiérarchie)
exposent, en **lecture seule** et à partir des **seules tables déjà persistées**, le récit
chronologique d'un membre. **Aucune migration, aucune nouvelle table, aucun agrégat
existant réécrit** — additif pur (`UserHierarchyService.parcours`).

| Source (déjà persistée) | Événements produits |
|---|---|
| `TenantMembership` (`joinedAt`, `markTransferred`/`isTransferred`) | `ADHESION/DEBUT`, `TRANSFERT/FIN` |
| `MemberRoleAssignment` (capacité × nœud, `assignedAt`/`endedAt`/`status`) | `CAPACITE/DEBUT`, `CAPACITE/FIN` |
| `MemberRelation` via `MemberRelationService.relationEvents` (**tous statuts**) | `RATTACHEMENT/DEBUT`, `RATTACHEMENT/FIN` |

- **Shape** : `{ userId, nomComplet, depuis, ancienneteJours, totalEvenements, evenements[],
  resume }`. Chaque événement = `horodatage` (ISO) / `type` (ADHESION | TRANSFERT | CAPACITE
  | RATTACHEMENT) / `action` (DEBUT | FIN) / `libelle` (phrase lisible) / `details`. Tri
  chronologique **ascendant**, les événements sans date en fin (`nullsLast`).
- **`resume`** : `parType` (décompte par nature), `transferts`, `rattachementsRompus` — les
  signaux faibles du suivi. Un transfert n'**efface** pas l'appartenance : il la **raconte**
  (ligne `ADHESION` conservée + ligne `TRANSFERT` ajoutée), cohérent avec la traçabilité
  `TenantMembership` (§ V223).
- **Garde & isolation** : tenant issu du seul contexte serveur ; un membre d'une autre
  église est **invisible (404)**, jamais 403. Nœuds, rôles et noms résolus en batch —
  **aucun N+1**.
- **Ratchet R4** : `relationEvents` ne porte **pas** de `@Transactional` propre (la classe
  `MemberRelationService` la porte déjà et `parcours` l'appelle dans sa transaction
  `readOnly`) — le plafond gelé (3032) reste tenu, `ArchitectureRulesTest` vert.
- **Test** : `UserHierarchyServiceParcoursTest` (4) — ordre chronologique, transfert
  raconté, fin de capacité distinguée d'une capacité active datée, rupture de rattachement,
  et 404 cross-tenant sans rechargement en aval.
- **Consommateurs UI** (additifs, montés sous les cartes existantes) :
  - **Web** — `frontend/src/components/relations/ParcoursCard.tsx`, monté dans `ProfilePage`
    (sous « Ma hiérarchie ») et dans `UserDetailModal` (sous le bloc hiérarchie). Le sujet est
    tranché par `useAuthOptional` : **soi** → `/hierarchy/me/parcours` (`isAuthenticated`),
    **un tiers** → `/hierarchy/users/{id}/parcours` (`RELATIONS_EXECUTE`). Un membre sans cette
    permission lit donc SON propre récit dans sa fiche, et la fiche d'un tiers s'efface
    simplement si la garde refuse — ni bloc vide, ni message d'erreur trompeur. Les codes
    (`ADHESION | TRANSFERT | CAPACITE | RATTACHEMENT`, `DEBUT | FIN`) passent par i18n sur les
    6 locales ; le `libelle` serveur ne sert que d'infobule. Tests : `ParcoursCard.test.tsx` (4).
  - **Mobile** — `mobile/lib/presentation/widgets/parcours_card.dart` (`MyParcoursCard` +
    `ParcoursCard`), monté dans `profile_screen.dart` sous « Ma hiérarchie », et
    `RelationService.myParcours()`. Le mobile ne lit que **son propre** parcours (`/me/...`) :
    la consultation d'un tiers suppose `RELATIONS_EXECUTE`, service de fiche déjà couvert côté
    web. Même discipline que `hierarchy_card.dart` : chargement isolé, repli silencieux quand il
    n'y a rien à raconter, échec explicite avec « Réessayer », parsing défensif
    (`ParcoursParsing`/`ParcoursEvent`) testable sans HTTP. 12 clés dans les 6 cartes
    `AppLocalizations`. Tests : `parcours_card_test.dart` (7).

---

## 8. Frontend Components

### 8.1 Organization Tree
```tsx
// components/org/OrganizationTree.tsx
<OrganizationTree
  tenantId={tenantId}
  onNodeSelect={handleNodeSelect}
  showConfigSource={true}
  editable={hasPermission('org.UPDATE')}
/>
```

### 8.2 Config Inheritance View
```tsx
// pages/admin/ConfigInheritancePage.tsx
<ConfigInheritanceView
  nodeId={selectedNodeId}
  showResolved={true}
  showOverrides={true}
  onOverride={handleOverride}
/>
```

### 8.3 Scope Selector
```tsx
// components/org/ScopeSelector.tsx
<ScopeSelector
  value={currentScope}
  onChange={setScope}
  allowedScopes={userAllowedScopes}
  showHierarchy={true}
/>
```

### 8.4 Roster des membres (web)

- `hooks/useOrganizationV3.ts :: useNodeMembers(nodeId, { scope, q, page, size })` —
  requête react-query `['t', tenantKey, 'org', 'members', nodeId, scope, q, page, size]`,
  `enabled: !!nodeId`, et **`placeholderData: keepPreviousData`** : changer de page ou
  taper dans le filtre conserve la lecture précédente (sinon le panneau est remplacé par
  un spinner à chaque frappe, et le champ de recherche disparaît sous le curseur).
- `components/organization/NodeRoster.tsx` — vue **partagée** : l'onglet « Membres » de la
  fiche nœud (`pages/admin/OrganizationNodeDetailPage.tsx`) et l'annuaire scopé
  (`pages/UsersPage.tsx` derrière `?nodeId=&scope=`) rendent le MÊME composant, donc ne
  peuvent pas diverger. Compteurs, badge « rattachés ici » vs « via : <nœud> », pagination
  (« n / total », seulement si plusieurs pages), et **dégradation explicite** : une erreur
  de lecture affiche un message, elle n'est jamais présentée comme « aucun membre ».
- `pages/OrganizationBrowserPage.tsx` — lien « Voir les membres » vers
  `/users?nodeId=…&scope=…` ; `UsersPage` garde l'`enabled: !scopedNodeId` sur sa requête
  d'annuaire : sous `?nodeId=`, l'annuaire entier n'est pas re-fetché.
- i18n : clés `orgV3.members.*`, **parité stricte sur les 6 locales** (fr, en, pt, es, sw,
  ar). Une clé non traduite ne doit JAMAIS fuiter à l'écran sous sa forme brute (test).

---

## 9. Mobile (Flutter)

### 9.1 Organization Tree Screen
```dart
// features/organization/org_tree_screen.dart
class OrgTreeScreen extends ConsumerWidget {
  // Riverpod provider for hierarchy
  // Expandable tree with config source indicators
  // Tap node → navigate to Space or Config
}
```

### 9.2 Scope-Aware Data
```dart
// core/scope/scope_provider.dart
final currentScopeProvider = StateProvider<ScopeType>((ref) => ScopeType.OWN);

// Automatic filtering based on scope
final visibleMembersProvider = FutureProvider((ref) async {
  final scope = ref.watch(currentScopeProvider);
  return api.getMembers(scope: scope);
});
```

### 9.3 Roster des membres (mobile)

- `data/services/organization_v3_api.dart :: nodeMembers(nodeId, scope, q, page, size)` —
  client réel (`api_service.dart`), lit `GET /tenant/organization/nodes/{id}/members` ;
  payload non-Map ⇒ `NodeMembersPage.empty(nodeId)` (jamais un crash d'écran).
- `data/models/node_member_roster.dart` — **parsing défensif** : champ absent, `null` ou
  mal typé (liste devenue objet, entier devenu chaîne) ⇒ repli, pas d'exception. Un
  `origine` absent reste `null` : la chaîne n'est **jamais inventée** (le défaut corrigé
  était un `?? 'ADHESION_NOEUD'` qui faisait afficher une adhésion là où le serveur
  n'avait rien dit).
- `presentation/screens/tenant/node_detail_screen.dart :: _membersSection()` — portée
  DIRECT/SUBTREE, recherche, badges, « Charger plus », erreur isolée : un échec du roster
  ne fait pas échouer la fiche complète.

> **Réalité de l'i18n mobile** (vérifié, à ne pas supposer) : la source de vérité est
> `lib/l10n/app_localizations.dart` — dictionnaires écrits à la main, six cartes
> (`_french`, `_english`, `_portuguese`, `_spanish`, `_swahili`, `_arabic`), repli sur FR,
> `translate(key)` + getters, substitution par gabarits `{count}` / `{name}` / `{direct}`.
> Les fichiers `.arb` de `lib/l10n/` ne sont **pas consommés** (aucun `l10n.yaml`, aucun
> import `flutter_gen` ; seul `generate: true` reste dans `pubspec.yaml`). Ajouter une clé
> dans les `.arb` seulement ne produit aucune traduction : il faut les six cartes.

---

## 10. Migration & Seeding

### 10.1 Default Hierarchy (Per Tenant)
```sql
-- Created automatically on tenant creation
INSERT INTO organization_nodes (id, tenant_id, parent_id, name, code, type, path, level, config_source, status)
VALUES 
  (gen_random_uuid(), :tenantId, null, 'Église Centrale', 'ROOT_CHURCH', 'CHURCH', 'root', 0, 'DEFAULT', 'ACTIVE');
```

### 10.2 Campus Creation (Onboarding Wizard)
```java
// OrganizationHierarchyService.createCampus()
public OrganizationNode createCampus(UUID tenantId, String name, String code, UUID pastorId) {
    UUID rootId = findRootChurch(tenantId);
    OrganizationNode campus = OrganizationNode.builder()
        .tenantId(tenantId)
        .parentId(rootId)
        .name(name)
        .code(code.toUpperCase())
        .type(NodeType.CAMPUS)
        .path(root.getPath() + "." + code.toLowerCase())
        .level(1)
        .configSource(ConfigSource.INHERITED)
        .build();
    return repository.save(campus);
}
```

---

## 11. Testing

### 11.1 Unit Tests
- `ConfigurationResolverTest` — Config inheritance chain
- `ScopeHierarchyTest` — Scope coverage rules
- `OrganizationHierarchyServiceTest` — CRUD + tree operations
- `NodeMemberRosterServiceTest` (17) — direct/sous-arbre, dédup au plus profond, compteurs
  insensibles à `q`, `direct ⊆ sous-arbre`, rôles agrégés hors arbitrage, repli données
  manquantes
- `OrganizationHierarchyMembersAccessTest` (6) — sens descendant corrigé (défaut B2 :
  l'ancienne comparaison échouerait ces tests), portée `TENANT` qui ne déborde plus

### 11.2 Integration Tests
- `OrganizationHierarchyIntegrationTest` — Full hierarchy lifecycle
- `ConfigPropagationTest` — Real-time config push < 5s
- `ScopeAuthorizationTest` — Permission checks at each level
- `NodeMembersSecurityTest` (10) — RBAC/HTTP : 200 authentifié, 404 cross-tenant (jamais
  403), clamp `size`/`page`, `scope` inventé ⇒ SUBTREE sans le renvoyer verbatim

### 11.3 Web & mobile
- `frontend/src/__tests__/NodeMembersRoster.test.tsx` (18) — contrat d'appel du hook,
  rendu/comportement `NodeRoster`, onglet « Membres » de la fiche nœud, parité i18n des
  6 locales rendues par le vrai `I18nProvider` (zéro clé brute à l'écran)
- `frontend/src/__tests__/UsersPageScopedRoster.test.tsx` (8) — annuaire scopé
  `?nodeId=&scope=`, garde-fous sur un `scope` malveillant, et **non-régression** : sans
  paramètre, l'annuaire se comporte exactement comme avant
- `mobile/test/node_member_roster_test.dart` (21) — parsing défensif, `origine` jamais
  inventé, table littérale des 9 clés × 6 locales, section rendue en fr/en/ar sur le vrai
  écran

---

## 12. References

- `docs/MULTI_TENANT_ARCHITECTURE.md` — Architecture overview
- `docs/RBAC.md` — Role-based access control
- `docs/ADMINISTRATION_MODEL.md` — Admin workflows
- `docs/TENANT_ONBOARDING.md` — Onboarding with hierarchy
- `backend/src/main/java/com/discipolat/modules/tenants/domain/OrganizationNode.java`
- `backend/src/main/java/com/discipolat/modules/tenants/domain/NodeMemberRosterService.java`
- `backend/src/main/java/com/discipolat/modules/tenants/api/NodeMemberRosterController.java`
- `backend/src/main/java/com/discipolat/modules/tenants/domain/ConfigurationResolver.java`
- `backend/src/main/java/com/discipolat/modules/tenants/domain/OrganizationHierarchyService.java`
- `docs/ECLESIASTICAL_HIERARCHY_TODO.md` — audit et plan d'exécution du roster (§1 verdicts,
  §2 tâches, §5 preuves d'exécution)