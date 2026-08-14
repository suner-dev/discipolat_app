-- V53__department_member_objectives.sql
-- ============================================================
-- DEPARTMENT MANAGEMENT SYSTEM — objectifs de progression
-- Suivi individuel des objectifs fixés à un membre par le
-- responsable (progression spirituelle, disciplinaire ou
-- opérationnelle), avec avancement 0..100 et échéance.
-- ============================================================

CREATE TABLE IF NOT EXISTS department_member_objectives (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    member_id UUID NOT NULL,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    echeance DATE,
    avancement INTEGER NOT NULL DEFAULT 0,
    statut VARCHAR(20) NOT NULL DEFAULT 'A_FAIRE',
    cree_par UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dept_objectives_dept ON department_member_objectives(department_id);
CREATE INDEX IF NOT EXISTS idx_dept_objectives_member ON department_member_objectives(member_id);
