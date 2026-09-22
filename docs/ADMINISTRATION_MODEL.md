# Administration Model — Discipolat Church OS

**Version**: 1.0  
**Date**: 2026-09-22  
**Status**: ✅ PRODUCTION READY

---

## 1. Administrative Hierarchy

```
PLATFORM_SUPER_ADMIN (Platform level)
    │
    ├── Manages all tenants
    ├── Manages SaaS plans & pricing
    ├── Impersonation (audited, 30-min TTL)
    └── Platform health monitoring
          │
          ▼
TENANT_OWNER (Tenant level - 1 per tenant)
    │
    ├── Full tenant administration
    ├── Billing & subscription management
    ├── Branding & settings
    ├── Module enablement
    └── User management (all roles)
          │
          ▼
TENANT_ADMIN (Tenant level - multiple)
    │
    ├── Day-to-day tenant operations
    ├── Space/Department/Family management
    ├── Member management
    ├── Event/Asset/Finance oversight
    └── Reports & analytics
          │
          ▼
REGION_ADMIN (Regional scope)
    │
    ├── Multiple churches/campuses in region
    ├── Cross-church coordination
    └── Regional reporting
          │
          ▼
CHURCH_ADMIN / CAMPUS_PASTOR (Church/Campus scope)
    │
    ├── Church/Campus leadership
    ├── Department oversight
    ├── Pastoral care coordination
    └── Local reporting
          │
          ▼
DEPARTMENT_ADMIN (Department scope)
    │
    ├── Department operations
    ├── Team/Family management
    ├── Task assignment
    └── Department reporting
          │
          ▼
FAMILY_LEADER (Family scope)
    │
    ├── Family/spiritual care
    ├── Member visits & follow-ups
    ├── Prayer requests
    └── Family reporting
          │
          ▼
DISCIPLE_MAKER (Assigned scope)
    │
    ├── Discipleship tracking
    ├── 1:1 follow-ups
    └── Progress reporting
          │
          ▼
MEMBER (Own scope)
    │
    ├── Personal profile
    ├── Event registration
    ├── Task completion
    └── Prayer requests
```

---

## 2. Role Definitions

### 2.1 Platform Roles

| Role | Key | Scope | Description |
|------|-----|-------|-------------|
| Platform Super Admin | `PLATFORM_SUPER_ADMIN` | Global | Full platform control, tenant lifecycle, billing, impersonation |

### 2.2 Tenant Roles

| Role | Key | Scope | Description |
|------|-----|-------|-------------|
| Tenant Owner | `TENANT_OWNER` | Tenant | Legal owner, billing, all permissions |
| Tenant Admin | `TENANT_ADMIN` | Tenant | Full operational control |
| Region Admin | `REGION_ADMIN` | Region | Multi-church coordination |
| Church Admin | `CHURCH_ADMIN` | Church | Single church leadership |
| Campus Pastor | `CAMPUS_PASTOR` | Campus | Campus-level leadership |
| Department Admin | `DEPARTMENT_ADMIN` | Department | Department management |
| Family Leader | `FAMILY_LEADER` | Family | Spiritual family care |
| Disciple Maker | `DISCIPLE_MAKER` | Assigned | Discipleship relationships |
| Member | `MEMBER` | Own | Basic participation |

---

## 3. Permission Model

### 3.1 Permission Structure

```
Permission = Resource + Action + Scope
```

| Resource | Actions | Scopes |
|----------|---------|--------|
| members | READ, CREATE, UPDATE, DELETE, ASSIGN_ROLE | TENANT, REGION, CHURCH, DEPARTMENT, FAMILY, ASSIGNED, OWN |
| departments | READ, CREATE, UPDATE, DELETE, MANAGE_POSTS | TENANT, REGION, CHURCH, DEPARTMENT |
| events | READ, CREATE, UPDATE, DELETE, CHECKIN, MANAGE_TEAM | TENANT, CHURCH, DEPARTMENT |
| assets | READ, CREATE, UPDATE, DELETE, CHECKOUT, MAINTENANCE | TENANT, CHURCH, DEPARTMENT, UNIT |
| finances | READ, CREATE, UPDATE, DELETE, RECONCILE, RECEIPT | TENANT, CHURCH |
| settings | READ, UPDATE (branding, modules, features) | TENANT |
| invitations | CREATE, RESEND, REVOKE, READ_LIST | TENANT |
| reports | READ, EXPORT, GENERATE | TENANT, REGION, CHURCH, DEPARTMENT |
| pastoral_notes | READ, WRITE | CHURCH, DEPARTMENT, ASSIGNED |
| ai_credits | READ_USAGE, MANAGE_QUOTA | TENANT |
| impersonation | START, STOP | TENANT (super admin only) |

### 3.2 Scope Resolution

```
TENANT > REGION > CHURCH > SUB_CHURCH > DEPARTMENT > FAMILY > ASSIGNED > OWN
```

- Parent scope **implicitly covers** children (validated in tests)
- `ASSIGNED` = explicitly assigned via `role_assignment`
- `OWN` = the user's own record only

---

## 4. Administrative Workflows

### 4.1 Tenant Creation (Super Admin)

```
1. POST /api/platform/admin/tenants
   { name, slug, legal_name, country, city, timezone, currency, language, plan_key }

2. System auto-seeds:
   - Default modules (people, events, notifications, dashboard, org)
   - Default roles & permissions
   - SaasPlan quotas applied
   - ROOT_CHURCH OrganizationNode created

3. Super Admin sends invitation to Tenant Owner
   POST /api/invitations { email, role_code: TENANT_OWNER }
```

### 4.2 User Onboarding (Tenant Admin)

```
1. POST /api/invitations { email, role_code, space_id?, organization_unit_id? }
   → Email sent with magic link (7-day expiry)

2. Recipient clicks link → AcceptInvitationPage
   → Creates User + TenantMembership + SpaceMemberships

3. Auto-assignment to default space (if configured)
   → Welcome notification sent
```

### 4.3 Space/Department Creation

```
1. POST /api/spaces { name, code, space_type, template_code, organization_unit_id }
   → Space created with template modules

2. PUT /api/spaces/{id}/modules/{moduleId} { enabled: true, configuration_json }
   → Module enabled with space-specific config

3. Real-time propagation → WebSocket push to all space members (< 5s)
```

### 4.4 Role Assignment

```
1. POST /api/role-assignments
   { person_id, role_id, organization_unit_id?, space_id?, started_at }

2. PermissionVersion incremented
   → Outbox event → RealTimeService.pushPermissionChange()
   → WebSocket push to affected users (< 5s)
   → UI adapts without logout
```

---

## 5. Impersonation (Super Admin Only)

| Property | Value |
|----------|-------|
| Token TTL | 30 minutes |
| Identity | Target user only (no elevation) |
| Audit | Start/end logged with IP, UA, duration, reason |
| Banner | Permanent "👁 Impersonation de X — Quitter" |
| Restrictions | Cannot impersonate other super admins |

---

## 6. Branding & Customization

| Setting | Scope | Managed By |
|---------|-------|------------|
| Logo | Tenant | TENANT_OWNER/ADMIN |
| Colors (primary, secondary, accent) | Tenant | TENANT_OWNER/ADMIN |
| Cover image | Tenant | TENANT_OWNER/ADMIN |
| Timezone | Tenant | TENANT_OWNER/ADMIN |
| Currency (EUR/FCFA/USD) | Tenant | TENANT_OWNER/ADMIN |
| Language (FR/EN/PT/ES/SW/AR) | Tenant | TENANT_OWNER/ADMIN |
| Custom CSS variables | Tenant | TENANT_OWNER/ADMIN |
| Module enablement | Tenant + Space | TENANT_ADMIN |
| Space-level branding | Space | SPACE_ADMIN |

---

## 7. Module Management

### 7.1 Module Categories

| Category | Modules | Source |
|----------|---------|--------|
| Core | Auth, Tenant, Users, People | CORE |
| Community | Events, Families, Departments, Souls | EXISTING |
| Discipleship | Discipleship Path, Mentoring, Prayer Journal | EXISTING |
| Finance | Finances, Tontine, Payments, Assets | EXISTING |
| Media | Sermons, Streaming, Documents | EXISTING |
| Analytics | Dashboard, KPI Narrative, Engagement Analytics | ENGINE |
| AI | Discipolat AI Assistant, Predictions | ENGINE |
| Admin | Workflow, Custom Fields, Custom Statuses | ENGINE |

### 7.2 Enforcement Points

1. **Backend**: `ModuleRouter` (Spring interceptor) → 403 if disabled
2. **Frontend**: `RequireFeature` guard → hides routes/menu items
3. **Mobile**: `ModuleFeature` check → conditional screen rendering

---

## 8. Audit & Compliance Admin

### 8.1 Audit Dashboards

| Dashboard | Access | Data |
|-----------|--------|------|
| Platform Audit | PLATFORM_SUPER_ADMIN | Cross-tenant audit events |
| Tenant Audit | TENANT_OWNER/ADMIN | Tenant-scoped audit events |
| Export Audit | TENANT_OWNER/ADMIN | All data exports (who/what/when) |
| Impersonation Audit | PLATFORM_SUPER_ADMIN | All impersonation sessions |
| Security Matrix | TENANT_ADMIN+ | Permission matrix visualization |

### 8.2 GDPR Compliance

| Endpoint | Purpose | Access |
|----------|---------|--------|
| `POST /api/compliance/export` | Full data export (JSON) | User + Tenant Admin |
| `POST /api/compliance/delete` | Right to erasure | User + Tenant Owner |
| `GET /api/compliance/audit` | Personal audit trail | User |

---

## 9. Monitoring & Health (Admin View)

| Metric | Alert Threshold | Dashboard |
|--------|----------------|-----------|
| API error rate | > 1% | Grafana: API Health |
| DB connection pool | > 80% | Grafana: Database |
| Redis memory | > 85% | Grafana: Cache |
| Sync lag (mobile) | > 10 s | Grafana: Sync |
| Failed webhooks | > 5/min | Grafana: Webhooks |
| AI credit exhaustion | > 90% | Grafana: AI Usage |

---

## 10. Backup Administration

| Operation | Frequency | Retention | Initiated By |
|-----------|-----------|-----------|--------------|
| PostgreSQL snapshot | Daily | 30 days | Automated (backup-postgres.yml) |
| PostgreSQL PITR | Continuous | 7 days | Automated (WAL) |
| Redis backup | Hourly | 7 days | Automated |
| File storage sync | Daily | 90 days | Automated (S3 versioning) |
| Full restore test | Monthly | N/A | TENANT_OWNER + Platform |

---

## 11. References

- `docs/RBAC.md` — Complete RBAC reference
- `docs/TENANT_SECURITY.md` — Security hardening guide
- `docs/ORGANIZATION_HIERARCHY.md` — Org unit management
- `docs/TENANT_ONBOARDING.md` — Step-by-step onboarding
- `docs/security/SECURITY_MATRIX.md` — Permission matrix
- `reports/SECURITY_AUDIT_REPORT.md` — Security audit