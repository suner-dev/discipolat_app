-- =====================================================================
-- V33 : Pont des transferts existants vers le moteur de workflow.
--
-- Les anciens transferts directs (transferFaiseur, reassignChef,
-- reassign d'âme) passent desormais par le moteur de workflow.
-- 1) Une demande peut porter ses propres regles d'execution (ex :
--    transfererAmes), fusionnees avec celles de la configuration au
--    moment de l'execution automatique.
-- 2) Alignement des roles initiateurs du changement de chef de famille :
--    l'ancien endpoint /families/{id}/chief etait accessible aux chefs
--    de famille — on preserve cet acces dans le parametrage par defaut.
-- =====================================================================

ALTER TABLE transfer_requests
    ADD COLUMN regles_execution jsonb;

COMMENT ON COLUMN transfer_requests.regles_execution IS
    'Regles d''execution propres a la demande (ex : transfererAmes), fusionnees avec celles de la configuration du workflow.';

UPDATE transfer_workflow_configs
SET roles_initiateurs = '["PASTEUR", "CHEF_DE_FAMILLE"]'::jsonb
WHERE transfer_type = 'CHEF_FAMILLE_TRANSFERT';
