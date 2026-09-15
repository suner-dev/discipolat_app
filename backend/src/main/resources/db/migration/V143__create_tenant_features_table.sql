-- V143__create_tenant_features_table.sql
-- ============================================================
-- G1.3 - §29 : TenantFeature — modules activables par tenant
-- Table tenant_features pour la gestion des modules par tenant
-- ============================================================

CREATE TABLE IF NOT EXISTS tenant_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    module_code VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configuration_json JSONB DEFAULT '{}'::jsonb,
    limits_json JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, module_code)
);

CREATE INDEX IF NOT EXISTS idx_tenant_features_tenant ON tenant_features(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_features_module ON tenant_features(module_code);

COMMENT ON TABLE tenant_features IS 'Modules activables par tenant (G1.3 - §29)';
