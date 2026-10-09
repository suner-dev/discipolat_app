# 01 — FAIT TOTALEMENT (53 fichiers)

Tous les fichiers dans ce dossier décrivent des fonctionnalités **entièrement implémentées** dans le code.

## Backend (Spring Boot)
- Contrôleurs, services, entités, migrations Flyway présents
- Endpoints REST documentés et fonctionnels
- Tests unitaires et d'intégration validés

## Frontend (React)
- Pages, composants, hooks implémentés
- i18n complète (6 locales)
- Tests Vitest validés

## Mobile (Flutter)
- Écrans, services, modèles implémentés
- Tests Flutter validés

## Liste des fichiers

| Fichier | Justification |
|---------|---------------|
| `AGENT_ORCHESTRATION.md` | `FirebaseAdminPushGateway`, `BackupService`, `PayoutProviderRegistry`, `ShardRouting` présents |
| `PASSATION_SOCIAL_AUTH.md` | `SocialAuthController`, `SocialIdentityVerifier`, `OidcSocialIdentityVerifier` présents |
| `PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md` | `OnboardingWizardController`, `TenantStatusGuard`, `TenantStatusInterceptor` présents |
| `SPEC_BACKEND_SERVICES_MOBILES_V233.md` | 4 contrôleurs backend + services mobile correspondants présents |
| `SUPER_ADMIN_AUDIT.md` | `SuperAdminController`, `PlatformProvisioningController`, `ImpersonationController` présents |
| `README.md` | Stack confirmée : Java 21, Spring Boot 3.4.7, React 19, Flutter 3, PostgreSQL 16, Redis 7 |
| `docs/ADR_001_MEMBER_RELATIONS.md` | `MemberRelation`, `MemberRelationService`, `UserHierarchyService`, `RelationController` présents |
| `docs/API.md` | 1423 chemins, 1764 opérations, 239 tags — auto-généré par springdoc |
| `docs/AUTH_SOCIAL.md` | `SocialAuthController`, `SocialIdentityVerifier`, migration V217 présents |
| `docs/DATABASE.md` | 191 migrations Flyway, isolation tenant_id + RLS, soft delete, JSONB |
| `docs/DEPLOYMENT.md` | docker-compose.yml, render.yaml, workflows CI/CD présents |
| `docs/DISTRIBUTION_TESTEURS.md` | `scripts/build-apk-distribution.sh` existe |
| `docs/ENV_TEMPLATE.md` | Toutes variables documentées lues par application.yml |
| `docs/ETAT_AVANCEMENT_CHURCH_OS.md` | 176 migrations, 209 contrôleurs, 239 pages web, 184 écrans mobile |
| `docs/GUIDE_BACK_OFFICE_COMMERCIAL.md` | `TenantController`, `InvitationController`, `TenantFeatureController` présents |
| `docs/GUIDE_SUPER_ADMIN.md` | `SuperAdminController`, `SuperAdminSaasPlanController`, `ImpersonationController` présents |
| `docs/GUIDE_UTILISATEUR.md` | 266 pages web + 210 écrans mobile couvrant tous les modules |
| `docs/PLAN_HIERARCHIE_RELATIONS_MEMBRES.md` | V231/V232, `MemberRelation`, `UserHierarchyService`, contrôleurs présents |
| `docs/PLAN_V233_BACKEND_SERVICES.md` | `DiscipleshipController`, `TaskController`, `HealthController`, `FinanceController` présents |
| `docs/SPEC_LOT2_TOUT_PARAMETRABLE.md` | V229/V230, `NavigationGroupService`, `UiCustomizationService` présents |
| `docs/SPEC_ONBOARDING_FLOWS.md` | V219/V220/V221, `JoinCodeService`, `TenantJoinCodeController` présents |
| `docs/SPEC_ORGANISATION_DENOMINATION_V2.md` | `TenantKind`, `TenantTransferService`, `TenantTransferController` présents |
| `docs/SPEC_ORGANISATION_MODULABLE_V3.md` | V224-V228, `OrganizationLevel`, `RoleTitle`, `MemberRoleAssignment` présents |
| `docs/TENANT_ONBOARDING.md` | Wizard 7 étapes, `POST /api/v1/platform/admin/provisioning` présent |
| `docs/TENANT_SECURITY.md` | Hibernate filter, TenantContext, RLS, rate limiting Bucket4j, 36 tests |
| `docs/WEB_MOBILE_PARITY.md` | 266 pages web + 210 écrans mobile, services `discipleshipService.ts` présents |
| `docs/migration/migration_map_events.md` | `LegacyMigrationService.migrateEvents()` présent |
| `docs/migration/migration_map_peoples.md` | `LegacyMigrationService.migratePeoples()` présent |
| `docs/migration/migration_map_spaces.md` | `LegacyMigrationService.migrateSpaces()` présent |
| `docs/qa/QA_SCENARIOS.md` | 50 scénarios, suites de tests backend/frontend/mobile présentes |
| `docs/rapports/RAPPORT_T-ORG-001.md` | `TenantOrganizationController`, `TenantTransferController`, `TenantKind` présents |
| `docs/rapports/RAPPORT_T-ORG-V3.md` | `OrganizationLevelController`, `RoleTitleController`, `MemberRoleAssignmentController` présents |
| `docs/rapports/README.md` | Stack technique confirmée |
| `e2/README.md` | `playwright.config.ts` existe |
| `e2/specs/README.md` | Dossier `e2/specs/` existe |
| `frontend/docs/ROLE_BASED_VISIBILITY.md` | `useAuth`, `activeRole`, `canManage`, `ProtectedRoute` présents |
| `mobile/README.md` | Projet Flutter 3.35+ avec Riverpod, Drift, GoRouter |
| `reports/GO_NO_GO_REPORT.md` | Tag `v1.0-commercial-release` existe, gates G0-G6 validés |
| `reports/PARITY_AUDIT_REPORT.md` | `GroupMessageController`, `LiveStreamController`, `InventoryController` présents |
| `reports/PERFORMANCE_REPORT.md` | Migration V176 avec index pg_trgm, `SearchService` optimisé |
| `reports/PRODUCTION_READINESS.md` | `PublicDirectoryController`, `LandingPage`, workflows `deploy-beta.yml` présents |
| `reports/QA_REPORT.md` | Suites de tests présentes : backend 1901, frontend 543, mobile 555 |
| `reports/REGRESSION_REPORT.md` | 143 classes de test backend, `e2e/results.json` avec CP1-CP8 |
| `reports/SECURITY_AUDIT_REPORT.md` | `TenantAwareSimpleJpaRepository`, `ci-cd.yml` avec gitleaks |
| `reports/plan-2agents/HANDOVER-VERS-AGENT-A.md` | `TeamTaskController` sur `/api/v1/team-tasks` |
| `reports/plan-2agents/INTEGRATION-1-2-3.md` | V193/V194, `InvitationController`, `TenantSwitcherController` présents |
| `reports/plan-2agents/INTEGRATION.md` | `OnboardingStepActions`, `FlywayMigrationChainPostgreSqlTest` présents |
| `reports/plan-2agents/MIGRATION-CONTRAT-IDS.md` | 7 modèles mobile avec `required int id`, `identifier_contract_test.dart` |
| `reports/plan-2agents/VERIFICATION.md` | Gates G-A (5/5) et G-B (7/7), recette `verify-tenant-onboarding.sh` |
| `reports/plan-2agents/agentA.md` | `TenantStatusGuard`, `OnboardingWizardService`, V183/V185 présents |
| `reports/plan-2agents/agentB.md` | `OnboardingWizardPage`, `RegistrationStatusPage`, services mobile présents |
| `reports/plan-2agents/contract-audit.md` | 7 endpoints onboarding-wizard résolvent des deux côtés |
| `reports/plan-2agents/endpoint-gap-audit.md` | `compliance_service.dart`, endpoints `/public/legal`, `/billing/stripe/*` |
| `reports/plan-2agents/security-recovery/RECOVERY-NOTES.md` | `UserSecretSerializationTest`, `@JsonProperty(WRITE_ONLY)` |
