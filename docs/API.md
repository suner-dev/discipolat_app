# API REST — Discipolat Church OS

> Version 2.0 — 2026-09-22 · §72 / G6.8. Base : `http://localhost:8080/api/v1` (local), `https://discipolat-api.onrender.com/api/v1` (prod).
> OpenAPI : `/api-docs` (JSON) · Swagger : `/swagger-ui.html` (springdoc 2.8.6). Auth : `Authorization: Bearer <JWT RS256, 15 min>` sauf mention contraire. Voir [ARCHITECTURE.md](ARCHITECTURE.md) · [RBAC.md](RBAC.md) · [matrice](security/SECURITY_MATRIX.md) · [ENV_TEMPLATE.md](ENV_TEMPLATE.md).

## Conventions

- Pagination Spring : `?page=0&size=20&sort=nom,asc` → `{content, totalElements, totalPages, size, number, first, last, empty}`.
- Erreurs : `400` validation · `401` non authentifie · `403` RBAC/module (voir `ModuleRouter`) · `404` · `409` conflit metier · `429` rate limit · `500`.
- Isolement : tout endpoint authentifie est scope par `tenant_id` + `organization_unit` (filtre Hibernate auto ; cross-tenant = 403/404 IDOR-safe).
- Rate limiting (Bucket4j/Redis) : login 10/min/IP · refresh 20/min · forgot-password 3/min · reset/activate/change-password 5/min · switch-role 30/min.

## Authentification (`/auth`, `/auth/2fa`)

| Methode | Endpoint | Description | Role |
|---|---|---|---|
| POST | `/auth/login` | Login (rate-limite, brute-force protege) | Public |
| POST | `/auth/refresh` | Rotation refresh token | Authentifie |
| POST | `/auth/forgot-password` `…/reset-password` `…/activate` `…/change-password` | Cycle mot de passe/activation | Public/Authentifie |
| POST | `/auth/switch-role` | Changer de role actif (multi-roles) | Authentifie |
| * | `/auth/2fa/**` | Enrollment/verification TOTP | Authentifie |

## Noyau eglise (scopes adaptes)

| Domaine | Base | Operations |
|---|---|---|
| Utilisateurs | `/users` | GET (PASTEUR, RESPONSABLE) · POST · GET/PUT `/{id}` · PATCH `/{id}/transfer` (workflow) |
| Membres/People | `/members`, `/people` | CRUD + competences (`/members/competences`), dossiers 360 |
| Ames | `/souls` | GET filtrable · POST · GET/PUT/DELETE `/{id}` · PATCH `/{id}/reassign` (workflow) · GET `/{id}/history` · notes `/souls/{soulId}/notes` · tags `/soul-tags` · discipline `/souls/{soulId}/discipline` |
| Familles | `/families` | GET/POST · GET/PUT/DELETE `/{id}` · PATCH `/{id}/chief` (workflow) · OS `/families/{familyId}/os` · cohesion `/family-cohesion` · reunions `/family-meetings` · ressources `/family-resources` |
| Departements | `/departments` | GET/POST · GET/PUT/DELETE `/{id}` · KPI `/department-kpis` · presences `/departments/{departmentId}/events/{eventId}/attendance` |
| Espaces (Church OS) | `/spaces`, `/space-templates` | CRUD espaces · application de templates (`V152`) · modules par espace (`space_module` V148) |
| Organisation | `/org`, `/tenants`, `/tenant-switcher` | Hierarchie `organization_unit` (V147) · switch de tenant · heritage config (V145) |
| Rapports | `/reports` (+ export `/reports/export`) | `POST/GET /reports/maker-weekly`, `POST /reports/family-weekly`, `GET /reports/family-weekly/{familyId}`, rapports departements/membres (V53-55) |
| Suivis paralleles | `/parallel-followups` | POST/GET · PATCH/DELETE `/{id}` |
| Alertes/Notifications | `/alerts`, `/smart-alerts`, `/notifications` (+ `/notifications/preferences`) | GET · PATCH `/{id}/read` · PATCH `/read-all` |
| Dashboard | `/dashboard` (+ `/sabbath-dashboard`, `/kpi-narrative`, `/executive-insights`, `/usage-analytics`, `/engagement-analytics`, `/growth-projections`, `/load-prediction`, `/benchmark`, `/church-comparisons`) | KPI consolides, par departement/famille, narratifs IA |
| Transferts (workflow) | `/transfers` (+ `/admin/transfers/workflows`, `/workflow-engine`, `/workflows`, `/workflow/automations`, `/automations`) | GET liste/detail/historique/decisions · POST creer · PUT brouillon · POST `/{id}/submit|decide|cancel|archive` ; configs moteur (V151, V98) |

## Evenements, communication, culte

| Domaine | Base |
|---|---|
| Evenements | `/events`, `/church-events` (V158), `/event-checklists`, `/calendar`, `/programs`, `/appointments`, `/visits` (`/pastoral-visits`), `/map`, `/geofencing` |
| Messagerie | `/messages`, `/group-messages`, `/stream-chat`, `/broadcast`, `/communications`, `/announcements`, `/conversations` (WS temps reel + outbox) |
| WhatsApp/Vocal/USSD | `/whatsapp` (+ `/public/whatsapp`), `/voice`, `/voice-notifications`, `/voice-reports`, `/ussd` |
| Documents/medias | `/documents`, `/files`, `/sermons` (+ `/sermons/translations`, `/sermon-assistant`), `/streams`, `/bible-reading`, `/spiritual-journals`, `/spiritual-journey`, `/spiritual-challenges`, `/prayers`, `/prayer-journal`, `/testimonies` |
| Vie communautaire | `/communities`, `/directory`, `/network`, `/cercle-faiseurs`, `/volunteers`, `/twin`, `/neighborhood-health`, `/health-observatory`, `/facerec` |

## Formation, accompagnement, RH d'eglise

| Domaine | Base |
|---|---|
| Parcours | `/discipleship-paths`, `/mentoring`, `/reverse-mentoring`, `/trainings`, `/development-plans`, `/personal-objectives`, `/objectives`, `/evaluations`, `/badges`, `/rewards` (+ `/reward-certificates`), `/quest`, `/weekly-challenges` |
| Passeports | `/passports` (+ `/public/passports`) |
| Cas pastoraux | pastoral-case (V139), `/pastorate`, `/prophetic`, `/discipline`, `/dress-codes` |
| Equipes | `/team-tasks`, `/team-gantt`, `/leave-requests`, `/referrals`, `/tickets`, `/admin-requests`, `/follow-up-requests`, `/maker-tracking`, `/succession`, `/skill-matching`, `/skills`, `/skills-matrix`, `/scoping`, `/intelligence`, `/search` (pg_trgm), `/forms`, `/surveys`, `/favorites`, `/encouragements` |
| Sante/infirmerie | health-infirmary (V141) via `/health`, `/health-observatory` |

## Finances, assets, marketplace

| Domaine | Base |
|---|---|
| Finances | `/finances`, `/payments` (+ `/payments/webhooks`), `/tontines`, `/marketplace`, `/aid`, `/currencies`, `/inventory`, `/assets` (moteur V138) — webhooks signes, simulation configurable (voir [ENV_TEMPLATE.md](ENV_TEMPLATE.md)) |

## IA & predictions

| Domaine | Base |
|---|---|
| IA | `/ai`, `/ai/module`, `/ai/credits` (quotas SaaS) · `/ai-visit-notes` · `/ai-predictions` + `/predictions` · `/kpi-narrative` · Ollama local (`OLLAMA_URL`, `AI_MODEL`) ou providers (Groq/Gemini/Mistral/HF) — fallback contextuel si indisponible |

## Plateforme / admin (TENANT_OWNER/ADMIN, SUPER_ADMIN)

| Domaine | Base |
|---|---|
| Tenants & SaaS | `/tenants` · `/platform`, `/platform/admin`, `/platform/admin/dashboard`, `/platform/admin/impersonation` (TTL 30 min, journalise) · `/admin/tenant-features`, `/admin/saas/plans`, `/admin/subscription`, `/admin/quotas`, `/ai/credits` (V144) · `/admin/dashboard`, `/admin/system-health`, `/admin/cache-stats` |
| Back-office | `/admin/settings`, `/admin/branding`, `/admin/features`, `/admin/roles`, `/admin/org`, `/admin/invitations`, `/admin/notifications`, `/admin/integrations`, `/admin/webhooks`, `/config`, `/connectors`, `/custom-fields` (V153), `/statuses` (V150), `/space-templates`, `/pages` (V65), `/permissions`, `/onboarding-wizard` |
| Invitations & acces | `/admin/invitations` (create/resend/revoke, token public rate-limite) · `/tenant-switcher` · `/auth/switch-role` |
| Conformite | `/gdpr` (export/effacement/audit) · `/compliance` · `/audit` (hash chain) · `/exports` · `/import`, `/data-migration` (moteur V164, dry-run+replay) · `/backups` |
| Integrations temps reel | SSE `/sse/entity-changes` (ex. `EntityChangeSseController`) · `/scoping` (portees global/local V146) · `/moderation` · `/api-docs` |
| Beta & feedback (V40-41) | `GET /public/meta` (public, betaMode…) · `POST /feedback` · `GET /admin/feedback…/stats` · `PATCH /admin/feedback/{id}/status` · `GET /admin/beta/status` · `POST /admin/beta/reset` (double garde prod+profil, jamais en prod) |
| Public | `/public/**`, `/public/docs`, `/directory` (annuaire opt-in `public_directory_enabled`) |

## Pagination & erreurs : voir Conventions en tete.
