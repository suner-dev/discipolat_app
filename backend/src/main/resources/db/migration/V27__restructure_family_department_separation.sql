-- V27__restructure_family_department_separation.sql
-- MISSION RESTRUCTURATION MAJEURE :
-- Séparation définitive Famille / Département
-- Ajout chef adjoint, user_id sur families
-- Table de liaison soul_departments (ManyToMany)
-- Historique des changements de rôle, département, famille

-- ============================================================
-- 1. FAMILIES : Supprimer la liaison departement_id
-- ============================================================

-- Avant de supprimer, on sauvegarde les anciennes relations
CREATE TABLE IF NOT EXISTS family_department_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    family_id UUID NOT NULL,
    old_departement_id UUID,
    changed_by UUID,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(id),
    FOREIGN KEY (old_departement_id) REFERENCES departments(id)
);

-- Copier les anciennes relations dans l'historique
INSERT INTO family_department_history (family_id, old_departement_id, changed_at)
SELECT id, departement_id, CURRENT_TIMESTAMP
FROM families
WHERE departement_id IS NOT NULL AND deleted = false;

-- Supprimer la contrainte FK et la colonne departement_id
ALTER TABLE families DROP CONSTRAINT IF EXISTS families_departement_id_fkey;
ALTER TABLE families DROP COLUMN IF EXISTS departement_id;

-- ============================================================
-- 2. FAMILIES : Ajouter chef_adjoint_id et user_id
-- ============================================================

-- user_id : lien avec le compte utilisateur du chef de famille
ALTER TABLE families ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id);
CREATE INDEX IF NOT EXISTS idx_families_user ON families(user_id);

-- chef_adjoint_id : chef adjoint de la famille
ALTER TABLE families ADD COLUMN IF NOT EXISTS chef_adjoint_id UUID REFERENCES users(id);
CREATE INDEX IF NOT EXISTS idx_families_chef_adjoint ON families(chef_adjoint_id);

-- Mettre à jour user_id avec chef_famille_id pour les familles existantes
UPDATE families SET user_id = chef_famille_id WHERE user_id IS NULL AND deleted = false;

-- ============================================================
-- 3. SOUL_DEPARTMENTS : Table de liaison ManyToMany âme-département
-- ============================================================

CREATE TABLE IF NOT EXISTS soul_departments (
    soul_id UUID NOT NULL,
    department_id UUID NOT NULL,
    date_affectation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_desaffectation TIMESTAMP,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (soul_id, department_id),
    FOREIGN KEY (soul_id) REFERENCES souls(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_soul_departments_soul ON soul_departments(soul_id);
CREATE INDEX IF NOT EXISTS idx_soul_departments_dept ON soul_departments(department_id);

-- ============================================================
-- 4. ROLE_HISTORY : Historique des changements de rôle
-- ============================================================

CREATE TABLE IF NOT EXISTS role_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    old_role VARCHAR(50),
    new_role VARCHAR(50) NOT NULL,
    changed_by UUID,
    raison TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_role_history_user ON role_history(user_id);

-- ============================================================
-- 5. DEPARTMENT_HISTORY : Historique des changements de département
-- ============================================================

CREATE TABLE IF NOT EXISTS department_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    soul_id UUID NOT NULL,
    old_department_id UUID,
    new_department_id UUID NOT NULL,
    changed_by UUID,
    raison TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (soul_id) REFERENCES souls(id),
    FOREIGN KEY (old_department_id) REFERENCES departments(id),
    FOREIGN KEY (new_department_id) REFERENCES departments(id)
);

CREATE INDEX IF NOT EXISTS idx_department_history_soul ON department_history(soul_id);

-- ============================================================
-- 6. FAMILY_HISTORY : Historique des changements de famille
-- ============================================================

CREATE TABLE IF NOT EXISTS family_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    soul_id UUID NOT NULL,
    old_family_id UUID,
    new_family_id UUID NOT NULL,
    changed_by UUID,
    raison TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (soul_id) REFERENCES souls(id),
    FOREIGN KEY (old_family_id) REFERENCES families(id),
    FOREIGN KEY (new_family_id) REFERENCES families(id)
);

CREATE INDEX IF NOT EXISTS idx_family_history_soul ON family_history(soul_id);

-- ============================================================
-- 7. USER_DEPARTMENTS : Table de liaison utilisateur-département
--    (pour les responsables qui gèrent plusieurs départements)
-- ============================================================

CREATE TABLE IF NOT EXISTS user_departments (
    user_id UUID NOT NULL,
    department_id UUID NOT NULL,
    role_dans_dept VARCHAR(50) NOT NULL DEFAULT 'MEMBRE',
    date_affectation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, department_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_departments_user ON user_departments(user_id);
CREATE INDEX IF NOT EXISTS idx_user_departments_dept ON user_departments(department_id);

-- ============================================================
-- 8. SEED : Migrer les données existantes
-- ============================================================

-- Pour chaque âme, créer une entrée soul_departments basée sur son département actuel
-- (via la famille qui appartenait à un département)
INSERT INTO soul_departments (soul_id, department_id, date_affectation, actif)
SELECT DISTINCT s.id, d.id, s.date_integration, true
FROM souls s
JOIN families f ON s.famille_id = f.id
JOIN departments d ON d.deleted = false
WHERE s.deleted = false
  AND f.deleted = false
  AND NOT EXISTS (
    SELECT 1 FROM soul_departments sd WHERE sd.soul_id = s.id AND sd.department_id = d.id
  )
ON CONFLICT (soul_id, department_id) DO NOTHING;

-- Pour les responsables, créer user_departments
INSERT INTO user_departments (user_id, department_id, role_dans_dept)
SELECT d.responsable_id, d.id, 'RESPONSABLE'
FROM departments d
WHERE d.deleted = false
  AND NOT EXISTS (
    SELECT 1 FROM user_departments ud WHERE ud.user_id = d.responsable_id AND ud.department_id = d.id
  )
ON CONFLICT (user_id, department_id) DO NOTHING;

-- ============================================================
-- 9. COMMENTAIRES
-- ============================================================

COMMENT ON TABLE soul_departments IS 'Table de liaison ManyToMany âme ↔ département. Un membre peut appartenir à plusieurs départements.';
COMMENT ON TABLE user_departments IS 'Table de liaison utilisateur ↔ département pour les responsables multi-départements.';
COMMENT ON TABLE role_history IS 'Historique complet des changements de rôle de chaque utilisateur.';
COMMENT ON TABLE department_history IS 'Historique des transferts entre départements.';
COMMENT ON TABLE family_history IS 'Historique des changements de famille.';
COMMENT ON TABLE family_department_history IS 'Historique des anciennes liaisons famille-département (supprimées par V27).';
