-- V160__family_os_visits_receptions_meetings.sql
-- ============================================================
-- G4.1 — Family OS : visites, réceptions, réunions, comptes-rendus
-- Nouveaux types d'activités pour l'espace FAMILY
-- ============================================================

-- 1. FAMILY VISIT (visite d'âme)
CREATE TABLE IF NOT EXISTS family_visit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    soul_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    faiseur_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    visit_date DATE NOT NULL,
    visit_type VARCHAR(50) NOT NULL CHECK (visit_type IN ('DOMICILE', 'ECOLE', 'HOPITAL', 'TELEPHONE', 'EGLISE', 'AUTRE')),
    subject TEXT,
    report TEXT,
    decisions TEXT,
    next_action_date DATE,
    next_action_type VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'POSTPONED')),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fv_family ON family_visit(family_id);
CREATE INDEX IF NOT EXISTS idx_fv_soul ON family_visit(soul_id);
CREATE INDEX IF NOT EXISTS idx_fv_faiseur ON family_visit(faiseur_id);
CREATE INDEX IF NOT EXISTS idx_fv_date ON family_visit(visit_date);
CREATE INDEX IF NOT EXISTS idx_fv_status ON family_visit(status);

COMMENT ON TABLE family_visit IS 'G4.1 : visites de suivi d''âmes par le faiseur';

-- 2. FAMILY RECEPTION (réception de nouveaux membres)
CREATE TABLE IF NOT EXISTS family_reception (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    soul_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reception_date DATE NOT NULL,
    reception_type VARCHAR(50) NOT NULL CHECK (reception_type IN ('BIENVENUE', 'BAPTEME', 'TRANSFERT', 'RECONNEXION', 'AUTRE')),
    welcome_by UUID REFERENCES users(id),
    notes TEXT,
    assigned_faiseur_id UUID REFERENCES users(id),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fr_family ON family_reception(family_id);
CREATE INDEX IF NOT EXISTS idx_fr_soul ON family_reception(soul_id);
CREATE INDEX IF NOT EXISTS idx_fr_date ON family_reception(reception_date);

COMMENT ON TABLE family_reception IS 'G4.1 : réceptions de nouveaux membres dans la famille';

-- 3. FAMILY MEETING (réunion de famille)
CREATE TABLE IF NOT EXISTS family_meeting (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    meeting_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    location VARCHAR(255),
    meeting_type VARCHAR(50) CHECK (meeting_type IN ('HEBDOMADAIRE', 'MENSUELLE', 'SPECIALE', 'FORMATION', 'PRIERE', 'AUTRE')),
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fm_family ON family_meeting(family_id);
CREATE INDEX IF NOT EXISTS idx_fm_date ON family_meeting(meeting_date);

COMMENT ON TABLE family_meeting IS 'G4.1 : réunions de famille (prière, formation, coordination)';

-- 4. FAMILY ACTIVITY (activité unifiée pour le bouton "Créer une activité")
CREATE TABLE IF NOT EXISTS family_activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL CHECK (activity_type IN ('VISIT', 'RECEPTION', 'MEETING', 'OTHER')),
    reference_id UUID NOT NULL, -- ID de family_visit, family_reception ou family_meeting
    title VARCHAR(255) NOT NULL,
    description TEXT,
    activity_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fa_family ON family_activity(family_id);
CREATE INDEX IF NOT EXISTS idx_fa_date ON family_activity(activity_date);
CREATE INDEX IF NOT EXISTS idx_fa_ref ON family_activity(reference_id);

COMMENT ON TABLE family_activity IS 'G4.1 : vue unifiée pour le bouton "Créer une activité" (multi-type)';

-- Triggers
DROP TRIGGER IF EXISTS update_family_visit_updated_at ON family_visit;
CREATE TRIGGER update_family_visit_updated_at BEFORE UPDATE ON family_visit FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_family_reception_updated_at ON family_reception;
CREATE TRIGGER update_family_reception_updated_at BEFORE UPDATE ON family_reception FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_family_meeting_updated_at ON family_meeting;
CREATE TRIGGER update_family_meeting_updated_at BEFORE UPDATE ON family_meeting FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_family_activity_updated_at ON family_activity;
CREATE TRIGGER update_family_activity_updated_at BEFORE UPDATE ON family_activity FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();