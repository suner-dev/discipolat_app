-- FamilyMeeting — réunions de famille avec ordre du jour auto-génération et compte-rendu.
-- Table créée par migration pour coller à ddl-auto=validate.

CREATE TABLE IF NOT EXISTS family_meetings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    family_id       UUID,
    organised_by    UUID,
    agenda          TEXT,
    minutes         TEXT,
    status          VARCHAR(20) DEFAULT 'DRAFT',
    scheduled_at    TIMESTAMP,
    attendees_count INTEGER DEFAULT 0,
    is_auto_generated BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_family_meetings_tenant
    ON family_meetings (tenant_id);
CREATE INDEX IF NOT EXISTS idx_family_meetings_family
    ON family_meetings (tenant_id, family_id);
CREATE INDEX IF NOT EXISTS idx_family_meetings_scheduled
    ON family_meetings (tenant_id, scheduled_at DESC);
