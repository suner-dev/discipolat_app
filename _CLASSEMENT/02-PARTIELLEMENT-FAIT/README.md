# 02 — PARTIELLEMENT FAIT (19 fichiers)

Fichiers décrivant des fonctionnalités **partiellement implémentées** dans le code.

## Liste des fichiers

| Fichier | Ce qui est fait | Ce qui manque |
|---------|-----------------|---------------|
| `TODO_EGLISE_DABORD_MARQUE_RETOUR.md` | `BrandLink`, `BackButton` dans `AuthLayout` | `ChurchPicker`, route `/e/:slug`, `landing_enabled`, `applyScopedBranding`, `church_landing_screen.dart` |
| `TODO_REPRISE_ONBOARDING_ORCHESTRATION.md` | Backend A1-A16, correctifs H1-H8, push FCM, backup, outbox, sharding fondations | Tâches clients B4-B13, arbitrages D1/D2/D3, campagne orchestration A0/A3/A5/A6 + B0-B5 |
| `AUDIT_ARCHITECTURE_FRONTEND.md` | — | 266 pages à plat, 259 `any`, `queryKey` non isolés par tenant |
| `docs/ADMINISTRATION_MODEL.md` | Hiérarchie rôles, impersonation TTL 30min, branding tenant | Versioning permissions V162 incomplet, monitoring admin partiel |
| `docs/ARCHITECTURE.md` | ~110 modules, filtre Hibernate, TenantContext, RBAC hiérarchique | "Zero mock" non respecté, modules IA/workflow partiels, 132 migrations annoncées vs ~191 réelles |
| `docs/MULTI_TENANT_ARCHITECTURE.md` | Isolation Hibernate sur 260+ entités, TenantInterceptor, RLS | "260+ entités" non vérifiable, ApplicationContext parfois KO en tests, provisioning H8 cassé |
| `docs/RBAC.md` | Hiérarchie 9 rôles, scope TENANT>REGION>CHURCH>DEPARTMENT>FAMILY>ASSIGNED/OWN | Scopes SUB_CHURCH/CAMPUS non systématiques, 320 cellules non toutes testées |
| `docs/ORGANIZATION_HIERARCHY.md` | OrganizationNode avec parentId/path ltree/level/type, NodeType enum | Provisioning H8 (`path` en ltree vs String), createCampus/createMinistry non implémentés |
| `docs/SCALING.md` | TenantDataSource + ShardRouting, V190 currencies ISO-4217 | Aucun ShardedTenantDataSource, aucune table partitionnée, mono-instance Redis/files |
| `docs/SECURITY.md` | JWT RS256 15min/refresh 7j, rate limiting Bucket4j, BCrypt coût 12 | 2FA TOTP non mentionné, matrice rôles simplifiée, pas de détail mobile security |
| `docs/security/SECURITY_MATRIX.md` | MultiTenantSecurityTests (36 tests), OnboardingWizardSecurityIT | 320 cellules non 100% prouvées, fix/schema-drift-h1-h5 pas mergé |
| `docs/CHANGELOG.md` | V231 member_relations, V232 notification_type backfill | "Tous les 7 Gates validés" non prouvé, releases antérieures non vérifiables |
| `docs/DECISIONS.md` | Spring Modulith hexagonal, JWT RS256, Bucket4j, BCrypt coût 12 | Pas d'ADR formel pour choix structurants (multi-tenant, migrations, i18n) |
| `docs/CONTRIBUTING.md` | Structure projet correcte, commandes Maven/npm/flutter | Prérequis Java 23+ non conforme, README réel 596 octets, pas de workflow branches |
| `docs/RUNBOOK.md` | Signaux/seuils Prometheus/Grafana, procédures incident, sauvegardes | TenantModuleIsolationEndToEndHttpTest non vérifiable, procédure restore non prouvée |
| `docs/RUNBOOK_OPS.md` | Environnements staging/beta/prod, pile Prometheus+Grafana, chaîne d'audit | Scripts smoke-check.sh/verify-beta.sh non vérifiés, monitoring non confirmé en conditions réelles |
| `docs/UX_UI_AUDIT.md` | `EmptyState`, `ConfirmDialog`, `SkeletonLoader`, `ErrorBoundary` | `components/dashboard/` n'existe pas, pages >1000 lignes non corrigées |
| `docs/architecture/schema-events-drift.md` | Constat réel: Event.java mappe `events` (table morte), V158 a renommé | Dette assumée: 2 entités sur table `event`, retrait ChurchEvent PAS fait |
| `reports/plan-2agents/MIGRATION-CONTRAT-IDS.md` | 7 modèles mobile avec `required int id`, `identifier_contract_test.dart` | Migration partielle effectuée |
