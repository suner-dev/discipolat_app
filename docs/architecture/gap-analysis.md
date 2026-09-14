# GAP ANALYSIS — Existant vs Church OS v1 (G0.1)

> Croisement : Existant (audit G0.1) ↔ Annexe C (§27-72 du `À faire.md`) ↔ Annexe D (43 sections PRMPT)
> Légende : ✅ Existant complet / ⚠️ Partiel / ❌ Manquant
> Date : 2026-09-14

---

## 1. Mapping §27-72 (Annexe C) → Existant

| § | Sujet | Existant | Statut | Preuves (fichiers) | Porte/Étape cible |
|---|---|---|---|---|---|
| **§27-28** | Tenant Settings / Branding complet | `church_settings` table (V35), `BrandingController` basique | ⚠️ Partiel | `modules/tenants/api/BrandingController.java`, `V35__church_settings.sql`, `frontend/pages/admin/BrandingPage.tsx` | **G1.2** |
| **§29** | Modules par tenant (TenantFeature) | Table `tenant_features` créée (V135), `ModuleFeatureController` existe | ⚠️ Partiel | `V135__create_multi_tenant_core_tables.sql`, `modules/tenants/api/ModuleFeatureController.java`, `TenantFeatureService` manquant | **G1.3** |
| **§30-31** | Plans SaaS / Quotas complets | Tables `saas_plans`, `tenant_subscriptions` (V135), `SaasPlanService` partiel | ⚠️ Partiel | `V135__create_multi_tenant_core_tables.sql`, `modules/tenants/domain/SaasPlan.java`, `SaasPlanService.java` (incomplet) | **G1.4** |
| **§32** | TenantContext strict | Backend OK (`CurrentTenantResolver`), Frontend `TenantContext.tsx` partiel | ⚠️ Partiel | `common/multitenancy/CurrentTenantResolver.java`, `frontend/contexts/TenantContext.tsx` | **G5.4** |
| **§33-35** | Tenant/Org Switcher complet | Backend OK, Frontend `TenantSwitcher.tsx` basique, Mobile OK | ⚠️ Partiel | `frontend/components/layout/TenantSwitcher.tsx`, `mobile/lib/features/organization/` | **G5.4 + G5.2** |
| **§37** | Offline Mobile tenant-aware | `tenant_session.dart` OK, cache Drift isolé, sync incomplet | ⚠️ Partiel | `mobile/lib/core/tenant/tenant_session.dart`, `mobile/lib/data/local/drift/` | **G5.7** |
| **§38** | Redis tenant-aware | `TenantAwareRedisManager` existe, audit clés non fait | ⚠️ Partiel | `common/infrastructure/cache/TenantAwareRedisManager.java` | **G6.3** |
| **§39** | File Storage isolation complète | Backend OK (`/{id}/download` avec tenant), Frontend upload/download incomplet | ⚠️ Partiel | `modules/files/api/FileController.java`, `frontend/components/FileUpload.tsx` (basique) | **G1.2 + G3.10/G5.6 + G6.6** |
| **§40** | WebSocket isolation complète | `WebSocketConfig` + `TenantDestinationUserNameProvider`, tests manquants | ⚠️ Partiel | `common/websocket/WebSocketConfig.java`, `TenantDestinationUserNameProvider.java` | **G2.10** |
| **§41** | Notifications multi-tenant | `WebSocketAuthInterceptor` extrait tenantId, envoi push non vérifié | ⚠️ Partiel | `common/websocket/WebSocketAuthInterceptor.java`, `modules/notifications/` | **G2.10** |
| **§42** | Audit Log robuste | `AuditLog` table existe, `AuditService` basique, hash chain partiel | ⚠️ Partiel | `modules/audit/domain/AuditLog.java`, `AuditService.java` | **G2.9** |
| **§43** | Impersonation Super Admin | Backend `SuperAdminController` a `/impersonate`, UI manquante | ⚠️ Partiel | `modules/admin/api/SuperAdminController.java` (endpoint impersonate) | **G1.9** |
| **§44-45** | IDOR Tests / Security Matrix | `MultiTenantSecurityTests` (18 tests), Security Matrix non implémentée en prod | ⚠️ Partiel | `backend/src/test/java/.../MultiTenantSecurityTests.java` | **G1.10 + G6.6** |
| **§46** | AuthorizationService complet | `AuthorizationService` existe, non utilisé dans tous contrôleurs | ⚠️ Partiel | `common/security/AuthorizationService.java` | **G1.11** |
| **§50-51** | Onboarding / Création sous-église | API `/admin/invitations` + `OrganizationHierarchyController`, UI wizard manquante | ⚠️ Partiel | `modules/admin/api/OrganizationHierarchyController.java`, `modules/admin/api/InvitationController.java` | **G1.5** |
| **§52** | Invitations workflow complet | Backend OK, email sending manquant, frontend accept page manquante | ⚠️ Partiel | `modules/tenants/api/InvitationController.java`, `modules/tenants/domain/Invitation.java` | **G1.6** |
| **§53** | Héritage configs (DEFAULT/INHERITED/OVERRIDDEN) | `OrganizationNode.metadata_json` existe, logique héritage manquante | ⚠️ Partiel | `V135__create_multi_tenant_core_tables.sql` (organization_nodes.metadata_json) | **G1.7** |
| **§54** | Ressources GLOBAL/LOCAL | `OrganizationNode` + scope, logique TENANT_GLOBAL/ORGANIZATION_LOCAL partielle | ⚠️ Partiel | `modules/tenants/domain/OrganizationNode.java` | **G1.8** |
| **§55-56** | Frontend Providers/Guards complets | `TenantContext.tsx` + `RouteGuards.tsx` existent, RequireScope/RequireFeature basiques | ⚠️ Partiel | `frontend/contexts/TenantContext.tsx`, `frontend/guards/RouteGuards.tsx` | **G5.4** |
| **§57-58** | Super Admin / Tenant Admin Web complet | Pages créées, fonctionnalités limitées (CRUD incomplets) | ⚠️ Partiel | `frontend/pages/admin/`, `frontend/pages/super-admin/` | **G5.5** |
| **§59** | Mobile Administration adaptée | Pages existent, permissions UI limitées | ⚠️ Partiel | `mobile/lib/features/admin/` | **G5.5** |
| **§60-62** | Search/Export/Delete tenant-aware | Backend partiel, validation scope manquante | ⚠️ Partiel | `modules/search/`, `modules/exports/` | **G6.1** |
| **§63-68** | IA/Academy/Chat/Payments/Analytics | Architecture prête (tenantId + organizationNodeId), implémentation manquante | ❌ Manquant | `modules/ai/`, `modules/academy/`, `modules/chat/`, `modules/payments/`, `modules/analytics/` | **G6.2** |
| **§70-71** | Tests non-régression / Performance | Tests sécurité OK, tests non-régression/perf manquants | ❌ Manquant | — | **G6.4 + G6.5** |
| **§72** | Documentation | `docs/MULTI_TENANT_ARCHITECTURE.md` manquante | ❌ Manquant | — | **G6.8** |

---

## 2. Mapping 43 Sections PRMPT (Annexe D) → Existant

| Section PRMPT | Titre | Existant | Statut | Porte/Étape |
|---|---|---|---|---|
| **1** | Rôle de l'agent | Défini dans master prompt | ✅ | §0.1 |
| **2** | Vision Church OS | Défini | ✅ | §0.1 |
| **3** | Règle absolue | Défini | ✅ | §0.3 |
| **4** | Architecture multi-tenant | Partielle (V70, V135, V136) | ⚠️ | **G1** (toutes) |
| **5** | Organization Engine | `OrganizationNode` (V135), `OrganizationHierarchyController` | ⚠️ | **G2.1** |
| **6** | Department Engine | `departments` module, mais pas "espaces configurables" | ⚠️ | **G2.6** |
| **7** | Module Engine | `ModuleFeatureController`, `tenant_features` table | ⚠️ | **G2.2** |
| **8** | Template Engine | `space_template` absent, `department_template` non | ❌ | **G2.3** |
| **9** | Custom Field Engine | `customfields` module + tables (V38) | ⚠️ | **G2.4** |
| **10** | Workflow Engine | `workflow` module + tables (V98), basique | ⚠️ | **G2.5** |
| **11** | People Engine | `souls` + `users` + `families` + `departments` | ⚠️ | **G3.1** |
| **12** | Role Engine (RBAC+scope) | 6 rôles fixes, `AuthorizationService` partiel | ⚠️ | **G3.2 + G1.11 + G4.4** |
| **13** | Event Engine | `events` module complet | ⚠️ | **G3.3** |
| **14** | Asset Engine | `inventory` module basique | ⚠️ | **G3.5** |
| **15** | Maintenance Engine | Dans `inventory`/`workflow` | ⚠️ | **G3.5** |
| **16** | Finance Engine | `finances` module + tables (V68) | ⚠️ | **G3.6** |
| **17** | Discipleship Engine | `discipleshipPath`, `spiritualJourney`, `mentoring` | ⚠️ | **G3.7** |
| **18** | Pastoral Care | Dans `souls` + `visits`, pas séparé sécurisé | ⚠️ | **G3.8** |
| **19** | Prayer Engine | `prayers` + `prayerJournal` modules | ⚠️ | **G3.9** |
| **20** | Media Engine | `sermon` + `files` + `documents` | ⚠️ | **G3.10** |
| **21** | Audit Engine | `audit` module + `AuditLog` table | ⚠️ | **G2.9** |
| **22** | Business History | Partiel via `AuditLog` + quelques history tables | ⚠️ | **G2.9** |
| **23** | Event Bus | Pas d'outbox transactionnel, pas de dispatcher | ❌ | **G2.8** |
| **24** | Real-Time | `WebSocketConfig` + STOMP, isolation partielle | ⚠️ | **G2.10** |
| **25** | Frontend (SaaS premium) | React 19/Tailwind, design system incomplet | ⚠️ | **G5.1-G5.5** |
| **26** | Department as an App | Pas de génération dynamique `/app/spaces/:id` | ❌ | **G5.3** |
| **27** | Mobile (vraies APIs) | Flutter + Dio, quelques mocks résiduels | ⚠️ | **G5.6** |
| **28** | Offline | Drift + `tenant_session`, sync incomplet | ⚠️ | **G5.7** |
| **29** | UX Exceptionnelle | Questionnaire §0.9 non appliqué systématiquement | ⚠️ | **G5.1 + §0.9** |
| **30** | Accessibilité | Non audité (WCAG AA) | ❌ | **G5.1** |
| **31** | Performance | Non testé (1K-10K tenants) | ❌ | **G6.5** |
| **32** | Database | 136 migrations, schémas Annexe A partiels | ⚠️ | **Annexe A + workflow** |
| **33** | Data Integrity | Soft delete partiel, pas de hash chain audit | ⚠️ | **Annexe A + G6.1** |
| **34** | API | OpenAPI/Swagger partiel | ⚠️ | **Workflow étape 6** |
| **35** | Security | `MultiTenantSecurityTests`, manques critiques | ⚠️ | **G1.10-G1.11 + G6.6** |
| **36** | Tests | Unitaires + 18 tests sécurité, pas E2E critiques | ⚠️ | **G6.4 + workflow** |
| **37** | Phases 0→20 | Mapping fait dans Annexe D | ✅ | **G0→G6** |
| **38** | Règle de livraison | Workflow 17 actions §0.4, DoD §0.5 | ✅ | **§0.4-0.5** |
| **39** | Interdictions | Listées §0.3 | ✅ | **§0.3** |
| **40** | Workflow de l'agent | Défini §0.4 | ✅ | **§0.4** |
| **41** | Git | Conventions §0.6, commits atomiques | ✅ | **§0.6** |
| **42** | Definition of Done | Globale §0.5 | ✅ | **§0.5** |
| **43** | Règle finale (ONE CORE) | Architecture visée, pas encore réalisée | ❌ | **G2-G4 + Annexe G** |

---

## 3. Mapping Exigences Utilisateur (1:06 PM) → Existant

| Exigence | Description | Existant | Statut | Porte/Étape |
|---|---|---|---|---|
| **Dress Code & patrimoine événementiel** | Dress codes par événement/groupe, archives, répertoires musicaux, debriefs | ❌ Absent | ❌ | **G3.4** |
| **FAMILY = espace configurable premier rang** | `families` module existe mais pas unifié avec `departments` | ⚠️ Partiel | ⚠️ | **G2.6 + G4.1** |
| **Inscription automatique au répertoire** | Self-signup → répertoire + liste "sans espace" | ❌ Absent | ❌ | **G3.1** |
| **Rôles vivants (changement interface temps réel)** | WebSocket + PermissionResolver, non implémenté | ❌ Absent | ❌ | **G4.4** |
| **Pasteur principal → transfert/nomination pasteurs campus** | `OrganizationHierarchyController` partiel, pas de workflow | ⚠️ Partiel | ⚠️ | **G4.3** |
| **Tout modulable par responsable (pages, boutons, couleurs, sous-équipes)** | `customfields` + `workflow` + `department_settings`, pas générateur UI | ⚠️ Partiel | ⚠️ | **G2.6 + G5.3** |
| **Import/Export** | `imports`/`exports` modules basiques, pas format canonique | ⚠️ Partiel | ⚠️ | **G4.5** |

---

## 4. Résumé des Gaps Critiques (Bloquants Commercialisation)

| Catégorie | Gaps Majeurs | Priorité |
|---|---|---|
| **Multi-tenant Core** | SUPER_ADMIN absent, User≠Multi-tenant, Default Tenant Fallback, Repos non scopés | 🔴 CRITIQUE |
| **SaaS Foundation** | Plans/Quotas incomplets, Feature Flags partiels, Billing absent | 🔴 CRITIQUE |
| **Configuration Engines** | Template Engine absent, Workflow Engine basique, Custom Status absent | 🟠 HAUTE |
| **Domain Engines** | Dress Code absent, Santé/Infirmerie absent, Asset/Finance central absent | 🟠 HAUTE |
| **Family OS / Rôles Vivants** | Family ≠ Department unifié, Rôles vivants absents, Pastorate workflow absent | 🟠 HAUTE |
| **UX 2 Niveaux** | Church OS shell absent, Department/Family OS généré absent, Design System incomplet | 🟠 HAUTE |
| **Mobile/Offline/Realtime** | Sync <5s non prouvé, Offline ciblé incomplet, WhatsApp/USSD absent | 🟠 HAUTE |
| **Sécurité/Qualité** | Security Matrix absente, Perf non testée, Non-régression absente, Docs manquantes | 🟠 HAUTE |

---

## 5. Preuves de l'Existant (Références Fichiers)

### Backend Core Multi-tenant
- `common/multitenancy/TenantContext.java` — ThreadLocal
- `common/multitenancy/TenantFilter.java` — Hibernate Filter
- `common/multitenancy/CurrentTenantResolver.java` — Résolution serveur
- `common/multitenancy/TenantAwareRepository.java` — Interface repos scopés
- `common/security/AuthorizationService.java` — Service central (partiel)
- `modules/tenants/domain/Tenant.java` — Entité Tenant
- `modules/tenants/domain/TenantMembership.java` — Membership (V135)
- `modules/tenants/domain/OrganizationNode.java` — Hiérarchie (V135)
- `modules/tenants/domain/SaasPlan.java` — Plans (V135)
- `modules/tenants/domain/TenantFeature.java` — Features tenant (V135)

### Migrations Critiques
- `V70__add_multitenancy.sql` — tenant_id sur 85+ tables
- `V135__create_multi_tenant_core_tables.sql` — Core tables MT (20 KB)
- `V136__add_membership_scope_and_role_fk.sql` — Scope sur memberships

### Frontend Multi-tenant
- `frontend/contexts/TenantContext.tsx` — Contexte tenant (partiel)
- `frontend/contexts/AuthContext.tsx` — Auth + membership
- `frontend/guards/RouteGuards.tsx` — Guards (basiques)
- `frontend/components/layout/TenantSwitcher.tsx` — Switcher (basique)
- `frontend/components/layout/OrgSwitcher.tsx` — Org switcher

### Mobile Multi-tenant
- `mobile/lib/core/tenant/tenant_session.dart` — Session tenant
- `mobile/lib/core/tenant/tenant_config.dart` — Config tenant
- `mobile/lib/data/local/drift/app_database.dart` — Schéma Drift isolé

### Tests Sécurité
- `backend/src/test/java/.../MultiTenantSecurityTests.java` — 18 tests (isolation, IDOR, cross-tenant, escalation)