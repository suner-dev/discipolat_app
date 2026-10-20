-- V161__pastorate_transfers_appointments.sql
-- ============================================================
-- G4.3 — Pastorate : transferts & nominations de pasteurs
-- ============================================================

-- 1. PASTORATE APPOINTMENT (nomination de pasteur)
CREATE TABLE IF NOT EXISTS pastorate_appointment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    pastor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_unit_id UUID NOT NULL REFERENCES organization_nodes(id) ON DELETE CASCADE,
    role_code VARCHAR(50) NOT NULL, -- PASTOR_CAMPUS, PASTOR_PRINCIPAL, PASTOR_ASSOCIATE, etc.
    title VARCHAR(200),
    start_date DATE NOT NULL,
    end_date DATE,
    appointment_type VARCHAR(50) NOT NULL CHECK (appointment_type IN ('NOMINATION', 'TRANSFER', 'REASSIGNMENT', 'PROMOTION')),
    reason TEXT,
    previous_org_unit_id UUID REFERENCES organization_nodes(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ENDED', 'SUSPENDED', 'TRANSFERRED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_pa_tenant ON pastorate_appointment(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pa_pastor ON pastorate_appointment(pastor_id);
CREATE INDEX IF NOT EXISTS idx_pa_org_unit ON pastorate_appointment(organization_unit_id);
CREATE INDEX IF NOT EXISTS idx_pa_status ON pastorate_appointment(status);
CREATE INDEX IF NOT EXISTS idx_pa_start_date ON pastorate_appointment(start_date);

COMMENT ON TABLE pastorate_appointment IS 'G4.3 : nominations & transferts de pasteurs (historique mandats)';

-- 2. PASTORATE TRANSFER (transfert de pasteur)
CREATE TABLE IF NOT EXISTS pastorate_transfer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    pastor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    from_org_unit_id UUID REFERENCES organization_nodes(id) ON DELETE SET NULL,
    to_org_unit_id UUID NOT NULL REFERENCES organization_nodes(id) ON DELETE CASCADE,
    transfer_date DATE NOT NULL,
    reason TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED')),
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMPTZ,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pt_tenant ON pastorate_transfer(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pt_pastor ON pastorate_transfer(pastor_id);
CREATE INDEX IF NOT EXISTS idx_pt_status ON pastorate_transfer(status);

COMMENT ON TABLE pastorate_transfer IS 'G4.3 : demandes de transfert de pasteurs';

-- Triggers
DROP TRIGGER IF EXISTS update_pastorate_appointment_updated_at ON pastorate_appointment;
CREATE TRIGGER update_pastorate_appointment_updated_at BEFORE UPDATE ON pastorate_appointment FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_pastorate_transfer_updated_at ON pastorate_transfer;
CREATE TRIGGER update_pastorate_transfer_updated_at BEFORE UPDATE ON pastorate_transfer FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();