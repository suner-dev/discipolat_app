-- V51__fix_notifications_type_constraint.sql
-- ============================================================
-- CORRECTION : la contrainte CHECK « notifications_type_check »
-- créée en V1 n'autorise que 5 types de notifications alors que
-- l'enum TypeNotification en compte 23 (transferts, DMS…). Toute
-- notification MEMBRE_AJOUTE, TACHE_ASSIGNEE, TRANSFERT_EXECUTEE…
-- échouait sur PostgreSQL avec « violates check constraint ».
-- On remplace la contrainte par la liste complète miroir de l'enum.
-- ============================================================

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (
    type IN (
        'RAPPORT_NON_SOUMIS',
        'ABSENCE_48H',
        'RAPPORT_FAMILLE_NON_SOUMIS',
        'ALERTE_ABSENCE',
        'INFORMATION',
        'PRIERE_EXAUCEE',
        'TRANSFERT_DEMANDE',
        'TRANSFERT_VALIDATION',
        'TRANSFERT_VALIDEE',
        'TRANSFERT_REFUSEE',
        'TRANSFERT_INFOS_DEMANDEES',
        'TRANSFERT_CORRECTION',
        'TRANSFERT_EXECUTEE',
        'TRANSFERT_ANNULEE',
        'TRANSFERT_DELAI_DEPASSE',
        'MEMBRE_AJOUTE',
        'MEMBRE_RETIRE',
        'TACHE_ASSIGNEE',
        'TACHE_EN_RETARD',
        'MEMBRE_AFFECTE'
    )
);
