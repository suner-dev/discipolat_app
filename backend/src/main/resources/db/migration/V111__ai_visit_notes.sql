-- AiVisitNote — notes de visites générées par IA (transcription, résumé, actions).
-- Table ajoutée par migration : indispensable au démarrage en dev (ddl-auto=validate).

CREATE TABLE IF NOT EXISTS ai_visit_notes (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL,
    visit_id         UUID,
    member_id        UUID,
    pastor_id        UUID,
    raw_transcription TEXT,
    ai_summary        TEXT,
    ai_action_items   TEXT,
    ai_sentiment      VARCHAR(20),
    is_verified       BOOLEAN DEFAULT FALSE,
    created_at        TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ai_visit_notes_tenant
    ON ai_visit_notes (tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_visit_notes_member
    ON ai_visit_notes (tenant_id, member_id);
CREATE INDEX IF NOT EXISTS idx_ai_visit_notes_visit
    ON ai_visit_notes (tenant_id, visit_id);
