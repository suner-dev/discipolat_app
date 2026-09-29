# Référence API — Discipolat

> ⚙️ **Fichier généré — ne pas éditer à la main.**
> Régénérer : `bash scripts/generate-api-docs.sh [URL]` — chemin d’export vérifié `springdoc.api-docs.path=/api-docs` (application.yml).
> Instances réelles (render.yaml) : prod `https://discipolat-api.onrender.com`, bêta `https://discipolat-beta-api.onrender.com`.
> Généré le 2026-09-29 — API `Discipolat API` v`1.0.0` : **1251 chemins, 1553 opérations, 216 tags**.

Interface interactive : `/swagger-ui.html` (même instance).
Authentification : JWT Bearer (`POST /api/v1/auth/login`) ; le tenant est porté par le jeton — voir [MULTI_TENANT_ARCHITECTURE.md](MULTI_TENANT_ARCHITECTURE.md) et [SECURITY.md](SECURITY.md).

## Sommaire

- [admin-cache-controller](#admin-cache-controller) — 3 opérations
- [admin-integration-controller](#admin-integration-controller) — 3 opérations
- [admin-request-controller](#admin-request-controller) — 7 opérations
- [admin-stats-controller](#admin-stats-controller) — 1 opérations
- [admin-system-health-controller](#admin-system-health-controller) — 1 opérations
- [ai-assistant-controller](#ai-assistant-controller) — 7 opérations
- [ai-credits-controller](#ai-credits-controller) — 4 opérations
- [ai-module-controller](#ai-module-controller) — 9 opérations
- [ai-prediction-controller](#ai-prediction-controller) — 10 opérations
- [ai-visit-note-controller](#ai-visit-note-controller) — 8 opérations
- [alert-controller](#alert-controller) — 8 opérations
- [announcement-controller](#announcement-controller) — 18 opérations
- [api-docs-controller](#api-docs-controller) — 2 opérations
- [appointment-controller](#appointment-controller) — 4 opérations
- [asset-controller](#asset-controller) — 3 opérations
- [asset-maintenance-controller](#asset-maintenance-controller) — 4 opérations
- [audit-controller](#audit-controller) — 10 opérations
- [auth-controller](#auth-controller) — 12 opérations
- [automation-controller](#automation-controller) — 12 opérations
- [backup-controller](#backup-controller) — 6 opérations
- [badge-controller](#badge-controller) — 4 opérations
- [benchmark-controller](#benchmark-controller) — 2 opérations
- [beta-admin-controller](#beta-admin-controller) — 2 opérations
- [bible-reading-controller](#bible-reading-controller) — 14 opérations
- [branding-controller](#branding-controller) — 5 opérations
- [broadcast-controller](#broadcast-controller) — 8 opérations
- [bulk-import-controller](#bulk-import-controller) — 6 opérations
- [calendar-controller](#calendar-controller) — 7 opérations
- [cercle-faiseurs-controller](#cercle-faiseurs-controller) — 3 opérations
- [church-comparison-controller](#church-comparison-controller) — 9 opérations
- [church-event-controller](#church-event-controller) — 23 opérations
- [communication-controller](#communication-controller) — 6 opérations
- [community-controller](#community-controller) — 26 opérations
- [competence-matching-controller](#competence-matching-controller) — 8 opérations
- [compliance-controller](#compliance-controller) — 16 opérations
- [configuration-controller](#configuration-controller) — 4 opérations
- [connector-controller](#connector-controller) — 5 opérations
- [consent-controller](#consent-controller) — 2 opérations
- [currency-controller](#currency-controller) — 18 opérations
- [custom-field-controller](#custom-field-controller) — 7 opérations
- [custom-status-controller](#custom-status-controller) — 8 opérations
- [dashboard-controller](#dashboard-controller) — 12 opérations
- [data-migration-controller](#data-migration-controller) — 8 opérations
- [demo-request-controller](#demo-request-controller) — 2 opérations
- [department-controller](#department-controller) — 11 opérations
- [department-event-attendance-controller](#department-event-attendance-controller) — 4 opérations
- [department-kpi-controller](#department-kpi-controller) — 6 opérations
- [department-management-controller](#department-management-controller) — 69 opérations
- [development-plan-controller](#development-plan-controller) — 8 opérations
- [dictionary-controller](#dictionary-controller) — 7 opérations
- [digital-twin-controller](#digital-twin-controller) — 2 opérations
- [directory-controller](#directory-controller) — 5 opérations
- [discipleship-path-controller](#discipleship-path-controller) — 12 opérations
- [document-controller](#document-controller) — 2 opérations
- [dress-code-controller](#dress-code-controller) — 6 opérations
- [emergency-aid-controller](#emergency-aid-controller) — 7 opérations
- [encouragement-controller](#encouragement-controller) — 5 opérations
- [endpoint-usage-controller](#endpoint-usage-controller) — 1 opérations
- [engagement-analytics-controller](#engagement-analytics-controller) — 8 opérations
- [enhanced-message-controller](#enhanced-message-controller) — 16 opérations
- [entity-change-sse-controller](#entity-change-sse-controller) — 2 opérations
- [evaluation-controller](#evaluation-controller) — 9 opérations
- [evangelism-controller](#evangelism-controller) — 6 opérations
- [evangelism-scoring-controller](#evangelism-scoring-controller) — 2 opérations
- [event-checklist-controller](#event-checklist-controller) — 14 opérations
- [event-controller](#event-controller) — 23 opérations
- [executive-insights-controller](#executive-insights-controller) — 5 opérations
- [export-controller](#export-controller) — 6 opérations
- [face-recognition-controller](#face-recognition-controller) — 7 opérations
- [family-cohesion-controller](#family-cohesion-controller) — 3 opérations
- [family-controller](#family-controller) — 16 opérations
- [family-meeting-controller](#family-meeting-controller) — 2 opérations
- [family-os-controller](#family-os-controller) — 11 opérations
- [family-resource-controller](#family-resource-controller) — 5 opérations
- [favorite-controller](#favorite-controller) — 3 opérations
- [feedback-controller](#feedback-controller) — 4 opérations
- [file-controller](#file-controller) — 6 opérations
- [finance-controller](#finance-controller) — 9 opérations
- [follow-up-request-controller](#follow-up-request-controller) — 7 opérations
- [form-controller](#form-controller) — 11 opérations
- [gdpr-controller](#gdpr-controller) — 5 opérations
- [geofencing-controller](#geofencing-controller) — 6 opérations
- [group-message-controller](#group-message-controller) — 12 opérations
- [growth-projection-controller](#growth-projection-controller) — 6 opérations
- [health-controller](#health-controller) — 22 opérations
- [health-observatory-controller](#health-observatory-controller) — 3 opérations
- [impersonation-controller](#impersonation-controller) — 2 opérations
- [import-controller](#import-controller) — 2 opérations
- [intelligence-controller](#intelligence-controller) — 6 opérations
- [interaction-controller](#interaction-controller) — 4 opérations
- [inventory-controller](#inventory-controller) — 10 opérations
- [invitation-controller](#invitation-controller) — 7 opérations
- [kingdom-mapping-controller](#kingdom-mapping-controller) — 2 opérations
- [kpi-narrative-controller](#kpi-narrative-controller) — 5 opérations
- [leave-request-controller](#leave-request-controller) — 6 opérations
- [legal-admin-controller](#legal-admin-controller) — 2 opérations
- [live-stream-controller](#live-stream-controller) — 6 opérations
- [load-prediction-controller](#load-prediction-controller) — 1 opérations
- [maker-report-controller](#maker-report-controller) — 13 opérations
- [maker-tracking-controller](#maker-tracking-controller) — 6 opérations
- [map-controller](#map-controller) — 2 opérations
- [marketplace-controller](#marketplace-controller) — 7 opérations
- [me-controller](#me-controller) — 1 opérations
- [member-controller](#member-controller) — 15 opérations
- [mentoring-controller](#mentoring-controller) — 7 opérations
- [message-controller](#message-controller) — 6 opérations
- [moderation-controller](#moderation-controller) — 5 opérations
- [module-catalog-controller](#module-catalog-controller) — 13 opérations
- [module-feature-controller](#module-feature-controller) — 6 opérations
- [neighborhood-health-controller](#neighborhood-health-controller) — 1 opérations
- [network-controller](#network-controller) — 22 opérations
- [notification-controller](#notification-controller) — 4 opérations
- [notification-preference-controller](#notification-preference-controller) — 2 opérations
- [notification-template-controller](#notification-template-controller) — 6 opérations
- [objective-controller](#objective-controller) — 5 opérations
- [onboarding-wizard-controller](#onboarding-wizard-controller) — 8 opérations
- [organization-hierarchy-controller](#organization-hierarchy-controller) — 29 opérations
- [organization-management-controller](#organization-management-controller) — 8 opérations
- [page-builder-controller](#page-builder-controller) — 9 opérations
- [parallel-followup-controller](#parallel-followup-controller) — 5 opérations
- [passport-controller](#passport-controller) — 8 opérations
- [passport-public-controller](#passport-public-controller) — 1 opérations
- [pastoral-visit-controller](#pastoral-visit-controller) — 6 opérations
- [pastorate-controller](#pastorate-controller) — 10 opérations
- [payment-controller](#payment-controller) — 13 opérations
- [payment-webhook-controller](#payment-webhook-controller) — 6 opérations
- [people-controller](#people-controller) — 13 opérations
- [permission-controller](#permission-controller) — 10 opérations
- [personal-objective-controller](#personal-objective-controller) — 6 opérations
- [platform-config-controller](#platform-config-controller) — 13 opérations
- [platform-currencies-controller](#platform-currencies-controller) — 1 opérations
- [platform-meta-controller](#platform-meta-controller) — 1 opérations
- [platform-provisioning-controller](#platform-provisioning-controller) — 4 opérations
- [platform-quota-usage-controller](#platform-quota-usage-controller) — 2 opérations
- [prayer-controller](#prayer-controller) — 8 opérations
- [prayer-journal-controller](#prayer-journal-controller) — 7 opérations
- [prediction-controller](#prediction-controller) — 8 opérations
- [program-controller](#program-controller) — 6 opérations
- [prophetic-journal-controller](#prophetic-journal-controller) — 10 opérations
- [public-api-docs-controller](#public-api-docs-controller) — 2 opérations
- [public-billing-controller](#public-billing-controller) — 1 opérations
- [public-churches-controller](#public-churches-controller) — 1 opérations
- [public-contact-controller](#public-contact-controller) — 1 opérations
- [public-legal-controller](#public-legal-controller) — 2 opérations
- [public-saas-plan-controller](#public-saas-plan-controller) — 1 opérations
- [push-token-controller](#push-token-controller) — 3 opérations
- [quest-controller](#quest-controller) — 9 opérations
- [quota-controller](#quota-controller) — 4 opérations
- [referral-controller](#referral-controller) — 6 opérations
- [report-export-controller](#report-export-controller) — 5 opérations
- [resource-scope-controller](#resource-scope-controller) — 2 opérations
- [reverse-mentoring-controller](#reverse-mentoring-controller) — 12 opérations
- [reward-certificate-controller](#reward-certificate-controller) — 4 opérations
- [reward-controller](#reward-controller) — 4 opérations
- [role-management-controller](#role-management-controller) — 27 opérations
- [sabbath-dashboard-controller](#sabbath-dashboard-controller) — 1 opérations
- [search-controller](#search-controller) — 3 opérations
- [sermon-assistant-controller](#sermon-assistant-controller) — 1 opérations
- [sermon-transcription-controller](#sermon-transcription-controller) — 7 opérations
- [sermon-translation-controller](#sermon-translation-controller) — 5 opérations
- [settings-controller](#settings-controller) — 4 opérations
- [skill-controller](#skill-controller) — 4 opérations
- [skill-match-controller](#skill-match-controller) — 6 opérations
- [skills-matrix-controller](#skills-matrix-controller) — 5 opérations
- [smart-alert-controller](#smart-alert-controller) — 2 opérations
- [social-auth-controller](#social-auth-controller) — 3 opérations
- [soul-controller](#soul-controller) — 27 opérations
- [soul-discipline-event-controller](#soul-discipline-event-controller) — 6 opérations
- [soul-note-controller](#soul-note-controller) — 4 opérations
- [soul-tag-controller](#soul-tag-controller) — 4 opérations
- [space-controller](#space-controller) — 8 opérations
- [space-export-controller](#space-export-controller) — 3 opérations
- [space-import-controller](#space-import-controller) — 1 opérations
- [space-template-controller](#space-template-controller) — 8 opérations
- [spiritual-challenge-controller](#spiritual-challenge-controller) — 6 opérations
- [spiritual-journal-controller](#spiritual-journal-controller) — 9 opérations
- [spiritual-journey-controller](#spiritual-journey-controller) — 1 opérations
- [stream-chat-controller](#stream-chat-controller) — 6 opérations
- [stripe-billing-controller](#stripe-billing-controller) — 4 opérations
- [stripe-webhook-controller](#stripe-webhook-controller) — 1 opérations
- [subscription-controller](#subscription-controller) — 7 opérations
- [succession-plan-controller](#succession-plan-controller) — 6 opérations
- [super-admin-controller](#super-admin-controller) — 23 opérations
- [super-admin-saas-plan-controller](#super-admin-saas-plan-controller) — 8 opérations
- [survey-controller](#survey-controller) — 6 opérations
- [system-config-summary-controller](#system-config-summary-controller) — 1 opérations
- [team-assignment-controller](#team-assignment-controller) — 7 opérations
- [team-task-controller](#team-task-controller) — 6 opérations
- [tenant-admin-controller](#tenant-admin-controller) — 5 opérations
- [tenant-admin-dashboard-controller](#tenant-admin-dashboard-controller) — 6 opérations
- [tenant-controller](#tenant-controller) — 7 opérations
- [tenant-feature-controller](#tenant-feature-controller) — 4 opérations
- [tenant-settings-controller](#tenant-settings-controller) — 5 opérations
- [tenant-switcher-controller](#tenant-switcher-controller) — 5 opérations
- [testimony-controller](#testimony-controller) — 6 opérations
- [ticket-controller](#ticket-controller) — 5 opérations
- [tontine-controller](#tontine-controller) — 10 opérations
- [training-controller](#training-controller) — 13 opérations
- [transfer-admin-controller](#transfer-admin-controller) — 6 opérations
- [transfer-controller](#transfer-controller) — 11 opérations
- [two-factor-controller](#two-factor-controller) — 4 opérations
- [usage-analytics-controller](#usage-analytics-controller) — 2 opérations
- [user-controller](#user-controller) — 26 opérations
- [ussd-controller](#ussd-controller) — 3 opérations
- [visit-controller](#visit-controller) — 7 opérations
- [voice-assistant-controller](#voice-assistant-controller) — 7 opérations
- [voice-notification-controller](#voice-notification-controller) — 4 opérations
- [voice-report-controller](#voice-report-controller) — 5 opérations
- [volunteer-controller](#volunteer-controller) — 7 opérations
- [webhook-controller](#webhook-controller) — 8 opérations
- [weekly-challenge-controller](#weekly-challenge-controller) — 6 opérations
- [whats-app-admin-controller](#whats-app-admin-controller) — 6 opérations
- [whats-app-webhook-controller](#whats-app-webhook-controller) — 2 opérations
- [workflow-automation-controller](#workflow-automation-controller) — 5 opérations
- [workflow-config-controller](#workflow-config-controller) — 4 opérations
- [workflow-engine-controller](#workflow-engine-controller) — 20 opérations

## admin-cache-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/cache-stats` | getCacheStats | — |
| `DELETE` | `/api/v1/admin/cache-stats` | evictAllCaches | — |
| `DELETE` | `/api/v1/admin/cache-stats/{name}` | evictCache | `name` *(req)* (path) |

## admin-integration-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/integrations/{category}` | getConfig_1 | `category` *(req)* (path) |
| `PUT` | `/api/v1/admin/integrations/{category}` | saveConfig_1 | `category` *(req)* (path) |
| `POST` | `/api/v1/admin/integrations/{category}/test` | testConnection_1 | `category` *(req)* (path) |

## admin-request-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin-requests` | list_54 | — |
| `POST` | `/api/v1/admin-requests` | create_74 | — |
| `GET` | `/api/v1/admin-requests/by-member/{membreId}` | listByMember_2 | `membreId` *(req)* (path) |
| `GET` | `/api/v1/admin-requests/stats` | stats_44 | — |
| `GET` | `/api/v1/admin-requests/{id}` | get_38 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/admin-requests/{id}` | delete_40 | `id` *(req)* (path) |
| `POST` | `/api/v1/admin-requests/{id}/process` | process | `id` *(req)* (path), `decision` *(req)* (query), `traiteurId` *(req)* (query), `commentaire` (query) |

## admin-stats-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/stats/overview` | statsOverview | `period` (query) |

## admin-system-health-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/system-health` | getSystemHealth_1 | — |

## ai-assistant-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/ai/analyze/{soulId}` | analyze_1 | `soulId` *(req)* (path) |
| `POST` | `/api/v1/ai/chat` | chat_1 | — |
| `GET` | `/api/v1/ai/chat/history` | chatHistory | — |
| `DELETE` | `/api/v1/ai/chat/history` | clearChatHistory | — |
| `GET` | `/api/v1/ai/context` | context | `query` *(req)* (query) |
| `GET` | `/api/v1/ai/encouragement/{soulId}` | encouragement_1 | `soulId` *(req)* (path) |
| `GET` | `/api/v1/ai/health` | health_1 | — |

## ai-credits-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/ai/credits/daily-usage` | getDailyUsage | `from` (query), `to` (query) |
| `GET` | `/api/v1/ai/credits/dashboard` | getDashboard_1 | `from` (query), `to` (query) |
| `GET` | `/api/v1/ai/credits/my-usage` | getMyUsage | — |
| `GET` | `/api/v1/ai/credits/tenant-usage` | getTenantUsage | `page` (query), `size` (query) |

## ai-module-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/ai/module/chat` | chat | — |
| `GET` | `/api/v1/ai/module/families/cohesion` | analyzeAllFamilies | — |
| `GET` | `/api/v1/ai/module/family/{familyId}/cohesion` | analyzeFamily | `familyId` *(req)* (path) |
| `GET` | `/api/v1/ai/module/kpi-narrative` | getKpiNarrative | — |
| `GET` | `/api/v1/ai/module/providers` | getProviders | — |
| `POST` | `/api/v1/ai/module/sermon/generate` | generateSermon | — |
| `GET` | `/api/v1/ai/module/soul/{soulId}/analyze` | analyzeSoul | `soulId` *(req)* (path) |
| `GET` | `/api/v1/ai/module/soul/{soulId}/encouragement` | encouragement | `soulId` *(req)* (path) |
| `GET` | `/api/v1/ai/module/summary` | getSummary_1 | — |

## ai-prediction-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/ai-predictions` | list_50 | — |
| `POST` | `/api/ai-predictions` | save_1 | — |
| `POST` | `/api/ai-predictions/generate` | generate_9 | — |
| `GET` | `/api/ai-predictions/risks` | listRisks | — |
| `GET` | `/api/ai-predictions/type/{type}` | listByType_5 | `type` *(req)* (path) |
| `GET` | `/api/v1/ai-predictions` | list_51 | — |
| `POST` | `/api/v1/ai-predictions` | save_2 | — |
| `POST` | `/api/v1/ai-predictions/generate` | generate_8 | — |
| `GET` | `/api/v1/ai-predictions/risks` | listRisks_1 | — |
| `GET` | `/api/v1/ai-predictions/type/{type}` | listByType_4 | `type` *(req)* (path) |

## ai-visit-note-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/ai-visit-notes` | list_48 | — |
| `POST` | `/api/ai-visit-notes` | create_68 | — |
| `GET` | `/api/ai-visit-notes/member/{memberId}` | byMember_2 | `memberId` *(req)* (path) |
| `POST` | `/api/ai-visit-notes/{id}/verify` | verify_3 | `id` *(req)* (path) |
| `GET` | `/api/v1/ai-visit-notes` | list_49 | — |
| `POST` | `/api/v1/ai-visit-notes` | create_69 | — |
| `GET` | `/api/v1/ai-visit-notes/member/{memberId}` | byMember_1 | `memberId` *(req)* (path) |
| `POST` | `/api/v1/ai-visit-notes/{id}/verify` | verify_4 | `id` *(req)* (path) |

## alert-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/alerts` | findAll_14 | `page` (query), `size` (query), `statut` (query), `familleId` (query) |
| `POST` | `/api/v1/alerts` | createManual | — |
| `GET` | `/api/v1/alerts/active` | findActive_2 | `page` (query), `size` (query) |
| `POST` | `/api/v1/alerts/resolve-batch` | resolveBatch | — |
| `GET` | `/api/v1/alerts/stats` | getStats_3 | — |
| `GET` | `/api/v1/alerts/{id}` | findById_15 | `id` *(req)* (path) |
| `POST` | `/api/v1/alerts/{id}/acknowledge` | acknowledge | `id` *(req)* (path) |
| `PATCH` | `/api/v1/alerts/{id}/resolve` | resolve_4 | `id` *(req)* (path) |

## announcement-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/announcements` | list_47 | — |
| `POST` | `/api/announcements` | create_67 | — |
| `GET` | `/api/announcements/stats` | stats_41 | — |
| `GET` | `/api/announcements/{id}` | get_12 | `id` *(req)* (path) |
| `PUT` | `/api/announcements/{id}` | update_32 | `id` *(req)* (path) |
| `DELETE` | `/api/announcements/{id}` | delete_24 | `id` *(req)* (path) |
| `POST` | `/api/announcements/{id}/cancel` | cancel_3 | `id` *(req)* (path) |
| `POST` | `/api/announcements/{id}/publish` | publish_3 | `id` *(req)* (path) |
| `POST` | `/api/announcements/{id}/schedule` | schedule | `id` *(req)* (path) |
| `GET` | `/api/v1/announcements` | list_46 | — |
| `POST` | `/api/v1/announcements` | create_66 | — |
| `GET` | `/api/v1/announcements/stats` | stats_42 | — |
| `GET` | `/api/v1/announcements/{id}` | get_13 | `id` *(req)* (path) |
| `PUT` | `/api/v1/announcements/{id}` | update_33 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/announcements/{id}` | delete_25 | `id` *(req)* (path) |
| `POST` | `/api/v1/announcements/{id}/cancel` | cancel_4 | `id` *(req)* (path) |
| `POST` | `/api/v1/announcements/{id}/publish` | publish_2 | `id` *(req)* (path) |
| `POST` | `/api/v1/announcements/{id}/schedule` | schedule_1 | `id` *(req)* (path) |

## api-docs-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/api-docs` | getApiDocs | — |
| `GET` | `/api/v1/api-docs/openapi.yaml` | getOpenApiSpec | — |

## appointment-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/appointments` | create_65 | — |
| `GET` | `/api/v1/appointments/inbox` | myInbox | — |
| `GET` | `/api/v1/appointments/my` | myRequests_1 | — |
| `PATCH` | `/api/v1/appointments/{id}/status` | updateStatus_10 | `id` *(req)* (path) |

## asset-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/assets/checkouts` | listCheckouts | `status` (query), `memberId` (query) |
| `POST` | `/api/v1/assets/{itemId}/checkout` | checkout_1 | `itemId` *(req)* (path) |
| `POST` | `/api/v1/assets/{itemId}/return` | returnAsset | `itemId` *(req)* (path) |

## asset-maintenance-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `PUT` | `/api/v1/assets/maintenance/{id}` | updateMaintenance | `id` *(req)* (path) |
| `GET` | `/api/v1/assets/{itemId}/maintenance` | getMaintenanceHistory | `itemId` *(req)* (path) |
| `POST` | `/api/v1/assets/{itemId}/maintenance` | scheduleMaintenance | `itemId` *(req)* (path) |
| `GET` | `/api/v1/assets/{itemId}/tco` | getTco | `itemId` *(req)* (path) |

## audit-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/audit` | findAll_18 | `page` (query), `size` (query), `utilisateurId` (query), `entiteType` (query), `action` (query), `debut` (query), `fin` (query) |
| `GET` | `/api/v1/audit/events` | findEvents | `page` (query), `size` (query), `actorId` (query), `entity` (query), `action` (query), `from` (query), `to` (query) |
| `GET` | `/api/v1/audit/events/export` | exportEventsCsv | `actorId` (query), `entity` (query), `action` (query), `from` (query), `to` (query) |
| `GET` | `/api/v1/audit/events/verify-chain` | verifyChain | — |
| `GET` | `/api/v1/audit/export` | exportCsv | `utilisateurId` (query), `entiteType` (query), `action` (query), `debut` (query), `fin` (query) |
| `GET` | `/api/v1/audit/history/space/{spaceId}` | getSpaceHistory | `spaceId` *(req)* (path) |
| `GET` | `/api/v1/audit/history/{objectType}/{objectId}` | getObjectHistory | `objectType` *(req)* (path), `objectId` *(req)* (path) |
| `GET` | `/api/v1/audit/recent` | getRecentActivity | `limit` (query) |
| `GET` | `/api/v1/audit/trend` | getAuditTrend | `jours` (query) |
| `GET` | `/api/v1/audit/{id}` | findById_14 | `id` *(req)* (path) |

## auth-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/auth/activate` | activateAccount | — |
| `POST` | `/api/v1/auth/change-password` | changePassword | — |
| `POST` | `/api/v1/auth/forgot-password` | forgotPassword | — |
| `POST` | `/api/v1/auth/login` | login | — |
| `POST` | `/api/v1/auth/logout` | logout | — |
| `GET` | `/api/v1/auth/me` | me_1 | — |
| `POST` | `/api/v1/auth/refresh` | refresh | — |
| `POST` | `/api/v1/auth/register` | register_1 | — |
| `POST` | `/api/v1/auth/registration-status` | registrationStatus | — |
| `POST` | `/api/v1/auth/resend-activation` | resendActivation | — |
| `POST` | `/api/v1/auth/reset-password` | resetPassword | — |
| `POST` | `/api/v1/auth/switch-role` | switchRole | — |

## automation-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/automations` | listRules | `page` (query), `size` (query) |
| `POST` | `/api/v1/automations` | createRule | — |
| `GET` | `/api/v1/automations/actions` | listActions | — |
| `GET` | `/api/v1/automations/active` | listActive | — |
| `GET` | `/api/v1/automations/stats` | stats_40 | — |
| `POST` | `/api/v1/automations/trigger/{event}` | trigger | `event` *(req)* (path) |
| `GET` | `/api/v1/automations/triggers` | listTriggers | — |
| `GET` | `/api/v1/automations/{id}` | getRule | `id` *(req)* (path) |
| `PATCH` | `/api/v1/automations/{id}` | updateRule | `id` *(req)* (path) |
| `DELETE` | `/api/v1/automations/{id}` | deleteRule | `id` *(req)* (path) |
| `GET` | `/api/v1/automations/{id}/executions` | listExecutions | `id` *(req)* (path) |
| `PATCH` | `/api/v1/automations/{id}/toggle` | toggleRule | `id` *(req)* (path) |

## backup-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/backups` | list_45 | — |
| `POST` | `/api/v1/backups` | create_64 | — |
| `GET` | `/api/v1/backups/{id}` | get_37 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/backups/{id}` | delete_39 | `id` *(req)* (path) |
| `GET` | `/api/v1/backups/{id}/download` | download_2 | `id` *(req)* (path) |
| `POST` | `/api/v1/backups/{id}/verify` | verify_1 | `id` *(req)* (path) |

## badge-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/badges/evaluate` | evaluate_1 | — |
| `GET` | `/api/v1/badges/leaderboard` | leaderboard_1 | — |
| `GET` | `/api/v1/badges/my` | myBadges | — |
| `GET` | `/api/v1/badges/users/{userId}` | userBadges | `userId` *(req)* (path) |

## benchmark-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/benchmark` | getBenchmark | — |
| `GET` | `/api/v1/benchmark/trends` | getTrends | — |

## beta-admin-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/admin/beta/reset` | reset_3 | — |
| `GET` | `/api/v1/admin/beta/status` | status_7 | — |

## bible-reading-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/bible-reading/entries` | createEntry | — |
| `GET` | `/api/v1/bible-reading/entries/{id}` | getEntry | `id` *(req)* (path) |
| `DELETE` | `/api/v1/bible-reading/entries/{id}` | deleteEntry | `id` *(req)* (path) |
| `POST` | `/api/v1/bible-reading/entries/{id}/mark-read` | markRead_1 | `id` *(req)* (path) |
| `PUT` | `/api/v1/bible-reading/entries/{id}/note` | addNote | `id` *(req)* (path) |
| `GET` | `/api/v1/bible-reading/family-progress` | familyProgress | — |
| `GET` | `/api/v1/bible-reading/plans` | listPlans_1 | — |
| `POST` | `/api/v1/bible-reading/plans` | createPlan | — |
| `GET` | `/api/v1/bible-reading/plans/{id}` | getPlan | `id` *(req)* (path) |
| `PUT` | `/api/v1/bible-reading/plans/{id}` | updatePlan | `id` *(req)* (path) |
| `DELETE` | `/api/v1/bible-reading/plans/{id}` | deletePlan | `id` *(req)* (path) |
| `GET` | `/api/v1/bible-reading/plans/{planId}/entries` | listEntries | `planId` *(req)* (path) |
| `GET` | `/api/v1/bible-reading/stats` | stats_39 | — |
| `GET` | `/api/v1/bible-reading/today` | today | — |

## branding-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/branding` | getBranding | — |
| `PUT` | `/api/v1/admin/branding` | updateBranding | — |
| `POST` | `/api/v1/admin/branding/assets` | uploadBrandingAsset_1 | `assetType` *(req)* (query) |
| `GET` | `/api/v1/admin/branding/css` | getBrandingCss_1 | — |
| `GET` | `/api/v1/admin/branding/public` | getPublicBranding_1 | — |

## broadcast-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/broadcast` | list_44 | `page` (query), `size` (query), `statut` (query) |
| `POST` | `/api/v1/broadcast` | create_63 | — |
| `GET` | `/api/v1/broadcast/stats` | getStats_2 | — |
| `GET` | `/api/v1/broadcast/{id}` | get_36 | `id` *(req)* (path) |
| `POST` | `/api/v1/broadcast/{id}/read` | markAsRead | `id` *(req)* (path) |
| `GET` | `/api/v1/broadcast/{id}/receipts` | getReceipts | `id` *(req)* (path) |
| `PATCH` | `/api/v1/broadcast/{id}/schedule` | schedule_2 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/broadcast/{id}/send` | send_5 | `id` *(req)* (path) |

## bulk-import-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/import/families` | importFamilies | — |
| `POST` | `/api/v1/import/souls` | importSouls | — |
| `POST` | `/api/v1/import/users` | importUsers | — |
| `POST` | `/api/v1/import/validate/families` | validateFamilies | — |
| `POST` | `/api/v1/import/validate/souls` | validateSouls | — |
| `POST` | `/api/v1/import/validate/users` | validateUsers | — |

## calendar-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/calendar` | list_43 | `start` (query), `end` (query) |
| `POST` | `/api/v1/calendar` | create_62 | — |
| `GET` | `/api/v1/calendar/feed.ics` | getICalFeed | — |
| `GET` | `/api/v1/calendar/{id}` | get_35 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/calendar/{id}` | delete_38 | `id` *(req)* (path) |
| `GET` | `/api/v1/calendar/{id}/ical` | getICal | `id` *(req)* (path) |
| `PATCH` | `/api/v1/calendar/{id}/status` | updateStatus_9 | `id` *(req)* (path) |

## cercle-faiseurs-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/cercle-faiseurs` | list_42 | `categorie` (query) |
| `POST` | `/api/v1/cercle-faiseurs` | create_61 | — |
| `POST` | `/api/v1/cercle-faiseurs/{id}/like` | like_1 | `id` *(req)* (path) |

## church-comparison-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/church-comparisons` | list_41 | — |
| `POST` | `/api/v1/church-comparisons` | create_60 | — |
| `GET` | `/api/v1/church-comparisons/by-category/{cat}` | listByCategory_7 | `cat` *(req)* (path) |
| `GET` | `/api/v1/church-comparisons/by-country/{pays}` | listByCountry_1 | `pays` *(req)* (path) |
| `GET` | `/api/v1/church-comparisons/clusters` | clusters | `ourId` (query) |
| `GET` | `/api/v1/church-comparisons/{id}` | get_11 | `id` *(req)* (path) |
| `PUT` | `/api/v1/church-comparisons/{id}` | update_31 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/church-comparisons/{id}` | delete_23 | `id` *(req)* (path) |
| `GET` | `/api/v1/church-comparisons/{id}/benchmark` | benchmark | `id` *(req)* (path) |

## church-event-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/church-events` | getEvents | `page` (query), `size` (query), `status` (query) |
| `POST` | `/api/v1/church-events` | createEvent_1 | — |
| `GET` | `/api/v1/church-events/calendar` | getCalendar | `from` *(req)* (query), `to` *(req)* (query) |
| `GET` | `/api/v1/church-events/locations` | getLocations | — |
| `POST` | `/api/v1/church-events/locations` | createLocation | — |
| `PUT` | `/api/v1/church-events/tasks/{taskId}/status` | updateTaskStatus | `taskId` *(req)* (path), `status` *(req)* (query) |
| `GET` | `/api/v1/church-events/{eventId}/attendance` | getAttendance | `eventId` *(req)* (path) |
| `GET` | `/api/v1/church-events/{eventId}/attendance/count` | getPresentCount | `eventId` *(req)* (path) |
| `POST` | `/api/v1/church-events/{eventId}/checkin` | checkIn_1 | `eventId` *(req)* (path), `personId` *(req)* (query), `method` (query), `spaceId` (query) |
| `POST` | `/api/v1/church-events/{eventId}/checkout` | checkOut_1 | `eventId` *(req)* (path), `personId` *(req)* (query) |
| `POST` | `/api/v1/church-events/{eventId}/flash` | flashAttendance | `eventId` *(req)* (path), `phone` *(req)* (query) |
| `GET` | `/api/v1/church-events/{eventId}/schedule` | getSchedule | `eventId` *(req)* (path) |
| `POST` | `/api/v1/church-events/{eventId}/schedule` | addScheduleItem | `eventId` *(req)* (path) |
| `GET` | `/api/v1/church-events/{eventId}/spaces` | getSpaces | `eventId` *(req)* (path) |
| `POST` | `/api/v1/church-events/{eventId}/spaces` | addSpace | `eventId` *(req)* (path), `spaceId` *(req)* (query), `role` (query) |
| `DELETE` | `/api/v1/church-events/{eventId}/spaces/{spaceId}` | removeSpace | `eventId` *(req)* (path), `spaceId` *(req)* (path) |
| `GET` | `/api/v1/church-events/{eventId}/tasks` | getTasks | `eventId` *(req)* (path) |
| `POST` | `/api/v1/church-events/{eventId}/tasks` | createTask_1 | `eventId` *(req)* (path) |
| `GET` | `/api/v1/church-events/{eventId}/teams` | getTeams | `eventId` *(req)* (path) |
| `POST` | `/api/v1/church-events/{eventId}/teams` | createTeam_1 | `eventId` *(req)* (path) |
| `GET` | `/api/v1/church-events/{id}` | getEvent | `id` *(req)* (path) |
| `PUT` | `/api/v1/church-events/{id}` | updateEvent | `id` *(req)* (path) |
| `DELETE` | `/api/v1/church-events/{id}` | deleteEvent | `id` *(req)* (path) |

## communication-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/communications` | listPublished_1 | — |
| `GET` | `/api/v1/communications/admin` | listAll_6 | — |
| `POST` | `/api/v1/communications/admin` | create_59 | — |
| `PUT` | `/api/v1/communications/admin/{id}` | update_30 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/communications/admin/{id}` | delete_22 | `id` *(req)* (path) |
| `POST` | `/api/v1/communications/admin/{id}/publish` | publish_1 | `id` *(req)* (path) |

## community-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/communities` | list_39 | — |
| `POST` | `/api/communities` | create_57 | — |
| `GET` | `/api/communities/category/{category}` | listByCategory_5 | `category` *(req)* (path) |
| `GET` | `/api/communities/{communityId}/posts` | listPosts_1 | `communityId` *(req)* (path) |
| `POST` | `/api/communities/{communityId}/posts` | createPost_1 | `communityId` *(req)* (path) |
| `PUT` | `/api/communities/{communityId}/posts/{postId}` | updatePost_1 | `communityId` *(req)* (path), `postId` *(req)* (path) |
| `DELETE` | `/api/communities/{communityId}/posts/{postId}` | deletePost_1 | `communityId` *(req)* (path), `postId` *(req)* (path) |
| `POST` | `/api/communities/{communityId}/posts/{postId}/like` | likePost | `communityId` *(req)* (path), `postId` *(req)* (path) |
| `POST` | `/api/communities/{communityId}/posts/{postId}/pin` | pinPost_1 | `communityId` *(req)* (path), `postId` *(req)* (path) |
| `POST` | `/api/communities/{communityId}/posts/{postId}/unpin` | unpinPost | `communityId` *(req)* (path), `postId` *(req)* (path) |
| `GET` | `/api/communities/{id}` | getById_1 | `id` *(req)* (path) |
| `PUT` | `/api/communities/{id}` | update_28 | `id` *(req)* (path) |
| `DELETE` | `/api/communities/{id}` | delete_20 | `id` *(req)* (path) |
| `GET` | `/api/v1/communities` | list_40 | — |
| `POST` | `/api/v1/communities` | create_58 | — |
| `GET` | `/api/v1/communities/category/{category}` | listByCategory_6 | `category` *(req)* (path) |
| `GET` | `/api/v1/communities/{communityId}/posts` | listPosts | `communityId` *(req)* (path) |
| `POST` | `/api/v1/communities/{communityId}/posts` | createPost | `communityId` *(req)* (path) |
| `PUT` | `/api/v1/communities/{communityId}/posts/{postId}` | updatePost | `communityId` *(req)* (path), `postId` *(req)* (path) |
| `DELETE` | `/api/v1/communities/{communityId}/posts/{postId}` | deletePost | `communityId` *(req)* (path), `postId` *(req)* (path) |
| `POST` | `/api/v1/communities/{communityId}/posts/{postId}/like` | likePost_1 | `communityId` *(req)* (path), `postId` *(req)* (path) |
| `POST` | `/api/v1/communities/{communityId}/posts/{postId}/pin` | pinPost | `communityId` *(req)* (path), `postId` *(req)* (path) |
| `POST` | `/api/v1/communities/{communityId}/posts/{postId}/unpin` | unpinPost_1 | `communityId` *(req)* (path), `postId` *(req)* (path) |
| `GET` | `/api/v1/communities/{id}` | getById_2 | `id` *(req)* (path) |
| `PUT` | `/api/v1/communities/{id}` | update_29 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/communities/{id}` | delete_21 | `id` *(req)* (path) |

## competence-matching-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/members/competences` | addCompetence | — |
| `POST` | `/api/v1/members/competences/match` | match | `minLevel` (query) |
| `GET` | `/api/v1/members/competences/mine` | myCompetences | — |
| `GET` | `/api/v1/members/competences/search` | search_3 | `competenceName` *(req)* (query) |
| `GET` | `/api/v1/members/competences/stats` | stats_22 | — |
| `GET` | `/api/v1/members/competences/user/{userId}` | userCompetences | `userId` *(req)* (path) |
| `PUT` | `/api/v1/members/competences/{id}` | updateCompetence | `id` *(req)* (path) |
| `DELETE` | `/api/v1/members/competences/{id}` | deleteCompetence | `id` *(req)* (path) |

## compliance-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/compliance/audit-hash/verify` | verifyAuditHash | — |
| `POST` | `/api/v1/compliance/consent` | logConsent | — |
| `GET` | `/api/v1/compliance/exports` | exports | — |
| `POST` | `/api/v1/compliance/exports` | createExport_1 | — |
| `GET` | `/api/v1/compliance/gdpr` | listRequests | `page` (query), `size` (query) |
| `POST` | `/api/v1/compliance/gdpr` | createRequest_1 | — |
| `GET` | `/api/v1/compliance/gdpr/{id}` | getRequest | `id` *(req)* (path) |
| `PATCH` | `/api/v1/compliance/gdpr/{id}/process` | processRequest_1 | `id` *(req)* (path) |
| `GET` | `/api/v1/compliance/portability/{userId}` | portability | `userId` *(req)* (path), `format` (query) |
| `GET` | `/api/v1/compliance/retention-policies` | retentionPolicies | — |
| `PUT` | `/api/v1/compliance/retention-policies` | setRetentionPolicy | — |
| `POST` | `/api/v1/compliance/retention-policies` | createRetentionPolicy | — |
| `POST` | `/api/v1/compliance/retention-policies/purge-all` | purgeAll | — |
| `DELETE` | `/api/v1/compliance/retention-policies/{id}` | deleteRetentionPolicy | `id` *(req)* (path) |
| `POST` | `/api/v1/compliance/retention-policies/{policyId}/execute` | executeRetentionPolicy | `policyId` *(req)* (path) |
| `GET` | `/api/v1/compliance/stats` | stats_38 | — |

## configuration-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/config/inheritance/{nodeId}` | getInheritanceStatus | `nodeId` *(req)* (path) |
| `PUT` | `/api/v1/config/local/{nodeId}` | updateLocalConfig | `nodeId` *(req)* (path) |
| `GET` | `/api/v1/config/resolved` | getResolvedConfig | `unitId` *(req)* (query), `keys` (query) |
| `PUT` | `/api/v1/config/source/{nodeId}` | setConfigSource | `nodeId` *(req)* (path) |

## connector-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/connectors` | list | — |
| `PUT` | `/api/v1/connectors` | save | — |
| `POST` | `/api/v1/connectors/finance-export` | financeExport | — |
| `POST` | `/api/v1/connectors/{connector}/sync` | sync | `connector` *(req)* (path) |
| `POST` | `/api/v1/connectors/{connector}/test` | test | `connector` *(req)* (path) |

## consent-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/compliance/consents` | record_2 | — |
| `GET` | `/api/v1/compliance/consents/mine` | myConsents | — |

## currency-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/currencies` | list_38 | — |
| `POST` | `/api/currencies` | create_56 | — |
| `POST` | `/api/currencies/convert` | convert | — |
| `GET` | `/api/currencies/primary` | getPrimary_1 | — |
| `GET` | `/api/currencies/stats` | stats_36 | — |
| `GET` | `/api/currencies/supported` | getSupported_1 | — |
| `GET` | `/api/currencies/timezones` | getTimezones_1 | — |
| `PUT` | `/api/currencies/{id}` | update_26 | `id` *(req)* (path) |
| `DELETE` | `/api/currencies/{id}` | delete_18 | `id` *(req)* (path) |
| `GET` | `/api/v1/currencies` | list_37 | — |
| `POST` | `/api/v1/currencies` | create_55 | — |
| `POST` | `/api/v1/currencies/convert` | convert_1 | — |
| `GET` | `/api/v1/currencies/primary` | getPrimary | — |
| `GET` | `/api/v1/currencies/stats` | stats_37 | — |
| `GET` | `/api/v1/currencies/supported` | getSupported | — |
| `GET` | `/api/v1/currencies/timezones` | getTimezones | — |
| `PUT` | `/api/v1/currencies/{id}` | update_27 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/currencies/{id}` | delete_19 | `id` *(req)* (path) |

## custom-field-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/custom-fields/definitions` | getDefinitions | `entiteType` *(req)* (query) |
| `POST` | `/api/v1/custom-fields/definitions` | createDefinition_1 | — |
| `GET` | `/api/v1/custom-fields/definitions/all` | getAllDefinitions | `entiteType` (query) |
| `PUT` | `/api/v1/custom-fields/definitions/{id}` | updateDefinition_1 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/custom-fields/definitions/{id}` | deleteDefinition_1 | `id` *(req)* (path) |
| `GET` | `/api/v1/custom-fields/{entiteType}/{entiteId}` | getBundle | `entiteType` *(req)* (path), `entiteId` *(req)* (path) |
| `PUT` | `/api/v1/custom-fields/{entiteType}/{entiteId}` | saveValues | `entiteType` *(req)* (path), `entiteId` *(req)* (path) |

## custom-status-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/statuses` | board | `entityType` *(req)* (query), `spaceId` (query) |
| `POST` | `/api/v1/statuses` | create_16 | `spaceId` (query) |
| `POST` | `/api/v1/statuses/change` | change | `entityType` *(req)* (query), `from` *(req)* (query), `to` *(req)* (query), `entityId` *(req)* (query), `spaceId` (query) |
| `GET` | `/api/v1/statuses/set` | set | `entityType` *(req)* (query), `spaceId` (query) |
| `GET` | `/api/v1/statuses/validate-transition` | validateTransition | `entityType` *(req)* (query), `from` *(req)* (query), `to` *(req)* (query), `spaceId` (query) |
| `GET` | `/api/v1/statuses/{statusId}` | get_3 | `statusId` *(req)* (path) |
| `PUT` | `/api/v1/statuses/{statusId}` | update_5 | `statusId` *(req)* (path) |
| `DELETE` | `/api/v1/statuses/{statusId}` | delete_2 | `statusId` *(req)* (path) |

## dashboard-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/dashboard/chef-famille` | getChefFamilleDashboard | `familleId` (query) |
| `GET` | `/api/v1/dashboard/crm-faiseur` | getCrmFaiseur | — |
| `GET` | `/api/v1/dashboard/family-risk` | getFamilyRisk | `seuil` (query) |
| `GET` | `/api/v1/dashboard/kpi` | getKPI | `periodeDebut` (query), `periodeFin` (query), `departementId` (query), `familleId` (query) |
| `GET` | `/api/v1/dashboard/my-metrics` | getMyMetrics | — |
| `GET` | `/api/v1/dashboard/pasteur` | getPasteurDashboard | — |
| `GET` | `/api/v1/dashboard/pasteur/kpis` | getPasteurKpis | — |
| `GET` | `/api/v1/dashboard/pasteur/presence-trend` | getPasteurPresenceTrend | `mois` (query) |
| `GET` | `/api/v1/dashboard/presence-trend` | getPresenceTrend | `mois` (query) |
| `GET` | `/api/v1/dashboard/report-completion` | getReportCompletion | — |
| `GET` | `/api/v1/dashboard/responsable` | getResponsableDashboard | `deptId` (query) |
| `GET` | `/api/v1/dashboard/summary` | getSummary | — |

## data-migration-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/data-migration` | list_36 | — |
| `POST` | `/api/v1/data-migration` | create_54 | — |
| `POST` | `/api/v1/data-migration/analyze` | analyze | — |
| `GET` | `/api/v1/data-migration/{id}` | get_34 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/data-migration/{id}` | rollback | `id` *(req)* (path) |
| `POST` | `/api/v1/data-migration/{id}/cancel` | cancel_2 | `id` *(req)* (path) |
| `POST` | `/api/v1/data-migration/{id}/execute` | execute | `id` *(req)* (path), `dryRun` (query) |
| `POST` | `/api/v1/data-migration/{id}/replay` | replay | `id` *(req)* (path) |

## demo-request-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/demo-requests` | list_70 | — |
| `POST` | `/api/v1/public/demo-requests` | create_31 | — |

## department-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/departments` | findAll_13 | `page` (query), `size` (query), `sortBy` (query), `sortDir` (query) |
| `POST` | `/api/v1/departments` | create_52 | — |
| `GET` | `/api/v1/departments/by-responsable/{responsableId}` | findByResponsable | `responsableId` *(req)* (path) |
| `GET` | `/api/v1/departments/{id}` | findById_9 | `id` *(req)* (path) |
| `PUT` | `/api/v1/departments/{id}` | update_24 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{id}` | delete_16 | `id` *(req)* (path) |
| `GET` | `/api/v1/departments/{id}/detail` | detail_1 | `id` *(req)* (path) |
| `GET` | `/api/v1/departments/{id}/kpi` | kpi | `id` *(req)* (path) |
| `GET` | `/api/v1/departments/{id}/members` | members | `id` *(req)* (path), `page` (query), `size` (query) |
| `GET` | `/api/v1/departments/{id}/report` | report_1 | `id` *(req)* (path), `semaine` (query) |
| `GET` | `/api/v1/departments/{id}/unassigned` | unassigned | `id` *(req)* (path) |

## department-event-attendance-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/departments/{departmentId}/events/{eventId}/attendance` | eventAttendance | `departmentId` *(req)* (path), `eventId` *(req)* (path) |
| `PUT` | `/api/v1/departments/{departmentId}/events/{eventId}/attendance` | markEventAttendance | `departmentId` *(req)* (path), `eventId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/events/{eventId}/attendance/export` | exportAttendance | `departmentId` *(req)* (path), `eventId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/events/{eventId}/attendance/mark-all` | markAllAttendance | `departmentId` *(req)* (path), `eventId` *(req)* (path), `present` (query) |

## department-kpi-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/department-kpis` | listByTenant | — |
| `POST` | `/api/v1/department-kpis` | create_53 | — |
| `GET` | `/api/v1/department-kpis/department/{departmentId}` | listByDepartment_1 | `departmentId` *(req)* (path) |
| `GET` | `/api/v1/department-kpis/department/{departmentId}/computed` | getComputedKpis | `departmentId` *(req)* (path) |
| `PUT` | `/api/v1/department-kpis/{id}` | update_25 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/department-kpis/{id}` | delete_17 | `id` *(req)* (path) |

## department-management-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/departments/{departmentId}/activity` | activity | `departmentId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/alerts/smart` | smartAlerts_1 | `departmentId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/announcements` | announcements | `departmentId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/announcements` | createAnnouncement | `departmentId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/announcements/{announcementId}` | deleteAnnouncement | `departmentId` *(req)* (path), `announcementId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/assignments` | assignments | `departmentId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/assignments` | assign_2 | `departmentId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/assignments/{assignmentId}` | endAssignment | `departmentId` *(req)* (path), `assignmentId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/checklists` | checklists | `departmentId` *(req)* (path), `cibleType` (query), `cibleId` (query) |
| `POST` | `/api/v1/departments/{departmentId}/checklists` | createChecklist | `departmentId` *(req)* (path) |
| `PUT` | `/api/v1/departments/{departmentId}/checklists/{checklistId}` | updateChecklist | `departmentId` *(req)* (path), `checklistId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/checklists/{checklistId}` | deleteChecklist | `departmentId` *(req)* (path), `checklistId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/checklists/{checklistId}/items` | addChecklistItem | `departmentId` *(req)* (path), `checklistId` *(req)* (path) |
| `PUT` | `/api/v1/departments/{departmentId}/checklists/{checklistId}/items/{itemId}` | toggleChecklistItem | `departmentId` *(req)* (path), `checklistId` *(req)* (path), `itemId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/checklists/{checklistId}/items/{itemId}` | deleteChecklistItem | `departmentId` *(req)* (path), `checklistId` *(req)* (path), `itemId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/documents` | documents | `departmentId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/documents` | createDocument | `departmentId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/documents/stats` | documentStats | `departmentId` *(req)* (path) |
| `PUT` | `/api/v1/departments/{departmentId}/documents/{documentId}` | updateDocument | `departmentId` *(req)* (path), `documentId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/documents/{documentId}` | deleteDocument | `departmentId` *(req)* (path), `documentId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/equipment` | equipment | `departmentId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/equipment` | createEquipment | `departmentId` *(req)* (path) |
| `PUT` | `/api/v1/departments/{departmentId}/equipment/{equipmentId}` | updateEquipment | `departmentId` *(req)* (path), `equipmentId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/equipment/{equipmentId}` | deleteEquipment | `departmentId` *(req)* (path), `equipmentId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/management` | overview | `departmentId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/members` | addMember_2 | `departmentId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/members/candidates` | candidates | `departmentId` *(req)* (path), `q` (query) |
| `POST` | `/api/v1/departments/{departmentId}/members/create` | createMember | `departmentId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/members/export` | exportMembers | `departmentId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/members/import` | importMembers | `departmentId` *(req)* (path), `preview` (query) |
| `GET` | `/api/v1/departments/{departmentId}/members/management` | membersManagement | `departmentId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/members/{memberId}` | removeMember_1 | `departmentId` *(req)* (path), `memberId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/members/{memberId}/dossier` | memberDossier | `departmentId` *(req)* (path), `memberId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/members/{memberId}/event-attendance` | memberEventAttendance | `departmentId` *(req)* (path), `memberId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/members/{memberId}/event-attendance/export` | exportMemberEventAttendance | `departmentId` *(req)* (path), `memberId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/members/{memberId}/event-attendance/mark-all` | markAllMemberEventAttendance | `departmentId` *(req)* (path), `memberId` *(req)* (path), `present` (query) |
| `GET` | `/api/v1/departments/{departmentId}/members/{memberId}/notes` | memberNotes | `departmentId` *(req)* (path), `memberId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/members/{memberId}/notes` | addMemberNote | `departmentId` *(req)* (path), `memberId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/members/{memberId}/objectives` | memberObjectives | `departmentId` *(req)* (path), `memberId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/members/{memberId}/objectives` | createObjective | `departmentId` *(req)* (path), `memberId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/members/{memberId}/reports` | memberReports | `departmentId` *(req)* (path), `memberId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/members/{memberId}/reports` | createMemberReport | `departmentId` *(req)* (path), `memberId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/notes/{noteId}` | deleteMemberNote | `departmentId` *(req)* (path), `noteId` *(req)* (path) |
| `PUT` | `/api/v1/departments/{departmentId}/objectives/{objectiveId}` | updateObjective | `departmentId` *(req)* (path), `objectiveId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/objectives/{objectiveId}` | deleteObjective | `departmentId` *(req)* (path), `objectiveId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/positions` | positions | `departmentId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/positions` | createPosition | `departmentId` *(req)* (path) |
| `PUT` | `/api/v1/departments/{departmentId}/positions/{positionId}` | updatePosition | `departmentId` *(req)* (path), `positionId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/positions/{positionId}` | archivePosition | `departmentId` *(req)* (path), `positionId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/reports` | saveDepartmentReport | `departmentId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/reports/generate` | generateDepartmentReport | `departmentId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/reports/list` | departmentReports | `departmentId` *(req)* (path) |
| `PUT` | `/api/v1/departments/{departmentId}/reports/saved/{reportId}` | updateDepartmentReport | `departmentId` *(req)* (path), `reportId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/reports/saved/{reportId}` | deleteDepartmentReport | `departmentId` *(req)* (path), `reportId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/reports/saved/{reportId}/export` | exportDepartmentReport | `departmentId` *(req)* (path), `reportId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/reports/{reportId}` | deleteMemberReport | `departmentId` *(req)* (path), `reportId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/search` | search_6 | `departmentId` *(req)* (path), `q` (query) |
| `GET` | `/api/v1/departments/{departmentId}/settings` | settings | `departmentId` *(req)* (path) |
| `PUT` | `/api/v1/departments/{departmentId}/settings` | updateSettings | `departmentId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/stats` | stats_35 | `departmentId` *(req)* (path), `periode` (query), `debut` (query), `fin` (query) |
| `GET` | `/api/v1/departments/{departmentId}/tasks` | tasks | `departmentId` *(req)* (path), `statut` (query), `teamId` (query) |
| `POST` | `/api/v1/departments/{departmentId}/tasks` | createTask | `departmentId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/tasks/stats` | taskStats | `departmentId` *(req)* (path) |
| `PUT` | `/api/v1/departments/{departmentId}/tasks/{taskId}` | updateTask | `departmentId` *(req)* (path), `taskId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/tasks/{taskId}` | deleteTask | `departmentId` *(req)* (path), `taskId` *(req)* (path) |
| `GET` | `/api/v1/departments/{departmentId}/teams` | teams | `departmentId` *(req)* (path) |
| `POST` | `/api/v1/departments/{departmentId}/teams` | createTeam | `departmentId` *(req)* (path) |
| `PUT` | `/api/v1/departments/{departmentId}/teams/{teamId}` | updateTeam | `departmentId` *(req)* (path), `teamId` *(req)* (path) |
| `DELETE` | `/api/v1/departments/{departmentId}/teams/{teamId}` | archiveTeam | `departmentId` *(req)* (path), `teamId` *(req)* (path) |

## development-plan-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/development-plans` | create_51 | — |
| `POST` | `/api/v1/development-plans/auto-generate` | autoGenerate_1 | `membreId` *(req)* (query), `deptId` *(req)* (query) |
| `GET` | `/api/v1/development-plans/by-department/{deptId}` | listByDepartment | `deptId` *(req)* (path) |
| `GET` | `/api/v1/development-plans/by-member/{membreId}` | listByMember_1 | `membreId` *(req)* (path) |
| `GET` | `/api/v1/development-plans/stats/{membreId}` | stats_34 | `membreId` *(req)* (path) |
| `GET` | `/api/v1/development-plans/{id}` | get_10 | `id` *(req)* (path) |
| `PUT` | `/api/v1/development-plans/{id}` | update_23 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/development-plans/{id}` | delete_15 | `id` *(req)* (path) |

## dictionary-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/dictionaries` | allDictionaries | — |
| `POST` | `/api/v1/admin/dictionaries/reset` | reset_2 | — |
| `PUT` | `/api/v1/admin/dictionaries/{id}` | update_36 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/admin/dictionaries/{id}` | delete_28 | `id` *(req)* (path) |
| `POST` | `/api/v1/admin/dictionaries/{key}` | create_73 | `key` *(req)* (path) |
| `GET` | `/api/v1/dictionaries` | activeDictionaries | — |
| `GET` | `/api/v1/dictionaries/{key}` | activeByKey | `key` *(req)* (path) |

## digital-twin-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/twin/simulate` | simulate | — |
| `GET` | `/api/v1/twin/snapshot` | snapshot | — |

## directory-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/directory` | listPublic | `page` (query), `size` (query) |
| `GET` | `/api/v1/directory/all` | listAllPublic | — |
| `GET` | `/api/v1/directory/me` | getMyEntry | — |
| `PUT` | `/api/v1/directory/me` | updateMyEntry | — |
| `PATCH` | `/api/v1/directory/me/toggle` | togglePublic | — |

## discipleship-path-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/discipleship-paths` | list_67 | — |
| `GET` | `/api/discipleship-paths/member/{memberId}` | getForMember | `memberId` *(req)* (path) |
| `POST` | `/api/discipleship-paths/member/{memberId}` | createForMember | `memberId` *(req)* (path) |
| `GET` | `/api/discipleship-paths/stage/{stage}` | byStage_1 | `stage` *(req)* (path) |
| `GET` | `/api/discipleship-paths/stats` | stats_33 | — |
| `POST` | `/api/discipleship-paths/{id}/advance` | advance | `id` *(req)* (path) |
| `GET` | `/api/v1/discipleship-paths` | list_68 | — |
| `GET` | `/api/v1/discipleship-paths/member/{memberId}` | getForMember_1 | `memberId` *(req)* (path) |
| `POST` | `/api/v1/discipleship-paths/member/{memberId}` | createForMember_1 | `memberId` *(req)* (path) |
| `GET` | `/api/v1/discipleship-paths/stage/{stage}` | byStage | `stage` *(req)* (path) |
| `GET` | `/api/v1/discipleship-paths/stats` | stats_32 | — |
| `POST` | `/api/v1/discipleship-paths/{id}/advance` | advance_1 | `id` *(req)* (path) |

## document-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/documents` | list_66 | — |
| `GET` | `/api/v1/documents/{id}` | getById_3 | `id` *(req)* (path) |

## dress-code-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/dress-codes` | list_35 | `spaceId` (query), `eventId` (query) |
| `POST` | `/api/v1/dress-codes` | create_50 | — |
| `GET` | `/api/v1/dress-codes/{id}` | getById | `id` *(req)* (path) |
| `PUT` | `/api/v1/dress-codes/{id}` | update_22 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/dress-codes/{id}` | delete_14 | `id` *(req)* (path) |
| `POST` | `/api/v1/dress-codes/{id}/archive` | archive_2 | `id` *(req)* (path) |

## emergency-aid-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/aid/emergency` | recent_1 | — |
| `POST` | `/api/v1/aid/emergency` | open | — |
| `GET` | `/api/v1/aid/emergency/open` | openRequests | — |
| `POST` | `/api/v1/aid/emergency/{id}/collect` | collect | `id` *(req)* (path), `amount` *(req)* (query) |
| `POST` | `/api/v1/aid/emergency/{id}/resolve` | resolve_2 | `id` *(req)* (path) |
| `GET` | `/api/v1/aid/exchange` | convert_2 | `amount` *(req)* (query), `from` (query), `to` (query) |
| `GET` | `/api/v1/aid/exchange/rates` | rates | — |

## encouragement-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/encouragements` | send_4 | — |
| `GET` | `/api/v1/encouragements/count/{userId}` | countFor | `userId` *(req)* (path) |
| `GET` | `/api/v1/encouragements/my-team` | myTeam | — |
| `GET` | `/api/v1/encouragements/received` | received | — |
| `GET` | `/api/v1/encouragements/sent` | sent | — |

## endpoint-usage-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/platform/admin/usage/endpoints` | report | `days` (query) |

## engagement-analytics-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/engagement-analytics` | list_33 | — |
| `POST` | `/api/engagement-analytics` | record | — |
| `GET` | `/api/engagement-analytics/category/{category}` | listByCategory_3 | `category` *(req)* (path) |
| `GET` | `/api/engagement-analytics/dashboard` | dashboard_4 | — |
| `GET` | `/api/v1/engagement-analytics` | list_34 | — |
| `POST` | `/api/v1/engagement-analytics` | record_1 | — |
| `GET` | `/api/v1/engagement-analytics/category/{category}` | listByCategory_4 | `category` *(req)* (path) |
| `GET` | `/api/v1/engagement-analytics/dashboard` | dashboard_5 | — |

## enhanced-message-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/messages/conversations/{id}/messages/enhanced` | getEnhancedMessages | `id` *(req)* (path) |
| `POST` | `/api/v1/messages/conversations/{id}/messages/enhanced` | sendEnhancedMessage | `id` *(req)* (path) |
| `POST` | `/api/v1/messages/conversations/{id}/reply` | sendReply | `id` *(req)* (path) |
| `POST` | `/api/v1/messages/conversations/{id}/voice` | sendVoiceMessage | `id` *(req)* (path) |
| `GET` | `/api/v1/messages/groups` | listMyGroups | — |
| `POST` | `/api/v1/messages/groups` | createGroup | — |
| `GET` | `/api/v1/messages/groups/{groupId}` | getGroup | `groupId` *(req)* (path) |
| `POST` | `/api/v1/messages/groups/{groupId}/members` | addMembers | `groupId` *(req)* (path) |
| `DELETE` | `/api/v1/messages/groups/{groupId}/members/{userId}` | removeMember | `groupId` *(req)* (path), `userId` *(req)* (path) |
| `GET` | `/api/v1/messages/groups/{groupId}/messages` | getGroupMessages | `groupId` *(req)* (path), `page` (query), `size` (query) |
| `POST` | `/api/v1/messages/groups/{groupId}/messages` | sendGroupMessage | `groupId` *(req)* (path) |
| `POST` | `/api/v1/messages/groups/{groupId}/voice` | sendGroupVoiceMessage | `groupId` *(req)* (path) |
| `GET` | `/api/v1/messages/messages/{messageId}/reactions` | getReactions | `messageId` *(req)* (path) |
| `POST` | `/api/v1/messages/messages/{messageId}/reactions` | toggleReaction | `messageId` *(req)* (path) |
| `GET` | `/api/v1/messages/messages/{messageId}/replies` | getReplies | `messageId` *(req)* (path) |
| `GET` | `/api/v1/messages/search` | searchMessages | `q` *(req)* (query), `page` (query), `size` (query) |

## entity-change-sse-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/events/entity-changes` | subscribe_2 | `entityTypes` (query) |
| `GET` | `/api/v1/events/entity-changes/stats` | stats_30 | — |

## evaluation-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/evaluations` | getAll | `page` (query), `size` (query), `search` (query), `categorie` (query) |
| `POST` | `/api/v1/evaluations` | submit_2 | — |
| `GET` | `/api/v1/evaluations/all` | getAllEvaluations | — |
| `GET` | `/api/v1/evaluations/me` | getMyEvaluations | — |
| `GET` | `/api/v1/evaluations/me/list` | getMyEvaluationsList | `page` (query), `size` (query), `categorie` (query) |
| `GET` | `/api/v1/evaluations/my/{evalueId}` | getMyEvaluationsFor | `evalueId` *(req)* (path) |
| `GET` | `/api/v1/evaluations/to-evaluate` | getPeopleToEvaluate | — |
| `GET` | `/api/v1/evaluations/user/{userId}` | getEvaluationsForUser | `userId` *(req)* (path) |
| `PUT` | `/api/v1/evaluations/{evalueId}` | submitOrUpdate | `evalueId` *(req)* (path) |

## evangelism-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/evangelism` | findAll_17 | `etape` (query), `search` (query) |
| `GET` | `/api/v1/evangelism/souls/{soulId}` | getOrCreate | `soulId` *(req)* (path) |
| `PUT` | `/api/v1/evangelism/souls/{soulId}` | updateStage | `soulId` *(req)* (path) |
| `GET` | `/api/v1/evangelism/souls/{soulId}/history` | history_2 | `soulId` *(req)* (path) |
| `GET` | `/api/v1/evangelism/stats` | stats_31 | — |
| `PATCH` | `/api/v1/evangelism/{soulId}` | moveStage | `soulId` *(req)* (path) |

## evangelism-scoring-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/evangelism/scoring` | allScores | — |
| `GET` | `/api/v1/evangelism/scoring/{soulId}` | score | `soulId` *(req)* (path) |

## event-checklist-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/event-checklists` | listAll_4 | — |
| `POST` | `/api/event-checklists` | addItem | — |
| `GET` | `/api/event-checklists/event/{eventId}` | getByEvent_1 | `eventId` *(req)* (path) |
| `POST` | `/api/event-checklists/event/{eventId}/generate` | generate_6 | `eventId` *(req)* (path) |
| `GET` | `/api/event-checklists/event/{eventId}/progress` | progress_3 | `eventId` *(req)* (path) |
| `DELETE` | `/api/event-checklists/{id}` | delete_46 | `id` *(req)* (path) |
| `POST` | `/api/event-checklists/{id}/toggle` | toggle_2 | `id` *(req)* (path) |
| `GET` | `/api/v1/event-checklists` | listAll_5 | — |
| `POST` | `/api/v1/event-checklists` | addItem_1 | — |
| `GET` | `/api/v1/event-checklists/event/{eventId}` | getByEvent | `eventId` *(req)* (path) |
| `POST` | `/api/v1/event-checklists/event/{eventId}/generate` | generate_7 | `eventId` *(req)* (path) |
| `GET` | `/api/v1/event-checklists/event/{eventId}/progress` | progress_4 | `eventId` *(req)* (path) |
| `DELETE` | `/api/v1/event-checklists/{id}` | delete_45 | `id` *(req)* (path) |
| `POST` | `/api/v1/event-checklists/{id}/toggle` | toggle_3 | `id` *(req)* (path) |

## event-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/events` | findAll_12 | `page` (query), `size` (query), `familleId` (query), `typeEvenement` (query), `statut` (query), `upcomingOnly` (query) |
| `POST` | `/api/v1/events` | create_49 | — |
| `GET` | `/api/v1/events/consolidated` | getConsolidatedUpcoming | `days` (query) |
| `GET` | `/api/v1/events/consolidated/by-family` | getConsolidatedByFamily | `days` (query) |
| `GET` | `/api/v1/events/department/{departmentId}` | findByDepartmentId | `departmentId` *(req)* (path), `page` (query), `size` (query) |
| `POST` | `/api/v1/events/program/generate` | generateWeekProgram | `semaine` (query) |
| `POST` | `/api/v1/events/program/generate-month` | generateMonthProgram | — |
| `GET` | `/api/v1/events/program/week` | getWeekProgram | `semaine` (query) |
| `GET` | `/api/v1/events/statistics` | getEventStatistics | `familleId` (query), `periodeDebut` (query), `periodeFin` (query) |
| `GET` | `/api/v1/events/templates` | getTemplates | — |
| `POST` | `/api/v1/events/templates` | createTemplate_2 | — |
| `PUT` | `/api/v1/events/templates/{id}` | updateTemplate_2 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/events/templates/{id}` | deleteTemplate_2 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/events/templates/{id}/toggle` | toggleTemplate | `id` *(req)* (path) |
| `GET` | `/api/v1/events/upcoming/mine` | myUpcoming | `days` (query) |
| `POST` | `/api/v1/events/{eventId}/attendance` | markAttendance | `eventId` *(req)* (path) |
| `POST` | `/api/v1/events/{eventId}/register` | register | `eventId` *(req)* (path) |
| `GET` | `/api/v1/events/{eventId}/registrations` | getRegistrations | `eventId` *(req)* (path) |
| `PUT` | `/api/v1/events/{eventId}/rsvp` | setRsvp | `eventId` *(req)* (path) |
| `DELETE` | `/api/v1/events/{eventId}/unregister` | unregister | `eventId` *(req)* (path) |
| `GET` | `/api/v1/events/{id}` | findById_8 | `id` *(req)* (path) |
| `PUT` | `/api/v1/events/{id}` | update_21 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/events/{id}` | delete_13 | `id` *(req)* (path) |

## executive-insights-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/executive-insights` | list_65 | — |
| `POST` | `/api/v1/executive-insights/generate` | generate_5 | — |
| `GET` | `/api/v1/executive-insights/stats` | stats_29 | — |
| `POST` | `/api/v1/executive-insights/{id}/dismiss` | dismiss | `id` *(req)* (path) |
| `POST` | `/api/v1/executive-insights/{id}/read` | markRead | `id` *(req)* (path) |

## export-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/exports` | createExport | — |
| `GET` | `/api/v1/exports/download/{exportId}` | downloadExport | `exportId` *(req)* (path) |
| `GET` | `/api/v1/exports/formats` | getExportFormats | — |
| `GET` | `/api/v1/exports/history` | getExportHistory | — |
| `GET` | `/api/v1/exports/types` | getExportTypes | — |
| `GET` | `/api/v1/exports/{exportId}` | getExportStatus | `exportId` *(req)* (path) |

## face-recognition-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/face/enroll` | enroll_1 | — |
| `POST` | `/api/v1/face/enroll-batch` | enrollBatch | — |
| `POST` | `/api/v1/face/identify` | identify | — |
| `POST` | `/api/v1/face/identify-configurable` | identifyConfigurable | — |
| `GET` | `/api/v1/face/stats` | stats_28 | — |
| `GET` | `/api/v1/face/templates` | templates | `q` (query) |
| `DELETE` | `/api/v1/face/templates/{id}` | deactivate_2 | `id` *(req)* (path) |

## family-cohesion-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/family-cohesion` | getUserCohesion | `userId` (query) |
| `POST` | `/api/v1/family-cohesion/calculate/{familleId}` | calculate | `familleId` *(req)* (path) |
| `GET` | `/api/v1/family-cohesion/{familleId}` | getLatest | `familleId` *(req)* (path) |

## family-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/families` | findAll_11 | `page` (query), `size` (query), `chefFamilleId` (query) |
| `POST` | `/api/v1/families` | create_48 | — |
| `GET` | `/api/v1/families/by-chef/{chefId}` | findByChef | `chefId` *(req)* (path) |
| `POST` | `/api/v1/families/compare` | compareFamilies | — |
| `GET` | `/api/v1/families/{id}` | findById_7 | `id` *(req)* (path) |
| `PUT` | `/api/v1/families/{id}` | update_20 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/families/{id}` | delete_12 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/families/{id}/chief` | reassignChief | `id` *(req)* (path) |
| `GET` | `/api/v1/families/{id}/chief-history` | getChiefHistory | `id` *(req)* (path) |
| `GET` | `/api/v1/families/{id}/faiseur-performance` | getFaiseurPerformance | `id` *(req)* (path), `semaine` (query) |
| `GET` | `/api/v1/families/{id}/history` | getFamilyHistory | `id` *(req)* (path) |
| `PATCH` | `/api/v1/families/{id}/restore` | restore_2 | `id` *(req)* (path) |
| `GET` | `/api/v1/families/{id}/risk` | getRiskAssessment | `id` *(req)* (path) |
| `GET` | `/api/v1/families/{id}/risk-history` | getRiskHistory | `id` *(req)* (path) |
| `PUT` | `/api/v1/families/{id}/risk-level` | setNiveauRisque | `id` *(req)* (path) |
| `GET` | `/api/v1/families/{id}/tree` | getFamilyTree | `id` *(req)* (path) |

## family-meeting-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/family-meetings` | list_64 | `from` (query), `to` (query) |
| `POST` | `/api/v1/family-meetings/{id}/complete` | complete_1 | `id` *(req)* (path) |

## family-os-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/families/{familyId}/os/activities` | getActivities | `familyId` *(req)* (path), `page` (query), `size` (query), `from` (query), `to` (query) |
| `GET` | `/api/v1/families/{familyId}/os/dashboard` | getDashboard | `familyId` *(req)* (path) |
| `GET` | `/api/v1/families/{familyId}/os/meetings` | getMeetings | `familyId` *(req)* (path), `from` (query), `to` (query) |
| `POST` | `/api/v1/families/{familyId}/os/meetings` | createMeeting | `familyId` *(req)* (path) |
| `POST` | `/api/v1/families/{familyId}/os/members` | addMember_1 | `familyId` *(req)* (path), `soulId` *(req)* (query), `faiseurId` (query) |
| `GET` | `/api/v1/families/{familyId}/os/receptions` | getReceptions | `familyId` *(req)* (path), `from` (query), `to` (query) |
| `POST` | `/api/v1/families/{familyId}/os/receptions` | createReception | `familyId` *(req)* (path) |
| `GET` | `/api/v1/families/{familyId}/os/search-souls` | searchSouls | `familyId` *(req)* (path), `search` (query), `scope` (query) |
| `GET` | `/api/v1/families/{familyId}/os/visits` | getVisits | `familyId` *(req)* (path), `from` (query), `to` (query) |
| `POST` | `/api/v1/families/{familyId}/os/visits` | createVisit | `familyId` *(req)* (path) |
| `PUT` | `/api/v1/families/{familyId}/os/visits/{visitId}` | updateVisit | `familyId` *(req)* (path), `visitId` *(req)* (path) |

## family-resource-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/family-resources` | list_32 | `userId` (query), `familleId` (query), `page` (query), `size` (query) |
| `POST` | `/api/v1/family-resources` | create_47 | — |
| `GET` | `/api/v1/family-resources/family/{familleId}` | listByFamily | `familleId` *(req)* (path) |
| `GET` | `/api/v1/family-resources/{id}` | get_33 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/family-resources/{id}` | delete_37 | `id` *(req)* (path) |

## favorite-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/favorites/is-favorite` | isFavorite | `entityType` *(req)* (query), `entityId` *(req)* (query) |
| `GET` | `/api/v1/favorites/souls` | soulFavorites | — |
| `POST` | `/api/v1/favorites/toggle` | toggle_1 | — |

## feedback-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/feedback` | list_69 | — |
| `GET` | `/api/v1/admin/feedback/stats` | stats_43 | — |
| `PATCH` | `/api/v1/admin/feedback/{id}/status` | updateStatus_11 | `id` *(req)* (path) |
| `POST` | `/api/v1/feedback` | create_46 | — |

## file-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/files` | findAll_10 | `page` (query), `size` (query), `familleId` (query), `evenementId` (query), `categorie` (query) |
| `POST` | `/api/v1/files` | upload | — |
| `GET` | `/api/v1/files/{id}` | findById_6 | `id` *(req)* (path) |
| `PUT` | `/api/v1/files/{id}` | update_19 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/files/{id}` | delete_11 | `id` *(req)* (path) |
| `GET` | `/api/v1/files/{id}/download` | download_1 | `id` *(req)* (path) |

## finance-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/finances/budgets` | listBudgets | `annee` (query) |
| `POST` | `/api/v1/finances/budgets` | upsertBudget | — |
| `DELETE` | `/api/v1/finances/budgets/{id}` | deleteBudget | `id` *(req)* (path) |
| `GET` | `/api/v1/finances/stats` | stats_27 | `annee` (query) |
| `GET` | `/api/v1/finances/stats/currency` | statsWithCurrency | `annee` (query), `currency` (query) |
| `GET` | `/api/v1/finances/transactions` | listTransactions | `type` (query), `categorie` (query), `debut` (query), `fin` (query) |
| `POST` | `/api/v1/finances/transactions` | createTransaction | — |
| `PUT` | `/api/v1/finances/transactions/{id}` | updateTransaction | `id` *(req)* (path) |
| `DELETE` | `/api/v1/finances/transactions/{id}` | deleteTransaction | `id` *(req)* (path) |

## follow-up-request-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/follow-up-requests` | create_45 | — |
| `GET` | `/api/v1/follow-up-requests/assigned-to-me` | assignedToMe | — |
| `GET` | `/api/v1/follow-up-requests/mine` | mine_3 | — |
| `GET` | `/api/v1/follow-up-requests/pending` | pending_2 | — |
| `GET` | `/api/v1/follow-up-requests/stats` | stats_26 | — |
| `POST` | `/api/v1/follow-up-requests/{id}/assign` | assign_1 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/follow-up-requests/{id}/status` | updateStatus_8 | `id` *(req)* (path) |

## form-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/forms` | listTemplates | — |
| `POST` | `/api/v1/forms` | createTemplate_1 | — |
| `GET` | `/api/v1/forms/published` | listPublished | — |
| `GET` | `/api/v1/forms/{id}` | getTemplate | `id` *(req)* (path) |
| `PUT` | `/api/v1/forms/{id}` | updateTemplate_1 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/forms/{id}` | deleteTemplate_1 | `id` *(req)* (path) |
| `POST` | `/api/v1/forms/{id}/archive` | archiveTemplate | `id` *(req)* (path) |
| `POST` | `/api/v1/forms/{id}/publish` | publishTemplate_1 | `id` *(req)* (path) |
| `GET` | `/api/v1/forms/{templateId}/responses` | getResponses | `templateId` *(req)* (path) |
| `POST` | `/api/v1/forms/{templateId}/responses` | submitResponse_1 | `templateId` *(req)* (path) |
| `GET` | `/api/v1/forms/{templateId}/stats` | getStats_1 | `templateId` *(req)* (path) |

## gdpr-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/gdpr/delete` | requestDataDeletion | — |
| `POST` | `/api/v1/gdpr/export` | requestDataExport | — |
| `GET` | `/api/v1/gdpr/requests` | listRequests_1 | `userId` (query) |
| `POST` | `/api/v1/gdpr/requests/{id}/process` | processRequest | `id` *(req)* (path) |
| `POST` | `/api/v1/gdpr/requests/{id}/reject` | rejectRequest | `id` *(req)* (path) |

## geofencing-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/geofencing/auto-check-in` | autoCheckIn | — |
| `POST` | `/api/v1/geofencing/check-in` | checkIn | — |
| `POST` | `/api/v1/geofencing/check-out` | checkOut | — |
| `GET` | `/api/v1/geofencing/config` | getConfig_2 | — |
| `GET` | `/api/v1/geofencing/history` | history_1 | — |
| `GET` | `/api/v1/geofencing/history/all` | historyAll | — |

## group-message-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/group-messages` | send_3 | — |
| `GET` | `/api/group-messages/group/{groupId}` | getMessages_1 | `groupId` *(req)* (path) |
| `GET` | `/api/group-messages/group/{groupId}/stats` | stats_25 | `groupId` *(req)* (path) |
| `GET` | `/api/group-messages/search` | search_5 | `groupId` *(req)* (query), `q` *(req)* (query) |
| `DELETE` | `/api/group-messages/{id}` | delete_43 | `id` *(req)* (path) |
| `POST` | `/api/group-messages/{id}/reaction` | react_1 | `id` *(req)* (path) |
| `POST` | `/api/v1/group-messages` | send_2 | — |
| `GET` | `/api/v1/group-messages/group/{groupId}` | getMessages | `groupId` *(req)* (path) |
| `GET` | `/api/v1/group-messages/group/{groupId}/stats` | stats_24 | `groupId` *(req)* (path) |
| `GET` | `/api/v1/group-messages/search` | search_4 | `groupId` *(req)* (query), `q` *(req)* (query) |
| `DELETE` | `/api/v1/group-messages/{id}` | delete_44 | `id` *(req)* (path) |
| `POST` | `/api/v1/group-messages/{id}/reaction` | react | `id` *(req)* (path) |

## growth-projection-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/growth-projections` | list_31 | — |
| `POST` | `/api/v1/growth-projections` | create_44 | — |
| `GET` | `/api/v1/growth-projections/prophecy` | prophecy | — |
| `POST` | `/api/v1/growth-projections/simulate` | simulate_1 | — |
| `GET` | `/api/v1/growth-projections/{id}` | get_32 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/growth-projections/{id}` | delete_36 | `id` *(req)* (path) |

## health-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/health/campaigns` | getCampaigns | `status` (query), `page` (query), `size` (query) |
| `POST` | `/api/v1/health/campaigns` | createCampaign | — |
| `GET` | `/api/v1/health/campaigns/{id}` | getCampaign | `id` *(req)* (path) |
| `GET` | `/api/v1/health/consultations` | getConsultations | `patientId` (query), `from` (query), `to` (query), `page` (query), `size` (query) |
| `POST` | `/api/v1/health/consultations` | createConsultation | — |
| `GET` | `/api/v1/health/consultations/{id}` | getConsultation | `id` *(req)* (path) |
| `GET` | `/api/v1/health/dashboard/stats` | getDashboardStats | — |
| `GET` | `/api/v1/health/patients` | getPatients | `search` (query), `page` (query), `size` (query) |
| `POST` | `/api/v1/health/patients` | createPatient | — |
| `GET` | `/api/v1/health/patients/{id}` | getPatient | `id` *(req)* (path) |
| `PUT` | `/api/v1/health/patients/{id}` | updatePatient | `id` *(req)* (path) |
| `GET` | `/api/v1/health/pharmacy/items` | getPharmacyItems | `search` (query), `categorie` (query), `page` (query), `size` (query) |
| `POST` | `/api/v1/health/pharmacy/items` | createPharmacyItem | — |
| `GET` | `/api/v1/health/pharmacy/items/{id}` | getPharmacyItem | `id` *(req)* (path) |
| `PUT` | `/api/v1/health/pharmacy/items/{id}` | updatePharmacyItem | `id` *(req)* (path) |
| `POST` | `/api/v1/health/pharmacy/movements` | createMovement | — |
| `GET` | `/api/v1/health/pharmacy/stock` | getPharmacyStock | `itemId` (query), `status` (query), `page` (query), `size` (query) |
| `GET` | `/api/v1/health/pharmacy/stock/alerts/expiring` | getExpiringSoonAlerts | `days` (query) |
| `GET` | `/api/v1/health/pharmacy/stock/alerts/low` | getLowStockAlerts | — |
| `GET` | `/api/v1/health/prescriptions` | getPrescriptions | `patientId` (query), `consultationId` (query), `page` (query), `size` (query) |
| `POST` | `/api/v1/health/prescriptions` | createPrescription | — |
| `GET` | `/api/v1/health/prescriptions/{id}` | getPrescription | `id` *(req)* (path) |

## health-observatory-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/health-observatory` | observatory | — |
| `GET` | `/api/v1/health-observatory/departments` | departmentScores | — |
| `GET` | `/api/v1/health-observatory/trend` | trend | — |

## impersonation-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/platform/admin/impersonation` | startImpersonation | — |
| `POST` | `/api/v1/platform/admin/impersonation/stop` | stopImpersonation | — |

## import-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/import/validate/{type}` | validate | `type` *(req)* (path) |
| `POST` | `/api/v1/import/{type}` | importData | `type` *(req)* (path) |

## intelligence-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/intelligence` | list_63 | — |
| `GET` | `/api/v1/intelligence/alerts` | alerts | — |
| `GET` | `/api/v1/intelligence/category/{category}` | listByCategory_2 | `category` *(req)* (path) |
| `GET` | `/api/v1/intelligence/dashboard` | dashboard_3 | — |
| `POST` | `/api/v1/intelligence/initialize` | initialize_1 | — |
| `PUT` | `/api/v1/intelligence/{id}/value` | updateValue | `id` *(req)* (path) |

## interaction-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/interactions/my-reminders` | myReminders | — |
| `DELETE` | `/api/v1/interactions/{id}` | delete_42 | `id` *(req)* (path) |
| `GET` | `/api/v1/souls/{soulId}/interactions` | findBySoul | `soulId` *(req)* (path) |
| `POST` | `/api/v1/souls/{soulId}/interactions` | create_22 | `soulId` *(req)* (path) |

## inventory-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/inventory` | list_30 | `categorie` (query), `statut` (query), `q` (query), `pageable` *(req)* (query) |
| `POST` | `/api/v1/inventory` | create_43 | — |
| `GET` | `/api/v1/inventory/alerts` | smartAlerts | — |
| `GET` | `/api/v1/inventory/stats` | stats_23 | — |
| `GET` | `/api/v1/inventory/{id}` | get_9 | `id` *(req)* (path) |
| `PUT` | `/api/v1/inventory/{id}` | update_18 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/inventory/{id}` | delete_10 | `id` *(req)* (path) |
| `POST` | `/api/v1/inventory/{id}/assign` | assign | `id` *(req)* (path) |
| `POST` | `/api/v1/inventory/{id}/maintenance` | markMaintenance | `id` *(req)* (path) |
| `POST` | `/api/v1/inventory/{id}/unassign` | unassign | `id` *(req)* (path) |

## invitation-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/invitations` | listInvitations | `page` (query), `size` (query), `status` (query), `q` (query) |
| `POST` | `/api/v1/admin/invitations` | createInvitation | — |
| `POST` | `/api/v1/admin/invitations/accept/{token}` | acceptInvitation | `token` *(req)* (path) |
| `GET` | `/api/v1/admin/invitations/validate/{token}` | validateInvitation | `token` *(req)* (path) |
| `GET` | `/api/v1/admin/invitations/{id}` | getInvitation | `id` *(req)* (path) |
| `DELETE` | `/api/v1/admin/invitations/{id}` | cancelInvitation | `id` *(req)* (path) |
| `POST` | `/api/v1/admin/invitations/{id}/resend` | resendInvitation | `id` *(req)* (path) |

## kingdom-mapping-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/map/heatmap` | heatmap | `typeDisciple` (query) |
| `GET` | `/api/v1/map/sectors` | sectors | — |

## kpi-narrative-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/kpi-narrative` | listAll_8 | — |
| `POST` | `/api/v1/kpi-narrative/generate` | generate_4 | — |
| `POST` | `/api/v1/kpi-narrative/generate-all` | generateAll_2 | — |
| `GET` | `/api/v1/kpi-narrative/période/{période}` | listByPériode | `période` *(req)* (path) |
| `GET` | `/api/v1/kpi-narrative/type/{type}` | listByType_3 | `type` *(req)* (path) |

## leave-request-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/leave-requests` | list_29 | `page` (query), `size` (query), `statut` (query) |
| `POST` | `/api/v1/leave-requests` | create_42 | — |
| `GET` | `/api/v1/leave-requests/{id}` | get_31 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/leave-requests/{id}/approve` | approve_2 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/leave-requests/{id}/cancel` | cancel_5 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/leave-requests/{id}/reject` | reject_2 | `id` *(req)* (path) |

## legal-admin-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/platform/admin/legal` | publish | — |
| `GET` | `/api/v1/platform/admin/legal/{code}/versions` | versions | `code` *(req)* (path), `language` (query) |

## live-stream-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/streams` | list_12 | `tenantId` *(req)* (query) |
| `POST` | `/api/v1/streams` | create_15 | — |
| `GET` | `/api/v1/streams/live` | live | `tenantId` *(req)* (query) |
| `POST` | `/api/v1/streams/{id}/end` | endStream | `id` *(req)* (path) |
| `POST` | `/api/v1/streams/{id}/go-live` | goLive | `id` *(req)* (path) |
| `POST` | `/api/v1/streams/{id}/viewer` | addViewer | `id` *(req)* (path) |

## load-prediction-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/load-prediction` | predict | — |

## maker-report-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/reports/correction` | correctReport | — |
| `GET` | `/api/v1/reports/correction/{reportId}` | getCorrections | `reportId` *(req)* (path) |
| `GET` | `/api/v1/reports/family-weekly` | getFamilyReports | `page` (query), `size` (query), `familleId` (query), `chefFamilleId` (query), `semaine` (query) |
| `POST` | `/api/v1/reports/family-weekly` | submitFamilyReport | — |
| `GET` | `/api/v1/reports/family-weekly/{familyId}` | getFamilyReportsByFamily | `familyId` *(req)* (path), `semaine` (query) |
| `PATCH` | `/api/v1/reports/family-weekly/{id}/validate` | validateFamilyReport | `id` *(req)* (path) |
| `GET` | `/api/v1/reports/maker-weekly` | getMakerReports | `page` (query), `size` (query), `faiseurId` (query), `familleId` (query), `ameId` (query), `semaine` (query) |
| `POST` | `/api/v1/reports/maker-weekly` | submitMakerReport | — |
| `POST` | `/api/v1/reports/maker-weekly/draft` | saveDraft | — |
| `GET` | `/api/v1/reports/maker-weekly/prefill/{faiseurId}` | getPreFilledReport | `faiseurId` *(req)* (path) |
| `GET` | `/api/v1/reports/maker-weekly/urgent-aid` | getUrgentAidRequests | `traite` (query) |
| `PATCH` | `/api/v1/reports/maker-weekly/urgent-aid/{reportId}/mark-treated` | markAidAsTreated | `reportId` *(req)* (path) |
| `GET` | `/api/v1/reports/maker-weekly/{id}` | getMakerReport | `id` *(req)* (path) |

## maker-tracking-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/maker-tracking` | listAll_3 | `faiseurId` (query), `page` (query), `size` (query) |
| `POST` | `/api/v1/maker-tracking` | create_41 | — |
| `GET` | `/api/v1/maker-tracking/by-faiseur/{faiseurId}` | list_62 | `faiseurId` *(req)* (path) |
| `GET` | `/api/v1/maker-tracking/resume/{faiseurId}` | resume | `faiseurId` *(req)* (path) |
| `GET` | `/api/v1/maker-tracking/{id}` | get_30 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/maker-tracking/{id}` | delete_35 | `id` *(req)* (path) |

## map-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/map/points` | mapPoints | — |
| `PATCH` | `/api/v1/map/souls/{soulId}/coordinates` | updateCoordinates | `soulId` *(req)* (path) |

## marketplace-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/marketplace` | list_28 | — |
| `POST` | `/api/v1/marketplace` | create_40 | — |
| `GET` | `/api/v1/marketplace/category/{category}` | listByCategory_1 | `category` *(req)* (path) |
| `POST` | `/api/v1/marketplace/templates/publish` | publishTemplate | — |
| `PUT` | `/api/v1/marketplace/{id}` | update_17 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/marketplace/{id}` | deactivate | `id` *(req)* (path) |
| `POST` | `/api/v1/marketplace/{id}/install` | install | `id` *(req)* (path) |

## me-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/me/permissions` | getCurrentPermissions | — |

## member-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/members/departments/{deptId}/presences` | departmentPresenceSheet | `deptId` *(req)* (path), `semaine` (query) |
| `POST` | `/api/v1/members/departments/{deptId}/presences` | submitDepartmentPresences | `deptId` *(req)* (path) |
| `GET` | `/api/v1/members/me/dashboard` | myDashboard | — |
| `GET` | `/api/v1/members/me/events` | myEvents | — |
| `GET` | `/api/v1/members/me/notes` | myNotes | — |
| `GET` | `/api/v1/members/me/presences` | myPresences | — |
| `POST` | `/api/v1/members/me/presences` | submitPresence | — |
| `PUT` | `/api/v1/members/me/profile` | updateProfile | — |
| `GET` | `/api/v1/members/me/progression` | myProgression | — |
| `GET` | `/api/v1/members/me/requests` | myRequests | — |
| `POST` | `/api/v1/members/me/requests` | createRequest | — |
| `GET` | `/api/v1/members/presences/recent` | scopedPresences | — |
| `POST` | `/api/v1/members/qr-checkin` | qrCheckin | — |
| `GET` | `/api/v1/members/requests/inbox` | inbox | — |
| `PATCH` | `/api/v1/members/requests/{id}/status` | updateRequestStatus | `id` *(req)* (path) |

## mentoring-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/mentoring` | list_61 | `page` (query), `size` (query), `chefId` (query) |
| `GET` | `/api/v1/mentoring/all` | listAll_7 | `chefId` (query) |
| `POST` | `/api/v1/mentoring/generate` | generate_3 | — |
| `GET` | `/api/v1/mentoring/stats` | stats_21 | `chefId` (query) |
| `GET` | `/api/v1/mentoring/{id}` | get_29 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/mentoring/{id}/archive` | archive_3 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/mentoring/{id}/read` | markAsRead_3 | `id` *(req)* (path) |

## message-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/messages/conversations` | conversations | — |
| `POST` | `/api/v1/messages/conversations` | startConversation | — |
| `GET` | `/api/v1/messages/conversations/unread-total` | unreadTotal | — |
| `GET` | `/api/v1/messages/conversations/{id}/messages` | messages | `id` *(req)* (path) |
| `POST` | `/api/v1/messages/conversations/{id}/messages` | sendMessage | `id` *(req)* (path) |
| `PATCH` | `/api/v1/messages/conversations/{id}/read` | markAsRead_2 | `id` *(req)* (path) |

## moderation-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/moderation` | listAll_2 | — |
| `POST` | `/api/v1/moderation` | submit_1 | — |
| `GET` | `/api/v1/moderation/pending` | listPending | — |
| `GET` | `/api/v1/moderation/stats` | stats_20 | — |
| `PUT` | `/api/v1/moderation/{id}/review` | review | `id` *(req)* (path) |

## module-catalog-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/modules/catalog` | getCatalog | — |
| `GET` | `/api/v1/modules/catalog/grouped` | getCatalogGrouped | — |
| `GET` | `/api/v1/modules/catalog/stats` | getCatalogStats | — |
| `GET` | `/api/v1/modules/catalog/{code}` | getModuleDefinition | `code` *(req)* (path) |
| `GET` | `/api/v1/modules/router/{moduleCode}` | getModuleRoute | `moduleCode` *(req)* (path), `spaceId` *(req)* (query) |
| `GET` | `/api/v1/spaces/{spaceId}/modules` | getSpaceModules | `spaceId` *(req)* (path) |
| `GET` | `/api/v1/spaces/{spaceId}/modules/enabled` | getEnabledSpaceModules | `spaceId` *(req)* (path) |
| `GET` | `/api/v1/spaces/{spaceId}/modules/{moduleCode}` | getSpaceModule | `spaceId` *(req)* (path), `moduleCode` *(req)* (path) |
| `PUT` | `/api/v1/spaces/{spaceId}/modules/{moduleCode}` | toggleSpaceModule | `spaceId` *(req)* (path), `moduleCode` *(req)* (path) |
| `PUT` | `/api/v1/spaces/{spaceId}/modules/{moduleCode}/config` | updateSpaceModuleConfig | `spaceId` *(req)* (path), `moduleCode` *(req)* (path) |
| `PUT` | `/api/v1/spaces/{spaceId}/modules/{moduleCode}/limits` | updateSpaceModuleLimits | `spaceId` *(req)* (path), `moduleCode` *(req)* (path) |
| `PUT` | `/api/v1/spaces/{spaceId}/modules/{moduleCode}/order` | updateSpaceModuleOrder | `spaceId` *(req)* (path), `moduleCode` *(req)* (path) |
| `GET` | `/api/v1/spaces/{spaceId}/routes` | getSpaceRoutes | `spaceId` *(req)* (path) |

## module-feature-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/features/flags` | getFeatureFlags | — |
| `PUT` | `/api/v1/admin/features/flags` | updateFeatureFlags | — |
| `GET` | `/api/v1/admin/features/modules` | getModules_1 | — |
| `PUT` | `/api/v1/admin/features/modules/{moduleKey}` | toggleModule_2 | `moduleKey` *(req)* (path) |
| `GET` | `/api/v1/admin/features/plans` | getPlans | — |
| `GET` | `/api/v1/admin/features/quotas` | getQuotas | — |

## neighborhood-health-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/neighborhood-health` | byZone | — |

## network-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/network/directory` | listDirectory | — |
| `GET` | `/api/v1/network/directory/country/{country}` | listByCountry | `country` *(req)* (path) |
| `GET` | `/api/v1/network/directory/mine` | getMyDirectoryEntry | — |
| `PUT` | `/api/v1/network/directory/mine` | updateMyDirectoryEntry | — |
| `POST` | `/api/v1/network/directory/mine/listing` | toggleListing | — |
| `GET` | `/api/v1/network/directory/search` | searchDirectory | `q` *(req)* (query) |
| `GET` | `/api/v1/network/events` | listUpcomingEvents | — |
| `POST` | `/api/v1/network/events` | createEvent | — |
| `GET` | `/api/v1/network/events/all` | listAllPublicEvents | — |
| `GET` | `/api/v1/network/events/mine` | listMyEvents | — |
| `GET` | `/api/v1/network/events/type/{type}` | listByEventType | `type` *(req)* (path) |
| `DELETE` | `/api/v1/network/events/{id}` | deactivateEvent | `id` *(req)* (path) |
| `POST` | `/api/v1/network/events/{id}/join` | joinEvent | `id` *(req)* (path) |
| `POST` | `/api/v1/network/events/{id}/leave` | leaveEvent | `id` *(req)* (path) |
| `GET` | `/api/v1/network/resources` | listSharedResources | — |
| `POST` | `/api/v1/network/resources` | createResource | — |
| `GET` | `/api/v1/network/resources/category/{category}` | listByCategory | `category` *(req)* (path) |
| `GET` | `/api/v1/network/resources/mine` | listMyResources | — |
| `GET` | `/api/v1/network/resources/search` | searchResources | `q` *(req)* (query) |
| `DELETE` | `/api/v1/network/resources/{id}` | deactivateResource | `id` *(req)* (path) |
| `POST` | `/api/v1/network/resources/{id}/download` | download | `id` *(req)* (path) |
| `GET` | `/api/v1/network/stats` | getNetworkStats | — |

## notification-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/notifications` | findAll_16 | `page` (query), `size` (query), `unreadOnly` (query) |
| `POST` | `/api/v1/notifications/mark-all-read` | markAllAsRead | — |
| `GET` | `/api/v1/notifications/unread-count` | getUnreadCount | — |
| `PATCH` | `/api/v1/notifications/{id}/read` | markAsRead_1 | `id` *(req)* (path) |

## notification-preference-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/notifications/preferences` | get_8 | — |
| `PUT` | `/api/v1/notifications/preferences` | update_16 | — |

## notification-template-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/notifications/events` | events | — |
| `GET` | `/api/v1/admin/notifications/templates` | list_53 | — |
| `POST` | `/api/v1/admin/notifications/templates` | create_72 | — |
| `PUT` | `/api/v1/admin/notifications/templates/{id}` | update_35 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/admin/notifications/templates/{id}` | delete_27 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/admin/notifications/templates/{id}/toggle` | toggle_7 | `id` *(req)* (path) |

## objective-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/objectives` | findAll_9 | — |
| `POST` | `/api/v1/objectives` | create_39 | — |
| `GET` | `/api/v1/objectives/my-progress` | myProgress | — |
| `DELETE` | `/api/v1/objectives/{id}` | delete_41 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/objectives/{id}/toggle` | toggle_5 | `id` *(req)* (path) |

## onboarding-wizard-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/onboarding-wizard` | getSteps | — |
| `POST` | `/api/v1/onboarding-wizard/initialize` | initialize | — |
| `GET` | `/api/v1/onboarding-wizard/progress` | progress_2 | — |
| `GET` | `/api/v1/onboarding-wizard/status` | status_4 | — |
| `GET` | `/api/v1/onboarding-wizard/templates/{role}` | roleTemplate | `role` *(req)* (path) |
| `POST` | `/api/v1/onboarding-wizard/{id}/complete` | complete | `id` *(req)* (path) |
| `POST` | `/api/v1/onboarding-wizard/{id}/skip` | skip | `id` *(req)* (path) |
| `POST` | `/api/v1/onboarding-wizard/{id}/start` | start | `id` *(req)* (path) |

## organization-hierarchy-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/org/campus` | createCampus | — |
| `POST` | `/api/v1/org/nodes` | createNode | — |
| `POST` | `/api/v1/org/nodes/bulk-create` | bulkCreate | — |
| `POST` | `/api/v1/org/nodes/bulk-delete` | bulkDelete | — |
| `POST` | `/api/v1/org/nodes/bulk-move` | bulkMoveNodes | — |
| `POST` | `/api/v1/org/nodes/bulk-status` | bulkUpdateStatus | — |
| `GET` | `/api/v1/org/nodes/parent/{parentId}` | getChildren | `parentId` *(req)* (path) |
| `GET` | `/api/v1/org/nodes/type/{type}` | getNodesByType | `type` *(req)* (path) |
| `GET` | `/api/v1/org/nodes/{id}` | getNode | `id` *(req)* (path) |
| `PUT` | `/api/v1/org/nodes/{id}` | updateNode | `id` *(req)* (path) |
| `DELETE` | `/api/v1/org/nodes/{id}` | deleteNode | `id` *(req)* (path), `forceCascade` (query) |
| `GET` | `/api/v1/org/nodes/{id}/ancestors` | getAncestors | `id` *(req)* (path) |
| `POST` | `/api/v1/org/nodes/{id}/copy` | copyNode | `id` *(req)* (path) |
| `GET` | `/api/v1/org/nodes/{id}/descendants` | getDescendants | `id` *(req)* (path) |
| `GET` | `/api/v1/org/nodes/{id}/effective-config` | getEffectiveConfig | `id` *(req)* (path) |
| `GET` | `/api/v1/org/nodes/{id}/members` | getMembersWithAccess | `id` *(req)* (path) |
| `POST` | `/api/v1/org/nodes/{id}/move` | moveNode | `id` *(req)* (path) |
| `PUT` | `/api/v1/org/nodes/{id}/responsible` | assignResponsible | `id` *(req)* (path) |
| `GET` | `/api/v1/org/nodes/{id}/siblings` | getSiblings | `id` *(req)* (path) |
| `GET` | `/api/v1/org/root` | getRoot | — |
| `POST` | `/api/v1/org/root-church` | createRootChurch | — |
| `GET` | `/api/v1/org/stats` | getStats | — |
| `GET` | `/api/v1/org/stats/count/{type}` | getCountByType | `type` *(req)* (path) |
| `GET` | `/api/v1/org/tree` | getTree | — |
| `GET` | `/api/v1/org/tree/flat` | getFlatTree | — |
| `GET` | `/api/v1/org/units` | getUnits | `type` (query), `parentId` (query) |
| `POST` | `/api/v1/org/units` | createUnit | — |
| `PATCH` | `/api/v1/org/units/{id}` | updateUnit | `id` *(req)* (path) |
| `DELETE` | `/api/v1/org/units/{id}` | deleteUnit | `id` *(req)* (path), `forceCascade` (query) |

## organization-management-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/admin/org/nodes` | createNode_1 | — |
| `PUT` | `/api/v1/admin/org/nodes/{id}` | updateNode_1 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/admin/org/nodes/{id}` | deleteNode_1 | `id` *(req)* (path) |
| `POST` | `/api/v1/admin/org/nodes/{id}/move` | moveNode_1 | `id` *(req)* (path) |
| `PUT` | `/api/v1/admin/org/nodes/{id}/responsible` | setResponsible | `id` *(req)* (path) |
| `GET` | `/api/v1/admin/org/nodes/{type}` | getNodesByType_1 | `type` *(req)* (path) |
| `GET` | `/api/v1/admin/org/stats` | getStats_4 | — |
| `GET` | `/api/v1/admin/org/tree` | getTree_1 | — |

## page-builder-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/pages` | listPages | — |
| `POST` | `/api/v1/pages` | createPage | — |
| `GET` | `/api/v1/pages/options` | options | — |
| `GET` | `/api/v1/pages/preview/{id}` | preview | `id` *(req)* (path) |
| `GET` | `/api/v1/pages/sources` | sources | — |
| `PUT` | `/api/v1/pages/{id}` | updatePage | `id` *(req)* (path) |
| `DELETE` | `/api/v1/pages/{id}` | deletePage | `id` *(req)* (path) |
| `POST` | `/api/v1/pages/{id}/publish` | setPublished | `id` *(req)* (path) |
| `GET` | `/api/v1/pages/{slug}` | render | `slug` *(req)* (path) |

## parallel-followup-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/parallel-followups` | findAll_8 | `page` (query), `size` (query), `initiateurId` (query), `statut` (query) |
| `POST` | `/api/v1/parallel-followups` | create_38 | — |
| `GET` | `/api/v1/parallel-followups/active` | findActive_1 | `page` (query), `size` (query) |
| `GET` | `/api/v1/parallel-followups/{id}` | findById_13 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/parallel-followups/{id}/close` | close | `id` *(req)* (path) |

## passport-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/passports/member/{memberId}` | getMemberPassport | `memberId` *(req)* (path) |
| `POST` | `/api/v1/passports/member/{memberId}` | issuePassport | `memberId` *(req)* (path) |
| `GET` | `/api/v1/passports/mine` | getMyPassport | — |
| `GET` | `/api/v1/passports/verifications` | getVerifications | `code` *(req)* (query) |
| `GET` | `/api/v1/passports/{passportId}/entries` | getEntries | `passportId` *(req)* (path) |
| `POST` | `/api/v1/passports/{passportId}/entries` | addEntry | `passportId` *(req)* (path) |
| `GET` | `/api/v1/passports/{passportId}/qr` | getQr | `passportId` *(req)* (path) |
| `POST` | `/api/v1/passports/{passportId}/revoke` | revoke | `passportId` *(req)* (path) |

## passport-public-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/public/passports/{code}` | verify_5 | `code` *(req)* (path) |

## pastoral-visit-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/pastoral-visits` | list_27 | `start` (query), `end` (query), `statut` (query) |
| `POST` | `/api/v1/pastoral-visits` | create_37 | — |
| `POST` | `/api/v1/pastoral-visits/auto-generate` | autoGenerate | — |
| `GET` | `/api/v1/pastoral-visits/{id}` | get_28 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/pastoral-visits/{id}/complete` | complete_2 | `id` *(req)* (path) |
| `POST` | `/api/v1/pastoral-visits/{id}/reschedule` | reschedule | `id` *(req)* (path) |

## pastorate-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/pastorate/appointments` | getAppointments | `pastorId` (query), `orgUnitId` (query) |
| `POST` | `/api/v1/pastorate/appointments` | createAppointment | — |
| `GET` | `/api/v1/pastorate/appointments/{id}` | getAppointment | `id` *(req)* (path) |
| `PUT` | `/api/v1/pastorate/appointments/{id}` | updateAppointment | `id` *(req)* (path) |
| `DELETE` | `/api/v1/pastorate/appointments/{id}` | endAppointment | `id` *(req)* (path), `reason` *(req)* (query) |
| `GET` | `/api/v1/pastorate/pastors/{pastorId}/history` | getPastorHistory | `pastorId` *(req)* (path) |
| `GET` | `/api/v1/pastorate/transfers` | getTransfers | `status` (query) |
| `POST` | `/api/v1/pastorate/transfers` | createTransfer | `pastorId` *(req)* (query), `toOrgUnitId` *(req)* (query), `reason` *(req)* (query) |
| `POST` | `/api/v1/pastorate/transfers/{id}/approve` | approveTransfer | `id` *(req)* (path) |
| `POST` | `/api/v1/pastorate/transfers/{id}/reject` | rejectTransfer | `id` *(req)* (path), `reason` *(req)* (query) |

## payment-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/payments` | recent_2 | — |
| `GET` | `/api/v1/payments/dashboard` | dashboard_2 | — |
| `POST` | `/api/v1/payments/initiate` | initiate | — |
| `GET` | `/api/v1/payments/mine` | mine_2 | — |
| `POST` | `/api/v1/payments/recurring` | createRecurring | — |
| `GET` | `/api/v1/payments/recurring/mine` | myRecurring | — |
| `GET` | `/api/v1/payments/recurring/stats` | recurringStats | — |
| `POST` | `/api/v1/payments/recurring/{id}/cancel` | cancelRecurring | `id` *(req)* (path) |
| `GET` | `/api/v1/payments/stats` | stats_19 | — |
| `POST` | `/api/v1/payments/webhook` | webhook | `X-Webhook-Secret` (header) |
| `GET` | `/api/v1/payments/{id}` | status_3 | `id` *(req)* (path) |
| `POST` | `/api/v1/payments/{id}/cancel` | cancel_1 | `id` *(req)* (path) |
| `GET` | `/api/v1/payments/{id}/tax-receipt` | taxReceipt | `id` *(req)* (path) |

## payment-webhook-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/payments/webhooks/generic` | genericCallback | `X-Webhook-Signature` (header) |
| `GET` | `/api/v1/payments/webhooks/logs` | listLogs | `provider` (query), `status` (query), `page` (query), `size` (query) |
| `GET` | `/api/v1/payments/webhooks/logs/stats` | logStats | — |
| `POST` | `/api/v1/payments/webhooks/mpesa` | mpesaCallback | — |
| `POST` | `/api/v1/payments/webhooks/mtn/verify` | mtnManualVerify | `X-MTN-Signature` (header) |
| `POST` | `/api/v1/payments/webhooks/orange` | orangeCallback | `X-Orange-Signature` (header) |

## people-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/people` | search_2 | `search` (query), `status` (query), `campusId` (query), `withoutSpace` (query), `withoutFamily` (query), `page` (query), `size` (query) |
| `POST` | `/api/v1/people/register` | registerPerson | `source` (query) |
| `DELETE` | `/api/v1/people/roles/{assignmentId}` | endRole | `assignmentId` *(req)* (path), `reason` (query) |
| `GET` | `/api/v1/people/without-space` | getPeopleWithoutSpace | — |
| `GET` | `/api/v1/people/{id}` | getPerson | `id` *(req)* (path) |
| `POST` | `/api/v1/people/{masterId}/merge/{duplicateId}` | mergePersons | `masterId` *(req)* (path), `duplicateId` *(req)* (path) |
| `GET` | `/api/v1/people/{personId}/event-assignments` | getPersonEventAssignments | `personId` *(req)* (path) |
| `GET` | `/api/v1/people/{personId}/roles` | getPersonRoles | `personId` *(req)* (path) |
| `POST` | `/api/v1/people/{personId}/roles` | assignRole | `personId` *(req)* (path), `roleId` *(req)* (query), `orgUnitId` (query), `spaceId` (query), `reason` (query) |
| `GET` | `/api/v1/people/{personId}/spaces` | getPersonSpaces | `personId` *(req)* (path) |
| `POST` | `/api/v1/people/{personId}/spaces` | assignToSpace | `personId` *(req)* (path), `spaceId` *(req)* (query), `membershipType` (query), `responsibility` (query) |
| `DELETE` | `/api/v1/people/{personId}/spaces/{spaceId}` | removeFromSpace | `personId` *(req)* (path), `spaceId` *(req)* (path) |
| `POST` | `/api/v1/people/{personId}/transfer` | transferPastor | `personId` *(req)* (path), `newOrgUnitId` *(req)* (query), `reason` *(req)* (query) |

## permission-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/permissions` | getAll_1 | — |
| `GET` | `/api/v1/permissions/catalog` | catalog | — |
| `GET` | `/api/v1/permissions/roles` | roles | — |
| `POST` | `/api/v1/permissions/roles` | createRole | — |
| `POST` | `/api/v1/permissions/roles/duplicate` | duplicateRole | — |
| `PUT` | `/api/v1/permissions/roles/{key}` | updateRole | `key` *(req)* (path) |
| `DELETE` | `/api/v1/permissions/roles/{key}` | deleteRole | `key` *(req)* (path) |
| `GET` | `/api/v1/permissions/{role}` | getByRole | `role` *(req)* (path) |
| `PUT` | `/api/v1/permissions/{role}/{permission}` | update_15 | `role` *(req)* (path), `permission` *(req)* (path) |
| `PUT` | `/api/v1/permissions/{role}/{permission}/rwd` | updateRWD | `role` *(req)* (path), `permission` *(req)* (path) |

## personal-objective-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/personal-objectives` | list_26 | — |
| `POST` | `/api/v1/personal-objectives` | create_36 | — |
| `GET` | `/api/v1/personal-objectives/stats` | stats_18 | — |
| `GET` | `/api/v1/personal-objectives/{id}` | get_27 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/personal-objectives/{id}/progress` | progress_1 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/personal-objectives/{id}/status` | updateStatus_7 | `id` *(req)* (path) |

## platform-config-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/platform/admin/menus` | allMenus | — |
| `GET` | `/api/v1/platform/gating` | gating | — |
| `GET` | `/api/v1/platform/menus` | myMenus | — |
| `POST` | `/api/v1/platform/menus` | createMenu | — |
| `POST` | `/api/v1/platform/menus/reorder` | reorderMenus | — |
| `PUT` | `/api/v1/platform/menus/{id}` | updateMenu | `id` *(req)* (path) |
| `DELETE` | `/api/v1/platform/menus/{id}` | deleteMenu | `id` *(req)* (path) |
| `GET` | `/api/v1/platform/modules` | modules_1 | — |
| `POST` | `/api/v1/platform/modules` | createModule | — |
| `PUT` | `/api/v1/platform/modules/{key}` | toggleModule | `key` *(req)* (path) |
| `DELETE` | `/api/v1/platform/modules/{key}` | deleteModule | `key` *(req)* (path) |
| `PUT` | `/api/v1/platform/modules/{key}/edit` | updateModule | `key` *(req)* (path) |
| `GET` | `/api/v1/platform/revisions` | revisions | `entityType` (query), `page` (query), `size` (query) |

## platform-currencies-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/platform/currencies` | list_60 | — |

## platform-meta-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/public/meta` | meta | — |

## platform-provisioning-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/platform/admin/provisioning` | provisionOrganization | — |
| `POST` | `/api/v1/platform/admin/provisioning/church` | provisionChurch | — |
| `POST` | `/api/v1/platform/admin/provisioning/department` | provisionDepartment | — |
| `POST` | `/api/v1/platform/admin/provisioning/family` | provisionFamily | — |

## platform-quota-usage-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/platform/admin/quota-usage/tenants` | listTenantUsage | `page` (query), `size` (query) |
| `GET` | `/api/v1/platform/admin/quota-usage/tenants/{tenantId}` | getTenantSnapshot | `tenantId` *(req)* (path) |

## prayer-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/prayers` | findAll_7 | `page` (query), `size` (query), `familleId` (query), `auteurId` (query), `statut` (query), `categorie` (query), `visibilite` (query) |
| `POST` | `/api/v1/prayers` | create_34 | — |
| `GET` | `/api/v1/prayers/actions-de-grace` | getActionsDeGrace | `familleId` (query) |
| `GET` | `/api/v1/prayers/by-ame/{ameId}` | findByAmeId | `ameId` *(req)* (path) |
| `GET` | `/api/v1/prayers/{id}` | findById_5 | `id` *(req)* (path) |
| `PUT` | `/api/v1/prayers/{id}` | update_14 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/prayers/{id}` | delete_9 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/prayers/{id}/answer` | markAsAnswered | `id` *(req)* (path) |

## prayer-journal-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/prayer-journal` | list_25 | `page` (query), `size` (query), `membreId` (query) |
| `POST` | `/api/v1/prayer-journal` | create_35 | — |
| `GET` | `/api/v1/prayer-journal/stats` | stats_17 | — |
| `GET` | `/api/v1/prayer-journal/{id}` | get_26 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/prayer-journal/{id}` | delete_34 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/prayer-journal/{id}/answered` | markAnswered | `id` *(req)* (path) |
| `PATCH` | `/api/v1/prayer-journal/{id}/remembered` | markRemembered | `id` *(req)* (path) |

## prediction-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/predictions` | list_59 | — |
| `POST` | `/api/predictions/generate` | generate_2 | — |
| `POST` | `/api/predictions/generate-all` | generateAll | — |
| `GET` | `/api/predictions/type/{type}` | listByType_2 | `type` *(req)* (path) |
| `GET` | `/api/v1/predictions` | list_58 | — |
| `POST` | `/api/v1/predictions/generate` | generate_1 | — |
| `POST` | `/api/v1/predictions/generate-all` | generateAll_1 | — |
| `GET` | `/api/v1/predictions/type/{type}` | listByType_1 | `type` *(req)* (path) |

## program-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/programs` | findAll_6 | `all` (query) |
| `POST` | `/api/v1/programs` | create_33 | — |
| `GET` | `/api/v1/programs/active` | findActive | — |
| `GET` | `/api/v1/programs/{id}` | findById_4 | `id` *(req)* (path) |
| `PUT` | `/api/v1/programs/{id}` | update_13 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/programs/{id}` | delete_8 | `id` *(req)* (path) |

## prophetic-journal-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/prophetic` | create_32 | — |
| `GET` | `/api/v1/prophetic/mine` | myEntries | — |
| `GET` | `/api/v1/prophetic/public` | publicEntries | — |
| `GET` | `/api/v1/prophetic/stats` | stats_16 | — |
| `GET` | `/api/v1/prophetic/tag/{tag}` | findByTag | `tag` *(req)* (path) |
| `GET` | `/api/v1/prophetic/type/{type}` | findByType | `type` *(req)* (path) |
| `GET` | `/api/v1/prophetic/{id}` | findById_3 | `id` *(req)* (path) |
| `PUT` | `/api/v1/prophetic/{id}` | update_12 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/prophetic/{id}` | delete_7 | `id` *(req)* (path) |
| `GET` | `/api/v1/prophetic/{id}/correlated` | correlated | `id` *(req)* (path) |

## public-api-docs-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/public/docs` | getPublicDocs | — |
| `GET` | `/api/v1/public/docs/openapi.yaml` | getOpenApiYaml | — |

## public-billing-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/public/billing/status` | status_2 | — |

## public-churches-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/public/churches` | list_57 | `country` (query), `q` (query) |

## public-contact-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/public/contact` | submitContact | — |

## public-legal-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/public/legal` | list_56 | — |
| `GET` | `/api/v1/public/legal/{code}` | get_25 | `code` *(req)* (path), `version` (query), `language` (query) |

## public-saas-plan-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/public/plans` | getPublicPlans | — |

## push-token-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/notifications/push-status` | pushStatus | — |
| `POST` | `/api/v1/notifications/register-token` | registerToken | — |
| `POST` | `/api/v1/notifications/unregister-token` | unregisterToken | — |

## quest-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/quest/award` | award | — |
| `GET` | `/api/v1/quest/contextual-badges` | contextualBadges | — |
| `GET` | `/api/v1/quest/leaderboard` | leaderboard | — |
| `GET` | `/api/v1/quest/leaderboard/groups` | groupLeaderboard | `by` (query) |
| `GET` | `/api/v1/quest/profile` | myProfile | — |
| `GET` | `/api/v1/quest/profile/{userId}` | profileFor | `userId` *(req)* (path) |
| `GET` | `/api/v1/quest/quests` | weeklyQuests | — |
| `GET` | `/api/v1/quest/stats` | stats_15 | — |
| `GET` | `/api/v1/quest/weekly-challenges` | weeklyChallenges | — |

## quota-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/quotas/check/{resource}` | checkQuota | `resource` *(req)* (path) |
| `GET` | `/api/v1/admin/quotas/features` | getAllFeatures | — |
| `POST` | `/api/v1/admin/quotas/features/{featureKey}` | toggleFeature | `featureKey` *(req)* (path) |
| `GET` | `/api/v1/admin/quotas/usage` | getQuotaUsage | — |

## referral-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/referrals` | list_24 | `page` (query), `size` (query) |
| `POST` | `/api/v1/referrals` | create_30 | — |
| `POST` | `/api/v1/referrals/invite` | invite | — |
| `GET` | `/api/v1/referrals/stats` | stats_14 | — |
| `GET` | `/api/v1/referrals/{id}` | get_24 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/referrals/{id}/status` | updateStatus_6 | `id` *(req)* (path) |

## report-export-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/reports/export/consolidated-pdf` | exportConsolidatedPdf | `familleId` (query), `semaine` (query) |
| `GET` | `/api/v1/reports/export/executive-pdf` | exportExecutivePdf | `periode` (query), `from` (query), `to` (query) |
| `GET` | `/api/v1/reports/export/family-weekly` | exportFamilyReports | `familleId` (query), `semaine` (query) |
| `GET` | `/api/v1/reports/export/maker-pdf` | exportMakerPdf | `faiseurId` (query), `familleId` (query), `semaine` (query) |
| `GET` | `/api/v1/reports/export/maker-weekly` | exportMakerReports | `faiseurId` (query), `familleId` (query), `semaine` (query) |

## resource-scope-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/scoping/scopes` | scopes | — |
| `GET` | `/api/v1/scoping/visible-units` | visibleUnits | `viewerUnitId` (query) |

## reverse-mentoring-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/reverse-mentoring` | list_22 | — |
| `POST` | `/api/reverse-mentoring` | create_28 | — |
| `GET` | `/api/reverse-mentoring/pending` | pending | — |
| `GET` | `/api/reverse-mentoring/stats` | stats_12 | — |
| `POST` | `/api/reverse-mentoring/{id}/accept` | accept_1 | `id` *(req)* (path) |
| `POST` | `/api/reverse-mentoring/{id}/resolve` | resolve | `id` *(req)* (path) |
| `GET` | `/api/v1/reverse-mentoring` | list_23 | — |
| `POST` | `/api/v1/reverse-mentoring` | create_29 | — |
| `GET` | `/api/v1/reverse-mentoring/pending` | pending_1 | — |
| `GET` | `/api/v1/reverse-mentoring/stats` | stats_13 | — |
| `POST` | `/api/v1/reverse-mentoring/{id}/accept` | accept | `id` *(req)* (path) |
| `POST` | `/api/v1/reverse-mentoring/{id}/resolve` | resolve_1 | `id` *(req)* (path) |

## reward-certificate-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/reward-certificates` | list_21 | — |
| `POST` | `/api/v1/reward-certificates` | issue | — |
| `GET` | `/api/v1/reward-certificates/eligible` | eligible | — |
| `GET` | `/api/v1/reward-certificates/mine` | mine_1 | — |

## reward-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/rewards` | list_20 | — |
| `POST` | `/api/v1/rewards` | create_27 | — |
| `POST` | `/api/v1/rewards/claim` | claim | — |
| `GET` | `/api/v1/rewards/my-claims` | myClaims | — |

## role-management-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/admin/roles` | createRole_1 | — |
| `POST` | `/api/v1/admin/roles/assign` | assignRoleToUser | — |
| `GET` | `/api/v1/admin/roles/audit` | getAuditReport | — |
| `GET` | `/api/v1/admin/roles/custom` | getCustomRoles | — |
| `GET` | `/api/v1/admin/roles/hierarchy` | getRoleHierarchy | — |
| `GET` | `/api/v1/admin/roles/key/{key}` | getRoleByKey | `key` *(req)* (path) |
| `GET` | `/api/v1/admin/roles/matrix` | getFullMatrix | — |
| `GET` | `/api/v1/admin/roles/matrix/check` | checkPermission | `userId` *(req)* (query), `permissionKey` *(req)* (query), `nodeId` (query) |
| `GET` | `/api/v1/admin/roles/matrix/node/{nodeId}` | getNodeMatrix | `nodeId` *(req)* (path) |
| `GET` | `/api/v1/admin/roles/matrix/scope/{scope}` | getMatrixByScope | `scope` *(req)* (path) |
| `GET` | `/api/v1/admin/roles/matrix/user/{userId}` | getUserMatrix | `userId` *(req)* (path) |
| `GET` | `/api/v1/admin/roles/matrix/users` | getAllUsersMatrix | — |
| `GET` | `/api/v1/admin/roles/overview` | getAllRoles | — |
| `GET` | `/api/v1/admin/roles/permissions` | getAllPermissions | — |
| `GET` | `/api/v1/admin/roles/permissions/category/{category}` | getPermissionsByCategory | `category` *(req)* (path) |
| `GET` | `/api/v1/admin/roles/permissions/grouped/category` | getPermissionsGroupedByCategory | — |
| `GET` | `/api/v1/admin/roles/permissions/grouped/scope` | getPermissionsGroupedByScope | — |
| `GET` | `/api/v1/admin/roles/permissions/scope/{scope}` | getPermissionsByScope | `scope` *(req)* (path) |
| `GET` | `/api/v1/admin/roles/system` | getSystemRoles | — |
| `GET` | `/api/v1/admin/roles/{id}` | getRole | `id` *(req)* (path) |
| `PUT` | `/api/v1/admin/roles/{id}` | updateRole_1 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/admin/roles/{id}` | deleteRole_1 | `id` *(req)* (path) |
| `GET` | `/api/v1/admin/roles/{id}/permissions` | getRolePermissions | `id` *(req)* (path) |
| `PUT` | `/api/v1/admin/roles/{id}/permissions` | assignPermissions | `id` *(req)* (path) |
| `POST` | `/api/v1/admin/roles/{id}/permissions` | addPermission | `id` *(req)* (path) |
| `DELETE` | `/api/v1/admin/roles/{id}/permissions/{permissionKey}` | removePermission | `id` *(req)* (path), `permissionKey` *(req)* (path) |
| `GET` | `/api/v1/admin/roles/{id}/validate` | validateRole | `id` *(req)* (path) |

## sabbath-dashboard-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/sabbath-dashboard` | dashboard_1 | — |

## search-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/search` | search_1 | `q` *(req)* (query), `page` (query), `size` (query) |
| `GET` | `/api/v1/search/autocomplete` | autocomplete | `q` *(req)* (query), `limit` (query) |
| `GET` | `/api/v1/search/profile/{soulId}` | getCompleteProfile | `soulId` *(req)* (path) |

## sermon-assistant-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/sermon-assistant/outlines` | outlines | — |

## sermon-transcription-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/sermons` | list_19 | `q` (query), `pageable` *(req)* (query) |
| `POST` | `/api/v1/sermons` | create_26 | — |
| `GET` | `/api/v1/sermons/stats` | stats_11 | — |
| `GET` | `/api/v1/sermons/{id}` | get_7 | `id` *(req)* (path) |
| `PUT` | `/api/v1/sermons/{id}` | update_11 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/sermons/{id}` | delete_6 | `id` *(req)* (path) |
| `POST` | `/api/v1/sermons/{id}/transcribe` | triggerTranscription | `id` *(req)* (path) |

## sermon-translation-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/sermons/translations` | listAll_1 | — |
| `POST` | `/api/v1/sermons/translations` | requestTranslation | — |
| `GET` | `/api/v1/sermons/translations/by-sermon/{sermonId}` | listBySermon | `sermonId` *(req)* (path) |
| `GET` | `/api/v1/sermons/translations/{id}` | get_23 | `id` *(req)* (path) |
| `POST` | `/api/v1/sermons/translations/{id}/complete` | completeTranslation | `id` *(req)* (path) |

## settings-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/public/settings` | publicBranding | — |
| `GET` | `/api/v1/settings` | get_6 | — |
| `PUT` | `/api/v1/settings` | update_10 | — |
| `POST` | `/api/v1/settings/reset` | reset_1 | — |

## skill-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/skills` | list_17 | — |
| `POST` | `/api/v1/skills` | create_24 | — |
| `GET` | `/api/v1/skills/matrix` | matrix | — |
| `GET` | `/api/v1/skills/member/{memberId}` | byMember | `memberId` *(req)* (path) |

## skill-match-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/skill-matching` | list_18 | — |
| `POST` | `/api/v1/skill-matching` | create_25 | — |
| `POST` | `/api/v1/skill-matching/run` | runMatching | — |
| `GET` | `/api/v1/skill-matching/stats` | stats_10 | — |
| `GET` | `/api/v1/skill-matching/{id}` | get_22 | `id` *(req)* (path) |
| `POST` | `/api/v1/skill-matching/{id}/respond` | respond | `id` *(req)* (path), `decision` *(req)* (query) |

## skills-matrix-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/skills-matrix` | listAll | — |
| `POST` | `/api/v1/skills-matrix` | evaluate | — |
| `GET` | `/api/v1/skills-matrix/department/{departmentId}/matrix` | getDepartmentMatrix | `departmentId` *(req)* (path) |
| `GET` | `/api/v1/skills-matrix/member/{membreId}` | listByMember | `membreId` *(req)* (path) |
| `GET` | `/api/v1/skills-matrix/member/{membreId}/matrix` | getMatrix | `membreId` *(req)* (path) |

## smart-alert-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/smart-alerts/scan` | runChecksNow | — |
| `GET` | `/api/v1/smart-alerts/summary` | getAnomalySummary | — |

## social-auth-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/auth/google` | googleLogin | — |
| `POST` | `/api/v1/auth/magic-link` | sendMagicLink | — |
| `GET` | `/api/v1/auth/magic-link/verify` | verifyMagicLink | `token` *(req)* (query) |

## soul-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/souls` | findAll_4 | `page` (query), `size` (query), `faiseurId` (query), `familleId` (query), `typeDisciple` (query), `statut` (query), `search` (query), `sortBy` (query), `sortDir` (query) |
| `POST` | `/api/v1/souls` | create_20 | — |
| `GET` | `/api/v1/souls/by-faiseur/{faiseurId}` | findByFaiseur | `faiseurId` *(req)* (path) |
| `GET` | `/api/v1/souls/by-famille/{familleId}` | findByFamille_1 | `familleId` *(req)* (path) |
| `GET` | `/api/v1/souls/en-difficulte` | findEnDifficulte | — |
| `GET` | `/api/v1/souls/filter` | filterSouls | `page` (query), `size` (query), `etatSpirituel` (query), `statut` (query), `faiseurId` (query), `familleId` (query) |
| `POST` | `/api/v1/souls/retraction-request` | createRetractionRequest | — |
| `PATCH` | `/api/v1/souls/retraction-request/{id}/approve` | approveRetraction | `id` *(req)* (path) |
| `PATCH` | `/api/v1/souls/retraction-request/{id}/reject` | rejectRetraction | `id` *(req)* (path) |
| `GET` | `/api/v1/souls/retraction-requests` | getRetractionRequests_1 | `statut` (query), `page` (query), `size` (query) |
| `GET` | `/api/v1/souls/suggest-faiseur/{familleId}` | suggestFaiseur | `familleId` *(req)* (path) |
| `GET` | `/api/v1/souls/trash` | trash | `page` (query), `size` (query) |
| `GET` | `/api/v1/souls/{id}` | findById_2 | `id` *(req)* (path) |
| `PUT` | `/api/v1/souls/{id}` | update_9 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/souls/{id}` | delete_5 | `id` *(req)* (path) |
| `POST` | `/api/v1/souls/{id}/exit` | markAsExited | `id` *(req)* (path) |
| `GET` | `/api/v1/souls/{id}/exits` | getExits | `id` *(req)* (path) |
| `GET` | `/api/v1/souls/{id}/history` | getHistory | `id` *(req)* (path) |
| `GET` | `/api/v1/souls/{id}/pastoral-360` | getPastoral360 | `id` *(req)* (path) |
| `GET` | `/api/v1/souls/{id}/qr-code` | getQrCode | `id` *(req)* (path) |
| `PATCH` | `/api/v1/souls/{id}/reassign` | reassign | `id` *(req)* (path) |
| `POST` | `/api/v1/souls/{id}/reintegrate` | reintegrate | `id` *(req)* (path) |
| `PATCH` | `/api/v1/souls/{id}/restore` | restore_1 | `id` *(req)* (path) |
| `GET` | `/api/v1/souls/{id}/spiritual-score` | spiritualScore | `id` *(req)* (path) |
| `GET` | `/api/v1/souls/{id}/spiritual-score-detail` | getSpiritualScoreDetail | `id` *(req)* (path) |
| `GET` | `/api/v1/souls/{id}/spiritual-score/history` | spiritualScoreHistory | `id` *(req)* (path) |
| `GET` | `/api/v1/souls/{soulId}/retraction-requests` | getRetractionRequests | `soulId` *(req)* (path) |

## soul-discipline-event-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/souls/{soulId}/discipline` | findAll_5 | `soulId` *(req)* (path), `page` (query), `size` (query), `categorie` (query) |
| `POST` | `/api/v1/souls/{soulId}/discipline` | create_23 | `soulId` *(req)* (path) |
| `GET` | `/api/v1/souls/{soulId}/discipline/stats` | stats_9 | `soulId` *(req)* (path) |
| `GET` | `/api/v1/souls/{soulId}/discipline/{id}` | findById_12 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/souls/{soulId}/discipline/{id}` | delete_33 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/souls/{soulId}/discipline/{id}/resolve` | resolve_3 | `id` *(req)* (path) |

## soul-note-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/souls/{soulId}/notes` | findBySoulId | `soulId` *(req)* (path) |
| `POST` | `/api/v1/souls/{soulId}/notes` | create_21 | `soulId` *(req)* (path) |
| `PUT` | `/api/v1/souls/{soulId}/notes/{noteId}` | update_8 | `noteId` *(req)* (path) |
| `DELETE` | `/api/v1/souls/{soulId}/notes/{noteId}` | delete_4 | `noteId` *(req)* (path) |

## soul-tag-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/soul-tags/available` | allTags | — |
| `GET` | `/api/v1/soul-tags/{soulId}` | getTags | `soulId` *(req)* (path) |
| `POST` | `/api/v1/soul-tags/{soulId}` | addTag | `soulId` *(req)* (path) |
| `DELETE` | `/api/v1/soul-tags/{soulId}/{tag}` | removeTag | `soulId` *(req)* (path), `tag` *(req)* (path) |

## space-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/spaces` | list_16 | `type` (query) |
| `POST` | `/api/v1/spaces` | create_19 | — |
| `GET` | `/api/v1/spaces/customizable` | customizable | — |
| `GET` | `/api/v1/spaces/organization-unit/{organizationUnitId}` | byOrganizationUnit | `organizationUnitId` *(req)* (path) |
| `GET` | `/api/v1/spaces/{spaceId}` | get_5 | `spaceId` *(req)* (path) |
| `PUT` | `/api/v1/spaces/{spaceId}` | update_7 | `spaceId` *(req)* (path) |
| `DELETE` | `/api/v1/spaces/{spaceId}` | archive | `spaceId` *(req)* (path) |
| `GET` | `/api/v1/spaces/{spaceId}/can-customize` | canCustomize | `spaceId` *(req)* (path) |

## space-export-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/spaces/import-format` | describeFormat | — |
| `GET` | `/api/v1/spaces/{spaceId}/export` | exportSpace | `spaceId` *(req)* (path) |
| `GET` | `/api/v1/tenant/export` | exportTenant | — |

## space-import-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/spaces/import` | importFile | `dryRun` (query) |

## space-template-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/space-templates` | getAllTemplates | `page` (query), `size` (query) |
| `POST` | `/api/v1/space-templates` | createTemplate | — |
| `GET` | `/api/v1/space-templates/code/{code}` | getTemplateByCode | `code` *(req)* (path) |
| `GET` | `/api/v1/space-templates/list` | getAllTemplatesList | — |
| `POST` | `/api/v1/space-templates/{code}/create-space` | createSpaceFromTemplate | `code` *(req)* (path) |
| `GET` | `/api/v1/space-templates/{id}` | getTemplateById | `id` *(req)* (path) |
| `PUT` | `/api/v1/space-templates/{id}` | updateTemplate | `id` *(req)* (path) |
| `DELETE` | `/api/v1/space-templates/{id}` | deleteTemplate | `id` *(req)* (path) |

## spiritual-challenge-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/spiritual-challenges` | list_15 | `page` (query), `size` (query) |
| `POST` | `/api/v1/spiritual-challenges` | create_18 | — |
| `GET` | `/api/v1/spiritual-challenges/stats` | stats_8 | — |
| `GET` | `/api/v1/spiritual-challenges/{id}` | get_21 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/spiritual-challenges/{id}/progress` | progress | `id` *(req)* (path) |
| `PATCH` | `/api/v1/spiritual-challenges/{id}/status` | updateStatus_5 | `id` *(req)* (path) |

## spiritual-journal-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/spiritual-journals` | create_17 | — |
| `GET` | `/api/v1/spiritual-journals/by-author/{authorId}` | listByAuthor | `authorId` *(req)* (path) |
| `GET` | `/api/v1/spiritual-journals/by-type/{authorId}/{type}` | listByType | `authorId` *(req)* (path), `type` *(req)* (path) |
| `GET` | `/api/v1/spiritual-journals/favorites/{authorId}` | listFavorites | `authorId` *(req)* (path) |
| `GET` | `/api/v1/spiritual-journals/stats/{authorId}` | stats_7 | `authorId` *(req)* (path) |
| `GET` | `/api/v1/spiritual-journals/{id}` | get_4 | `id` *(req)* (path) |
| `PUT` | `/api/v1/spiritual-journals/{id}` | update_6 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/spiritual-journals/{id}` | delete_3 | `id` *(req)* (path) |
| `POST` | `/api/v1/spiritual-journals/{id}/toggle-favorite` | toggleFavorite | `id` *(req)* (path) |

## spiritual-journey-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/spiritual-journey` | getJourney | — |

## stream-chat-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/stream-chat/{streamId}` | list_14 | `streamId` *(req)* (path) |
| `POST` | `/api/stream-chat/{streamId}` | send_1 | `streamId` *(req)* (path) |
| `GET` | `/api/stream-chat/{streamId}/count` | count | `streamId` *(req)* (path) |
| `GET` | `/api/v1/stream-chat/{streamId}` | list_13 | `streamId` *(req)* (path) |
| `POST` | `/api/v1/stream-chat/{streamId}` | send | `streamId` *(req)* (path) |
| `GET` | `/api/v1/stream-chat/{streamId}/count` | count_1 | `streamId` *(req)* (path) |

## stripe-billing-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/billing/stripe/checkout` | checkout | — |
| `POST` | `/api/v1/billing/stripe/portal` | portal | — |
| `GET` | `/api/v1/billing/stripe/status` | status_5 | — |
| `GET` | `/api/v1/billing/stripe/subscription` | currentSubscription | — |

## stripe-webhook-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/payments/webhooks/stripe` | stripeWebhook | `Stripe-Signature` (header) |

## subscription-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/admin/subscription/cancel` | cancelSubscription | — |
| `POST` | `/api/v1/admin/subscription/change-plan` | changePlan | — |
| `GET` | `/api/v1/admin/subscription/current` | getCurrentSubscription | — |
| `GET` | `/api/v1/admin/subscription/plans` | getAvailablePlans | — |
| `POST` | `/api/v1/admin/subscription/reactivate` | reactivateSubscription | — |
| `POST` | `/api/v1/admin/subscription/subscribe` | subscribe | — |
| `GET` | `/api/v1/admin/subscription/usage` | getUsage | — |

## succession-plan-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/succession` | list_11 | — |
| `POST` | `/api/v1/succession` | create_14 | — |
| `GET` | `/api/v1/succession/{id}` | get_20 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/succession/{id}` | delete_32 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/succession/{id}/readiness` | updateReadiness | `id` *(req)* (path) |
| `PATCH` | `/api/v1/succession/{id}/status` | updateStatus_4 | `id` *(req)* (path) |

## super-admin-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/platform/admin/audit-logs` | listAuditLogs | `page` (query), `size` (query) |
| `GET` | `/api/v1/platform/admin/dashboard` | getAdminDashboard | — |
| `GET` | `/api/v1/platform/admin/dashboard/overview` | getOverview | — |
| `GET` | `/api/v1/platform/admin/feature-flags` | getFeatureFlags_1 | — |
| `POST` | `/api/v1/platform/admin/feature-flags` | createFeatureFlag | — |
| `PUT` | `/api/v1/platform/admin/feature-flags/{key}` | setFeatureFlag | `key` *(req)* (path) |
| `DELETE` | `/api/v1/platform/admin/feature-flags/{key}` | deleteFeatureFlag | `key` *(req)* (path) |
| `GET` | `/api/v1/platform/admin/health` | getSystemHealth | — |
| `POST` | `/api/v1/platform/admin/impersonate` | startImpersonation_1 | — |
| `POST` | `/api/v1/platform/admin/impersonate/stop` | stopImpersonation_1 | — |
| `GET` | `/api/v1/platform/admin/plans` | listPlans | — |
| `POST` | `/api/v1/platform/admin/plans` | createOrUpdatePlan | — |
| `GET` | `/api/v1/platform/admin/registration-requests` | listRegistrationRequests | `page` (query), `size` (query) |
| `POST` | `/api/v1/platform/admin/registration-requests/{id}/approve` | approveRegistrationRequest | `id` *(req)* (path) |
| `POST` | `/api/v1/platform/admin/registration-requests/{id}/reject` | rejectRegistrationRequest | `id` *(req)* (path) |
| `GET` | `/api/v1/platform/admin/subscriptions` | getSubscriptions | `page` (query), `size` (query), `status` (query) |
| `GET` | `/api/v1/platform/admin/tenants` | listTenants | `page` (query), `size` (query), `status` (query), `plan` (query), `search` (query), `cursor` (query) |
| `POST` | `/api/v1/platform/admin/tenants` | createTenant | — |
| `GET` | `/api/v1/platform/admin/tenants/{id}` | getTenantDetails | `id` *(req)* (path) |
| `PUT` | `/api/v1/platform/admin/tenants/{id}` | updateTenant | `id` *(req)* (path) |
| `POST` | `/api/v1/platform/admin/tenants/{id}/archive` | archiveTenant | `id` *(req)* (path) |
| `POST` | `/api/v1/platform/admin/tenants/{id}/reactivate` | reactivateTenant | `id` *(req)* (path) |
| `POST` | `/api/v1/platform/admin/tenants/{id}/suspend` | suspendTenant | `id` *(req)* (path) |

## super-admin-saas-plan-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/saas/plans` | getAllPlans | — |
| `POST` | `/api/v1/admin/saas/plans` | createPlan_1 | — |
| `GET` | `/api/v1/admin/saas/plans/active` | getActivePlans | — |
| `GET` | `/api/v1/admin/saas/plans/subscriptions` | getAllSubscriptions | — |
| `GET` | `/api/v1/admin/saas/plans/usage/{tenantId}` | getUsage_1 | `tenantId` *(req)* (path) |
| `PUT` | `/api/v1/admin/saas/plans/{key}` | updatePlan_1 | `key` *(req)* (path) |
| `DELETE` | `/api/v1/admin/saas/plans/{key}` | deactivatePlan | `key` *(req)* (path) |
| `POST` | `/api/v1/admin/saas/plans/{key}/subscribe/{tenantId}` | subscribe_1 | `key` *(req)* (path), `tenantId` *(req)* (path), `billingCycle` (query) |

## survey-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/surveys` | list_10 | `page` (query), `size` (query), `statut` (query) |
| `POST` | `/api/v1/surveys` | create_13 | — |
| `GET` | `/api/v1/surveys/{id}` | get_19 | `id` *(req)* (path) |
| `POST` | `/api/v1/surveys/{id}/responses` | submitResponse | `id` *(req)* (path) |
| `GET` | `/api/v1/surveys/{id}/results` | results | `id` *(req)* (path) |
| `PATCH` | `/api/v1/surveys/{id}/status` | updateStatus_3 | `id` *(req)* (path) |

## system-config-summary-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/system/config-summary` | configSummary | — |

## team-assignment-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/team-gantt` | list_9 | `start` *(req)* (query), `end` *(req)* (query) |
| `POST` | `/api/v1/team-gantt` | create_12 | — |
| `GET` | `/api/v1/team-gantt/overloads/{equipeId}` | detectOverloads | `equipeId` *(req)* (path), `start` *(req)* (query), `end` *(req)* (query) |
| `GET` | `/api/v1/team-gantt/team/{equipeId}` | listByTeam | `equipeId` *(req)* (path) |
| `GET` | `/api/v1/team-gantt/{id}` | get_18 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/team-gantt/{id}` | delete_31 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/team-gantt/{id}/status` | updateStatus_2 | `id` *(req)* (path) |

## team-task-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/team-tasks` | list_8 | — |
| `POST` | `/api/v1/team-tasks` | create_11 | — |
| `GET` | `/api/v1/team-tasks/stats` | stats_6 | — |
| `GET` | `/api/v1/team-tasks/{id}` | get_17 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/team-tasks/{id}/progression` | updateProgression | `id` *(req)* (path) |
| `PATCH` | `/api/v1/team-tasks/{id}/status` | updateStatus_1 | `id` *(req)* (path) |

## tenant-admin-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/dashboard` | getDashboard_2 | — |
| `GET` | `/api/v1/admin/members` | listMembers | `page` (query), `size` (query), `role` (query), `search` (query) |
| `GET` | `/api/v1/admin/modules` | getModules | — |
| `PUT` | `/api/v1/admin/modules/{moduleKey}` | toggleModule_1 | `moduleKey` *(req)* (path) |
| `GET` | `/api/v1/admin/roles` | listRoles | — |

## tenant-admin-dashboard-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/dashboard/activity` | getActivity | `page` (query), `size` (query) |
| `GET` | `/api/v1/admin/dashboard/members` | getMembers | `page` (query), `size` (query), `role` (query), `status` (query), `search` (query) |
| `GET` | `/api/v1/admin/dashboard/org/stats` | getOrgStats | — |
| `GET` | `/api/v1/admin/dashboard/overview` | getOverview_1 | — |
| `GET` | `/api/v1/admin/dashboard/permissions` | getPermissionsByScope_1 | — |
| `GET` | `/api/v1/admin/dashboard/roles` | getRoles | — |

## tenant-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/tenants` | list_7 | — |
| `POST` | `/api/v1/tenants` | create_10 | — |
| `GET` | `/api/v1/tenants/by-slug/{slug}` | getBySlug | `slug` *(req)* (path) |
| `GET` | `/api/v1/tenants/{id}` | get_2 | `id` *(req)* (path) |
| `PUT` | `/api/v1/tenants/{id}` | update_4 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/tenants/{id}` | delete_1 | `id` *(req)* (path) |
| `POST` | `/api/v1/tenants/{id}/reactivate` | reactivate | `id` *(req)* (path) |

## tenant-feature-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/tenant-features` | getFeatures | — |
| `GET` | `/api/v1/admin/tenant-features/enabled` | getEnabledFeatures | — |
| `PUT` | `/api/v1/admin/tenant-features/{moduleCode}` | updateFeature | `moduleCode` *(req)* (path) |
| `DELETE` | `/api/v1/admin/tenant-features/{moduleCode}` | disableFeature | `moduleCode` *(req)* (path) |

## tenant-settings-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/settings` | getSettings | — |
| `PUT` | `/api/v1/admin/settings` | updateSettings_1 | — |
| `POST` | `/api/v1/admin/settings/branding/assets` | uploadBrandingAsset | `assetType` *(req)* (query) |
| `GET` | `/api/v1/admin/settings/branding/css` | getBrandingCss | — |
| `GET` | `/api/v1/admin/settings/public-branding` | getPublicBranding | — |

## tenant-switcher-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/tenant-switcher/context` | getCurrentContext | — |
| `GET` | `/api/v1/tenant-switcher/my-tenants` | getMyTenants | — |
| `POST` | `/api/v1/tenant-switcher/refresh` | refreshContext | — |
| `POST` | `/api/v1/tenant-switcher/switch` | switchTenant | — |
| `POST` | `/api/v1/tenant-switcher/switch-org` | switchOrganization | — |

## testimony-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/testimonies` | list_6 | `page` (query), `size` (query), `statut` (query), `categorie` (query) |
| `POST` | `/api/v1/testimonies` | create_9 | — |
| `GET` | `/api/v1/testimonies/{id}` | get_16 | `id` *(req)* (path) |
| `POST` | `/api/v1/testimonies/{id}/approve` | approve_1 | `id` *(req)* (path) |
| `POST` | `/api/v1/testimonies/{id}/like` | like | `id` *(req)* (path) |
| `POST` | `/api/v1/testimonies/{id}/reject` | reject_1 | `id` *(req)* (path) |

## ticket-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/tickets` | list_5 | `page` (query), `size` (query), `statut` (query), `categorie` (query) |
| `POST` | `/api/v1/tickets` | create_8 | — |
| `GET` | `/api/v1/tickets/{id}` | get_15 | `id` *(req)* (path) |
| `POST` | `/api/v1/tickets/{id}/messages` | addMessage | `id` *(req)* (path) |
| `PATCH` | `/api/v1/tickets/{id}/status` | updateStatus | `id` *(req)* (path) |

## tontine-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/tontines` | list_4 | — |
| `POST` | `/api/v1/tontines` | create_7 | — |
| `GET` | `/api/v1/tontines/stats` | stats_5 | — |
| `GET` | `/api/v1/tontines/{id}` | detail | `id` *(req)* (path) |
| `POST` | `/api/v1/tontines/{id}/contributions/{memberId}/pay` | markPaid | `id` *(req)* (path), `memberId` *(req)* (path), `tour` (query), `note` (query) |
| `GET` | `/api/v1/tontines/{id}/dashboard` | dashboard | `id` *(req)* (path) |
| `POST` | `/api/v1/tontines/{id}/members` | addMember | `id` *(req)* (path) |
| `POST` | `/api/v1/tontines/{id}/next-round` | nextRound | `id` *(req)* (path) |
| `POST` | `/api/v1/tontines/{id}/notify-due` | notifyDue | `id` *(req)* (path) |
| `GET` | `/api/v1/tontines/{id}/overdue` | overdue | `id` *(req)* (path) |

## training-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/trainings/courses` | findAll_3 | — |
| `POST` | `/api/v1/trainings/courses` | createCourse | — |
| `GET` | `/api/v1/trainings/courses/{courseId}` | findById_11 | `courseId` *(req)* (path) |
| `POST` | `/api/v1/trainings/courses/{courseId}/enroll` | enroll | `courseId` *(req)* (path) |
| `GET` | `/api/v1/trainings/courses/{courseId}/modules` | modules | `courseId` *(req)* (path) |
| `POST` | `/api/v1/trainings/courses/{courseId}/modules` | addModule | `courseId` *(req)* (path) |
| `POST` | `/api/v1/trainings/courses/{courseId}/modules/{moduleId}/complete` | completeModuleRead | `courseId` *(req)* (path), `moduleId` *(req)* (path) |
| `POST` | `/api/v1/trainings/courses/{courseId}/quiz/submit` | submitQuiz | `courseId` *(req)* (path) |
| `POST` | `/api/v1/trainings/modules/{moduleId}/questions` | addQuestion | `moduleId` *(req)* (path) |
| `GET` | `/api/v1/trainings/modules/{moduleId}/quiz` | quiz | `moduleId` *(req)* (path) |
| `GET` | `/api/v1/trainings/my-certificates` | myCertificates | — |
| `GET` | `/api/v1/trainings/my-enrollments` | myEnrollments | — |
| `GET` | `/api/v1/trainings/stats` | stats_4 | — |

## transfer-admin-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/transfers/workflows` | findAll_15 | — |
| `POST` | `/api/v1/admin/transfers/workflows` | create_71 | — |
| `GET` | `/api/v1/admin/transfers/workflows/{id}` | findById_10 | `id` *(req)* (path) |
| `PUT` | `/api/v1/admin/transfers/workflows/{id}` | update_34 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/admin/transfers/workflows/{id}` | delete_26 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/admin/transfers/workflows/{id}/toggle` | toggle_6 | `id` *(req)* (path) |

## transfer-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/transfers` | findAll_2 | `page` (query), `size` (query), `statut` (query), `type` (query) |
| `POST` | `/api/v1/transfers` | create_6 | — |
| `GET` | `/api/v1/transfers/configurations` | configurations | — |
| `GET` | `/api/v1/transfers/{id}` | findById_1 | `id` *(req)* (path) |
| `PUT` | `/api/v1/transfers/{id}` | update_3 | `id` *(req)* (path) |
| `POST` | `/api/v1/transfers/{id}/archive` | archive_1 | `id` *(req)* (path) |
| `POST` | `/api/v1/transfers/{id}/cancel` | cancel | `id` *(req)* (path) |
| `POST` | `/api/v1/transfers/{id}/decide` | decide | `id` *(req)* (path) |
| `GET` | `/api/v1/transfers/{id}/decisions` | decisions | `id` *(req)* (path) |
| `GET` | `/api/v1/transfers/{id}/history` | history | `id` *(req)* (path) |
| `POST` | `/api/v1/transfers/{id}/submit` | submit | `id` *(req)* (path) |

## two-factor-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/auth/2fa/disable` | disable | — |
| `POST` | `/api/v1/auth/2fa/enable` | enable | — |
| `GET` | `/api/v1/auth/2fa/status` | status_6 | — |
| `POST` | `/api/v1/auth/2fa/verify` | verify_2 | — |

## usage-analytics-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/usage-analytics/summary` | summary | `days` (query) |
| `POST` | `/api/v1/usage-analytics/track` | track | — |

## user-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/users` | findAll_1 | `page` (query), `size` (query), `role` (query), `statut` (query), `sortBy` (query), `sortDir` (query) |
| `POST` | `/api/v1/users` | create_5 | — |
| `GET` | `/api/v1/users/by-famille/{familleId}` | findByFamille | `familleId` *(req)* (path) |
| `GET` | `/api/v1/users/by-role/{role}` | findByRole | `role` *(req)* (path) |
| `GET` | `/api/v1/users/evaluation-scores` | getEvaluationScores | `userIds` *(req)* (query) |
| `GET` | `/api/v1/users/faiseur-workload` | getFaiseurWorkload | `familleId` (query) |
| `GET` | `/api/v1/users/me` | me | — |
| `PUT` | `/api/v1/users/me` | updateMyProfile | — |
| `GET` | `/api/v1/users/search` | search | `q` *(req)* (query) |
| `GET` | `/api/v1/users/{id}` | findById | `id` *(req)* (path) |
| `PUT` | `/api/v1/users/{id}` | update_2 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/users/{id}/activate` | activate | `id` *(req)* (path) |
| `PATCH` | `/api/v1/users/{id}/active-role` | setActiveRole | `id` *(req)* (path) |
| `PATCH` | `/api/v1/users/{id}/deactivate` | deactivate_1 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/users/{id}/demote` | demote | `id` *(req)* (path) |
| `PATCH` | `/api/v1/users/{id}/demote-chef` | demoteFromChefDeFamille | `id` *(req)* (path) |
| `GET` | `/api/v1/users/{id}/detail` | getUserDetail | `id` *(req)* (path) |
| `GET` | `/api/v1/users/{id}/faiseur-history` | getFaiseurHistory | `id` *(req)* (path) |
| `DELETE` | `/api/v1/users/{id}/hard-delete` | hardDelete | `id` *(req)* (path) |
| `PATCH` | `/api/v1/users/{id}/promote-chef` | promoteToChefDeFamille | `id` *(req)* (path) |
| `PATCH` | `/api/v1/users/{id}/promote-faiseur` | promoteToFaiseur | `id` *(req)* (path) |
| `PATCH` | `/api/v1/users/{id}/restore` | restore | `id` *(req)* (path) |
| `PUT` | `/api/v1/users/{id}/roles` | replaceRoles | `id` *(req)* (path) |
| `POST` | `/api/v1/users/{id}/roles` | addRole | `id` *(req)* (path) |
| `DELETE` | `/api/v1/users/{id}/roles/{role}` | removeRole | `id` *(req)* (path), `role` *(req)* (path) |
| `PATCH` | `/api/v1/users/{id}/transfer` | transferFaiseur | `id` *(req)* (path) |

## ussd-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/ussd/callback` | callback | `X-Ussd-Secret` (header), `sessionId` *(req)* (query), `phoneNumber` *(req)* (query), `text` (query), `serviceCode` *(req)* (query), `networkCode` (query) |
| `GET` | `/api/v1/ussd/stats` | stats_3 | — |
| `GET` | `/api/v1/ussd/status` | status_1 | — |

## visit-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/visits` | findAll | `page` (query), `size` (query), `search` (query), `statut` (query), `typeVisite` (query) |
| `POST` | `/api/v1/visits` | create_4 | — |
| `GET` | `/api/v1/visits/my` | myVisits | — |
| `GET` | `/api/v1/visits/souls/{soulId}` | findBySoul_1 | `soulId` *(req)* (path) |
| `GET` | `/api/v1/visits/upcoming` | upcoming | — |
| `PATCH` | `/api/v1/visits/{id}` | update_37 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/visits/{id}` | delete_29 | `id` *(req)* (path) |

## voice-assistant-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/voice/commands` | getCommands | — |
| `GET` | `/api/v1/voice/health` | health | — |
| `POST` | `/api/v1/voice/process` | processVoice | — |
| `GET` | `/api/v1/voice/stt-status` | sttStatus | — |
| `POST` | `/api/v1/voice/transcribe` | transcribe | `language` (query), `sessionId` (query) |
| `POST` | `/api/v1/voice/tts` | textToSpeech | — |
| `GET` | `/api/v1/voice/tts-status` | ttsStatus | — |

## voice-notification-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/voice-notifications/status` | status | — |
| `POST` | `/api/v1/voice-notifications/test/all` | testNotifyAll | — |
| `POST` | `/api/v1/voice-notifications/test/role/{role}` | testNotifyRole | `role` *(req)* (path) |
| `POST` | `/api/v1/voice-notifications/test/user/{userId}` | testNotifyUser | `userId` *(req)* (path) |

## voice-report-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/voice-reports` | recent | — |
| `POST` | `/api/v1/voice-reports` | create_3 | — |
| `GET` | `/api/v1/voice-reports/action-items` | actionItems | — |
| `GET` | `/api/v1/voice-reports/mine` | mine | — |
| `GET` | `/api/v1/voice-reports/{id}/structured` | structured | `id` *(req)* (path) |

## volunteer-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/volunteers` | list_3 | — |
| `POST` | `/api/v1/volunteers` | create_2 | — |
| `GET` | `/api/v1/volunteers/match` | match_1 | `skill` (query), `disponibilite` (query) |
| `GET` | `/api/v1/volunteers/stats` | stats_2 | — |
| `GET` | `/api/v1/volunteers/{id}` | get_1 | `id` *(req)* (path) |
| `PUT` | `/api/v1/volunteers/{id}` | update_1 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/volunteers/{id}` | delete | `id` *(req)* (path) |

## webhook-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/admin/webhooks` | list_52 | — |
| `POST` | `/api/v1/admin/webhooks` | create_70 | — |
| `GET` | `/api/v1/admin/webhooks/api-keys` | apiKeys | — |
| `POST` | `/api/v1/admin/webhooks/api-keys` | createApiKey | — |
| `DELETE` | `/api/v1/admin/webhooks/api-keys/{id}` | revokeApiKey | `id` *(req)* (path) |
| `GET` | `/api/v1/admin/webhooks/logs` | logs | — |
| `DELETE` | `/api/v1/admin/webhooks/{id}` | delete_47 | `id` *(req)* (path) |
| `POST` | `/api/v1/admin/webhooks/{id}/test` | test_1 | `id` *(req)* (path) |

## weekly-challenge-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/weekly-challenges` | list_2 | — |
| `POST` | `/api/v1/weekly-challenges` | create_1 | — |
| `POST` | `/api/v1/weekly-challenges/generate` | generate | — |
| `GET` | `/api/v1/weekly-challenges/my` | listMy | — |
| `GET` | `/api/v1/weekly-challenges/stats` | stats_1 | — |
| `PUT` | `/api/v1/weekly-challenges/{id}/progress` | updateProgress | `id` *(req)* (path) |

## whats-app-admin-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `POST` | `/api/v1/whatsapp/broadcast` | broadcast | — |
| `GET` | `/api/v1/whatsapp/config` | getConfig | — |
| `PUT` | `/api/v1/whatsapp/config` | saveConfig | — |
| `POST` | `/api/v1/whatsapp/config/test` | testConnection | — |
| `GET` | `/api/v1/whatsapp/messages` | messages_1 | `limit` (query) |
| `GET` | `/api/v1/whatsapp/stats` | stats | — |

## whats-app-webhook-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/public/whatsapp/webhook` | verify | `hub.mode` *(req)* (query), `hub.verify_token` *(req)* (query), `hub.challenge` *(req)* (query) |
| `POST` | `/api/v1/public/whatsapp/webhook` | receive | `X-Hub-Signature-256` (header) |

## workflow-automation-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/workflow/automations` | list_1 | — |
| `POST` | `/api/v1/workflow/automations` | create | — |
| `GET` | `/api/v1/workflow/automations/{id}` | get_14 | `id` *(req)* (path) |
| `DELETE` | `/api/v1/workflow/automations/{id}` | delete_30 | `id` *(req)* (path) |
| `PATCH` | `/api/v1/workflow/automations/{id}/toggle` | toggle_4 | `id` *(req)* (path) |

## workflow-config-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/workflows` | list_55 | — |
| `GET` | `/api/v1/workflows/{key}` | get | `key` *(req)* (path) |
| `PUT` | `/api/v1/workflows/{key}` | update | `key` *(req)* (path) |
| `POST` | `/api/v1/workflows/{key}/toggle` | toggle | `key` *(req)* (path) |

## workflow-engine-controller

| Méthode | Chemin | Résumé | Paramètres |
|---|---|---|---|
| `GET` | `/api/v1/workflow-engine/definitions` | listDefinitions | — |
| `POST` | `/api/v1/workflow-engine/definitions` | createDefinition | — |
| `GET` | `/api/v1/workflow-engine/definitions/{workflowId}` | getDefinition | `workflowId` *(req)* (path) |
| `PUT` | `/api/v1/workflow-engine/definitions/{workflowId}` | updateDefinition | `workflowId` *(req)* (path) |
| `DELETE` | `/api/v1/workflow-engine/definitions/{workflowId}` | deleteDefinition | `workflowId` *(req)* (path) |
| `GET` | `/api/v1/workflow-engine/definitions/{workflowId}/graph` | graph | `workflowId` *(req)* (path) |
| `GET` | `/api/v1/workflow-engine/definitions/{workflowId}/steps` | listSteps | `workflowId` *(req)* (path) |
| `POST` | `/api/v1/workflow-engine/definitions/{workflowId}/steps` | addStep | `workflowId` *(req)* (path) |
| `POST` | `/api/v1/workflow-engine/definitions/{workflowId}/transitions` | addTransition | `workflowId` *(req)* (path) |
| `POST` | `/api/v1/workflow-engine/escalations/run` | runEscalations | — |
| `POST` | `/api/v1/workflow-engine/instances` | startInstance | — |
| `POST` | `/api/v1/workflow-engine/instances/{instanceId}/reset` | reset | `instanceId` *(req)* (path) |
| `GET` | `/api/v1/workflow-engine/instances/{instanceId}/timeline` | timeline | `instanceId` *(req)* (path) |
| `PUT` | `/api/v1/workflow-engine/steps/{stepId}` | updateStep | `stepId` *(req)* (path) |
| `DELETE` | `/api/v1/workflow-engine/steps/{stepId}` | deleteStep | `stepId` *(req)* (path) |
| `GET` | `/api/v1/workflow-engine/tasks/pending` | pendingTasks | — |
| `POST` | `/api/v1/workflow-engine/tasks/{taskId}/approve` | approve | `taskId` *(req)* (path) |
| `POST` | `/api/v1/workflow-engine/tasks/{taskId}/comment` | comment | `taskId` *(req)* (path) |
| `POST` | `/api/v1/workflow-engine/tasks/{taskId}/reject` | reject | `taskId` *(req)* (path) |
| `DELETE` | `/api/v1/workflow-engine/transitions/{transitionId}` | deleteTransition | `transitionId` *(req)* (path) |

