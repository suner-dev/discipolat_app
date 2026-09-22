-- V154__people_engine.sql
-- ============================================================
-- G3.1 — People Engine : identite unique + inscription automatique
-- G3.2 — Membership, SpaceMembership & RoleAssignment (3 dim + historisation)
-- Contrat : Annexe A §A.2-A.3
-- ============================================================

-- 1. PERSON (identite unique par tenant, dedoublonnage email/phone)
CREATE TABLE IF NOT EXISTS person (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    display_name VARCHAR(200),
    gender VARCHAR(20) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    birth_date DATE,
    phone_normalized VARCHAR(30),
    email_normalized VARCHAR(255),
    address TEXT,
    photo_url VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'ARCHIVED')),
    visibility_scope VARCHAR(30) NOT NULL DEFAULT 'CHURCH' CHECK (visibility_scope IN ('PRIVATE', 'TEAM', 'CHURCH', 'PUBLIC')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_person_tenant_email ON person(tenant_id, email_normalized) WHERE email_normalized IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_person_tenant_phone ON person(tenant_id, phone_normalized) WHERE phone_normalized IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_person_tenant ON person(tenant_id);
CREATE INDEX IF NOT EXISTS idx_person_status ON person(status);
CREATE INDEX IF NOT EXISTS idx_person_deleted ON person(deleted_at);

COMMENT ON TABLE person IS 'G3.1 : identite unique par eglise — une seule fiche par personne';
COMMENT ON COLUMN person.email_normalized IS 'Email normalise (lowercase, trim) pour dedoublonnage';
COMMENT ON COLUMN person.phone_normalized IS 'Telephone normalise (E.164) pour dedoublonnage';
COMMENT ON COLUMN person.visibility_scope IS 'Portee de visibilite dans l\'annuaire (G4.2)';

-- 2. MEMBERSHIP (appartenance a l'eglise/tenant)
CREATE TABLE IF NOT EXISTS membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    membership_status VARCHAR(30) NOT NULL DEFAULT 'MEMBRE' CHECK (membership_status IN ('VISITEUR', 'NOUVEAU_CONVERTI', 'MEMBRE', 'EN_VEILLE', 'DECROCHE', 'TRANSFERE', 'DECES')),
    joined_at DATE NOT NULL DEFAULT CURRENT_DATE,
    left_at DATE,
    source VARCHAR(30) CHECK (source IN ('INSCRIPTION', 'INVITATION', 'IMPORT', 'EVANGELISATION', 'TRANSFERT')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_membership_tenant ON membership(tenant_id);
CREATE INDEX IF NOT EXISTS idx_membership_person ON membership(person_id);
CREATE INDEX IF NOT EXISTS idx_membership_status ON membership(membership_status);

COMMENT ON TABLE membership IS 'G3.2 : appartenance a l\'eglise (tenant) — distincte des espaces';

-- 3. SPACE_MEMBERSHIP (appartenance a un espace : departement, famille, sous-equipe)
CREATE TABLE IF NOT EXISTS space_membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    space_id UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'SUSPENDED', 'LEFT')),
    membership_type VARCHAR(30) NOT NULL DEFAULT 'MEMBER' CHECK (membership_type IN ('LEADER', 'CO_LEADER', 'MEMBER', 'ASSISTANT', 'OBSERVER')),
    responsibility TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sm_space ON space_membership(space_id);
CREATE INDEX IF NOT EXISTS idx_sm_person ON space_membership(person_id);
CREATE INDEX IF NOT EXISTS idx_sm_status ON space_membership(status);
CREATE INDEX IF NOT EXISTS idx_sm_tenant ON space_membership(tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sm_person_space ON space_membership(person_id, space_id) WHERE left_at IS NULL;

COMMENT ON TABLE space_membership IS 'G3.2 : appartenance a un espace (departement/famille/sous-equipe) — dimension 2';
COMMENT ON COLUMN space_membership.membership_type IS 'Type de responsabilite dans l\'espace';

-- 4. ROLE_ASSIGNMENT (fonction permanente dans l'organisation)
CREATE TABLE IF NOT EXISTS role_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    organization_unit_id UUID REFERENCES organization_nodes(id) ON DELETE SET NULL,
    space_id UUID REFERENCES spaces(id) ON DELETE SET NULL,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    started_at DATE NOT NULL DEFAULT CURRENT_DATE,
    ended_at DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ENDED', 'SUSPENDED')),
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ra_tenant ON role_assignment(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ra_person ON role_assignment(person_id);
CREATE INDEX IF NOT EXISTS idx_ra_org_unit ON role_assignment(organization_unit_id);
CREATE INDEX IF NOT EXISTS idx_ra_space ON role_assignment(space_id);
CREATE INDEX IF NOT EXISTS idx_ra_status ON role_assignment(status);

COMMENT ON TABLE role_assignment IS 'G3.2 : fonction permanente (role org) — dimension 1, historisee (jamais d\'ecrasement)';
COMMENT ON COLUMN role_assignment.ended_at IS 'Date de fin du mandat — cloture sans suppression pour historique';

-- 5. EVENT_ASSIGNMENT (fonction evenementielle)
CREATE TABLE IF NOT EXISTS event_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    event_id UUID NOT NULL, -- FK vers events (cree en G3.3)
    role_code VARCHAR(60) NOT NULL,
    location_id UUID,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED' CHECK (status IN ('ASSIGNED', 'CONFIRMED', 'DECLINED', 'COMPLETED', 'NO_SHOW')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ea_tenant ON event_assignment(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ea_person ON event_assignment(person_id);
CREATE INDEX IF NOT EXISTS idx_ea_event ON event_assignment(event_id);
CREATE INDEX IF NOT EXISTS idx_ea_status ON event_assignment(status);

COMMENT ON TABLE event_assignment IS 'G3.2 : affectation evenementielle — dimension 3, independante des roles permanents';

-- Triggers updated_at
DROP TRIGGER IF EXISTS update_person_updated_at ON person;
CREATE TRIGGER update_person_updated_at BEFORE UPDATE ON person FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_membership_updated_at ON membership;
CREATE TRIGGER update_membership_updated_at BEFORE UPDATE ON membership FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_space_membership_updated_at ON space_membership;
CREATE TRIGGER update_space_membership_updated_at BEFORE UPDATE ON space_membership FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_role_assignment_updated_at ON role_assignment;
CREATE TRIGGER update_role_assignment_updated_at BEFORE UPDATE ON role_assignment FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_event_assignment_updated_at ON event_assignment;
CREATE TRIGGER update_event_assignment_updated_at BEFORE UPDATE ON event_assignment FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 6. Seed roles catalogue (complement roles existants)
INSERT INTO roles (id, tenant_id, key, label, description, system, priority) VALUES
    (gen_random_uuid(), NULL, 'HEALTH_STAFF', 'Personnel Sante', 'Infirmier/Medecin de l\'espace sante', TRUE, 250),
    (gen_random_uuid(), NULL, 'HEALTH_LEAD', 'Responsable Sante', 'Responsable de l\'infirmerie', TRUE, 300),
    (gen_random_uuid(), NULL, 'FAMILY_LEADER', 'Chef de Famille', 'Responsable d\'un espace FAMILY', TRUE, 300),
    (gen_random_uuid(), NULL, 'DISCIPLE_MAKER', 'Faiseur de Disciples', 'Accompagnateur spirituel (active par template FAMILY/DISCIPLESHIP)', TRUE, 200)
ON CONFLICT (tenant_id, key) DO NOTHING;