-- V50__department_dms_expansion.sql
-- ============================================================
-- DEPARTMENT MANAGEMENT SYSTEM — expansion v2
-- 1. Traçabilité directe des rattachements (qui a ajouté, d'où
--    provient le rattachement : manuel / inscription / transfert).
-- 2. Notes départementales par membre (dossier de gestion).
-- 3. Annonces du département (communication interne).
-- ============================================================

-- ------------------------------------------------------------
-- 1. soul_departments : qui / d'où / quand
-- created_by : compte utilisateur à l'origine du rattachement.
-- origine    : MANUEL (ajout par un responsable/pasteur),
--              SIGNUP (auto-affectation à l'inscription),
--              TRANSFERT (exécution d'un workflow de transfert).
-- ------------------------------------------------------------
ALTER TABLE soul_departments ADD COLUMN IF NOT EXISTS created_by UUID;
ALTER TABLE soul_departments ADD COLUMN IF NOT EXISTS origine VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_soul_departments_created_by ON soul_departments(created_by);

-- ------------------------------------------------------------
-- 2. Notes départementales par membre
-- Notes internes du responsable sur un membre de son département
-- (dossier de gestion) — distinctes des notes de disciple.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department_member_notes (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    member_id UUID NOT NULL,
    auteur_id UUID NOT NULL,
    contenu TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_dept_member_notes_dept ON department_member_notes(department_id);
CREATE INDEX IF NOT EXISTS idx_dept_member_notes_member ON department_member_notes(member_id);

-- ------------------------------------------------------------
-- 3. Annonces du département (communication interne)
-- Cible : tous les membres, une équipe (team_id) ou un poste
-- (position_id). Affichées dans l'espace responsable et le
-- dossier des membres concernés.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department_announcements (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    auteur_id UUID NOT NULL,
    titre VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    cible VARCHAR(20) NOT NULL DEFAULT 'TOUS',
    team_id UUID REFERENCES department_teams(id),
    position_id UUID REFERENCES department_positions(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_dept_announcements_dept ON department_announcements(department_id);
CREATE INDEX IF NOT EXISTS idx_dept_announcements_team ON department_announcements(team_id);
