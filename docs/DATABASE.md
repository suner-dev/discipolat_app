# Base de donnees — Discipolat Church OS

> Version 2.0 — 2026-09-22 · §72 / G6.8. PostgreSQL 16 · JPA/Hibernate 6 · Flyway · 132 migrations (V1→V164). Voir [ARCHITECTURE.md](ARCHITECTURE.md) · [MULTI_TENANT_ARCHITECTURE.md](MULTI_TENANT_ARCHITECTURE.md) · [DEPLOYMENT.md](DEPLOYMENT.md).

## 1. Technologies & regles

- DDL auto `none` (prod) / `validate` (dev) ; migrations auto au boot (`baseline-on-migrate: true`, `classpath:db/migration`).
- Isolation : `tenant_id UUID` sur toutes les tables metier + filtre Hibernate `tenantFilter` (voir [MULTI_TENANT_ARCHITECTURE.md](MULTI_TENANT_ARCHITECTURE.md)) ; RLS sur `audit_event`, `finance_transaction`, `file_entity`.
- Soft delete : `deleted` + `deleted_at` + `deleted_by` (+ audit `soft_delete_audit`, retention 7 ans).
- JSONB : presences, stats agregees, metadata timeline, valeur KPI, custom fields (V153).
- Recherche : pg_trgm fulltext (V163). Fichiers : metadonnees en base, blobs en stockage S3-compatible (§39).

## 2. Historique canonique des migrations (V1→V164)

| Plage | Contenu |
|---|---|
| V1–V11 | Schema initial + seed demo · prieres/evenements/fichiers · prieres seed · role admin/activation · cultes/`weekly_program_templates` · niveaux visibilite priere · evaluations |
| V12–V31 | `soul.user_id` · discipline · multi-roles · index perf/securite · statuts user · `member_space` · situations familiales · presences/requetes · fonctionnalites avancees/premium · evangelisation/objectifs · visites/badges · trainings · rendez-vous · geoloc · separation famille/departement · alertes · risque famille · types programmes · presences soul_key |
| V32–V40 | Workflow transferts (V32-33) · pieces jointes · `church_settings` · modules/menus plateforme · catalogue roles/permissions · custom fields v1 · seed plateforme · beta testing/feedback |
| V41–V67 | Dictionnaires + modules/menus/alertes/notifications · gestion departements (V48-61 : membres, objectifs, rapports, checklists, equipement, documents, annonces, event teams, rappels) · fix contraintes events · upsert evaluations · revisions config (V64) · pages custom (V65) |
| V68–V73 | Finances (V68) · communication (V69) · **multitenancy `tenant_id` (V70)** · templates notification (V71) · permissions enrichies (V72) · integrations (V73) · inventaire (V74) |
| V90–V99 | GDPR (V90) · transcriptions sermons (V91) · prophetique/conformite/competences (V92) · croissance/engagement (V93) · face templates (V94) · WhatsApp (V95, V97) · workflow configs (V98) · configs integration admin (V99) |
| V100–V117 | Reseau (V100, V103) · certificats/recompenses (V101) · passeports spirituels (V102) · bible reading (V104) · audit providers paiement (V105) · demo requests (V106) · defaut tenant roles (V107) · webhook logs (V108) · IA predictions (V109) · conversations chat IA (V110) · notes visite IA (V111) · dons recurrents (V112) · requetes admin (V113) · tables manquantes (V114) · assouplissement statuts (V115) · sessions USSD (V116) · module vocal (V117) |
| V127–V154 | KPI departements (V127) · reunions familles (V128) · checklists events (V129) · messages groupes (V130) · insights executifs (V131) · marketplace (V132) · streaming (V133) · communaute (V134) · **coeur multi-tenant (V135-136)** · dress code (V137) · moteur assets (V138) · cas pastoraux (V139) · infirmerie/sante (V141) · `tenant_settings` (V142) · `tenant_features` (V143) · **plans SaaS seed (V144)** · **heritage config (V145)** · **portees global/local (V146)** · **hierarchie `organization_unit` generalisee (V147)** · **`module_definition`+`space_module` (V148)** · **`spaces` (V149)** · **statuts (V150)** · **moteur workflow (V151)** · **templates espaces (V152)** · **custom fields moteur (V153)** · **moteur people (V154)** |
| V158–V164 | Migration events legacy → `church_event` (V158) · family OS visites/receptions (V160) · pastorat transferts/RDV (V161) · versioning permissions/roles vivants (V162) · fulltext pg_trgm (V163) · **moteur migration legacy dry-run+replay (V164)** |

## 3. Schema conceptuel (extrait)

```
tenant (1) ──< tenant_membership >── (1) user (personne physique)
tenant (1) ──< organization_node (path ltree : ROOT_CHURCH → CAMPUS → DEPARTMENT → FAMILY)
tenant (1) ──< space ──< space_module >── module_definition
users (1) ──< departments (responsable) · families (chef) · souls (faiseur) · maker_reports · notifications
departments (1) ──< families (1) ──< souls (1) ──< maker_reports / parallel_followups / alerts / soul_history
families (1) ──< family_reports · family_meetings ; souls (1) ──< soul_notes / discipline_events
spaces (1) ──< events (church_event) / assets / finances / messages / documents
outbox_event (1) ──→ SSE/webhooks ; audit_event (hash chain prev_hash/hash, 95 j) ; business_history (7 ans)
```

Tables cles : `tenant`, `tenant_membership`, `organization_node`, `space`, `space_module`, `module_definition`, `space_templates`, `roles/permissions/role_permissions/role_assignments`, `tenant_settings`, `tenant_features`, `saas_plans/subscriptions/quotas`, `users`, `departments`, `families`, `souls`, `maker_reports`, `family_reports`, `parallel_followups`, `alerts`, `notifications`, `events/church_event/event_registrations/event_checklists`, `assets`, `finances/payments`, `messages/group_messages`, `whatsapp_*`, `ussd_sessions`, `ai_predictions/ai_chat_conversations/ai_visit_notes`, `workflow_configs/engine`, `custom_fields/statuses`, `config_revisions`, `custom_pages`, `audit_event/business_history/export_audit/impersonation_audit/soft_delete_audit`, `gdpr_requests`, `files/entity_attachments`, `webhook_delivery_logs`, `data_migration_jobs`, `culte_config`, `dashboard_metrics`.

## 4. Index cles & perfs

- Index FK + statuts + `created_at` + `deleted` + `email` ; composites `idx_{table}_tenant[_col]` ; uniques `(tenant_id, email|code|slug)` ; ltree `path` (V147) ; trgm (V163). Objectifs : p95 API < 500 ms, bootstrap espace < 1 s, sync mobile < 5 s (voir [RUNBOOK.md](RUNBOOK.md)).

## 5. Exploitation

Sauvegardes : snapshots quotidiens + PITR WAL (RPO 1 h, RTO 30 min) + dump mensuel chiffre (workflow `backup-postgres.yml`) + restauration testee en staging — detail [RUNBOOK.md](RUNBOOK.md), procedure [DEPLOYMENT.md](DEPLOYMENT.md). Migrations legacy : dry-run + replay (V164), jamais d'effacement source avant validation.
