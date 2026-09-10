-- EventChecklistItem — listes de vérification pour événements (templates auto-générés).
-- Table créée par migration pour coller à ddl-auto=validate.

CREATE TABLE IF NOT EXISTS event_checklists (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    event_id        UUID NOT NULL,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    status          VARCHAR(20) DEFAULT 'PENDING',
    assigned_to     UUID,
    order_index     INTEGER DEFAULT 0,
    created_at      TIMESTAMP DEFAULT now(),
    completed_at    TIMESTAMP,
    is_auto_generated BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_event_checklists_tenant
    ON event_checklists (tenant_id);
CREATE INDEX IF NOT EXISTS idx_event_checklists_event
    ON event_checklists (tenant_id, event_id);
CREATE INDEX IF NOT EXISTS idx_event_checklists_status
    ON event_checklists (tenant_id, event_id, status);
