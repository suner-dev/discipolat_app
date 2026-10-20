-- V249__pricing_free_plan_key.sql
-- ============================================================
-- TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T8.4 (correction de la fuite F10)
--
-- Pourquoi : le provisionnement plateforme distingue deux intents qu'il ne
-- faut surtout PAS confondre.
--
--   * Le super-admin cree un tenant sur un plan PAYANT : acte commercial
--     volontaire, encaisse ou facture hors Stripe. Doit rester possible.
--
--   * Un visiteur s'inscrit en ligne depuis /register?plan=startup : il cree un
--     compte, il n'a RIEN paye. Lui donner un plan payant, c'est offrir un
--     service a credit sans encaissement — un client qu'on ne peut plus facturer
--     apres coup parce que le systeme croit deja qu'il est abonne.
--
-- On nomme donc explicitement le plan gratuit de l'entree de gamme plutot que de
-- le deviner : le super-admin peut le changer, et le code n'a pas a supposer que
-- ca s'appelle DISCOVERY.
-- ============================================================

ALTER TABLE saas_pricing_settings
    ADD COLUMN IF NOT EXISTS free_plan_key VARCHAR(30) NOT NULL DEFAULT 'DISCOVERY';

UPDATE saas_pricing_settings
   SET free_plan_key = 'DISCOVERY'
 WHERE id = 1 AND (free_plan_key IS NULL OR free_plan_key = '');

COMMENT ON COLUMN saas_pricing_settings.free_plan_key IS
    'Plan attribue a une inscription en ligne sans paiement. Le provisionnement plateforme n''est pas concerne : il peut poser un plan payant.';

