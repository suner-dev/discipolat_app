-- ============================================================
-- V63 : evaluations — categorie MEMBRE + modification d'une
-- evaluation existante.
-- 1) La categorie MEMBRE permet d'evaluer les membres (disciples
--    lies a un compte) et sert de repli pour les roles sans
--    categorie dediee.
-- 2) La contrainte UNIQUE(evaluateur_id, evalue_id, categorie)
--    reste en place : « modifier » = upsert sur la meme paire
--    (evaluateur, evalue, categorie).
-- ============================================================
ALTER TABLE evaluations DROP CONSTRAINT IF EXISTS evaluations_categorie_check;
ALTER TABLE evaluations ADD CONSTRAINT evaluations_categorie_check
    CHECK (categorie IN ('RESPONSABLE', 'CHEF_FAMILLE', 'FAISEUR', 'MEMBRE'));

INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default)
VALUES ('EVALUATION_CATEGORIE', 'MEMBRE', 'Membre', '#8b5cf6', 4, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;
