-- V95 : Pont WhatsApp Business + opt-in diffusion + préférences utilisateur
-- + tables manquantes des modules compliance/currency/onboarding (fix démarrage)

-- ── Configuration WhatsApp par église ─────────────────────────────────
CREATE TABLE IF NOT EXISTS whatsapp_configs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    phone_number_id VARCHAR(64),
    display_phone_number VARCHAR(32),
    access_token_encrypted TEXT,
    webhook_verify_token VARCHAR(128),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    welcome_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_whatsapp_configs_tenant ON whatsapp_configs(tenant_id);

-- ── Journal des messages WhatsApp ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS whatsapp_messages (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    direction VARCHAR(16) NOT NULL,
    phone_number VARCHAR(32) NOT NULL,
    wa_message_id VARCHAR(128),
    status VARCHAR(16) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    body TEXT,
    reference_type VARCHAR(64),
    reference_id UUID,
    user_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_wa_messages_tenant_created ON whatsapp_messages(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_wa_messages_phone ON whatsapp_messages(tenant_id, phone_number);

-- ── Opt-in WhatsApp + fuseau horaire utilisateur (features #1 et #6) ──
ALTER TABLE users ADD COLUMN IF NOT EXISTS whatsapp_opt_in BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS timezone VARCHAR(64);

-- ── Consentements RGPD (feature #4 — table manquante du module compliance) ──
CREATE TABLE IF NOT EXISTS consent_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    utilisateur_id UUID NOT NULL,
    type_consentement VARCHAR(64) NOT NULL,
    accorde BOOLEAN NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_consent_logs_tenant ON consent_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_consent_logs_user ON consent_logs(utilisateur_id, type_consentement);

-- ── Config devises (feature #6 — table manquante du module currency) ──
CREATE TABLE IF NOT EXISTS currency_configs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    currency_code VARCHAR(8) NOT NULL,
    currency_symbol VARCHAR(16) NOT NULL,
    timezone VARCHAR(64),
    locale VARCHAR(16),
    exchange_rate_to_usd DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_currency_configs_tenant ON currency_configs(tenant_id);

-- ── Étapes wizard d'onboarding (feature #10 — table manquante) ────────
CREATE TABLE IF NOT EXISTS onboarding_wizard_steps (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    step_type VARCHAR(32) NOT NULL,
    step_order INTEGER NOT NULL,
    config TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    completed_data TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- ── Connecteurs tiers (feature #3) ────────────────────────────────────
CREATE TABLE IF NOT EXISTS integration_configs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    connector VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    endpoint_url VARCHAR(512),
    api_key_encrypted TEXT,
    ical_url VARCHAR(512),
    last_sync_at TIMESTAMP,
    last_sync_status VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_integration_configs_tenant ON integration_configs(tenant_id);

-- ── Chaîne de hachage du journal d'audit (feature #4) ─────────────────
CREATE TABLE IF NOT EXISTS audit_hash_chain (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    audit_log_id UUID NOT NULL,
    previous_hash VARCHAR(64),
    entry_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_hash_chain_tenant ON audit_hash_chain(tenant_id, created_at);

-- ── Politiques de rétention RGPD (feature #4) ────────────────────────
CREATE TABLE IF NOT EXISTS retention_policies (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    data_type VARCHAR(64) NOT NULL,
    retention_days INTEGER NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    hard_delete BOOLEAN NOT NULL DEFAULT FALSE,
    last_purge_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, data_type)
);

-- ── Traces d'export RGPD (feature #4) ────────────────────────────────
CREATE TABLE IF NOT EXISTS data_export_records (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID,
    format VARCHAR(16) NOT NULL,
    motif VARCHAR(32) NOT NULL,
    record_count INTEGER,
    fichier_path VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_data_export_tenant ON data_export_records(tenant_id, created_at);
