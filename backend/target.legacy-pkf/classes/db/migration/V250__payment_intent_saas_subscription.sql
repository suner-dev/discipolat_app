-- V250__payment_intent_saas_subscription.sql
-- ============================================================
-- TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T6.3 (partie 1/2)
-- Nouveau purpose SAAS_SUBSCRIPTION pour payment_intents
-- ============================================================

-- Vérifier la contrainte CHECK existante sur payment_intents.purpose
-- et l'étendre avec SAAS_SUBSCRIPTION

DO $$
BEGIN
    -- Supprimer l'ancienne contrainte si elle existe
    IF EXISTS (
        SELECT 1 FROM information_schema.check_constraints
        WHERE constraint_name = 'ck_payment_intents_purpose'
    ) THEN
        ALTER TABLE payment_intents DROP CONSTRAINT ck_payment_intents_purpose;
    END IF;

    -- Recréer avec la nouvelle valeur
    ALTER TABLE payment_intents
        ADD CONSTRAINT ck_payment_intents_purpose
        CHECK (purpose IN ('DIME', 'OFFRANDE', 'PROMESSE', 'PROJET_SPECIAL', 'DON_DIASPORA', 'SAAS_SUBSCRIPTION'));
END $$;

COMMENT ON COLUMN payment_intents.purpose IS
    'DIME, OFFRANDE, PROMESSE, PROJET_SPECIAL, DON_DIASPORA, SAAS_SUBSCRIPTION (abonnements SaaS via Mobile Money)';