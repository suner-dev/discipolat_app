-- =====================================================================
-- V33 : Pont des transferts existants vers le moteur de workflow.
--
-- Les anciens transferts directs (transferFaiseur, reassignChef,
-- reassign d'âme) passent désormais par le moteur de workflow.
-- 1) Une demande peut porter ses propres règles d'exécution (ex :
--    transfererAmes), fusionnées avec celles de la configuration au
--    moment de l'exécution automatique.
-- 2) Alignement des rôles initiateurs du changement de chef de famille :
--    l'ancien endpoint /families/{id}/chief était accessible aux chefs
--    de famille — on préserve cet accès dans le paramétrage par défaut.
-- =====================================================================

ALTER TABLE transfer_requests
    ADD COLUMN regles_execution jsonb;

COMMENT ON COLUMN transfer_requests.regles_execution IS
    'Règles d''exécution propres à la demande (ex : transfererAmes), fusionnées avec celles de la configuration du workflow.';

UPDATE transfer_workflow_configs
SET roles_initiateurs = '["PASTEUR", "CHEF_DE_FAMILLE"]'::jsonb
WHERE transfer_type = 'CHEF_FAMILLE_TRANSFERT';
