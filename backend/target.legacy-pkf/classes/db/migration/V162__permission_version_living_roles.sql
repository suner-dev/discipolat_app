-- V162__permission_version_living_roles.sql
-- ============================================================
-- G4.4 — Roles vivantes : versionnage des permissions
-- Table de cache versionne pour propagation temps reel (web + mobile)
-- ============================================================

CREATE TABLE IF NOT EXISTS permission_version (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    version BIGINT NOT NULL DEFAULT 1,
    permissions_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uk_pv_tenant_user UNIQUE (tenant_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_pv_tenant_user ON permission_version(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_pv_updated_at ON permission_version(updated_at);

COMMENT ON TABLE permission_version IS 'G4.4 : cache versionne des permissions utilisateur pour propagation temps reel';