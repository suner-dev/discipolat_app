-- V30__program_types_config.sql
-- Système de présences flexible :
-- Le pasteur configure les types de programmes (dimanche, convention, séminaire,
-- retraite, campagne, réunion spéciale, répétition, évangélisation, autre).
-- Chaque type peut avoir des sous-programmes (ex : premier culte, deuxième culte).
-- Lorsqu'une présence est enregistrée, l'utilisateur choisit Programme puis Sous-programme.

-- ============================================================
-- 1. PROGRAM_TYPES : types de programmes configurables
-- ============================================================

CREATE TABLE IF NOT EXISTS program_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    a_sous_programmes BOOLEAN NOT NULL DEFAULT FALSE,
    couleur VARCHAR(20),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    ordre INTEGER NOT NULL DEFAULT 0,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_program_types_actif ON program_types(actif);
CREATE INDEX IF NOT EXISTS idx_program_types_ordre ON program_types(ordre);

-- ============================================================
-- 2. PROGRAM_SUB_TYPES : sous-programmes d'un type
-- ============================================================

CREATE TABLE IF NOT EXISTS program_sub_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    program_type_id UUID NOT NULL,
    label VARCHAR(100) NOT NULL,
    heure_debut TIME,
    heure_fin TIME,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    ordre INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (program_type_id) REFERENCES program_types(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_program_sub_types_type ON program_sub_types(program_type_id);

-- ============================================================
-- 3. SEED : types de programmes par défaut
-- ============================================================

INSERT INTO program_types (code, label, description, a_sous_programmes, couleur, actif, ordre) VALUES
('DIMANCHE', 'Dimanche', 'Culte du dimanche', TRUE, '#8b5cf6', TRUE, 1),
('CONVENTION', 'Convention', 'Convention annuelle ou périodique', TRUE, '#f59e0b', TRUE, 2),
('SEMINAIRE', 'Séminaire', 'Séminaire de formation', TRUE, '#06b6d4', TRUE, 3),
('RETRAITE', 'Retraite', 'Retraite spirituelle', TRUE, '#10b981', TRUE, 4),
('CAMPAGNE', 'Campagne', 'Campagne d''évangélisation', TRUE, '#ef4444', TRUE, 5),
('REUNION_SPECIALE', 'Réunion spéciale', 'Réunion spéciale', FALSE, '#f97316', TRUE, 6),
('REPETITION', 'Répétition', 'Répétition (chorale, théâtre...)', FALSE, '#6366f1', TRUE, 7),
('EVANGELISATION', 'Évangélisation', 'Sortie d''évangélisation', FALSE, '#22c55e', TRUE, 8),
('ETUDE_BIBLIQUE', 'Étude biblique', 'Étude biblique en semaine', FALSE, '#14b8a6', TRUE, 9),
('AUTRE', 'Autre', 'Autre type de programme', FALSE, '#64748b', TRUE, 99)
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- 4. SEED : sous-programmes du Dimanche
-- ============================================================

INSERT INTO program_sub_types (program_type_id, label, heure_debut, actif, ordre)
SELECT id, 'Premier culte', '08:00', TRUE, 1 FROM program_types WHERE code = 'DIMANCHE' AND NOT EXISTS (
    SELECT 1 FROM program_sub_types WHERE program_type_id = (SELECT id FROM program_types WHERE code = 'DIMANCHE') AND label = 'Premier culte'
);

INSERT INTO program_sub_types (program_type_id, label, heure_debut, actif, ordre)
SELECT id, 'Deuxième culte', '10:00', TRUE, 2 FROM program_types WHERE code = 'DIMANCHE' AND NOT EXISTS (
    SELECT 1 FROM program_sub_types WHERE program_type_id = (SELECT id FROM program_types WHERE code = 'DIMANCHE') AND label = 'Deuxième culte'
);

INSERT INTO program_sub_types (program_type_id, label, heure_debut, actif, ordre)
SELECT id, 'Troisième culte', '12:00', TRUE, 3 FROM program_types WHERE code = 'DIMANCHE' AND NOT EXISTS (
    SELECT 1 FROM program_sub_types WHERE program_type_id = (SELECT id FROM program_types WHERE code = 'DIMANCHE') AND label = 'Troisième culte'
);

INSERT INTO program_sub_types (program_type_id, label, heure_debut, actif, ordre)
SELECT id, 'Culte des jeunes', '16:00', TRUE, 4 FROM program_types WHERE code = 'DIMANCHE' AND NOT EXISTS (
    SELECT 1 FROM program_sub_types WHERE program_type_id = (SELECT id FROM program_types WHERE code = 'DIMANCHE') AND label = 'Culte des jeunes'
);

-- ============================================================
-- 5. MEMBER_PRESENCES : enrichir avec programme + sous-programme
-- ============================================================

ALTER TABLE member_presences ADD COLUMN IF NOT EXISTS type_programme VARCHAR(50);
ALTER TABLE member_presences ADD COLUMN IF NOT EXISTS sous_programme VARCHAR(100);
ALTER TABLE member_presences ADD COLUMN IF NOT EXISTS present BOOLEAN;

CREATE INDEX IF NOT EXISTS idx_member_presences_type ON member_presences(type_programme);
CREATE INDEX IF NOT EXISTS idx_member_presences_semaine ON member_presences(semaine);

COMMENT ON TABLE program_types IS 'Types de programmes configurables par le pasteur (dimanche, convention, séminaire, retraite, campagne, etc.).';
COMMENT ON TABLE program_sub_types IS 'Sous-programmes d''un type (ex : premier culte, deuxième culte pour le dimanche).';
COMMENT ON COLUMN member_presences.type_programme IS 'Type de programme choisi lors de la saisie de présence.';
COMMENT ON COLUMN member_presences.sous_programme IS 'Sous-programme choisi (si le type en a).';
