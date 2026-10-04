-- ============================================================
-- SPEC_ONBOARDING_FLOWS (BE-5) — gouvernance plateforme :
-- avertissements et litiges par tenant. Les blocages/bannissements
-- réutilisent TenantStatus existant (SUSPENDED = bloqué,
-- CANCELLED = banni) ; la raison est tracée ici (D5).
-- ============================================================

CREATE TABLE IF NOT EXISTS tenant_warnings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    message VARCHAR(1000) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO'
        CHECK (severity IN ('INFO', 'FORMAL', 'FINAL')),
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged_at TIMESTAMPTZ,
    acknowledged_by UUID
);

CREATE INDEX IF NOT EXISTS idx_tenant_warnings_tenant
    ON tenant_warnings(tenant_id, created_at DESC);

COMMENT ON TABLE tenant_warnings IS
    'SPEC_ONBOARDING_FLOWS : avertissements émis par la plateforme à un tenant';

CREATE TABLE IF NOT EXISTS tenant_disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'IN_REVIEW', 'CLOSED')),
    resolution VARCHAR(2000),
    opened_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ,
    closed_by UUID
);

CREATE INDEX IF NOT EXISTS idx_tenant_disputes_tenant_status
    ON tenant_disputes(tenant_id, status, created_at DESC);

COMMENT ON TABLE tenant_disputes IS
    'SPEC_ONBOARDING_FLOWS : litiges suivis par la console Super Admin';
