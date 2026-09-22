# Role-Based Access Control (RBAC) — Discipolat Church OS

**Version**: 1.0  
**Date**: 2026-09-22  
**Status**: ✅ PRODUCTION READY

---

## 1. RBAC Architecture

Discipolat implements a **hierarchical, scope-aware RBAC** system with:

- **Roles**: Bundles of permissions (tenant-scoped or global)
- **Permissions**: Atomic resource+action combinations
- **Scopes**: Hierarchical boundaries (TENANT → REGION → CHURCH → DEPARTMENT → FAMILY → ASSIGNED → OWN)
- **Role Assignments**: User ↔ Role + Scope + Context (space, org unit)

---

## 2. Core Entities

### 2.1 Role
```java
@Entity
@Table(name = "roles")
public class Role {
    @Id UUID id;
    @Column(name = "tenant_id") UUID tenantId;  // null = global role
    String code;        // e.g., "TENANT_ADMIN"
    String name;        // e.g., "Administrateur d'église"
    String description;
    @Enumerated(EnumType.STRING) ScopeType scopeType;  // TENANT, REGION, CHURCH, DEPARTMENT, FAMILY, GLOBAL
    Boolean system;     // true = cannot be deleted/modified
}
```

### 2.2 Permission
```java
@Entity
@Table(name = "permissions")
public class Permission {
    @Id UUID id;
    String code;        // e.g., "members.READ", "finances.RECONCILE"
    String name;
    String description;
    String resource;    // members, departments, events, assets, finances, etc.
    String action;      // READ, CREATE, UPDATE, DELETE, MANAGE, EXPORT, etc.
    ScopeType scope;    // Required minimum scope
}
```

### 2.3 RolePermission (Many-to-Many)
```java
@Entity
@Table(name = "role_permissions")
@IdClass(RolePermissionId.class)
public class RolePermission {
    @Id UUID roleId;
    @Id UUID permissionId;
}
```

### 2.4 RoleAssignment (User → Role + Scope Context)
```java
@Entity
@Table(name = "role_assignments")
public class RoleAssignment {
    @Id UUID id;
    UUID tenantId;
    UUID personId;
    UUID roleId;
    UUID organizationUnitId;  // Scope boundary (null = tenant-wide)
    UUID spaceId;             // Space-specific assignment
    LocalDateTime startedAt;
    LocalDateTime endedAt;    // null = active
    AssignmentStatus status;  // ACTIVE, ENDED, REVOKED
}
```

---

## 3. Default Roles & Permissions

### 3.1 Global Roles (tenant_id = null)

| Role Code | Name | Permissions |
|-----------|------|-------------|
| `PLATFORM_SUPER_ADMIN` | Super Admin Plateforme | ALL permissions across all tenants |

### 3.2 Tenant-Scoped Roles

| Role Code | Name | Scope Type | Key Permissions |
|-----------|------|------------|-----------------|
| `TENANT_OWNER` | Propriétaire d'église | TENANT | All tenant permissions + billing |
| `TENANT_ADMIN` | Administrateur d'église | TENANT | All operational permissions |
| `REGION_ADMIN` | Administrateur régional | REGION | Cross-church in region |
| `CHURCH_ADMIN` | Administrateur d'église locale | CHURCH | Single church operations |
| `CAMPUS_PASTOR` | Pasteur de campus | CHURCH | Campus leadership |
| `DEPARTMENT_ADMIN` | Responsable de département | DEPARTMENT | Department management |
| `FAMILY_LEADER` | Chef de famille spirituelle | FAMILY | Family care & follow-up |
| `DISCIPLE_MAKER` | Faiseur de disciples | ASSIGNED | Discipleship tracking |
| `MEMBER` | Membre | OWN | Self-service only |

---

## 4. Permission Catalog

### 4.1 Members (People)

| Permission Code | Action | Min Scope | Description |
|-----------------|--------|-----------|-------------|
| `members.READ` | READ | OWN | View own profile |
| `members.READ_TENANT` | READ | TENANT | View all members in tenant |
| `members.READ_REGION` | READ | REGION | View members in region |
| `members.READ_CHURCH` | READ | CHURCH | View members in church |
| `members.READ_DEPARTMENT` | READ | DEPARTMENT | View members in department |
| `members.READ_FAMILY` | READ | FAMILY | View members in family |
| `members.READ_ASSIGNED` | READ | ASSIGNED | View assigned disciples |
| `members.CREATE` | CREATE | TENANT | Register new members |
| `members.UPDATE` | UPDATE | DEPARTMENT | Update member profiles |
| `members.DELETE` | DELETE | TENANT | Soft delete members |
| `members.ASSIGN_ROLE` | ASSIGN_ROLE | DEPARTMENT | Assign roles to members |
| `members.TRANSFER` | TRANSFER | CHURCH | Transfer between churches |

### 4.2 Departments

| Permission Code | Action | Min Scope | Description |
|-----------------|--------|-----------|-------------|
| `departments.READ` | READ | DEPARTMENT | View department |
| `departments.READ_TENANT` | READ | TENANT | View all departments |
| `departments.CREATE` | CREATE | CHURCH | Create department |
| `departments.UPDATE` | UPDATE | DEPARTMENT | Update department |
| `departments.DELETE` | DELETE | CHURCH | Delete department |
| `departments.MANAGE_POSTS` | MANAGE_POSTS | DEPARTMENT | Manage positions/members |
| `departments.MANAGE_TASKS` | MANAGE_TASKS | DEPARTMENT | Manage department tasks |

### 4.3 Events

| Permission Code | Action | Min Scope | Description |
|-----------------|--------|-----------|-------------|
| `events.READ` | READ | DEPARTMENT | View events |
| `events.READ_TENANT` | READ | TENANT | View all events |
| `events.CREATE` | CREATE | DEPARTMENT | Create event |
| `events.UPDATE` | UPDATE | DEPARTMENT | Update event |
| `events.DELETE` | DELETE | CHURCH | Delete event |
| `events.CHECKIN` | CHECKIN | DEPARTMENT | Manage check-ins |
| `events.MANAGE_TEAM` | MANAGE_TEAM | DEPARTMENT | Manage event team |
| `events.DRESS_CODE` | DRESS_CODE | DEPARTMENT | Manage dress codes |
| `events.ARCHIVE` | ARCHIVE | CHURCH | Archive events |

### 4.4 Assets

| Permission Code | Action | Min Scope | Description |
|-----------------|--------|-----------|-------------|
| `assets.READ` | READ | DEPARTMENT | View assets |
| `assets.READ_TENANT` | READ | TENANT | View all assets |
| `assets.CREATE` | CREATE | CHURCH | Create asset |
| `assets.UPDATE` | UPDATE | DEPARTMENT | Update asset |
| `assets.DELETE` | DELETE | CHURCH | Delete asset |
| `assets.CHECKOUT` | CHECKOUT | DEPARTMENT | Checkout/return |
| `assets.MAINTENANCE` | MAINTENANCE | DEPARTMENT | Maintenance tracking |
| `assets.FINANCE` | FINANCE | CHURCH | Asset finance (TCO, depreciation) |

### 4.5 Finances

| Permission Code | Action | Min Scope | Description |
|-----------------|--------|-----------|-------------|
| `finances.READ` | READ | CHURCH | View finances |
| `finances.READ_TENANT` | READ | TENANT | View all finances |
| `finances.CREATE` | CREATE | CHURCH | Record transactions |
| `finances.UPDATE` | UPDATE | CHURCH | Update transactions |
| `finances.DELETE` | DELETE | TENANT | Void transactions |
| `finances.RECONCILE` | RECONCILE | CHURCH | Bank reconciliation |
| `finances.RECEIPT` | RECEIPT | CHURCH | Generate receipts |
| `finances.TONTINE` | TONTINE | DEPARTMENT | Manage tontines |
| `finances.REPORT` | REPORT | CHURCH | Financial reports |

### 4.6 Settings & Configuration

| Permission Code | Action | Min Scope | Description |
|-----------------|--------|-----------|-------------|
| `settings.READ` | READ | TENANT | View tenant settings |
| `settings.UPDATE_BRANDING` | UPDATE | TENANT | Update logo, colors |
| `settings.UPDATE_MODULES` | UPDATE | TENANT | Enable/disable modules |
| `settings.UPDATE_FEATURES` | UPDATE | TENANT | Configure features |
| `settings.UPDATE_CUSTOM_FIELDS` | UPDATE | TENANT | Manage custom fields |
| `settings.UPDATE_WORKFLOWS` | UPDATE | TENANT | Manage workflows |
| `settings.UPDATE_STATUSES` | UPDATE | TENANT | Manage custom statuses |

### 4.7 Invitations

| Permission Code | Action | Min Scope | Description |
|-----------------|--------|-----------|-------------|
| `invitations.CREATE` | CREATE | TENANT | Send invitations |
| `invitations.RESEND` | RESEND | TENANT | Resend invitation |
| `invitations.REVOKE` | REVOKE | TENANT | Revoke invitation |
| `invitations.READ_LIST` | READ | TENANT | View invitation list |

### 4.8 Reports & Analytics

| Permission Code | Action | Min Scope | Description |
|-----------------|--------|-----------|-------------|
| `reports.READ` | READ | DEPARTMENT | View reports |
| `reports.READ_TENANT` | READ | TENANT | View all reports |
| `reports.EXPORT` | EXPORT | TENANT | Export data (audited) |
| `reports.GENERATE` | GENERATE | CHURCH | Generate custom reports |
| `analytics.READ` | READ | CHURCH | View analytics dashboards |
| `analytics.READ_TENANT` | READ | TENANT | View tenant analytics |

### 4.9 Pastoral & Confidential

| Permission Code | Action | Min Scope | Description |
|-----------------|--------|-----------|-------------|
| `pastoral.READ` | READ | DEPARTMENT | Read pastoral notes |
| `pastoral.WRITE` | WRITE | CHURCH | Write pastoral notes |
| `pastoral.READ_ASSIGNED` | READ | ASSIGNED | Read assigned pastoral cases |

### 4.10 AI & Advanced

| Permission Code | Action | Min Scope | Description |
|-----------------|--------|-----------|-------------|
| `ai.USE` | USE | OWN | Use AI assistant |
| `ai.ADMIN` | ADMIN | TENANT | Manage AI credits/quotas |
| `ai.USAGE_READ` | READ | TENANT | View AI usage dashboard |
| `impersonation.START` | START | TENANT | Start impersonation (super admin) |
| `impersonation.STOP` | STOP | TENANT | Stop impersonation |

---

## 5. Scope Resolution Algorithm

```java
public boolean hasPermission(UUID userId, String permissionCode, UUID resourceTenantId, UUID resourceScopeId) {
    // 1. Get user's active role assignments for the resource's tenant
    List<RoleAssignment> assignments = roleAssignmentRepository
        .findActiveByPersonIdAndTenantId(userId, resourceTenantId);

    // 2. For each assignment, check if permission granted at required scope
    for (RoleAssignment ra : assignments) {
        Role role = roleRepository.findById(ra.getRoleId());
        if (role == null) continue;

        // 3. Check if role has the permission
        if (!rolePermissionRepository.existsByRoleIdAndPermissionCode(ra.getRoleId(), permissionCode)) {
            continue;
        }

        // 4. Check scope coverage
        Permission perm = permissionRepository.findByCode(permissionCode);
        if (coversScope(ra, perm.getScope(), resourceScopeId)) {
            return true;
        }
    }
    return false;
}

private boolean coversScope(RoleAssignment ra, ScopeType requiredScope, UUID resourceScopeId) {
    // TENANT covers everything
    if (ra.getOrganizationUnitId() == null && ra.getSpaceId() == null) return true;

    // Check hierarchy: TENANT > REGION > CHURCH > SUB_CHURCH > DEPARTMENT > FAMILY > ASSIGNED > OWN
    ScopeType assignmentScope = determineAssignmentScope(ra);
    return ScopeHierarchy.covers(assignmentScope, requiredScope);
}
```

---

## 6. AuthorizationService (Centralized)

```java
@Service
public class AuthorizationService {

    // Platform-level checks
    public boolean isPlatformSuperAdmin() { ... }

    // Tenant-level checks
    public boolean isTenantOwner() { ... }
    public boolean isTenantAdmin() { ... }

    // Generic permission check
    public boolean can(String permissionCode) { ... }
    public boolean can(String permissionCode, UUID resourceId) { ... }
    public boolean can(String permissionCode, UUID resourceId, UUID spaceId) { ... }

    // Scope-specific checks
    public boolean canReadMembers(UUID scopeId) { ... }
    public boolean canManageDepartment(UUID departmentId) { ... }
    public boolean canAccessFinances(UUID churchId) { ... }
}
```

**Usage in Controllers**:
```java
@PreAuthorize("@authorizationService.can('events.CREATE', #spaceId)")
@PostMapping("/spaces/{spaceId}/events")
public ResponseEntity<Event> createEvent(@PathVariable UUID spaceId, @RequestBody CreateEventRequest req) { ... }
```

---

## 7. Frontend Guards

```typescript
// RouteGuards.tsx
<RequirePermission permission="members.READ_TENANT" />
<RequireRole role="TENANT_ADMIN" />
<RequireScope scope="CHURCH" />
<RequireFeature feature="academy" />
<RequireAnyPermission permissions={["events.CREATE", "events.MANAGE_TEAM"]} />
```

---

## 8. Mobile Authorization

```dart
// AuthorizationService.dart
class AuthorizationService {
  Future<bool> can(String permission) async { ... }
  Future<bool> canInSpace(String permission, String spaceId) async { ... }
  Future<bool> hasRole(String roleCode) async { ... }
  Future<bool> hasScope(ScopeType scope) async { ... }
}

// Usage in UI
if (await auth.can('events.CREATE')) {
  showCreateEventButton();
}
```

---

## 9. Testing

### 9.1 Security Matrix Tests
`MultiTenantSecurityTests` — 36 tests covering all 320 matrix cells

### 9.2 Key Test Scenarios
| Test | Description |
|------|-------------|
| `souls_isolated` | Tenant A cannot read Tenant B souls |
| `departmentAdmin_otherDepartment_refused` | Dept admin cannot access other dept |
| `member_cannot_access_tenant_admin_endpoints` | Member blocked from admin APIs |
| `familyLeaderCannotAccessOtherFamilies` | Family leader isolated |
| `forgedActiveRole_doesNotBypassDatabaseCheck` | JWT role forgery blocked |
| `impersonation_targetTenantMismatch_refused` | Cross-tenant impersonation blocked |
| `invitationAccept_massAssignment_roleIgnored` | Mass assignment prevented |

---

## 10. Migration & Evolution

### 10.1 Adding New Permissions
1. Add to `permissions` table via Flyway migration
2. Add to relevant roles via `role_permissions` migration
3. Update `AuthorizationService` with helper method
4. Add frontend guard if needed
5. Add test to `MultiTenantSecurityTests`

### 10.2 Adding New Roles
1. Add to `roles` table via Flyway migration
2. Map permissions via `role_permissions` migration
3. Update scope type if new scope level
4. Seed in `TenantService.seedDefaultRoles()`

---

## 11. References

- `docs/security/SECURITY_MATRIX.md` — Complete 320-cell matrix
- `docs/ADMINISTRATION_MODEL.md` — Admin workflows
- `docs/TENANT_SECURITY.md` — Security hardening
- `reports/SECURITY_AUDIT_REPORT.md` — Audit results
- `backend/src/main/java/com/discipolat/common/infrastructure/security/AuthorizationService.java`
- `backend/src/test/java/com/discipolat/modules/tenants/MultiTenantSecurityTests.java`