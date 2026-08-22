-- V93: Nouvelles fonctionnalités proposées dans les audits
-- Quest Gamification (XP), Tontine numérique, Mobile Money,
-- Webhooks & API publique, Rapports vocaux IA, Secours humanitaire

-- ============ Gamification "Discipolat Quest" (XP / Niveaux / Quêtes) ============
CREATE TABLE IF NOT EXISTS xp_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    points INTEGER NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_xp_ledger_tenant ON xp_ledger(tenant_id);
CREATE INDEX idx_xp_ledger_user ON xp_ledger(user_id);
CREATE INDEX idx_xp_ledger_action ON xp_ledger(action);

-- ============ Tontine Numérique (confiance & vœux) ============
CREATE TABLE IF NOT EXISTS tontine_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    montant_par_tour DECIMAL(12,2) NOT NULL DEFAULT 0,
    periodicite VARCHAR(20) NOT NULL DEFAULT 'MENSUELLE',
    tour_actuel INTEGER NOT NULL DEFAULT 1,
    statut VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_tontine_groups_tenant ON tontine_groups(tenant_id);

CREATE TABLE IF NOT EXISTS tontine_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    group_id UUID NOT NULL REFERENCES tontine_groups(id) ON DELETE CASCADE,
    soul_id UUID,
    user_id UUID,
    nom VARCHAR(255) NOT NULL,
    ordre_passage INTEGER NOT NULL DEFAULT 1,
    a_recu_tour BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tontine_members_group ON tontine_members(group_id);

CREATE TABLE IF NOT EXISTS tontine_contributions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    group_id UUID NOT NULL REFERENCES tontine_groups(id) ON DELETE CASCADE,
    member_id UUID NOT NULL REFERENCES tontine_members(id) ON DELETE CASCADE,
    tour INTEGER NOT NULL DEFAULT 1,
    montant DECIMAL(12,2) NOT NULL DEFAULT 0,
    paye BOOLEAN NOT NULL DEFAULT FALSE,
    date_paiement TIMESTAMP,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tontine_contributions_group ON tontine_contributions(group_id);
CREATE INDEX idx_tontine_contributions_member ON tontine_contributions(member_id);

-- ============ Passerelle Mobile Money (dîmes & offrandes 2.0) ============
CREATE TABLE IF NOT EXISTS payment_intents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID,
    soul_id UUID,
    operator VARCHAR(30) NOT NULL,
    phone_number VARCHAR(30),
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'XOF',
    purpose VARCHAR(30) NOT NULL DEFAULT 'OFFRANDE',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_reference VARCHAR(100),
    transaction_id UUID,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP
);

CREATE INDEX idx_payment_intents_tenant ON payment_intents(tenant_id);
CREATE INDEX idx_payment_intents_status ON payment_intents(status);
CREATE INDEX idx_payment_intents_operator ON payment_intents(operator);

-- ============ Webhooks & API publique (connecteur écosystème) ============
CREATE TABLE IF NOT EXISTS webhook_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    secret VARCHAR(255) NOT NULL,
    events VARCHAR(1000) NOT NULL DEFAULT '*',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_regs_tenant ON webhook_registrations(tenant_id);

CREATE TABLE IF NOT EXISTS webhook_delivery_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    webhook_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT,
    response_code INTEGER,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INTEGER NOT NULL DEFAULT 1,
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_logs_tenant ON webhook_delivery_logs(tenant_id);
CREATE INDEX idx_webhook_logs_webhook ON webhook_delivery_logs(webhook_id);

CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    key_hash VARCHAR(255) NOT NULL,
    prefix VARCHAR(20) NOT NULL,
    scopes VARCHAR(500) NOT NULL DEFAULT 'read',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_keys_tenant ON api_keys(tenant_id);
CREATE UNIQUE INDEX idx_api_keys_prefix ON api_keys(prefix);

-- ============ Rapports Vocaux IA (synoptique de terrain offline) ============
CREATE TABLE IF NOT EXISTS voice_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    author_id UUID NOT NULL,
    duration_seconds INTEGER NOT NULL DEFAULT 0,
    transcript TEXT,
    extracted_entities TEXT,
    related_soul_id UUID,
    related_family_id UUID,
    synced_offline BOOLEAN NOT NULL DEFAULT FALSE,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_voice_reports_tenant ON voice_reports(tenant_id);
CREATE INDEX idx_voice_reports_author ON voice_reports(author_id);

-- ============ Secours humanitaire & urgences pastorales ============
CREATE TABLE IF NOT EXISTS emergency_aid_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    requested_by UUID NOT NULL,
    soul_id UUID,
    family_id UUID,
    urgency VARCHAR(20) NOT NULL DEFAULT 'HAUTE',
    category VARCHAR(50) NOT NULL DEFAULT 'URGENCE_PASTORALE',
    description TEXT NOT NULL,
    plan_json TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OUVERT',
    amount_collected DECIMAL(12,2) NOT NULL DEFAULT 0,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_emergency_aid_tenant ON emergency_aid_requests(tenant_id);
CREATE INDEX idx_emergency_aid_status ON emergency_aid_requests(status);
