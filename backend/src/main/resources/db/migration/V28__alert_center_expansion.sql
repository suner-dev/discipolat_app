-- V28__alert_center_expansion.sql
-- Centre d'alertes complet :
-- Le pasteur, le responsable, le chef de famille et le faiseur peuvent créer
-- des alertes ciblant une personne, un département, une famille, un groupe ou l'église.

-- ============================================================
-- 1. Rendre ame_id et faiseur_id optionnels (alertes d'église / département)
-- ============================================================
ALTER TABLE alerts ALTER COLUMN ame_id DROP NOT NULL;
ALTER TABLE alerts ALTER COLUMN faiseur_id DROP NOT NULL;

-- ============================================================
-- 2. Nouveaux champs de ciblage
-- ============================================================
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS department_id UUID REFERENCES departments(id);
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS cible VARCHAR(50) NOT NULL DEFAULT 'PERSONNE'
    CHECK (cible IN ('PERSONNE', 'DEPARTEMENT', 'FAMILLE', 'GROUPE', 'EGLISE'));
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS priorite VARCHAR(20) NOT NULL DEFAULT 'MOYENNE'
    CHECK (priorite IN ('BASSE', 'MOYENNE', 'HAUTE', 'URGENTE'));
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS titre VARCHAR(255);
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS type_alerte_manuel VARCHAR(50);

-- Étendre les types d'alerte automatiques
ALTER TABLE alerts DROP CONSTRAINT IF EXISTS alerts_type_alerte_check;
ALTER TABLE alerts ADD CONSTRAINT alerts_type_alerte_check CHECK (
    type_alerte IN ('ABSENCE_48H', 'RAPPORT_NON_SOUMIS', 'RAPPORT_FAMILLE_NON_SOUMIS', 'MANUEL')
);

-- ============================================================
-- 3. Index pour le ciblage
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_alerts_cible ON alerts(cible);
CREATE INDEX IF NOT EXISTS idx_alerts_department ON alerts(department_id);
CREATE INDEX IF NOT EXISTS idx_alerts_priorite ON alerts(priorite);

COMMENT ON COLUMN alerts.cible IS 'Cible de l''alerte : PERSONNE, DEPARTEMENT, FAMILLE, GROUPE, EGLISE';
COMMENT ON COLUMN alerts.priorite IS 'Priorité : BASSE, MOYENNE, HAUTE, URGENTE';
COMMENT ON COLUMN alerts.type_alerte_manuel IS 'Type libre pour les alertes créées manuellement';
