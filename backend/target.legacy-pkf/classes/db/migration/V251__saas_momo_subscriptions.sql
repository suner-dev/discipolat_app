-- V251__saas_momo_subscriptions.sql
-- ============================================================
-- TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T6.3 (partie 2/2)
-- Table abonnements SaaS via Mobile Money
-- ============================================================

CREATE TABLE IF NOT EXISTS saas_momo_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    plan_key VARCHAR(30) NOT NULL REFERENCES saas_plans(key) ON DELETE RESTRICT,
    price_id UUID NOT NULL REFERENCES saas_plan_prices(id) ON DELETE RESTRICT,
    plan_price_minor BIGINT NOT NULL,              -- montant figé en unités mineures (ISO-4217)
    currency CHAR(3) NOT NULL,                     -- devise du prix (ex: XAF)
    operator VARCHAR(30) NOT NULL,                 -- MTN_MOMO (seul supporté pour récurrent)
    phone VARCHAR(32) NOT NULL,                    -- numéro Mobile Money
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, ACTIVE, FAILED, CANCELLED, EXPIRED
    next_charge_at TIMESTAMPTZ,                    -- prochaine tentative de prélèvement
    provider_reference VARCHAR(255),               -- référence opérateur (token MTN)
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Un seul abonnement actif/en attente par tenant
CREATE UNIQUE INDEX IF NOT EXISTS uq_saas_momo_subscription_tenant
    ON saas_momo_subscriptions (tenant_id)
    WHERE status IN ('ACTIVE', 'PENDING');

-- Index pour le scheduler de renouvellement
CREATE INDEX IF NOT EXISTS idx_saas_momo_sub_next_charge
    ON saas_momo_subscriptions (next_charge_at)
    WHERE status = 'ACTIVE';

-- Index pour webhook operator reference
CREATE INDEX IF NOT EXISTS idx_saas_momo_sub_provider_ref
    ON saas_momo_subscriptions (provider_reference)
    WHERE provider_reference IS NOT NULL;

COMMENT ON TABLE saas_momo_subscriptions IS
    'Abonnements SaaS via Mobile Money (MTN MoMo tokenization). '
    'M-Pesa STK Push exclu : non rejouable automatiquement. '
    'Seul MTN MoMo supporte la tokenization pour récurrent.';

COMMENT ON COLUMN saas_momo_subscriptions.plan_price_minor IS
    'Montant figé en unités mineures ISO-4217 au moment de la souscription.';

COMMENT ON COLUMN saas_momo_subscriptions.provider_reference IS
    'Token de paiement MTN MoMo (API user token) pour prélèvements récurrents.';

COMMENT ON COLUMN saas_momo_subscriptions.next_charge_at IS
    'Prochaine échéance de prélèvement. Mis à jour après chaque paiement réussi.';