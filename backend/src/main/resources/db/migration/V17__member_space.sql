-- V17__member_space.sql
-- Espace Membre : départements du membre (many-to-many), profil disciple enrichi,
-- départements ministères (Chorale, Audiovisuel).

-- ============================================================
-- PROFIL DISCIPLE ENRICHI (niveau d'étude, nombre d'enfants)
-- ============================================================
ALTER TABLE souls ADD COLUMN IF NOT EXISTS niveau_etude VARCHAR(150);
ALTER TABLE souls ADD COLUMN IF NOT EXISTS nb_enfants INTEGER;

-- Étendre la contrainte situation_familiale (Espace Membre : parent célibataire)
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_situation_familiale_check;
ALTER TABLE souls DROP CONSTRAINT IF EXISTS souls_situation_familiale_check;

ALTER TABLE users ADD CONSTRAINT users_situation_familiale_check
    CHECK (situation_familiale IN ('CELIBATAIRE', 'MARIE', 'DIVORCE', 'VEUF', 'PARENT_CELIBATAIRE', 'AUTRE'));

ALTER TABLE souls ADD CONSTRAINT souls_situation_familiale_check
    CHECK (situation_familiale IN ('CELIBATAIRE', 'MARIE', 'DIVORCE', 'VEUF', 'PARENT_CELIBATAIRE', 'AUTRE'));

-- ============================================================
-- MEMBER_DEPARTMENTS — Un membre (âme) peut appartenir à plusieurs départements
-- ============================================================
CREATE TABLE member_departments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    soul_id UUID NOT NULL REFERENCES souls(id) ON DELETE CASCADE,
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_member_departments UNIQUE (soul_id, department_id)
);

CREATE INDEX idx_member_departments_soul ON member_departments(soul_id);
CREATE INDEX idx_member_departments_department ON member_departments(department_id);

-- ============================================================
-- DÉPARTEMENTS MINISTÈRES (Chorale, Audiovisuel)
-- ============================================================
INSERT INTO departments (id, nom, description, responsable_id, statut, created_at, updated_at)
VALUES
    ('b0000000-0000-0000-0000-000000000003', 'Chorale', 'Ministère de louange et de musique',
     'a0000000-0000-0000-0000-000000000002', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000004', 'Audiovisuel', 'Son, image et retransmission des cultes',
     'a0000000-0000-0000-0000-000000000003', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
