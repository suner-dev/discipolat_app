-- V166__fix_tenants_schema_and_outbox.sql
-- ============================================================
-- 1. Complete le schema tenants avec les colonnes attendues par
--    l'entite JPA Tenant (Tenant.java) — V70 ne creait que les
--    colonnes de base.
-- 2. Cree la table outbox_event utilisee par OutboxEvent.java
--    (transactional outbox / propagation temps reel).
-- ============================================================

ALTER TABLE tenants ADD COLUMN IF NOT EXISTS country VARCHAR(2);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS currency VARCHAR(3);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS timezone VARCHAR(64);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS locale VARCHAR(10);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS branding_json JSONB;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS features_json JSONB;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS settings_json JSONB;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS trial_ends_at TIMESTAMPTZ;

UPDATE tenants SET country = 'CM' WHERE country IS NULL;
UPDATE tenants SET currency = 'XAF' WHERE currency IS NULL;
UPDATE tenants SET timezone = 'Africa/Douala' WHERE timezone IS NULL;
UPDATE tenants SET locale = 'fr' WHERE locale IS NULL;

CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_available ON outbox_event(status, available_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox_event(aggregate_type, aggregate_id);
CREATE INDEX IF NOT EXISTS idx_outbox_tenant ON outbox_event(tenant_id);