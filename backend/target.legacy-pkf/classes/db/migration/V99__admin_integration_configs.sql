CREATE TABLE IF NOT EXISTS admin_integration_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    category VARCHAR(100) NOT NULL,
    config_data JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, category)
);

CREATE INDEX idx_admin_integration_configs_tenant ON admin_integration_configs(tenant_id);
