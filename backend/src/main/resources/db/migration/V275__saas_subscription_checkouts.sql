-- V275__saas_subscription_checkouts.sql
-- ============================================================
-- Corrélation des passages en caisse d'ABONNEMENT SaaS payés via un
-- prestataire à redirection externe (agrégateur CinetPay : Orange Money,
-- MTN MoMo, Wave, Visa/Mastercard).
--
-- TODO_TARIFS_PAIEMENTS_INTERNATIONAL — continuité (T6 abonnements OM/MoMo/Visa).
--
-- POURQUOI UNE TABLE DÉDIÉE.
--   * saas_momo_subscriptions (V…) modélise la <em>tokenization récurrente MTN
--     MoMo</em> : prélèvements relançables sans interaction, phone OBLIGATOIRE,
--     price_id OBLIGATOIRE, un seul par tenant. Un encaissement CinetPay n'est
--     pas cela — c'est une intention de paiement ponctuelle à chaque échéance,
--     avec redirection CPS, et la carte ne fournit pas de MSISDN. On ne force
--     donc pas ces sémantiques étrangères l'une sur l'autre.
--   * payment_intents ne porte ni le plan, ni le cycle, ni le canal choisi :
--     cette table porte le rapprochement provider_reference → abonnement, lu au
--     webhook pour ACTIVER l'abonnement à la confirmation.
--
-- ADDITIF ET NON-DESTRUCTIF : table nouvelle uniquement. Aucune colonne ni table
-- existante n'est modifiée. Le contrat d'unités est respecté : amount_minor en
-- unités mineures ISO-4217 (cf. V245).
-- ============================================================

CREATE TABLE IF NOT EXISTS saas_subscription_checkouts (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID        NOT NULL,
    user_id            UUID,
    plan_key           VARCHAR(30) NOT NULL,
    billing_cycle      VARCHAR(20) NOT NULL,
    region_code        VARCHAR(10),
    currency           CHAR(3)     NOT NULL,
    -- Unités mineures ISO-4217 (contrat §2.1) : montant réellement encaissé.
    amount_minor       BIGINT      NOT NULL,
    -- Canal retenu pour ce passage en caisse : CARD (Visa/MC) ou MOMO.
    payment_channel    VARCHAR(20) NOT NULL,
    -- Opérateur routé vers le prestataire : ORANGE_MONEY / MTN_MOMO / WAVE / CARD.
    operator           VARCHAR(30) NOT NULL,
    -- Ligne payment_intents correspondante (corrélation bidirectionnelle).
    payment_intent_id  UUID,
    -- Notre transaction_id = provider_reference du CPS : CLÉ DU RAPPROCHEMENT WEBHOOK.
    provider_reference VARCHAR(255),
    -- PENDING | ACTIVE | FAILED | CANCELLED.
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    checkout_url       TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_sub_checkout_amount   CHECK (amount_minor >= 0),
    CONSTRAINT ck_sub_checkout_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_sub_checkout_cycle    CHECK (billing_cycle IN ('MONTHLY','YEARLY'))
);

-- Le webhook rapproche une transaction CPS par sa référence.
CREATE UNIQUE INDEX IF NOT EXISTS uq_sub_checkout_provider_ref
    ON saas_subscription_checkouts (provider_reference)
    WHERE provider_reference IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sub_checkout_tenant
    ON saas_subscription_checkouts (tenant_id, status);

COMMENT ON TABLE  saas_subscription_checkouts IS
    'Passages en caisse d''abonnement SaaS via redirection externe (CinetPay OM/MoMo/Wave/Visa). Rapproche provider_reference vers abonnement pour activation a la confirmation. amount_minor en unites mineures ISO-4217.';
COMMENT ON COLUMN saas_subscription_checkouts.status IS
    'PENDING (caisse ouverte) | ACTIVE (payé, abonnement activé) | FAILED | CANCELLED.';
COMMENT ON COLUMN saas_subscription_checkouts.provider_reference IS
    'transaction_id imposé à CPS = provider_reference de la payment_intent. Clé idempotente du notify/validate.';
