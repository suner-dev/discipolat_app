# Organization Hierarchy — Discipolat Church OS

**Version**: 1.0  
**Date**: 2026-09-22  
**Status**: ✅ PRODUCTION READY

---

## 1. Hierarchy Model

Discipolat uses a **materialized path hierarchy** (ltree-compatible) for organization units:

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
    NodeType type;  // CHURCH, CAMPUS, MINISTRY, DEPARTMENT, SUB_DEPARTMENT, TEAM, CELL, GROUP, FAMILY
    
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
    @Column(name = "path", columnDefinition = "ltree")
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

### 11.2 Integration Tests
- `OrganizationHierarchyIntegrationTest` — Full hierarchy lifecycle
- `ConfigPropagationTest` — Real-time config push < 5s
- `ScopeAuthorizationTest` — Permission checks at each level

---

## 12. References

- `docs/MULTI_TENANT_ARCHITECTURE.md` — Architecture overview
- `docs/RBAC.md` — Role-based access control
- `docs/ADMINISTRATION_MODEL.md` — Admin workflows
- `docs/TENANT_ONBOARDING.md` — Onboarding with hierarchy
- `backend/src/main/java/com/discipolat/modules/tenants/domain/OrganizationNode.java`
- `backend/src/main/java/com/discipolat/modules/tenants/service/ConfigurationResolver.java`
- `backend/src/main/java/com/discipolat/modules/tenants/service/OrganizationHierarchyService.java`