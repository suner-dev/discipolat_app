-- ============================================================
-- V56 — Événements de département
-- Ajoute un rattachement optionnel des événements à un département
-- (espace Responsable : événements du département liés à ses équipes).
-- ============================================================

ALTER TABLE events ADD COLUMN IF NOT EXISTS department_id UUID REFERENCES departments(id);

CREATE INDEX IF NOT EXISTS idx_events_department ON events(department_id);
