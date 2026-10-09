# Documentation Discipolat

> **Index navigable de la documentation projet.**
>
> Les 40 fichiers ci-dessous sont classés par catégorie. Chaque entrée pointe vers
> le fichier réel dans `docs/`. Les versions périmées ont été corrigées (README
> racine, chiffres de migration, etc.).

---

## 🏗️ Architecture & Conception

| Fichier | Description |
|---------|-------------|
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Vue d'ensemble : modules (~110), filtre tenant, RBAC hiérarchique, espaces, outbox, 200+ pages web |
| [`MULTI_TENANT_ARCHITECTURE.md`](MULTI_TENANT_ARCHITECTURE.md) | Isolation Hibernate, TenantContext, RLS, Redis tenant-aware, AuthorizationService centralisé |
| [`ORGANIZATION_HIERARCHY.md`](ORGANIZATION_HIERARCHY.md) | OrganizationNode (ltree), NodeType, Space/SpaceType, ConfigurationResolver, héritage config |
| [`RBAC.md`](RBAC.md) | 9 rôles, scope TENANT>REGION>CHURCH>DEPARTMENT>FAMILY>ASSIGNED/OWN, AuthorizationService, cache temps réel |
| [`ADMINISTRATION_MODEL.md`](ADMINISTRATION_MODEL.md) | Hiérarchie rôles, impersonation TTL 30min auditée, branding tenant, ModuleRouter, espace/space_module |
| [`SCALING.md`](SCALING.md) | TenantDataSource + ShardRouting (hash), V190 currencies ISO-4217, fondations multi-bases |
| [`architecture/ADR-001-separer-les-repos-et-gitlab.md`](architecture/ADR-001-separer-les-repos-et-gitlab.md) | Proposition : découpage repos + migration GitLab (non implémentée) |
| [`architecture/frontend-target-architecture.md`](architecture/frontend-target-architecture.md) | Cible : tranches verticales, contrat-first, design system, i18n lazy (non implémentée) |
| [`architecture/PREPARATION-DUE-DILIGENCE.md`](architecture/PREPARATION-DUE-DILIGENCE.md) | 5 travaux prioritaires (Testcontainers, ddl-auto, JaCoCo, README, zéro TODO) |
| [`architecture/schema-events-drift.md`](architecture/schema-events-drift.md) | Dette `events`/`event` : 2 entités sur même table, retrait ChurchEvent non fait |

---

## 🔐 Sécurité & Conformité

| Fichier | Description |
|---------|-------------|
| [`SECURITY.md`](SECURITY.md) | JWT RS256 15min/refresh 7j, rate limiting Bucket4j, BCrypt coût 12, headers sécurité |
| [`TENANT_SECURITY.md`](TENANT_SECURITY.md) | Filtre tenant, RLS, rate limiting, 36 MultiTenantSecurityTests, GDPR endpoints |
| [`security/SECURITY_MATRIX.md`](security/SECURITY_MATRIX.md) | 36 tests isolation, OnboardingWizardSecurityIT, CrossTenantReadScopeTest |

---

## 🚀 Déploiement & Ops

| Fichier | Description |
|---------|-------------|
| [`DEPLOYMENT.md`](DEPLOYMENT.md) | docker-compose, render.yaml, 8 workflows CI/CD, SMTP, scheduler, quotas Render |
| [`RUNBOOK.md`](RUNBOOK.md) | Signaux Prometheus/Grafana, procédures incident (5xx, pool PG, Redis, Flyway, mémoire, cert) |
| [`RUNBOOK_OPS.md`](RUNBOOK_OPS.md) | Environnements staging/beta/prod, monitoring, backup-postgres.yml, chaîne audit |
| [`ENV_TEMPLATE.md`](ENV_TEMPLATE.md) | Toutes variables lues par application.yml (DB, JWT, encryption, email, Mobile Money, AI, Redis) |
| [`DISTRIBUTION_TESTEURS.md`](DISTRIBUTION_TESTEURS.md) | `scripts/build-apk-distribution.sh`, `api_config.dart` → `discipolat-api.onrender.com` |

---

## 💾 Base de données & Migrations

| Fichier | Description |
|---------|-------------|
| [`DATABASE.md`](DATABASE.md) | 204 migrations Flyway (V1→V241), isolation tenant_id+RLS, soft delete, JSONB, pg_trgm V163 |
| [`migration/migration_map_events.md`](migration/migration_map_events.md) | `LegacyMigrationService.migrateEvents()`, V158 `events`→`legacy_events` + `event` |
| [`migration/migration_map_peoples.md`](migration/migration_map_peoples.md) | `migratePeoples()`, entités Person, Membership |
| [`migration/migration_map_spaces.md`](migration/migration_map_spaces.md) | `migrateSpaces()`, entités Space, OrganizationNode |

---

## 📋 Spécifications & Plans

| Fichier | Description |
|---------|-------------|
| [`SPEC_ORGANISATION_MODULABLE_V3.md`](SPEC_ORGANISATION_MODULABLE_V3.md) | Dimensions A→E, niveaux configurables, modules/branding par nœud, agrégats sans PII |
| [`SPEC_ORGANISATION_DENOMINATION_V2.md`](SPEC_ORGANISATION_DENOMINATION_V2.md) | TenantKind, rootTenantId, parentTenantId, TenantTransferService |
| [`SPEC_ONBOARDING_FLOWS.md`](SPEC_ONBOARDING_FLOWS.md) | Wizard 7 étapes, V219/220/221, JoinCodeService, SelfServiceChurchService |
| [`SPEC_LOT2_TOUT_PARAMETRABLE.md`](SPEC_LOT2_TOUT_PARAMETRABLE.md) | V229/V230, NavigationGroupService, UiCustomizationService, branding scopé |
| [`PLAN_HIERARCHIE_RELATIONS_MEMBRES.md`](PLAN_HIERARCHIE_RELATIONS_MEMBRES.md) | V231/V232, MemberRelation, UserHierarchyService, arborescence + pagination |
| [`PLAN_V233_BACKEND_SERVICES.md`](PLAN_V233_BACKEND_SERVICES.md) | DiscipleshipController, TaskController, HealthController, FinanceController |
| [`PLAN_FUSION.md`](PLAN_FUSION.md) | Plan de fusion branches (obsolète, branches fusionnées autrement) |
| [`PLAN_HIERARCHIE_RELATIONS_MEMBRES.md`](PLAN_HIERARCHIE_RELATIONS_MEMBRES.md) | V231 member_relations, V232 notification_type backfill |

---

## 🧪 QA & Rapports

| Fichier | Description |
|---------|-------------|
| [`qa/QA_SCENARIOS.md`](qa/QA_SCENARIOS.md) | 50 scénarios web/mobile/offline/realtime, 1901 tests backend, 543 frontend, 555 mobile |
| [`rapports/RAPPORT_T-ORG-001.md`](rapports/RAPPORT_T-ORG-001.md) | TenantOrganizationController, TenantTransferController, TenantKind, V222/V223 |
| [`rapports/RAPPORT_T-ORG-V3.md`](rapports/RAPPORT_T-ORG-V3.md) | OrganizationLevelController, RoleTitleController, V224-V228 |
| [`rapports/README.md`](rapports/README.md) | Stack confirmée : Java 21, Spring Boot 3.4.7, React 19, Flutter 3, PG 16, Redis 7 |

---

## 📖 Guides Utilisateur & Admin

| Fichier | Description |
|---------|-------------|
| [`GUIDE_UTILISATEUR.md`](GUIDE_UTILISATEUR.md) | 266 pages web + 210 écrans mobile, tous modules (âmes, événements, finances, santé, IA) |
| [`GUIDE_SUPER_ADMIN.md`](GUIDE_SUPER_ADMIN.md) | SuperAdminController, SaasPlanController, ImpersonationController, AuditController |
| [`GUIDE_BACK_OFFICE_COMMERCIAL.md`](GUIDE_BACK_OFFICE_COMMERCIAL.md) | TenantController, InvitationController, QuotaController, dashboard admin |
| [`TENANT_ONBOARDING.md`](TENANT_ONBOARDING.md) | Wizard 7 étapes, provisioning, activation owner, limites honnêtes documentées |
| [`AUTH_SOCIAL.md`](AUTH_SOCIAL.md) | SocialAuthController, SocialIdentityVerifier, OIDC, V217 user_identities |

---

## 📚 Divers

| Fichier | Description |
|---------|-------------|
| [`CHANGELOG.md`](CHANGELOG.md) | V231 member_relations, V232 notification_type, 8 failles post-audit corrigées |
| [`DECISIONS.md`](DECISIONS.md) | Spring Modulith, JWT RS256, Bucket4j, BCrypt 12, TanStack Query, Tailwind |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Structure projet, commandes Maven/npm/flutter, prérequis Java 21 |
| [`ETAT_AVANCEMENT_CHURCH_OS.md`](ETAT_AVANCEMENT_CHURCH_OS.md) | 176 migrations, 209 contrôleurs, 239 pages web, 184 écrans mobile, gates G0-G6 |
| [`SPEC_ORGANISATION_MODULABLE_V3.md`](SPEC_ORGANISATION_MODULABLE_V3.md) | Dimensions A→E complétées |
| [`ADR_001_MEMBER_RELATIONS.md`](ADR_001_MEMBER_RELATIONS.md) | MemberRelation, UserHierarchyService, V231, TenantFilterDefArchitectureTest |
| [`UX_UI_AUDIT.md`](UX_UI_AUDIT.md) | EmptyState, ConfirmDialog, SkeletonLoader, ErrorBoundary (dashboard à faire) |
| [`WEB_MOBILE_PARITY.md`](WEB_MOBILE_PARITY.md) | 266 pages web + 210 écrans mobile, services API partagés |

---

## 🔗 Liens utiles

- **README racine** : [`../README.md`](../README.md) (stack, chiffres, démarrage)
- **OpenAPI** : [`openapi.json`](openapi.json) (1423 chemins, 1764 opérations, 239 tags)
- **API auto-générée** : [`API.md`](API.md) (reflet exact springdoc)
- **Scripts de recette** : `scripts/verify-tenant-onboarding.sh`, `scripts/test-restore.sh`
- **Rapports agents** : `reports/plan-2agents/`

---

*Généré et maintenu à jour avec le code. Dernière mise à jour : 2026-10-09.*