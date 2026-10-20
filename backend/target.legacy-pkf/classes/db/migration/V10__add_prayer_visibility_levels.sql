-- V10__add_prayer_visibility_levels.sql
-- Phase 9: Add role-based visibility levels for prayers
-- New levels: GENERALE (all), PASTEUR_RESPONSABLE (pasteur+responsables), FAISEUR (faiseurs+chefs)

-- Drop old CHECK constraint and add new one with extended values
ALTER TABLE prayers DROP CONSTRAINT IF EXISTS prayers_visibilite_check;
ALTER TABLE prayers ADD CONSTRAINT prayers_visibilite_check
    CHECK (visibilite IN ('PRIVEE', 'PARTAGEE', 'GENERALE', 'PASTEUR_RESPONSABLE', 'FAISEUR'));

-- Index on visibilite for faster filtering
CREATE INDEX IF NOT EXISTS idx_prayers_visibilite_filter ON prayers(visibilite);
