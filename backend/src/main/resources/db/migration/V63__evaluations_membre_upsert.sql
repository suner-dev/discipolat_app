-- ============================================================
-- V63 : évaluations — catégorie MEMBRE + modification d'une
-- évaluation existante.
-- 1) La catégorie MEMBRE permet d'évaluer les membres (disciples
--    liés à un compte) et sert de repli pour les rôles sans
--    catégorie dédiée.
-- 2) La contrainte UNIQUE(evaluateur_id, evalue_id, categorie)
--    reste en place : « modifier » = upsert sur la même paire
--    (évaluateur, évalué, catégorie).
-- ============================================================
ALTER TABLE evaluations DROP CONSTRAINT IF EXISTS evaluations_categorie_check;
ALTER TABLE evaluations ADD CONSTRAINT evaluations_categorie_check
    CHECK (categorie IN ('RESPONSABLE', 'CHEF_FAMILLE', 'FAISEUR', 'MEMBRE'));

INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default)
VALUES ('EVALUATION_CATEGORIE', 'MEMBRE', 'Membre', '#8b5cf6', 4, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;
