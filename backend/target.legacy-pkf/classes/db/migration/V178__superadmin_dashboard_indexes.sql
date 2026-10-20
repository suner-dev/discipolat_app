-- Additional composite indexes for Super Admin dashboard performance
-- These indexes optimize the aggregated queries used in the Super Admin dashboard
--
-- CORRECTION 2026-09-28 (constat H2, verifie par execution). Cette migration
-- visait 7 tables qui N'EXISTENT PAS dans la chaine de migrations : `events`,
-- `financial_transactions` (x2), `reports`, `whatsapp_templates`,
-- `whatsapp_contacts`, `prophetic_journal`. Consequence : `flyway migrate`
-- ECHOUE sur une base vierge, donc aucun deploiement neuf n'etait possible.
-- Les verifications suivantes ont ete faites contre le schema reellement cree par
-- V1..V177, et les noms de tables comme de colonnes ont ete alignes sur lui :
--   events                  -> event            (et `deleted` -> `deleted_at`)
--   financial_transactions  -> finance_transactions (`transaction_date` -> `date_transaction`)
--   reports                 -> maker_reports
--   whatsapp_templates      -> whatsapp_configs
--   whatsapp_contacts       -> whatsapp_messages
--   prophetic_journal       -> prayer_journal_entries (`statut` existe)
-- Aucun de ces index n'est de la decoration : chacun porte une agregation du
-- tableau de bord Super Admin.

-- Tenants: composite index for dashboard stats queries
CREATE INDEX IF NOT EXISTS idx_tenants_status_created ON tenants(status, created_at);

-- Users: composite indexes for tenant-level aggregations
CREATE INDEX IF NOT EXISTS idx_users_tenant_status_deleted ON users(tenant_id, statut, deleted);
CREATE INDEX IF NOT EXISTS idx_users_tenant_whatsapp_optin ON users(tenant_id, whatsapp_opt_in) WHERE whatsapp_opt_in = true;

-- Tenant Memberships: composite indexes for dashboard aggregations
CREATE INDEX IF NOT EXISTS idx_tenant_memberships_tenant_status ON tenant_memberships(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_tenant_memberships_tenant_role_status ON tenant_memberships(tenant_id, role_id, status) WHERE status = 'ACTIVE';

-- Organization Nodes: composite indexes for dashboard aggregations
CREATE INDEX IF NOT EXISTS idx_org_nodes_tenant_type ON organization_nodes(tenant_id, type);
CREATE INDEX IF NOT EXISTS idx_org_nodes_tenant_type_status ON organization_nodes(tenant_id, type, status);

-- Tenant Subscriptions: composite indexes for dashboard aggregations
CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_tenant_status ON tenant_subscriptions(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_status_period ON tenant_subscriptions(status, current_period_end) WHERE status IN ('ACTIVE', 'TRIAL', 'PAST_DUE');

-- Audit Logs: composite indexes for platform activity stats
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at_tenant ON audit_logs(created_at DESC, tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_created ON audit_logs(tenant_id, created_at DESC);

-- Payment Intents: indexes for payment dashboard
CREATE INDEX IF NOT EXISTS idx_payment_intents_tenant_status ON payment_intents(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_payment_intents_tenant_operator_status ON payment_intents(tenant_id, operator, status) WHERE status = 'CONFIRMED';

-- Souls/Persons: indexes for people counts per tenant
CREATE INDEX IF NOT EXISTS idx_souls_tenant ON souls(tenant_id) WHERE deleted = false;

-- Events: indexes for event counts per tenant
CREATE INDEX IF NOT EXISTS idx_event_tenant_status ON event(tenant_id, status) WHERE deleted_at IS NULL;

-- Financial Transactions: indexes for finance dashboard
CREATE INDEX IF NOT EXISTS idx_finance_transactions_tenant_type ON finance_transactions(tenant_id, type);
CREATE INDEX IF NOT EXISTS idx_finance_transactions_tenant_date ON finance_transactions(tenant_id, date_transaction DESC);

-- Reports: indexes for report counts
CREATE INDEX IF NOT EXISTS idx_maker_reports_tenant ON maker_reports(tenant_id);

-- WhatsApp: indexes for WhatsApp usage per tenant
CREATE INDEX IF NOT EXISTS idx_whatsapp_configs_tenant ON whatsapp_configs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_messages_tenant ON whatsapp_messages(tenant_id);

-- Network: indexes for network module per tenant
CREATE INDEX IF NOT EXISTS idx_network_resources_tenant ON network_resources(tenant_id);
CREATE INDEX IF NOT EXISTS idx_network_events_tenant ON network_events(tenant_id);

-- AI Predictions: indexes for AI usage per tenant
CREATE INDEX IF NOT EXISTS idx_ai_predictions_tenant_created ON ai_predictions(tenant_id, created_at DESC);

-- Voice Reports: indexes for voice reports per tenant
CREATE INDEX IF NOT EXISTS idx_voice_reports_tenant_created ON voice_reports(tenant_id, created_at DESC);

-- Prophetic Journal: indexes for prophetic journal per tenant
CREATE INDEX IF NOT EXISTS idx_prayer_journal_entries_tenant_created ON prayer_journal_entries(tenant_id, created_at DESC);