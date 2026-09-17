-- V158__migrate_legacy_events_to_church_event.sql
-- ============================================================
-- G3.3 — Migration des événements legacy vers Church OS Event Engine
-- Renomme events -> legacy_events, crée event (Church OS), migre les données
-- ============================================================

-- 1. Renommer la table legacy
ALTER TABLE IF EXISTS events RENAME TO legacy_events;

-- Renommer les index
ALTER INDEX IF EXISTS idx_events_organisateur RENAME TO idx_legacy_events_organisateur;
ALTER INDEX IF EXISTS idx_events_famille RENAME TO idx_legacy_events_famille;
ALTER INDEX IF EXISTS idx_events_type RENAME TO idx_legacy_events_type;
ALTER INDEX IF EXISTS idx_events_date RENAME TO idx_legacy_events_date;
ALTER INDEX IF EXISTS idx_events_statut RENAME TO idx_legacy_events_statut;
ALTER INDEX IF EXISTS idx_events_deleted RENAME TO idx_legacy_events_deleted;

-- 2. Créer la nouvelle table event (Church OS schema)
CREATE TABLE IF NOT EXISTS event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) CHECK (type IN ('SERVICE', 'MEETING', 'TRAINING', 'EVANGELISM', 'CONFERENCE', 'RETREAT', 'WEDDING', 'BAPTISM', 'FUNERAL', 'OTHER')),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED', 'ARCHIVED')),
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ,
    timezone VARCHAR(64),
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_rule TEXT,
    visibility VARCHAR(30) NOT NULL DEFAULT 'CHURCH' CHECK (visibility IN ('PRIVATE', 'TEAM', 'CHURCH', 'PUBLIC')),
    organizer_id UUID REFERENCES users(id),
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
CREATE INDEX IF NOT EXISTS idx_event_organizer ON event(organizer_id);

COMMENT ON TABLE event IS 'G3.3 : Church OS événement central — schema anglais, multi-tenant, configurable';
COMMENT ON COLUMN event.tenant_id IS 'Tenant obligatoire (multi-tenancy)';
COMMENT ON COLUMN event.type IS 'Type d\'événement (configurable via custom_status)';
COMMENT ON COLUMN event.status IS 'Statut (configurable via custom_status)';

-- 3. Migrer les données de legacy_events vers event
-- Note: tenant_id est requis. Pour les événements existants, on associe au premier tenant trouvé
-- ou on crée un tenant par défaut si aucun n'existe.
INSERT INTO event (id, tenant_id, title, description, type, status, start_at, end_at, 
                   organizer_id, visibility, created_at, updated_at, deleted_at)
SELECT 
    id,
    COALESCE((SELECT id FROM tenants LIMIT 1), gen_random_uuid()) as tenant_id,
    titre as title,
    description,
    CASE 
        WHEN type_evenement = 'SORTIE' THEN 'MEETING'
        WHEN type_evenement = 'RETRAITE' THEN 'RETREAT'
        WHEN type_evenement = 'EVANGELISATION' THEN 'EVANGELISM'
        WHEN type_evenement = 'REUNION' THEN 'MEETING'
        WHEN type_evenement = 'VISITE' THEN 'MEETING'
        WHEN type_evenement = 'CONFERENCE' THEN 'CONFERENCE'
        WHEN type_evenement = 'FORMATION' THEN 'TRAINING'
        WHEN type_evenement = 'ANNIVERSAIRE' THEN 'OTHER'
        ELSE 'OTHER'
    END as type,
    CASE 
        WHEN statut = 'PLANIFIE' THEN 'DRAFT'
        WHEN statut = 'EN_COURS' THEN 'PUBLISHED'
        WHEN statut = 'TERMINE' THEN 'COMPLETED'
        WHEN statut = 'ANNULE' THEN 'CANCELLED'
        ELSE 'DRAFT'
    END as status,
    date_debut AT TIME ZONE 'UTC' as start_at,
    date_fin AT TIME ZONE 'UTC' as end_at,
    organisateur_id as organizer_id,
    'CHURCH' as visibility,
    created_at AT TIME ZONE 'UTC' as created_at,
    updated_at AT TIME ZONE 'UTC' as updated_at,
    CASE WHEN deleted THEN deleted_at AT TIME ZONE 'UTC' ELSE NULL END as deleted_at
FROM legacy_events
WHERE NOT EXISTS (SELECT 1 FROM event WHERE event.id = legacy_events.id);

-- 4. Mettre à jour event_registrations pour pointer vers event (même IDs)
-- Les IDs sont conservés, donc pas de changement nécessaire pour la FK
-- Mais il faut renommer la contrainte
ALTER TABLE event_registrations 
    DROP CONSTRAINT IF EXISTS event_registrations_event_id_fkey,
    ADD CONSTRAINT event_registrations_event_id_fkey 
        FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE;

-- 5. Mettre à jour files (evenement_id -> event_id)
ALTER TABLE files 
    DROP CONSTRAINT IF EXISTS files_evenement_id_fkey,
    RENAME COLUMN evenement_id TO event_id,
    ADD CONSTRAINT files_event_id_fkey 
        FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE SET NULL;

-- 6. Mettre à jour department_event_attendance
ALTER TABLE department_event_attendance 
    DROP CONSTRAINT IF EXISTS department_event_attendance_event_id_fkey,
    ADD CONSTRAINT department_event_attendance_event_id_fkey 
        FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE;

-- 7. Mettre à jour department_teams
ALTER TABLE department_teams 
    DROP CONSTRAINT IF EXISTS department_teams_event_id_fkey,
    ADD CONSTRAINT department_teams_event_id_fkey 
        FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE SET NULL;

-- 8. Index sur les FK mises à jour
CREATE INDEX IF NOT EXISTS idx_event_reg_event ON event_registrations(event_id);
CREATE INDEX IF NOT EXISTS idx_files_event ON files(event_id);
CREATE INDEX IF NOT EXISTS idx_dept_event_att_event ON department_event_attendance(event_id);
CREATE INDEX IF NOT EXISTS idx_dept_teams_event ON department_teams(event_id);

-- 9. Trigger updated_at pour event
DROP TRIGGER IF EXISTS update_event_updated_at ON event;
CREATE TRIGGER update_event_updated_at 
    BEFORE UPDATE ON event 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE legacy_events IS 'Table legacy conservée pour compatibilité — ne pas modifier';
COMMENT ON TABLE event IS 'G3.3 : Church OS événement central — schéma anglais, multi-tenant, configurable';