-- V55__department_reports_checklists_equipment.sql
-- ============================================================
-- DEPARTMENT MANAGEMENT SYSTEM — rapports de département,
-- checklists et inventaire matériel
-- ============================================================

-- ------------------------------------------------------------
-- Rapports de département (synthèses sauvegardées / exportables)
-- type : HEBDOMADAIRE | MENSUEL | TRIMESTRIEL | ANNUEL |
--        EVENEMENT | INCIDENT | DISCIPLINE | ACTIVITE |
--        EFFECTIF | ASSIDUITE | PERFORMANCE | SYNTHESE
-- statut : BROUILLON | SOUMIS | ARCHIVE
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department_reports (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    auteur_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    titre VARCHAR(255) NOT NULL,
    periode_debut DATE,
    periode_fin DATE,
    contenu TEXT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dept_reports_dept ON department_reports(department_id);
CREATE INDEX IF NOT EXISTS idx_dept_reports_type ON department_reports(department_id, type);

-- ------------------------------------------------------------
-- Checklists du département (événement, tâche, équipe, membre, général)
-- cible_type : GENERAL | TACHE | EVENEMENT | EQUIPE | MEMBRE
-- statut : OUVERTE | TERMINEE
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department_checklists (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    titre VARCHAR(255) NOT NULL,
    cible_type VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    cible_id UUID,
    statut VARCHAR(20) NOT NULL DEFAULT 'OUVERTE',
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dept_checklists_dept ON department_checklists(department_id);
CREATE INDEX IF NOT EXISTS idx_dept_checklists_cible ON department_checklists(cible_type, cible_id);

CREATE TABLE IF NOT EXISTS department_checklist_items (
    id UUID PRIMARY KEY,
    checklist_id UUID NOT NULL REFERENCES department_checklists(id) ON DELETE CASCADE,
    libelle VARCHAR(255) NOT NULL,
    fait BOOLEAN NOT NULL DEFAULT FALSE,
    ordre INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dept_checklist_items_checklist ON department_checklist_items(checklist_id);

-- ------------------------------------------------------------
-- Inventaire / matériel du département
-- etat : NEUF | BON | USAGE | REPARATION | HORS_SERVICE
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department_equipment (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    quantite INTEGER NOT NULL DEFAULT 1,
    etat VARCHAR(20) NOT NULL DEFAULT 'BON',
    responsable_id UUID,
    affecte_a_id UUID,
    localisation VARCHAR(255),
    date_acquisition DATE,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dept_equipment_dept ON department_equipment(department_id);
CREATE INDEX IF NOT EXISTS idx_dept_equipment_responsable ON department_equipment(responsable_id);
