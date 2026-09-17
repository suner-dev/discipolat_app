-- V155__event_engine.sql
-- ============================================================
-- G3.3 — Event Engine transversal
-- Contrat : Annexe A §A.8
-- L'événement est central : relie espaces, personnes, tâches, assets, budget, dépenses, lieux, présence, documents
-- ============================================================

-- 1. LOCATION (lieux d'événements)
CREATE TABLE IF NOT EXISTS location (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address TEXT,
    capacity INTEGER,
    type VARCHAR(30) CHECK (type IN ('INDOOR', 'OUTDOOR', 'VIRTUAL', 'HYBRID')),
    parent_location_id UUID REFERENCES location(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_location_tenant ON location(tenant_id);
CREATE INDEX IF NOT EXISTS idx_location_parent ON location(parent_location_id);

COMMENT ON TABLE location IS 'G3.3 : lieux d\'événements (hiérarchie possible : bâtiment > salle)';

-- 2. EVENT (événement central)
CREATE TABLE IF NOT EXISTS event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) CHECK (type IN ('SERVICE', 'REUNION', 'FORMATION', 'EVANGELISATION', 'CONFERENCE', 'RETRAITE', 'MARIAGE', 'BAPTEME', 'OBSEQUES', 'AUTRE')),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED', 'ARCHIVED')),
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ,
    timezone VARCHAR(64),
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_rule TEXT, -- RRULE iCal
    visibility VARCHAR(30) NOT NULL DEFAULT 'CHURCH' CHECK (visibility IN ('PRIVATE', 'TEAM', 'CHURCH', 'PUBLIC')),
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_event_tenant ON event(tenant_id);
CREATE INDEX IF NOT EXISTS idx_event_status ON event(status);
CREATE INDEX IF NOT EXISTS idx_event_start ON event(start_at);
CREATE INDEX IF NOT EXISTS idx_event_tenant_start ON event(tenant_id, start_at);
CREATE INDEX IF NOT EXISTS idx_event_deleted ON event(deleted_at);

COMMENT ON TABLE event IS 'G3.3 : événement central — relie espaces, personnes, tâches, assets, budget, lieux, présence, documents';

-- 3. EVENT_SPACE (N:N espaces concernés par l'événement)
CREATE TABLE IF NOT EXISTS event_space (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    space_id UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    role VARCHAR(30) CHECK (role IN ('ORGANIZER', 'PARTICIPANT', 'HOST', 'TECHNICAL')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_event_space UNIQUE (event_id, space_id)
);

CREATE INDEX IF NOT EXISTS idx_es_event ON event_space(event_id);
CREATE INDEX IF NOT EXISTS idx_es_space ON event_space(space_id);

COMMENT ON TABLE event_space IS 'G3.3 : espaces liés à un événement (multi-espaces possible)';

-- 4. EVENT_TEAM (équipes par événement)
CREATE TABLE IF NOT EXISTS event_team (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    space_id UUID REFERENCES spaces(id) ON DELETE SET NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    lead_person_id UUID REFERENCES person(id) ON DELETE SET NULL,
    color VARCHAR(7),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_et_event ON event_team(event_id);
CREATE INDEX IF NOT EXISTS idx_et_space ON event_team(space_id);

COMMENT ON TABLE event_team IS 'G3.3 : équipes par événement (ex: Équipe Son, Équipe Accueil, Équipe Streaming)';

-- 5. EVENT_ASSIGNMENT (affectation personnes à postes — déjà créé en V154)
-- Voir V154 pour la table event_assignment

-- 6. EVENT_TASK (tâches liées à l'événement)
CREATE TABLE IF NOT EXISTS event_task (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    space_id UUID REFERENCES spaces(id) ON DELETE SET NULL,
    event_team_id UUID REFERENCES event_team(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    priority INTEGER DEFAULT 0,
    assignee_id UUID REFERENCES person(id) ON DELETE SET NULL,
    due_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_etk_event ON event_task(event_id);
CREATE INDEX IF NOT EXISTS idx_etk_assignee ON event_task(assignee_id);
CREATE INDEX IF NOT EXISTS idx_etk_status ON event_task(status);

COMMENT ON TABLE event_task IS 'G3.3 : tâches opérationnelles liées à un événement';

-- 7. EVENT_ASSET (matériel alloué à l'événement)
CREATE TABLE IF NOT EXISTS event_asset (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    asset_id UUID NOT NULL, -- FK vers asset (G3.5)
    quantity INTEGER NOT NULL DEFAULT 1,
    condition_before VARCHAR(30) CHECK (condition_before IN ('NEW', 'GOOD', 'FAIR', 'POOR')),
    condition_after VARCHAR(30) CHECK (condition_after IN ('NEW', 'GOOD', 'FAIR', 'POOR', 'DAMAGED', 'LOST')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ea_event ON event_asset(event_id);
CREATE INDEX IF NOT EXISTS idx_ea_asset ON event_asset(asset_id);

COMMENT ON TABLE event_asset IS 'G3.3 : matériel alloué à un événement (liaison vers Asset Engine G3.5)';

-- 8. EVENT_EXPENSE (dépenses liées à l'événement)
CREATE TABLE IF NOT EXISTS event_expense (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    expense_id UUID NOT NULL, -- FK vers expense (G3.6)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ee_event ON event_expense(event_id);
CREATE INDEX IF NOT EXISTS idx_ee_expense ON event_expense(expense_id);

COMMENT ON TABLE event_expense IS 'G3.3 : liaison événement <-> dépense (Finance Engine G3.6)';

-- 9. EVENT_ATTENDANCE (présence / check-in)
CREATE TABLE IF NOT EXISTS event_attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    space_id UUID REFERENCES spaces(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PRESENT' CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED', 'LEFT_EARLY')),
    check_in_at TIMESTAMPTZ,
    check_out_at TIMESTAMPTZ,
    check_in_method VARCHAR(30) CHECK (check_in_method IN ('QR', 'MANUAL', 'PHONE_FLASH', 'FACE', 'IMPORT')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_attendance_event_person UNIQUE (event_id, person_id)
);

CREATE INDEX IF NOT EXISTS idx_ea_event ON event_attendance(event_id);
CREATE INDEX IF NOT EXISTS idx_ea_person ON event_attendance(person_id);
CREATE INDEX IF NOT EXISTS idx_ea_status ON event_attendance(status);

COMMENT ON TABLE event_attendance IS 'G3.3 : présence à un événement (check-in QR, flash téléphone, manuel)';

-- 10. EVENT_DOCUMENT (documents liés à l'événement)
CREATE TABLE IF NOT EXISTS event_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    document_id UUID NOT NULL, -- FK vers document (fichier isolé §39)
    type VARCHAR(30) CHECK (type IN ('PROGRAM', 'SCRIPT', 'LYRICS', 'SLIDES', 'RECORDING', 'REPORT', 'OTHER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ed_event ON event_document(event_id);

COMMENT ON TABLE event_document IS 'G3.3 : documents liés à l\'événement (partition, programme, slides, enregistrement)';

-- 11. EVENT_SCHEDULE (programme détaillé / timeline)
CREATE TABLE IF NOT EXISTS event_schedule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    event_team_id UUID REFERENCES event_team(id) ON DELETE SET NULL,
    location_id UUID REFERENCES location(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ,
    order_index INTEGER NOT NULL DEFAULT 0,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_esched_event ON event_schedule(event_id);
CREATE INDEX IF NOT EXISTS idx_esched_time ON event_schedule(start_at);

COMMENT ON TABLE event_schedule IS 'G3.3 : programme détaillé de l\'événement (timeline par équipe/lieu)';

-- Triggers updated_at
DROP TRIGGER IF EXISTS update_location_updated_at ON location;
CREATE TRIGGER update_location_updated_at BEFORE UPDATE ON location FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_event_updated_at ON event;
CREATE TRIGGER update_event_updated_at BEFORE UPDATE ON event FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_event_team_updated_at ON event_team;
CREATE TRIGGER update_event_team_updated_at BEFORE UPDATE ON event_team FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_event_task_updated_at ON event_task;
CREATE TRIGGER update_event_task_updated_at BEFORE UPDATE ON event_task FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_event_schedule_updated_at ON event_schedule;
CREATE TRIGGER update_event_schedule_updated_at BEFORE UPDATE ON event_schedule FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();