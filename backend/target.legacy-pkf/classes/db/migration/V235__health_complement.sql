-- V235__health_complement.sql
-- ============================================================
-- HEALTH COMPLÉMENT (médicaments, kits, devoirs, participants
-- aux campagnes)
-- Plan : docs/SPEC_BACKEND_SERVICES_MOBILES_V233.md
--
-- Le module health existe déjà (V141). Cette migration ajoute
-- les tables manquantes pour le contrat mobile.
-- ============================================================

CREATE TABLE IF NOT EXISTS health_medications (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    dosage          VARCHAR(100),
    frequency       VARCHAR(100),
    stock_quantity  INT NOT NULL DEFAULT 0,
    unit            VARCHAR(50),
    expiry_date     TIMESTAMPTZ,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS health_kits (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    category        VARCHAR(100),
    quantity        INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS health_duties (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    duty_type       VARCHAR(50),
    scheduled_at    TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS campaign_participants (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    campaign_id     UUID NOT NULL REFERENCES health_campaigns(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    registered_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    status          VARCHAR(20) NOT NULL DEFAULT 'REGISTERED'
        CHECK (status IN ('REGISTERED', 'ATTENDED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_health_med_tenant ON health_medications (tenant_id);
CREATE INDEX IF NOT EXISTS idx_health_kits_tenant ON health_kits (tenant_id);
CREATE INDEX IF NOT EXISTS idx_health_duties_tenant ON health_duties (tenant_id);
CREATE INDEX IF NOT EXISTS idx_campaign_part_tenant ON campaign_participants (tenant_id);
CREATE INDEX IF NOT EXISTS idx_campaign_part_campaign ON campaign_participants (tenant_id, campaign_id);

COMMENT ON TABLE health_medications IS 'Médicaments (V235).';
COMMENT ON TABLE health_kits IS 'Kits de santé (V235).';
COMMENT ON TABLE health_duties IS 'Devoirs de santé (V235).';
COMMENT ON TABLE campaign_participants IS 'Participants aux campagnes (V235).';
