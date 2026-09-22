# Architecture Technique — Discipolat Church OS

> Version 2.0 — 2026-09-22 · §72 / G6.8 · Statut : a jour.
> References : [Multi-tenant](MULTI_TENANT_ARCHITECTURE.md) · [RBAC](RBAC.md) · [Administration](ADMINISTRATION_MODEL.md) · [Securite](SECURITY.md) · [Base de donnees](DATABASE.md) · [API](API.md) · [Deploiement](DEPLOYMENT.md) · [Cible](architecture/target-architecture.md) · [Gap analysis](architecture/gap-analysis.md) · [Mapping](architecture/multitenancy-mapping.md) · [Etat actuel](architecture/current-state.md)

## 1. Vue d'ensemble

```
React 19 SPA + Nginx  ─┐
Flutter offline-first ─┼─► HTTPS/REST + WebSocket/SSE ─► Nginx/CDN ─► Spring Boot (~110 modules, :8080)
Postman / API publique┘                                                     ├─► PostgreSQL 16 (Flyway, 132+ migrations)
                                                                         ├─► Redis 7 (Bucket4j, cache tenant-aware)
                                                                         └─► Stockage S3-compatible + SMTP + WhatsApp/MoMo
```

## 2. Principes structurants

| Principe | Mise en oeuvre |
|---|---|
| Multi-tenant natif | `tenant_id` partout, filtre Hibernate `tenantFilter`, `TenantContext` (ThreadLocal), RLS sur tables critiques — voir [MULTI_TENANT_ARCHITECTURE.md](MULTI_TENANT_ARCHITECTURE.md) |
| Espaces (Spaces) | Unite de deploiement modulaire : `space` + `space_module` + `space_templates` (V148-V152). `ModuleRouter` → 403 si module desactive |
| Moteurs transverses | People, Org, Events, Assets, Finance, Discipleship, Prayer, Media, Health, Workflow, Configuration (custom fields, statuts, permissions), Audit, Notification, Realtime, Offline — voir [cible](architecture/target-architecture.md) |
| Outbox transactionnel | `core.OutboxEvent` + `OutboxPublisher`/`OutboxDispatcher`/`OutboxConsumers` : publication puis propagation (WebSocket/SSE < 5 s, webhooks `webhook_delivery_logs` V108) |
| RBAC hierarchique | `PLATFORM_SUPER_ADMIN` → `TENANT_OWNER` → `TENANT_ADMIN` → `REGION_ADMIN` → `CHURCH_ADMIN`/`CAMPUS_PASTOR` → `DEPARTMENT_ADMIN` → `FAMILY_LEADER` → `DISCIPLE_MAKER` → `MEMBER` ; scopes `TENANT > REGION > CHURCH > DEPARTMENT > FAMILY > ASSIGNED/OWN` — voir [RBAC.md](RBAC.md), [matrice](security/SECURITY_MATRIX.md) |
| Configuration heritee | `DEFAULT → INHERITED → OVERRIDDEN`, `ConfigurationResolver` + cache + invalidation temps reel (V145) |
| Zero mock | Integrations reelles (WhatsApp, Orange/MTN/M-Pesa, IA) avec fallback local explicite documente — voir [ENV_TEMPLATE.md](ENV_TEMPLATE.md) |

## 3. Backend (Spring Boot 3 · Java 21, Spring Modulith hexagonal)

Chaque module : `api/` (controleurs, DTOs) · `domain/` (entites, services, repositories) · `infrastructure/` · `config/`.

Noyau : `authentication` (JWT RS256 15 min, refresh 7 j, 2FA TOTP), `tenants`/`platform` (tenants, invitations, impersonation TTL 30 min, quotas/plans SaaS V144), `spaces`, `people` (V154), `souls`, `families`, `departments`, `events` (+ `church-events` V158), `finances`/`payments` (Orange/MTN/M-Pesa), `messages`/`broadcast`/`whatsapp`/`ussd`/`voice`, `ai`/`aiPredictions`/`aiVisitNotes`, `workflow`/`automations` (moteur V151), `customfields`/`statuses` (V150/V153), `audit` (hash chain), `notifications`, `dashboard`/`kpiNarrative`/`engagementAnalytics`, `files`/`imports`/`exports`, `gdpr`/`compliance`, `integrations`/`webhooks`/`publicapi`, `backups`/`dataMigration` (V164), `health` (actuator), `search` (pg_trgm V163).

Couches transverses : `TenantInterceptor` + `TenantFilterInterceptor` (resolution JWT → filtre auto) · `AuthorizationService` (point unique `@PreAuthorize`) · `ModuleRouter` (403 module desactive) · `BruteForceProtectionFilter` + Bucket4j/Redis (login 10/min, refresh 20/min…) · Actuator + Micrometer/Prometheus (`/actuator/health`, `/actuator/prometheus`).

## 4. Frontend (React 19 + TypeScript, 231 pages)

`components/` (layout, shared, `guards/RouteGuards`) · `contexts/` (Auth, Tenant + TenantSwitcher) · `hooks/`/`lib/` (Axios + intercepteurs, TanStack Query) · `pages/` (Admin*, Pasteur*, finances, IA, Academy…) · `App.tsx`/`main.tsx`/`index.css` (Tailwind). Gardes : `RequirePermission`/`RequireRole`/`RequireFeature`. Aide integree : modales « ? » + wizard onboarding — voir [Guide utilisateur](GUIDE_UTILISATEUR.md).

## 5. Mobile (Flutter, offline-first)

`core/` (theme, branding par tenant) · `data/` (API, repositories, Drift/SQLite + file outbox locale) · `domain/` · `presentation/` (+ `AuthorizationService` client). Sync pull/push JWT, propagation < 5 s, degrade USSD/SMS.

## 6. Donnees & migrations

PostgreSQL 16 + Flyway, 132 migrations (`V1`→`V164`) : transfer workflow (V32-33), multitenancy (V70), permissions (V72), GDPR (V90), WhatsApp (V95-97), reseau (V100), IA (V109-111), coeur multi-tenant (V135-136), hierarchie `organization_unit` (V147), `module_definition`/`space_module` (V148), `spaces` (V149), statuts (V150), workflow (V151), templates (V152), people (V154), legacy (V158, V164). Detail : [DATABASE.md](DATABASE.md).

## 7. Deploiement & operations

Local : `docker-compose.yml` (db, redis, api, mailhog, web, nginx). Prod : `render.yaml` (API Docker, frontend statique CDN, Postgres, Redis) + `ci.yml`, `ci-cd.yml`, `deploy-beta.yml`, `backup-postgres.yml` (dump chiffre AES-256), `keep-alive.yml`. Monitoring Prometheus + Grafana (`infra/monitoring`). Detail : [DEPLOYMENT.md](DEPLOYMENT.md) · [ENV_TEMPLATE.md](ENV_TEMPLATE.md) · [RUNBOOK.md](RUNBOOK.md).

## 8. References

- [MULTI_TENANT_ARCHITECTURE.md](MULTI_TENANT_ARCHITECTURE.md) · [ADMINISTRATION_MODEL.md](ADMINISTRATION_MODEL.md) · [RBAC.md](RBAC.md) · [security/SECURITY_MATRIX.md](security/SECURITY_MATRIX.md)
- [DATABASE.md](DATABASE.md) · [API.md](API.md) · [DEPLOYMENT.md](DEPLOYMENT.md) · [ENV_TEMPLATE.md](ENV_TEMPLATE.md)
- [GUIDE_UTILISATEUR.md](GUIDE_UTILISATEUR.md) · [GUIDE_BACK_OFFICE_COMMERCIAL.md](GUIDE_BACK_OFFICE_COMMERCIAL.md) · [RUNBOOK.md](RUNBOOK.md)
- [architecture/target-architecture.md](architecture/target-architecture.md) · [architecture/gap-analysis.md](architecture/gap-analysis.md) · [architecture/multitenancy-mapping.md](architecture/multitenancy-mapping.md) · [architecture/current-state.md](architecture/current-state.md)
