-- V54__department_member_reports.sql
-- ============================================================
-- DEPARTMENT MANAGEMENT SYSTEM — rapports du responsable sur un membre
-- Bilan rédigé par le responsable (comportement, assiduité,
-- capacité, progression, incident, discipline, recommandation).
-- Complète les rapports hebdomadaires des faiseurs.
-- ============================================================

CREATE TABLE IF NOT EXISTS department_member_reports (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    member_id UUID NOT NULL,
    auteur_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    contenu TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dept_member_reports_dept ON department_member_reports(department_id);
CREATE INDEX IF NOT EXISTS idx_dept_member_reports_member ON department_member_reports(member_id);
