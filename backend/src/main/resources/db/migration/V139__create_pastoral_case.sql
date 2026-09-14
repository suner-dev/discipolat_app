-- Pastoral Case Module (G3.8)
-- Confidential pastoral care with strict access control and audit

CREATE TABLE pastoral_case (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    person_id UUID NOT NULL,
    assigned_to UUID,
    confidentiality_level VARCHAR(20) NOT NULL DEFAULT 'INTERNAL',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    title VARCHAR(200) NOT NULL,
    description TEXT,
    visibility_scope VARCHAR(50),
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_pastoral_case_tenant ON pastoral_case(tenant_id);
CREATE INDEX idx_pastoral_case_person ON pastoral_case(tenant_id, person_id);
CREATE INDEX idx_pastoral_case_assigned ON pastoral_case(tenant_id, assigned_to);
CREATE INDEX idx_pastoral_case_status ON pastoral_case(tenant_id, status);

COMMENT ON TABLE pastoral_case IS 'Confidential pastoral cases with strict access control';
