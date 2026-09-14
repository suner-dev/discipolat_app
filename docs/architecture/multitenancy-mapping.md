# CARTOGRAPHIE FINE — Existant Multi-tenant (§27-72) — G1.1

> Remplissage de la fiche de suivi avec statut ✅/⚠️/❌ et **preuves** (fichier + endpoint + écran) pour chaque §27-72.
> Source : `docs/À faire.md` §27-72, `docs/MULTI_TENANT_ARCHITECTURE_AUDIT.md`, `docs/tenant.md`
> Date : 2026-09-14

---

## Mapping §27-72 → Existant avec Preuves

| § | Sujet | Statut | Preuves (Backend) | Preuves (Frontend) | Preuves (Mobile) | Porte/Étape |
|---|---|---|---|---|---|---|
| **§27-28** | Tenant Settings / Branding complet | ⚠️ Partiel | `modules/tenants/domain/Tenant.java` (settings JSON basique)<br>`modules/tenants/api/BrandingController.java` (GET/PUT `/api/tenants/{id}/branding`)<br>`V35__church_settings.sql` table `church_settings` | `frontend/pages/admin/BrandingPage.tsx` (basique)<br>`frontend/contexts/SettingsContext.tsx` | `mobile/lib/presentation/screens/tenant/tenant_settings_screen.dart` (incomplet, erreurs) | **G1.2** |
| **§29** | Modules par tenant (TenantFeature) | ⚠️ Partiel | `V135__create_multi_tenant_core_tables.sql` table `tenant_features`<br>`modules/tenants/domain/TenantFeature.java` (entité)<br>`modules/tenants/api/ModuleFeatureController.java` (CRUD basique)<br>`modules/tenants/service/TenantFeatureService.java` (manquant) | `frontend/pages/admin/ModuleFeaturePage.tsx` (basique) | — | **G1.3** |
| **§30-31** | Plans SaaS / Quotas complets | ⚠️ Partiel | `V135__create_multi_tenant_core_tables.sql` tables `saas_plans`, `tenant_subscriptions`<br>`modules/tenants/domain/SaasPlan.java`<br>`modules/tenants/service/SaasPlanService.java` (partiel)<br>`modules/tenants/service/QuotaService.java` (manquant) | `frontend/pages/super-admin/PlansPage.tsx` (basique)<br>`frontend/pages/PricingPage.tsx` (existe) | — | **G1.4** |
| **§32** | TenantContext strict | ⚠️ Partiel | `common/multitenancy/CurrentTenantResolver.java` (résolution serveur)<br>`common/multitenancy/TenantContext.java` (ThreadLocal)<br>`common/multitenancy/TenantInterceptor.java` (extraction JWT) | `frontend/contexts/TenantContext.tsx` (partiellement intégré)<br>`frontend/contexts/AuthContext.tsx` (tenantId absent du User) | `mobile/lib/core/tenant/tenant_config.dart` (basique)<br>`mobile/lib/core/tenant/tenant_session.dart` | **G5.4** |
| **§33-35** | Tenant/Org Switcher complet | ⚠️ Partiel | `modules/tenants/api/TenantController.java` (liste tenants user)<br>`modules/org/api/OrganizationHierarchyController.java` | `frontend/components/layout/TenantSwitcher.tsx` (basique)<br>`frontend/components/layout/OrgSwitcher.tsx` (existant) | `mobile/lib/features/organization/organization_switcher_screen.dart` (existant) | **G5.4 + G5.2** |
| **§37** | Offline Mobile tenant-aware | ⚠️ Partiel | — | — | `mobile/lib/core/tenant/tenant_session.dart` (OK)<br>`mobile/lib/data/local/drift/app_database.dart` (schéma isolé)<br>`mobile/lib/data/local/sync_queue.dart` (partiel) | **G5.7** |
| **§38** | Redis tenant-aware | ⚠️ Partiel | `common/infrastructure/cache/TenantAwareRedisManager.java` (existe)<br>Audit clés : non fait | — | — | **G6.3** |
| **§39** | File Storage isolation complète | ⚠️ Partiel | `modules/files/api/FileController.java` (`/{id}/download` avec check tenant)<br>`modules/files/service/FileStorageService.java` (path `tenants/{tenantId}/...`) | `frontend/components/FileUpload.tsx` (basique)<br>`frontend/components/FileDownload.tsx` (manquant) | `mobile/lib/features/files/file_upload.dart` (basique) | **G1.2 + G3.10/G5.6 + G6.6** |
| **§40** | WebSocket isolation complète | ⚠️ Partiel | `common/websocket/WebSocketConfig.java` (STOMP)<br>`common/websocket/TenantDestinationUserNameProvider.java` (isolation tenant)<br>Tests d'isolation : manquants | `frontend/hooks/useWebSocket.ts` (basique) | `mobile/lib/core/websocket/websocket_service.dart` (basique) | **G2.10** |
| **§41** | Notifications multi-tenant | ⚠️ Partiel | `common/websocket/WebSocketAuthInterceptor.java` (extrait tenantId)<br>`modules/notifications/service/NotificationService.java`<br>Envoi push : non vérifié | `frontend/hooks/useNotifications.ts` | `mobile/lib/core/notifications/notification_service.dart` | **G2.10** |
| **§42** | Audit Log robuste | ⚠️ Partiel | `modules/audit/domain/AuditLog.java` (table + entité)<br>`modules/audit/service/AuditService.java` (basique)<br>Hash chain : partiel<br>Export/Retention : manquant | `frontend/pages/admin/AuditLogPage.tsx` (basique) | — | **G2.9** |
| **§43** | Impersonation Super Admin | ⚠️ Partiel | `modules/admin/api/SuperAdminController.java` (`POST /impersonate`)<br>JWT impersonation : partiel<br>Journal impersonation : manquant | `frontend/pages/super-admin/ImpersonationPage.tsx` (manquante) | — | **G1.9** |
| **§44-45** | IDOR Tests / Security Matrix | ⚠️ Partiel | `backend/src/test/.../MultiTenantSecurityTests.java` (18 tests : isolation, IDOR, cross-tenant, escalation)<br>Security Matrix : non implémentée en prod | — | — | **G1.10 + G6.6** |
| **§46** | AuthorizationService complet | ⚠️ Partiel | `common/security/AuthorizationService.java` (existe, méthodes `can()`)<br>Non utilisé dans tous contrôleurs (ex: `OrganizationManagementController`) | `frontend/guards/RouteGuards.tsx` (`RequirePermission`, `RequireScope` basiques) | — | **G1.11** |
| **§50-51** | Onboarding / Création sous-église | ⚠️ Partiel | `modules/admin/api/InvitationController.java` (`/admin/invitations`)<br>`modules/admin/api/OrganizationHierarchyController.java` (CRUD org nodes)<br>UI wizard : manquante | `frontend/pages/admin/OnboardingWizard.tsx` (manquante) | — | **G1.5** |
| **§52** | Invitations workflow complet | ⚠️ Partiel | `modules/tenants/domain/Invitation.java` (entité + token_hash)<br>`modules/tenants/api/InvitationController.java` (CRUD)<br>Email sending : manquant<br>Page acceptation : manquante | `frontend/pages/AcceptInvitationPage.tsx` (manquante) | — | **G1.6** |
| **§53** | Héritage configs (DEFAULT/INHERITED/OVERRIDDEN) | ⚠️ Partiel | `V135__create_multi_tenant_core_tables.sql` : `organization_nodes.metadata_json` existe<br>Logique résolution héritage : manquante | — | — | **G1.7** |
| **§54** | Ressources GLOBAL/LOCAL | ⚠️ Partiel | `modules/tenants/domain/OrganizationNode.java` (champ `scope`)<br>Logique TENANT_GLOBAL/ORGANIZATION_LOCAL : partielle | — | — | **G1.8** |
| **§55-56** | Frontend Providers/Guards complets | ⚠️ Partiel | — | `frontend/contexts/TenantContext.tsx` (existe)<br>`frontend/contexts/AuthContext.tsx` (existe)<br>`frontend/guards/RouteGuards.tsx` (`RequireScope`, `RequireFeature` basiques)<br>`frontend/hooks/usePermissions.ts` (partiel) | — | **G5.4** |
| **§57-58** | Super Admin / Tenant Admin Web complet | ⚠️ Partiel | — | `frontend/pages/super-admin/` (Dashboard, Tenants, Plans, Users, FeatureFlags, Support, Audit, Settings — CRUD incomplets)<br>`frontend/pages/admin/` (Overview, Organization, Churches, SubChurches, Campuses, Departments, Groups, Members, Users, Roles, Permissions, Courses, Events, Notifications, Branding, Settings, Modules, Subscription, Audit — fonctionnalités limitées) | — | **G5.5** |
| **§59** | Mobile Administration adaptée | ⚠️ Partiel | — | — | `mobile/lib/features/admin/` (écrans existent, permissions UI limitées) | **G5.5** |
| **§60-62** | Search/Export/Delete tenant-aware | ❌ Manquant | `modules/search/` (partiel)<br>`modules/exports/` (basique)<br>Validation scope : manquante | — | — | **G6.1** |
| **§63-68** | IA/Academy/Chat/Payments/Analytics | ❌ Manquant | `modules/ai/`, `modules/academy/`, `modules/chat/`, `modules/payments/`, `modules/analytics/` (architecture prête : tenantId + organizationNodeId) | — | — | **G6.2** |
| **§70-71** | Tests non-régression / Performance | ❌ Manquant | Tests sécurité OK (MultiTenantSecurityTests)<br>Tests non-régression : non automatisés<br>Tests performance : non faits | — | — | **G6.4 + G6.5** |
| **§72** | Documentation | ❌ Manquant | `docs/MULTI_TENANT_ARCHITECTURE.md` manquante<br>`docs/ADMINISTRATION_MODEL.md` manquante<br>`docs/RBAC.md` manquante<br>`docs/TENANT_SECURITY.md` manquante<br>`docs/ORGANIZATION_HIERARCHY.md` manquante<br>`docs/TENANT_ONBOARDING.md` manquante | — | — | **G6.8** |

---

## Résumé par Statut

| Statut | Nombre | Sections |
|---|---|---|
| ✅ Existant complet | 0 | — |
| ⚠️ Partiel | 26 | §27-59, §32-46, §50-59 |
| ❌ Manquant | 6 | §60-62, §63-68, §70-72 |

> **Note** : Aucune section n'est "✅ Existant complet" — tout nécessite du travail pour atteindre le niveau commercialisation v1.

---

## Preuves Détaillées par Composant

### Backend — Infrastructure Multi-tenant (Core)

| Composant | Fichier | Description |
|---|---|---|
| ThreadLocal Tenant | `common/multitenancy/TenantContext.java` | `CURRENT_TENANT: ThreadLocal<UUID>` |
| Hibernate Filter | `common/multitenancy/TenantFilter.java` | `@FilterDef(name="tenantFilter", condition="tenant_id = :tenantId")` |
| Intercepteur JWT→Tenant | `common/multitenancy/TenantInterceptor.java` | `ORDER = HIGHEST_PRECEDENCE`, extrait `tenantId` claim |
| Filtre Hibernate | `common/multitenancy/TenantFilterInterceptor.java` | `ORDER = HIGHEST_PRECEDENCE + 1`, active filter |
| Résolveur Contexte | `common/multitenancy/CurrentTenantResolver.java` | `resolveTenantId()` — source unique de vérité |
| Repo Scopé | `common/multitenancy/TenantAwareRepository.java` | Interface `findByIdAndTenantId(id, tenantId)` |

### Backend — Entités Core Multi-tenant (V135, V136)

| Entité | Table | Fichier | Description |
|---|---|---|---|
| `Tenant` | `tenants` | `modules/tenants/domain/Tenant.java` | Racine, settings JSON, branding JSON |
| `TenantMembership` | `tenant_memberships` | `modules/tenants/domain/TenantMembership.java` | User ↔ Tenant + rôle + scope |
| `OrganizationNode` | `organization_nodes` | `modules/tenants/domain/OrganizationNode.java` | Hiérarchie (path, level, metadata_json) |
| `Role` | `roles` | `modules/tenants/domain/Role.java` | Catalogue rôles (system + custom) |
| `Permission` | `permissions` | `modules/tenants/domain/Permission.java` | Permissions atomiques + scope |
| `RolePermission` | `role_permissions` | — | Liaison rôle ↔ permission |
| `SaasPlan` | `saas_plans` | `modules/tenants/domain/SaasPlan.java` | Plans + quotas + pricing |
| `TenantSubscription` | `tenant_subscriptions` | `modules/tenants/domain/TenantSubscription.java` | Abonnement tenant + plan |
| `TenantFeature` | `tenant_features` | `modules/tenants/domain/TenantFeature.java` | Modules activables par tenant |
| `Invitation` | `invitations` | `modules/tenants/domain/Invitation.java` | Workflow invitation + token |

### Backend — Contrôleurs Multi-tenant

| Contrôleur | Endpoints | Statut |
|---|---|---|
| `TenantController` | `GET/POST/PUT /api/tenants`, `GET /api/tenants/current` | ✅ CRUD basique |
| `BrandingController` | `GET/PUT /api/tenants/{id}/branding` | ⚠️ Partiel |
| `ModuleFeatureController` | `GET/PUT/DELETE /api/admin/tenant-features` | ⚠️ Partiel |
| `SaasPlanController` | `GET/POST/PUT/DELETE /api/admin/saas-plans` | ⚠️ Partiel |
| `InvitationController` | `GET/POST /api/admin/invitations`, `POST /api/invitations/accept` | ⚠️ Partiel |
| `OrganizationHierarchyController` | `GET/POST/PUT/DELETE /api/org/nodes` | ⚠️ Partiel |
| `SuperAdminController` | `GET /api/super-admin/tenants`, `POST /api/super-admin/impersonate` | ⚠️ Partiel |

### Frontend — Multi-tenant

| Composant | Fichier | Description |
|---|---|---|
| TenantContext | `frontend/contexts/TenantContext.tsx` | Provider tenant actif, switch, branding |
| AuthContext | `frontend/contexts/AuthContext.tsx` | User + memberships + permissions |
| RouteGuards | `frontend/guards/RouteGuards.tsx` | `RequireAuth`, `RequireTenant`, `RequirePermission`, `RequireScope`, `RequireFeature` |
| TenantSwitcher | `frontend/components/layout/TenantSwitcher.tsx` | Dropdown sélection tenant |
| OrgSwitcher | `frontend/components/layout/OrgSwitcher.tsx` | Sélecteur organisation/campus/département |

### Mobile — Multi-tenant

| Composant | Fichier | Description |
|---|---|---|
| TenantSession | `mobile/lib/core/tenant/tenant_session.dart` | Session tenant active, cache isolation |
| TenantConfig | `mobile/lib/core/tenant/tenant_config.dart` | Config tenant (branding, features, offline_mode) |
| Drift Schema | `mobile/lib/data/local/drift/app_database.dart` | Tables avec `tenant_id` + isolation |
| Org Switcher | `mobile/lib/features/organization/organization_switcher_screen.dart` | Changement organisation |
| Tenant Selector | `mobile/lib/features/auth/tenant_selector_screen.dart` | Sélection tenant au login |

---

## Tests Existant (Preuves Sécurité)

| Test | Fichier | Couverture |
|---|---|---|
| Cross-tenant isolation | `MultiTenantSecurityTests$TenantIsolationTests` | A→A OK, A→B REFUSED, B→A REFUSED |
| Role isolation | `MultiTenantSecurityTests$RoleIsolationTests` | Member→Admin REFUSED, DeptAdmin→AutreDept REFUSED |
| Resource isolation | `MultiTenantSecurityTests$ResourceIsolationTests` | Members, Families, Reports, Events, Courses, Files, Messages, Notifications, Payments |
| IDOR automation | `MultiTenantSecurityTests$IdorTests` | Tests automatisés sur endpoints critiques |
| Impersonation | `MultiTenantSecurityTests$ImpersonationTests` | ⚠️ Partiels |

---

## Gaps Critiques pour Commercialisation (Bloquants G1)

| Catégorie | Gaps | Priorité |
|---|---|---|
| **SUPER_ADMIN** | Niveau plateforme absent, pas d'impersonation auditée | 🔴 CRITIQUE |
| **User Multi-tenant** | `User.tenant_id` direct, pas de `TenantMembership` exploité | 🔴 CRITIQUE |
| **Default Tenant Fallback** | `TenantAutoSetListener` → écritures cross-tenant silencieuses | 🔴 CRITIQUE |
| **Repo `findById` nu** | 80+ repositories exposent `findById(id)` sans tenant check | 🔴 CRITIQUE |
| **Security Matrix** | Non implémentée en prod (seulement tests) | 🔴 CRITIQUE |
| **AuthorizationService** | Non utilisé partout (controllers contournent) | 🟠 HAUTE |
| **Feature Flags** | Backend enforcement manquant | 🟠 HAUTE |
| **Plans/Quotas** | API CRUD + enforcement incomplets | 🟠 HAUTE |
| **Branding/UI** | Frontend partiel, mobile erreurs | 🟠 HAUTE |
| **Invitations** | Email + page acceptation manquantes | 🟠 HAUTE |
| **Onboarding** | UI wizard manquante | 🟠 HAUTE |
| **Héritage/Global-Local** | Logique manquante | 🟡 MOYENNE |
| **Search/Export/Delete** | Scope validation manquante | 🟡 MOYENNE |
| **Documentation** | 6 docs manquantes | 🟡 MOYENNE |

---

## Prochaines Actions (G1.2 → G1.12)

1. **G1.2** — Compléter Tenant Settings + Branding dynamique (backend + frontend + mobile + propagation WS)
2. **G1.3** — TenantFeature CRUD complet + enforcement backend
3. **G1.4** — Plans SaaS + Quotas CRUD + API + page `/pricing` + géo-pricing
4. **G1.5** — Onboarding wizard + création sous-église/campus
5. **G1.6** — Invitations workflow complet (email + page acceptation + token)
6. **G1.7** — Héritage configs DEFAULT/INHERITED/OVERRIDDEN (ConfigurationResolver)
7. **G1.8** — Ressources GLOBAL/LOCAL (scope + unit_id sur ressources)
8. **G1.9** — Impersonation Super Admin (UI + workflow + journal)
9. **G1.10** — Security Matrix + tests IDOR automatisés (matrice complète)
10. **G1.11** — AuthorizationService sur tous contrôleurs sensibles (0 `if (role==)` ad hoc)
11. **G1.12** — Gate G1 : checklist complète

> **Règle** : Une étape = un commit atomique. DoD global à chaque étape. Protocole STOP-NEED-HELP si ambiguïté.