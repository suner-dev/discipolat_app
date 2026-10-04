-- ============================================================
-- SPEC_ONBOARDING_FLOWS (BE-1) — codes de rejointure par église /
-- sous-église + demandes d'accès par code (join_mode APPROVAL).
--
-- D2 : code lisible format « BETHEL-7K2X », stocké en MAJUSCULES,
-- alphabet non ambigu ABCDEFGHJKLMNPQRSTUVWXYZ23456789.
-- D3 : org_node_id nullable → code rattaché à une sous-église du même
-- tenant (OrganizationNode), sinon code principal de l'église.
-- Un seul code ACTIF par valeur (index partiel WHERE is_active).
-- ============================================================

CREATE TABLE IF NOT EXISTS tenant_join_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    org_node_id UUID,
    code VARCHAR(64) NOT NULL,
    label VARCHAR(120),
    join_mode VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (join_mode IN ('OPEN', 'APPROVAL')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    rotated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_join_codes_active_code
    ON tenant_join_codes(code) WHERE is_active;
CREATE INDEX IF NOT EXISTS idx_tenant_join_codes_tenant
    ON tenant_join_codes(tenant_id, is_active);
CREATE INDEX IF NOT EXISTS idx_tenant_join_codes_org_node
    ON tenant_join_codes(org_node_id) WHERE org_node_id IS NOT NULL;

COMMENT ON TABLE tenant_join_codes IS
    'SPEC_ONBOARDING_FLOWS : codes courts de rejointure (église + sous-église)';

CREATE TABLE IF NOT EXISTS tenant_join_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    org_node_id UUID,
    user_id UUID,
    email VARCHAR(255),
    code VARCHAR(64) NOT NULL,
    message VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    handled_at TIMESTAMPTZ,
    handled_by UUID
);

CREATE INDEX IF NOT EXISTS idx_tenant_join_requests_tenant_status
    ON tenant_join_requests(tenant_id, status, created_at);

COMMENT ON TABLE tenant_join_requests IS
    'SPEC_ONBOARDING_FLOWS : demandes d''accès émises quand le code est en mode APPROVAL';
