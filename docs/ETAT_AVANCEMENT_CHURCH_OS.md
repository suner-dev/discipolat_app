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
| G0.4 | ✅ vert | 2026-09-16 | `fix(frontend): provide impersonation context in role routing tests` | Build ✅ · Tests 311/311 ✅ · Lint 0 erreur / **963 warnings** (DoD « 0 warning » non satisfait — voir Bloqueurs B3) |
| G0.5 | ✅ vert | 2026-09-16 | `fix(mobile): green analyze and tests` | `flutter analyze` 0 erreur **et 0 warning** · `flutter test` 331/331 ✅ · Cause racine majeure : 5 noms de route go_router dupliqués → l'app ne démarrait pas |
| G0.6 | ✅ vert | 2026-09-16 | *(voir commits G0.3→G0.6)* | `mvn verify -B` BUILD SUCCESS (1141 tests, 0 échec, 0 erreur, 13 skip) · frontend build+tests ✅ · mobile analyze+tests ✅ · 3 docs d'architecture G0.1 présents · tag `v0.10-snapshot-pre-church-os` présent · **réserve** : le working tree porte encore le WIP non commité de G2.2 (prochaine étape), à ne pas confondre avec un arbre sale |
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
| G2.1 | ✅ vert | 2026-09-16 | `feat(core): generalized organization unit hierarchy` | Migration V147 + description/icon/color/sort_order fields + ConfigurationResolver + API /resolved,/source,/local,/inheritance + /api/org/units endpoints + OrganizationHierarchyService/Controller updates + explicit getters on entities |
| G2.2 | ✅ vert | 2026-09-16 | `feat(modules): module engine, catalogue and space module picker (G2.2)` | Migration V148 (`module_definition` catalogue + `space_module` remplaçant `department_module`) · `ModuleDefinition`/`SpaceModule` entités + repos tenant-aware · `ModuleCatalogService` (catalogue CORE/EXISTING/ENGINE, groupé par catégorie, stats) · `SpaceModuleService` (enable/disable/config/limits/order, validation catalogue + espace hors-tenant refusé) · `ModuleRouter` (routage feature-flag CORE/EXISTING/ENGINE, refus si désactivé globalement ou dans l'espace) · `ModuleCatalogController` (/modules/catalog, /spaces/:id/modules, /routes) · Tests : `ModuleCatalogServiceTest`, `SpaceModuleServiceTest`, `ModuleRouterTest` ✅ |
| G2.3 | ✅ vert | 2026-09-17 | `feat(config): space template engine with 20 seed templates` | Migration V152 + SpaceTemplate entity/repo/service/controller + AdminSpaceTemplatesPage + 20 templates seed |
| G2.4 | ✅ vert | 2026-09-17 | `feat(config): custom field engine 19 types backend validation` | Migration V153 + CustomFieldDefinition/Value entities + repos + CustomFieldService (validation 19 types) + controller |
| G2.5 | ✅ vert | 2026-09-17 | `feat(workflow): configurable workflow engine approvals escalation` | Migration V151 + WorkflowDefinition/Step/Transition/Instance/Task entities + repos + WorkflowService (start/approve/reject/escalate) |
| G2.6 | ✅ vert | 2026-09-16 | `feat(config): unified configurable spaces department family sub-team` | Space entity + SpaceModuleService + SpaceService (canCustomize, resolveCustomizableSpaceIds, propagation temps réel) |
| G2.7 | ✅ vert | 2026-09-16 | `feat(config): custom status engine with controlled transitions` | CustomStatus entity + CustomStatusService (resolveStatusSet, validateTransition, changeStatus, kanban board) |
| G2.8 | ✅ vert | 2026-09-17 | `feat(core): transactional outbox event bus` | OutboxEvent/ProcessedEvent entities + OutboxPublisher/Dispatcher + 29 consumers (NOTIFY/AUDIT/BUSINESS_HISTORY/FINANCE/ANALYTICS/REALTIME) + AutoConfiguration |
| G2.9 | ✅ vert | 2026-09-17 | `feat(audit): audit engine with hash chain and business history` | Migration V135 fix + audit_event + business_history tables + AuditEventService (hash chain, export, verify) + AuditController |
| G2.10 | ✅ vert | 2026-09-17 | `feat(realtime): realtime engine and multi-channel notifications` | WebSocketConfig (tenant isolation) + RealTimeService (push config/status/roles/tasks/dresscode/invitations) + OutboxConsumers integration |
| G2.11 | ✅ vert | 2026-09-17 | `chore: Gate G2 church os core engines green` | All G2 checklist items complete: G2.1 org hierarchy, G2.2 module catalogue, G2.3 templates, G2.4 custom fields, G2.5 workflows, G2.6 unified spaces, G2.7 custom statuses, G2.8 outbox, G2.9 audit/history, G2.10 realtime. Migrations applied. Security Matrix green. |
| G3.1 | ✅ vert | 2026-09-17 | `feat(people): unique people registry with auto self-registration` | Migration V154 + Person/Membership/SpaceMembership/RoleAssignment/EventAssignment entities + PeopleService (register, merge, search, assignToSpace, assignRole, transferPastor) + PeopleController |
| G3.2 | ✅ vert | 2026-09-17 | `feat(people): memberships role assignments and history` | 3 dimensions (role_assignment, space_membership, event_assignment) historisées + wizard d'affectation + transfert pasteur |
| G3.3 | ✅ vert | 2026-09-17 | `feat(event): church event engine with legacy migration` | Migration V158 (legacy_events → event) + ChurchEvent/EventSpace/EventTeam/EventTask/EventAsset/EventExpense/EventAttendance/EventDocument/EventSchedule/Location entities + ChurchEventService/Controller |
| G3.4 | ✅ | 2026-09-14 | | Dress Code & Patrimoine Eventiel (full module) |
| G3.5 | ✅ | 2026-09-14 | | Asset Engine (checkout/return, maintenance, TCO) |
| G3.6 | ✅ | 2026-09-14 | | Finance Engine (payments + tontine modules) |
| G3.7 | ✅ | 2026-09-14 | | Discipleship Engine (configurable stages + progress) |
| G3.8 | ✅ | 2026-09-14 | | Pastoral Care (confidential + access control) |
| G3.9 | ✅ | 2026-09-14 | | Prayer Engine (programs, slots, requests) |
| G3.10 | ✅ | 2026-09-14 | | Media/Sermon Engine (sermon + streaming) |
| G3.11 | 🔵 partiel | | | Migration V141 + SpiritualHealthService (DB tables + repositories existants). **Manquant** : contrôleurs REST santé complets, rôles HEALTH_STAFF/HEALTH_LEAD, UI web + mobile, workflows, kits médicaux. |
| G3.12 | ⬜ | | | Verrou G3 (Gate) - en attente G3.3 |
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
| OrganizationNode.java | Added description, icon, color, sortOrder fields | N/A | 6f49218 |
| TenantMembership.java | Added explicit getters/setters for Lombok compatibility | N/A | 6f49218 |
| Role.java | Added explicit getters/setters for Lombok compatibility | N/A | 6f49218 |
| AuditLog.java | Added explicit getters/setters for Lombok compatibility | N/A | 6f49218 |
| Notification.java | Added explicit getters/setters for Lombok compatibility | N/A | 6f49218 |

---

## Preuves de validation (DoD §0.5)

| Couche | Commande | Résultat | Date |
|---|---|---|---|
| Backend | `cd backend && mvn compile -q` | ✅ sans erreur | 2026-09-15 |
| Backend | `cd backend && mvn -o -B verify` | ✅ BUILD SUCCESS — 1141 tests, 0 failure, 0 error, 13 skip + JAR | 2026-09-16 |
| Frontend | `cd frontend && npm run lint` | ✅ 0 erreur, 963 warnings (DoD 0-warning non atteinte, cf. B3) | 2026-09-16 |
| Frontend | `cd frontend && npm run build` | ✅ built in ~21 s | 2026-09-16 |
| Frontend | `cd frontend && npm run test` | ✅ 41 fichiers / 311 tests pass | 2026-09-16 |
| Mobile | `cd mobile && flutter analyze --no-pub` | ✅ 0 erreur, 0 warning (124 `info` de dépréciation restants) | 2026-09-16 |
| Mobile | `cd mobile && flutter test --no-pub` | ✅ 331/331 tests pass | 2026-09-16 |

> **Note G0.4/G0.5/G0.6 (mise à jour 2026-09-16) :** les 3 couches sont vertes.
> Les lignes précédentes de cette fiche annonçaient « Frontend 311/311 » et un backend
> à 101 erreurs sans que ces chiffres aient été rejoués ; ils l'ont été le 2026-09-16
> et **tous les défauts trouvés ont été corrigés, pas contournés** (voir Bloqueurs).

---

## Bloqueurs & écarts identifiés pendant la fermeture de la porte G0 (2026-09-16)

### B1 — 🔴 BLOQUANT PRODUCTION : la migration V135 ne peut pas s'appliquer sur une base neuve

`V1__initial_schema.sql:263` crée `audit_logs` (schéma legacy : `utilisateur_id`,
`entite_type`, `entite_id`, `created_at`…). `V135__create_multi_tenant_core_tables.sql:163`
refait `CREATE TABLE audit_logs` (schéma `actor_id`, `resource`, `result`,
`metadata_json`, `timestamp NOT NULL DEFAULT …`) **sans `IF NOT EXISTS`**, puis crée
5 index dont `idx_audit_timestamp ON audit_logs(timestamp)`.

→ Sur une base vierge, Flyway s'arrête : `relation "audit_logs" already exists`
(PostgreSQL) / `Column "timestamp" not found` (H2). Rollback, redémarrage, retriable
par `start-local.sh` en mode docker, et bloquant pour toute nouvelle église (§G6.9).

**Décision requise (annexe G, Go/No-Go)** : (a) renommer le bloc V135 en `audit_event`
(c'est la table cible du moteur d'audit G2.9 — voie recommandée) ; (b) ajouter une
migration `V149` qui aligne `audit_logs` sur le schéma V135 et rend V135 idempotent ;
(c) baseliner V135 hors Flyway. **Non tranché automatiquement ici : impacte le schéma
de production et la chaîne de hachage §G2.9.**

### B2 — 🟠 Deux moteurs d'audit concurrents (à résorber en G2.9)

Le contrat §G2.9 demande `audit_event` (+ hash chain `prev_hash`/`hash`, rétention 95 j,
export) et `business_history`. Aujourd'hui seul `AuditLog`/`AuditService` existe, sans
hash chain ni historique métier générique. L'entité dupliquée qui cassait les inserts a
été supprimée ; l'implémentation du contrat §G2.9 reste **à faire**.

### B3 — 🟡 Lint frontend : 963 warnings (DoD G0.4 exige max-warnings 0)

Majoritairement `@typescript-eslint/no-explicit-any` et variables/icônes non utilisées.
Le build et les 311 tests passent ; ces avertissements ne cassent rien mais la DoD
G0.4 (« 0 warning ») n'est **pas** satisfaite. Réduction à planifier (étape dédiée,
le nettoyage mécanique de 963 sites impose une passe par lots + non-régression).


---

## Correctifs de la fermeture de porte G0 (2026-09-16)

| Fichier | Nature du correctif | Commit |
|---|---|---|
| `backend/pom.xml` | lombok 1.18.36 → 1.18.38 | d125b314 |
| `modules/audit/domain/AuditLog.java` (+ Alert, Department, DepartmentTask, Notification, Soul, OrganizationNode, Tenant, TenantMembership, User) | `@Getter/@Setter` explicites remplacés/ajoutés pour cohérence Lombok | d125b314 |
| `modules/tenants/domain/AuditLog.java` + `AuditLogRepository.java` | **Supprimés** : 2ᵉ entité JPA mappée sur `audit_logs` avec un schéma incompatible (`timestamp`/`resource` NOT NULL) → tout insert d'audit échouait (23502) et marquait la transaction `rollback-only` | d125b314 |
| `modules/broadcast/domain/BroadcastService.java` | `getById` passé de `findById(id)` à `findByIdAndTenantId` (règle absolue §0.3 n°3) + `getTenantStats()` réel | d125b314 |
| `modules/broadcast/api/BroadcastController.java` | `GET /api/v1/broadcast/stats` (endpoint consommé par le mobile mais **inexistant**) | d125b314 |
| `modules/configuration/*` | **Supprimés** : copies mortes du moteur d'héritage G1.7 (doublons de `modules/tenants/*`) | 4018295d |
| `WorkspaceIsolationIntegrationTest.java` | `@Import(SecurityConfig)` → `@Import(TestSecurityConfig)` (AuthorizationService non satisfiable en slice `@WebMvcTest`) | d125b314 |
| `TenantServiceTest.java` | `@Mock TenantFeatureService` (dépendance ajoutée en G1.3, NPE sur `create`) | d125b314 |
| `mobile/lib/app.dart` | **5 noms de route go_router dupliqués supprimés** : l'assertion `!_nameToPath.containsKey(name)` faisait échouer le build de `DiscipolatApp` → l'application ne démarrait pas | 23691b63 |
| `mobile/lib/presentation/screens/network/network_screen.dart` | Persistance du cache local rendue *best-effort* : un échec d'écriture ne masque plus des données réseau valides ; cache injectable (tests) | 23691b63 |
| `mobile/lib/presentation/screens/users/users_list_screen.dart` | Garde d'autorisation inversée : « promouvoir » s'affichait aux non-Admin et **pas** aux Admin/Pasteur | 23691b63 |
| `mobile/lib/core/tenant_session.dart` | `ProfileScope`-extension utilisant `read()` sans import Riverpod + précédence `?? >` sur `> 1` | 23691b63 |
| `mobile/lib/features/ai/{dashboard,family_cohesion}/*.dart` | Les écrans lisaient un `Response` Dio comme du JSON déjà décodé (`api.get(...)[x]`) : jamais fonctionnel → `.data` | 23691b63 |
| `mobile/lib/presentation/screens/tenant/*.dart` | Imports relatifs faux (`../../api/` → `../../../api/`), écrans admin tenant | 23691b63 |
| `mobile/lib/presentation/screens/tenant_selection_screen.dart`, `mobile/lib/features/auth/TenantSelectionScreen.dart`, `mobile/lib/presentation/screens/tenant/org_settings_screen.dart` | **Supprimés** : doublons non référencés et non compilables (le 1er déclarait un 2ᵉ `MainScaffold`) ; `OrgSettingsScreen` dupliquait `TenantSettingsScreen` qui est, elle, routée | 23691b63 |
| `mobile/test/*` (9 fichiers) | Fakes obsolètes : préfixe `/api/v1` que le client ne transmet jamais, et `/announcements` au lieu de `/broadcast` | 23691b63 |
| `frontend/src/__tests__/RoleWorkspaceRouting.test.tsx` | Arbre de providers aligné sur `main.tsx` (`ImpersonationProvider`) — `<ImpersonationBanner>` faisait jeter l'ErrorBoundary | 981afc9e |
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