-- V248__subscription_price_snapshot.sql
-- ============================================================
-- TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T1.4
-- Figeage du prix sur l'abonnement (contrat §2.4).
--
-- Pourquoi : si le super-admin change un tarif, un abonnement en cours ne doit
-- PAS etre re-facture au nouveau prix avant son renouvellement. On fige donc
-- le prix sur la ligne d'abonnement a la souscription, avec un instantane JSON
-- du plan pour pouvoir expliquer factuellement la difference a l'utilisateur.
--
-- ADDITIF : aucune colonne existante n'est supprimee.
-- ============================================================

ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS price_currency      CHAR(3),
    ADD COLUMN IF NOT EXISTS price_minor         BIGINT,
    ADD COLUMN IF NOT EXISTS price_cycle         VARCHAR(20),
    -- STRIPE | MOMO | STRONG | MANUAL | ADMIN | MIGRATION
    ADD COLUMN IF NOT EXISTS price_source        VARCHAR(20) NOT NULL DEFAULT 'MIGRATION',
    ADD COLUMN IF NOT EXISTS price_snapshot_json JSONB;

COMMENT ON COLUMN tenant_subscriptions.price_minor IS
    'Prix fige en unites mineures ISO-4217 au moment de la souscription (§2.4).';
COMMENT ON COLUMN tenant_subscriptions.price_source IS
    'Origine du prix : STRIPE | MOMO | STRONG | MANUAL | ADMIN | MIGRATION (abonnements anterieurs).';
COMMENT ON COLUMN tenant_subscriptions.price_snapshot_json IS
    'Instantane du plan au moment du figeage, pour expliquer une evolution de tarif.';

-- Les abonnements anterieurs n'ont pas de prix fige : on le dit explicitement
-- plutot que de laisser NULL silencieusement (l'UI affiche « tarif non renseigne »).
UPDATE tenant_subscriptions
   SET price_source = 'MIGRATION',
       price_snapshot_json = jsonb_build_object(
           'migrated_at',   CURRENT_TIMESTAMP,
           'plan_key',      plan_key,
           'note',          'Aucun prix fige avant T1 : le tarif sera fige au prochain renouvellement.')
 WHERE price_source = 'MIGRATION'
   AND price_snapshot_json IS NULL;

-- Index de contrôle : abonnements payants sans prix figé (doit tendre vers 0).
CREATE INDEX IF NOT EXISTS idx_subscription_price_missing
    ON tenant_subscriptions(status)
    WHERE price_minor IS NULL AND status IN ('ACTIVE','TRIAL','PAST_DUE');