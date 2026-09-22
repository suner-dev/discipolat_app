-- ============================================================
-- V56 — Evenements de departement
-- Ajoute un rattachement optionnel des evenements a un departement
-- (espace Responsable : evenements du departement lies a ses equipes).
-- ============================================================

ALTER TABLE events ADD COLUMN IF NOT EXISTS department_id UUID REFERENCES departments(id);

CREATE INDEX IF NOT EXISTS idx_events_department ON events(department_id);
