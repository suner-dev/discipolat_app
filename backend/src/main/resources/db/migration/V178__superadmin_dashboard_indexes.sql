-- Additional composite indexes for Super Admin dashboard performance
-- These indexes optimize the aggregated queries used in the Super Admin dashboard

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
CREATE INDEX IF NOT EXISTS idx_events_tenant_status ON events(tenant_id, status) WHERE deleted = false;

-- Financial Transactions: indexes for finance dashboard
CREATE INDEX IF NOT EXISTS idx_financial_transactions_tenant_type ON financial_transactions(tenant_id, type);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_tenant_date ON financial_transactions(tenant_id, transaction_date DESC);

-- Reports: indexes for report counts
CREATE INDEX IF NOT EXISTS idx_reports_tenant_status ON reports(tenant_id, status);

-- WhatsApp: indexes for WhatsApp usage per tenant
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_tenant ON whatsapp_templates(tenant_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_contacts_tenant ON whatsapp_contacts(tenant_id);

-- Network: indexes for network module per tenant
CREATE INDEX IF NOT EXISTS idx_network_resources_tenant ON network_resources(tenant_id);
CREATE INDEX IF NOT EXISTS idx_network_events_tenant ON network_events(tenant_id);

-- AI Predictions: indexes for AI usage per tenant
CREATE INDEX IF NOT EXISTS idx_ai_predictions_tenant_created ON ai_predictions(tenant_id, created_at DESC);

-- Voice Reports: indexes for voice reports per tenant
CREATE INDEX IF NOT EXISTS idx_voice_reports_tenant_created ON voice_reports(tenant_id, created_at DESC);

-- Prophetic Journal: indexes for prophetic journal per tenant
CREATE INDEX IF NOT EXISTS idx_prophetic_journal_tenant_created ON prophetic_journal(tenant_id, created_at DESC);