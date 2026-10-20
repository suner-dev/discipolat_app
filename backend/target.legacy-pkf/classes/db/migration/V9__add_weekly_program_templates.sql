-- V9__add_weekly_program_templates.sql
-- US-50: Weekly program templates for recurring events

CREATE TABLE IF NOT EXISTS weekly_program_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    type_evenement VARCHAR(50) NOT NULL,
    jour_semaine VARCHAR(20) NOT NULL CHECK (jour_semaine IN (
        'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE'
    )),
    heure_debut TIME NOT NULL,
    heure_fin TIME,
    lieu VARCHAR(255),
    duree_minutes INTEGER,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    couleur VARCHAR(20),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_weekly_program_templates_actif ON weekly_program_templates(actif);
CREATE INDEX IF NOT EXISTS idx_weekly_program_templates_jour ON weekly_program_templates(jour_semaine);
