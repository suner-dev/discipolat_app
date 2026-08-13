-- V48__department_management.sql
-- ============================================================
-- DEPARTMENT MANAGEMENT SYSTEM v1
-- Transforme le module Responsable de département en un véritable
-- outil de gestion : sous-départements/équipes (hiérarchie
-- récursive), postes, affectations de membres (avec dates et
-- traçabilité), tâches et journal d'activité du département.
-- ============================================================

-- ------------------------------------------------------------
-- 1. ÉQUIPES / SOUS-DÉPARTEMENTS
-- parent_id NULL = racine ; sinon sous-équipe (récursif, sans
-- limite de profondeur). TYPE : SOUS_DEPARTEMENT | EQUIPE_PERMANENTE
-- | EQUIPE_TEMPORAIRE (avec date_debut/date_fin pour les équipes
-- temporaires, archivage à la fin de période).
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department_teams (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    parent_id UUID REFERENCES department_teams(id),
    nom VARCHAR(150) NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'EQUIPE_PERMANENTE',
    chef_id UUID,
    adjoint_id UUID,
    objectif VARCHAR(500),
    description TEXT,
    date_debut DATE,
    date_fin DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dept_teams_department ON department_teams(department_id);
CREATE INDEX IF NOT EXISTS idx_dept_teams_parent ON department_teams(parent_id);

-- ------------------------------------------------------------
-- 2. POSTES DU DÉPARTEMENT (technicien son, vidéaste, secrétaire…)
-- Noms entièrement configurables par le responsable.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department_positions (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    nom VARCHAR(150) NOT NULL,
    description TEXT,
    competences_requises VARCHAR(1000),
    statut VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dept_positions_department ON department_positions(department_id);

-- ------------------------------------------------------------
-- 3. AFFECTATIONS MEMBRE -> ÉQUIPE / POSTE
-- member_id = soul_id. Traçabilité : created_by, dates, actif.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department_assignments (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    team_id UUID REFERENCES department_teams(id),
    position_id UUID REFERENCES department_positions(id),
    member_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'MEMBRE',
    date_debut DATE,
    date_fin DATE,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dept_assignments_department ON department_assignments(department_id);
CREATE INDEX IF NOT EXISTS idx_dept_assignments_member ON department_assignments(member_id);
CREATE INDEX IF NOT EXISTS idx_dept_assignments_team ON department_assignments(team_id);

-- ------------------------------------------------------------
-- 4. TÂCHES DU DÉPARTEMENT
-- Statuts : A_FAIRE | EN_COURS | BLOQUEE | TERMINEE | VALIDEE | ANNULEE
-- Priorités : BASSE | MOYENNE | HAUTE
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department_tasks (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    team_id UUID REFERENCES department_teams(id),
    titre VARCHAR(250) NOT NULL,
    description TEXT,
    assigned_to UUID,
    priorite VARCHAR(15) NOT NULL DEFAULT 'MOYENNE',
    statut VARCHAR(20) NOT NULL DEFAULT 'A_FAIRE',
    date_debut DATE,
    echeance DATE,
    avancement INTEGER NOT NULL DEFAULT 0,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dept_tasks_department ON department_tasks(department_id);
CREATE INDEX IF NOT EXISTS idx_dept_tasks_statut ON department_tasks(statut);
CREATE INDEX IF NOT EXISTS idx_dept_tasks_assigned ON department_tasks(assigned_to);

-- ------------------------------------------------------------
-- 5. JOURNAL D'ACTIVITÉ DU DÉPARTEMENT
-- Historique des mouvements : création d'équipe, affectation,
-- transfert, tâche, etc. (qui a fait quoi, quand).
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department_activity (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    actor_id UUID,
    actor_nom VARCHAR(200),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(30),
    entity_id UUID,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dept_activity_department ON department_activity(department_id, created_at DESC);
