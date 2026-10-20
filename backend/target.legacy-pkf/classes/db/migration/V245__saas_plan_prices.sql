-- V245__saas_plan_prices.sql
-- ============================================================
-- TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T1.1
-- Catalogue de prix multi-devises / multi-régions / multi-canaux.
--
-- ADDITIF : aucune colonne ni table existante n'est supprimée ou modifiée.
-- Les colonnes legacy price_eur / price_xaf / price_usd / price_monthly /
-- price_yearly restent lisibles et en écriture jusqu'à la fin de la transition.
--
-- CONTRAT D'UNITÉS : amount_minor est TOUJOURS en unités mineures ISO-4217
-- (XAF/XOF/JPY = 0 décimale ; EUR/USD/CAD/GBP/CHF = 2 décimales).
-- Le nombre de décimales provient exclusivement de
-- modules/currency/domain/Iso4217CurrencyValidator#minorUnits.
-- ============================================================

CREATE TABLE IF NOT EXISTS saas_plan_prices (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_key                VARCHAR(30) NOT NULL REFERENCES saas_plans(key) ON DELETE CASCADE,
    currency                CHAR(3)     NOT NULL,
    billing_cycle           VARCHAR(20) NOT NULL,
    -- CONTRAT §2.1 : unités mineures ISO-4217.
    amount_minor            BIGINT      NOT NULL,
    -- 'GLOBAL' sentinelle (JAMAIS NULL) : PostgreSQL ne déduplique pas les
    -- NULL dans une contrainte UNIQUE, deux lignes GLOBAL passeraient.
    region_code             VARCHAR(10) NOT NULL DEFAULT 'GLOBAL',
    -- Canal STRONG : devise réellement débitée quand elle diffère de l'affichage.
    checkout_currency       CHAR(3),

    -- Visibilité et choix : chaque interrupteur est piloté par le super-admin.
    is_enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    is_visible              BOOLEAN NOT NULL DEFAULT TRUE,
    is_selectable           BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order              INTEGER  NOT NULL DEFAULT 0,

    -- Canaux de paiement, CSV ordonné : CARD,MOMO,STRONG,BANK,CONTACT.
    -- L'ordre du CSV fait foi pour l'affichage des CTA.
    payment_channels        VARCHAR(100) NOT NULL DEFAULT 'CARD',

    -- Synchronisation Stripe (Q3a) : création des Price via l'API Stripe.
    stripe_price_id         VARCHAR(255),
    stripe_price_status     VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUIRED',
    stripe_price_synced_at  TIMESTAMPTZ,
    stripe_sync_error       TEXT,

    created_by              UUID,
    updated_by              UUID,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata                JSONB NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT ck_plan_price_amount   CHECK (amount_minor >= 0),
    CONSTRAINT ck_plan_price_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_plan_price_cycle    CHECK (billing_cycle IN ('MONTHLY','YEARLY')),
    CONSTRAINT ck_plan_price_channels CHECK (payment_channels <> '')
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_plan_price
    ON saas_plan_prices (plan_key, currency, billing_cycle, region_code);

CREATE INDEX IF NOT EXISTS idx_plan_price_lookup
    ON saas_plan_prices (currency, billing_cycle, is_enabled, is_visible);

-- Index partiel : le batch de synchronisation Stripe ne voit que les lignes à traiter.
CREATE INDEX IF NOT EXISTS idx_plan_price_stripe_pending
    ON saas_plan_prices (stripe_price_status)
    WHERE stripe_price_status IN ('PENDING','FAILED');

CREATE INDEX IF NOT EXISTS idx_plan_price_plan
    ON saas_plan_prices (plan_key);

COMMENT ON TABLE  saas_plan_prices IS
    'Prix par plan x devise x cycle x région. amount_minor en unités mineures ISO-4217 (contrat §2.1).';
COMMENT ON COLUMN saas_plan_prices.amount_minor IS
    'Montant en unités mineures ISO-4217. 15000 XAF = 15 000 FCFA ; 2900 EUR = 29,00 €.';
COMMENT ON COLUMN saas_plan_prices.region_code IS
    '''GLOBAL'' = tarif monde entier. Sinon code région (AF, EU, NA, US, CA, UK...).';
COMMENT ON COLUMN saas_plan_prices.stripe_price_status IS
    'NOT_REQUIRED | PENDING | SYNCED | FAILED | MANUAL — piloté par StripePriceSyncService.';