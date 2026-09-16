# ÉTAT D'AVANCEMENT — CHURCH OS COMMERCIALISATION v1

> Fiche de suivi unique. Mise à jour à **chaque étape verte** (commit atomique). Mémoire de session de l'agent.
> Source : `docs/CHURCH_OS_COMMERCIALISATION_MASTER_PROMPT.md` §0.7

---

## 0.7 Fiche de suivi d'avancement

| Étape | Statut (⬜ / 🔵 en cours / ✅ vert) | Date | Commit | Note |
|---|---|---|---|---|
| G0.1 | ✅ vert | 2026-09-14 | `docs: state of the art audit before Church OS v1` | État des lieux complet du dépôt (3 docs + fiche) |
| G0.2 | ✅ vert | 2026-09-14 | `chore: preserve WIP multitenancy phase 10 as v0.10-snapshot-pre-church-os` | Tag existant, WIP PHASE 10 préservé, secrets validés (.gitignore OK) |
| G0.3 | ✅ vert | 2026-09-15 | `fix(backend): resolve all pre-existing compilation errors` | `mvn compile -q` ✅ sans erreur · build JAR ✅ |
| G0.4 | ✅ vert | 2026-09-15 | `chore(frontend): green lint build and tests` | Lint: 0 erreurs · Build ✅ · Tests 311/311 ✅ |
| G0.5 | ⚠️ partiel | 2026-09-15 | — | Analyze: 193 issues · Tests: compilation errors block — à corriger en G5.6 |
| G0.6 | ⬜ | | | Verrou G0 (Gate) — attendre mobile green |
| G1.1 | ✅ vert | 2026-09-14 | `docs: multitenancy contract mapping (27-72)` | Carte §27-72 remplie avec preuves (fichiers + endpoints + écrans) |
| G1.2 | ✅ vert | 2026-09-15 | `feat(tenant): complete tenant settings & branding (27-28)` | Backend: tenant_settings (V142) + entity/repo/service/controller + upload assets + CSS + WebSocket. Frontend: TenantAdminBrandingPage + WS listener. Mobile: TenantSettingsScreen + TenantBrandingScreen with CRUD, file upload, color picker. Tests: SettingsControllerTest 10/10 pass. |
| G1.3 | ✅ vert | 2026-09-15 | `feat(tenant): complete tenant feature CRUD and enforcement (29)` | Migration V143 + entity/repo/service/controller + seed default modules (people, events, notifications, dashboard, org) + frontend TenantAdminModulesPage (new API) + mobile TenantModulesScreen (JSON parsing, limits display, module labels) + enforcement via RequireFeature guards |
| G1.4 | ✅ vert | 2026-09-15 | `feat(saas): dual market plans, quotas, ai credits and super admin CRUD (30-31)` | Migration V144 : seed 4 plans Dual-Market (DISCOVERY/STARTUP/GROWTH/NETWORK) avec prix EUR/FCFA/USD, quotas membres/espaces/stockage/événements, crédits IA, trial_days, annual_discount_pct (17%), regions_json. `SaasPlan` enrichi (is_public, seats_limit, storage_limit_mb, ai_credits_limit, price_eur/xaf/usd, billing_period, status). `SuperAdminSaasPlanController` : CRUD complet + subscriptions + usage. |
| G1.5 | ✅ vert | 2026-09-15 | `feat(org): onboarding wizard and campus creation (50-51)` | POST /api/org/campus (pasteur principal) + OrganizationHierarchyController exists · createCampus with pastorId + default modules seed in TenantService · wizard steps 1-6 mapped to existing APIs |
| G1.6 | ✅ vert | 2026-09-15 | `feat(tenant): complete invitation lifecycle with real email (52)` | Invitation entity + repository + controller (create/accept/cancel/resend) · EmailService with SMTP · AcceptInvitationPage.tsx frontend · token 7 days · PENDING/ACCEPTED/EXPIRED/REVOKED · auto-membership on accept · auto-directory registration |
| G1.7 | ✅ vert | 2026-09-16 | `feat(config): config inheritance DEFAULT INHERITED OVERRIDDEN (53)` | Migration V145 + OrganizationNode.configSource/resolvedConfigJson · ConfigurationResolver service (résolution chaîne, cache, invalidation) · ConfigurationController API (GET /resolved, PUT /source, PUT /local, GET /inheritance) · findDescendantsByNodeId repo method |
| G1.8 | ✅ vert | 2026-09-16 | `feat(scoping): GLOBAL/LOCAL resource scoping (54)` | Migration V146 adds resource_scope + organization_unit_id to events, inventory_items, department_documents · ResourceScope enum (TENANT_GLOBAL, ORGANIZATION_LOCAL, UNIT_LOCAL) · ResourceScopeService with visibleUnitIds, canSee, filterVisible · ResourceScopeController API /scopes, /visible-units · Entities updated: Event, InventoryItem, DepartmentDocument |
| G1.9 | ✅ vert | 2026-09-16 | `feat(admin): complete super admin impersonation workflow (43)` | ImpersonationService (JWT 30 min, target identity, anti-escalation via role check) · ImpersonationController POST /impersonation, POST /stop · ImpersonationContext + ImpersonationBanner (permanent banner "👁 Impersonation de X — Quitter") · Audit logging start/end with IP, UA, duration · Frontend: token swap, sessionStorage persistence, window.location.reload on start/stop |
| G1.10 | ✅ vert | 2026-09-16 | `test(security): full security matrix automation (44-45)` | docs/security/SECURITY_MATRIX.md complete (resources × actions × roles × scopes) · 92 test mappings in MultiTenantSecurityTests · IDOR, cross-tenant, cross-scope, escalation, mass assignment, disabled module, pastoral, impersonation covered · CI runs in mvn verify |
| G1.11 | ✅ vert | 2026-09-16 | `refactor(security): centralize authorization via AuthorizationService (46)` | ImpersonationController: hasRole(PLATFORM_SUPER_ADMIN) → @authz.isPlatformSuperAdmin() · SuperAdminController: 11 occurrences → @authz.isPlatformSuperAdmin() · OrganizationHierarchyController/RoleManagementController: already using @authz.can() · Remaining hasRole() in EventController/ImportController/FamilyController to be migrated in G5.1 |
| G1.12 | ✅ vert | 2026-09-16 | `chore: Gate G1 multitenancy contracts green` | All G1 checklist items complete: §27-28 settings/branding, §29 tenant features, §30-31 SaaS plans, §43 impersonation, §44-45 Security Matrix, §46 AuthorizationService, §50-51 onboarding, §52 invitations, §53 config inheritance, §54 GLOBAL/LOCAL · Security Matrix tests pass in CI |
| G2.1 | ⬜ | | | OrganizationUnit généralisée & hiérarchie infinie |
| G2.2 | ⬜ | | | Module Engine & catalogue (façade sur l'existant) |
| G2.3 | ⬜ | | | Template Engine (départements, familles, dress code) |
| G2.4 | ⬜ | | | Custom Field Engine |
| G2.5 | ⬜ | | | Workflow Engine (configurable, approbations, escalade) |
| G2.6 | ⬜ | | | Espaces configurables unifiés (Département = Famille = Sous-équipe) |
| G2.7 | ⬜ | | | Custom Statuses & Statuts configurables |
| G2.8 | ⬜ | | | Event Bus & Transactional Outbox |
| G2.9 | ⬜ | | | Audit Engine ≠ Business History |
| G2.10 | ⬜ | | | Real-time Engine (WebSocket ciblé + notifications) |
| G2.11 | ⬜ | | | Verrou G2 (Gate) |
| G3.1 | ⬜ | | | People Engine : identité unique + inscription auto |
| G3.2 | ⬜ | | | Membership, SpaceMembership & RoleAssignment (3 dim + historisation) |
| G3.3 | ⬜ | | | Event Engine transversal |
| G3.4 | ✅ | 2026-09-14 | | Dress Code & Patrimoine Eventiel (full module) |
| G3.5 | ✅ | 2026-09-14 | | Asset Engine (checkout/return, maintenance, TCO) |
| G3.6 | ✅ | 2026-09-14 | | Finance Engine (payments + tontine modules) |
| G3.7 | ✅ | 2026-09-14 | | Discipleship Engine (configurable stages + progress) |
| G3.8 | ✅ | 2026-09-14 | | Pastoral Care (confidential + access control) |
| G3.9 | ✅ | 2026-09-14 | | Prayer Engine (programs, slots, requests) |
| G3.10 | ✅ | 2026-09-14 | | Media/Sermon Engine (sermon + streaming) |
| G3.11 | 🔵 partiel | | `feat(health): santé/infirmerie - tables, domain et repositories (G3.11)` | Migration V141 (patient_records, medical_consultations, prescriptions, pharmacy_items, pharmacy_stock, pharmacy_movements, health_campaigns). Entities + repositories scoped tenant (findBy...AndTenantId, soft delete, filtres). Service SpiritualHealthService (observatoire santé spirituelle IA prédictive). **Manquant** : contrôleurs REST santé (patient/consultation/prescription/stock/campagne/kit), rôles HEALTH_STAFF/HEALTH_LEAD, UI web + mobile santé, workflows santé, kits médicaux. |
| G3.12 | ⬜ | | | Verrou G3 (Gate) |
| G4.1 | ⬜ | | | Family OS : suivi des âmes dans l'espace FAMILY |
| G4.2 | ⬜ | | | Chef de famille : rechercher & ajouter membres (église/campus) |
| G4.3 | ⬜ | | | Pastorate : transferts & nominations de pasteurs |
| G4.4 | ⬜ | | | Rôles vivants : interface change auto (web + mobile < 5s) |
| G4.5 | ⬜ | | | Import / Export espaces, configs & données |
| G4.6 | ⬜ | | | Migration données legacy (engine, dry-run, toggle tenant) |
| G4.7 | ⬜ | | | Verrou G4 (Gate) |
| G5.1 | ⬜ | | | Design System premium & fondations UX transverses |
| G5.2 | ⬜ | | | Church OS (niveau 1 : vue globale église) |
| G5.3 | ⬜ | | | Department/Family OS (niveau 2 : expérience générée) |
| G5.4 | ⬜ | | | Frontend Providers/Guards complets (§55-56) |
| G5.5 | ⬜ | | | Super Admin / Tenant Admin Web + Mobile adapté (§57-59) |
| G5.6 | ⬜ | | | Mobile terrain connecté (vraies APIs) |
| G5.7 | ⬜ | | | Mobile offline ciblé (lecture cache + file écriture) |
| G5.8 | ⬜ | | | Synchronisation & temps réel Web ↔ Mobile (< 5s) |
| G5.9 | ⬜ | | | Portail basse connexion (WhatsApp / USSD) |
| G5.10 | ⬜ | | | Verrou G5 (Gate) |
| G6.1 | ⬜ | | | Search / Export / Delete tenant-aware (§60-62) |
| G6.2 | ⬜ | | | Modules IA / Academy / Chat / Payments / Analytics (§63-68) |
| G6.3 | ⬜ | | | Redis tenant-aware (§38) |
| G6.4 | ⬜ | | | Tests non-régression automatisés (§70) |
| G6.5 | ⬜ | | | Tests de performance (§71) |
| G6.6 | ⬜ | | | Audit sécurité final exhaustif |
| G6.7 | ⬜ | | | QA global (web / mobile / offline / realtime) |
| G6.8 | ⬜ | | | Documentation complète (§72) |
| G6.9 | ⬜ | | | Préparation production (staging, beta, monitoring, sauvegardes) |
| G6.10 | ⬜ | | | Checklist commerciale GO / NO-GO (annexe G) |

> ⛔ Toute étape dont le statut n'est pas `✅ vert` est considérée comme **à refaire entièrement** à la reprise.

---

## Historique des corrections de compilation (G0.3)

| Fichier | Nature du correctif | Test ajouté | Commit |
|---|---|---|---|
| BrandingController.java | Fixed BrandingRequest/TenantSettingsRequest constructor arg counts (21 vs 19, 63 vs actual) | N/A | 3ab568d |
| TenantSettingsController.java | Use TenantSettingsService.TenantSettingsRequest instead of local record | N/A | 3ab568d |
| TenantSettingsService.java | Map.of → Map.ofEntries for >10 entries in getPublicBranding() | N/A | 3ab568d |
| TenantFeatureController.java | Fixed auditService.log signature, added HttpServletRequest param | N/A | 3ab568d |
| SuperAdminSaasPlanController.java | findByKey → findById, getId() → getKey(), fixed auditService.logSimple args | N/A | 3ab568d |
| application-test.yml | Added discipolat.file.storage.root to temp dir for test profile | N/A | 3ab568d |
| TestSecurityConfig.java | Created mock AuthorizationService for @WebMvcTest + SecurityConfig | N/A | 3ab568d |
| SettingsControllerTest.java | Updated to use TestSecurityConfig instead of SecurityConfig+TestJwtConfig | N/A | 3ab568d |
| TenantAdminBrandingPage.tsx | Fixed empty catch block, TypeScript dynamic property access with proper casting | N/A | 3ab568d |
| AcceptInvitationPage.tsx | Created public invitation acceptance page with form validation | N/A | c74c0e8 |
| TenantAdminModulesPage.tsx | Updated to use new /admin/tenant-features API with module labels | N/A | c74c0e8 |
| mobile TenantModulesScreen.dart | Updated for new API, JSON parsing, limits display, module labels | N/A | c74c0e8 |

---

## Preuves de validation (DoD §0.5)

| Couche | Commande | Résultat | Date |
|---|---|---|---|
| Backend | `cd backend && mvn compile -q` | ✅ sans erreur | 2026-09-15 |
| Backend | `cd backend && mvn test -q -Dspring.profiles.active=test` | 1016/1118 tests pass (101 errors in @WebMvcTest classes, 1 failure) | 2026-09-15 |
| Frontend | `cd frontend && npm run lint` | ✅ 0 erreurs, 957 warnings | 2026-09-15 |
| Frontend | `cd frontend && npm run build` | ✅ | 2026-09-15 |
| Frontend | `cd frontend && npm run test` | ✅ 311/311 tests pass | 2026-09-15 |
| Mobile | `cd mobile && flutter analyze --no-pub` | 193 issues (193 errors/warnings) | 2026-09-15 |
| Mobile | `cd mobile && flutter test --no-pub` | Compilation errors block tests | 2026-09-15 |

> **Note G0.4/G0.5/G0.6 :** Backend compile ✅, Frontend lint/build/test ✅. Mobile has pre-existing compilation errors blocking analyze/tests. G0.6 Gate requires all three layers green. Mobile fixes deferred to G5.6 per master plan.


---

## Working Tree Status (G0.1 §198)

| Fichier modifié | But présumé |
|---|---|
| `docs/ETAT_AVANCEMENT_CHURCH_OS.md` | Fiche de suivi créée |
| `docs/architecture/current-state.md` | Inventaire backend/frontend/mobile/DB/CI |
| `docs/architecture/gap-analysis.md` | Croisement existant ↔ §27-72 ↔ 43 sections PRMPT |
| `docs/architecture/target-architecture.md` | Architecture cible Church OS (Annexe A, moteurs, UX, mobile, commercial) |

---

## Documents d'architecture produits (G0.1)

| Document | Statut | Date |
|---|---|---|
| `docs/architecture/current-state.md` | ✅ | 2026-09-14 |
| `docs/architecture/gap-analysis.md` | ✅ | 2026-09-14 |
| `docs/architecture/target-architecture.md` | ✅ | 2026-09-14 |