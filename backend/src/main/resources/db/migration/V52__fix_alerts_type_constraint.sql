-- V52__fix_alerts_type_constraint.sql
-- ============================================================
-- CORRECTION : la contrainte CHECK « alerts_type_alerte_check »
-- n'autorise que 4 types d'alertes. Les alertes automatiques du
-- scheduler (ABSENCE_3_SEMAINES, TACHE_EN_RETARD) et les alertes
-- intelligentes du Department Management System (ABSENCE_REPETEE)
-- echouaient sur PostgreSQL (« violates check constraint »).
-- On etend la contrainte avec l'ensemble des types reellement emis.
-- ============================================================

ALTER TABLE alerts DROP CONSTRAINT IF EXISTS alerts_type_alerte_check;

ALTER TABLE alerts ADD CONSTRAINT alerts_type_alerte_check CHECK (
    type_alerte IN (
        'ABSENCE_48H',
        'RAPPORT_NON_SOUMIS',
        'RAPPORT_FAMILLE_NON_SOUMIS',
        'MANUEL',
        'ABSENCE_3_SEMAINES',
        'TACHE_EN_RETARD',
        'ABSENCE_REPETEE'
    )
);
