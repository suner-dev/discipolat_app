-- V236__finances_complement.sql
-- ============================================================
-- FINANCES COMPLÉMENT (comptes, dons, tontines)
-- Plan : docs/SPEC_BACKEND_SERVICES_MOBILES_V233.md
--
-- Le module finances existe déjà. Cette migration ajoute les tables
-- manquantes pour le contrat mobile (accounts, donations, tontines).
-- ============================================================

CREATE TABLE IF NOT EXISTS finance_accounts (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    account_number  VARCHAR(100),
    bank_name       VARCHAR(200),
    balance         NUMERIC(14,2) NOT NULL DEFAULT 0,
    devise          VARCHAR(3) NOT NULL DEFAULT 'XOF',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS finance_donations (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    donor_name      VARCHAR(200) NOT NULL,
    amount          NUMERIC(14,2) NOT NULL,
    devise          VARCHAR(3) NOT NULL DEFAULT 'XOF',
    donation_date   TIMESTAMPTZ NOT NULL DEFAULT now(),
    purpose         TEXT,
    is_anonymous    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS finance_tontines (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    amount_per_turn NUMERIC(14,2) NOT NULL,
    frequency       VARCHAR(20) NOT NULL DEFAULT 'MONTHLY'
        CHECK (frequency IN ('WEEKLY','MONTHLY','QUARTERLY','YEARLY')),
    start_date      TIMESTAMPTZ NOT NULL DEFAULT now(),
    end_date        TIMESTAMPTZ,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS finance_tontine_members (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    tontine_id      UUID NOT NULL REFERENCES finance_tontines(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    turn_order      INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS finance_tontine_payouts (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    tontine_id      UUID NOT NULL REFERENCES finance_tontines(id) ON DELETE CASCADE,
    member_id       UUID NOT NULL REFERENCES finance_tontine_members(id) ON DELETE CASCADE,
    amount          NUMERIC(14,2) NOT NULL,
    payout_date     TIMESTAMPTZ NOT NULL DEFAULT now(),
    turn_number     INT NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_fin_accounts_tenant ON finance_accounts (tenant_id);
CREATE INDEX IF NOT EXISTS idx_fin_donations_tenant ON finance_donations (tenant_id);
CREATE INDEX IF NOT EXISTS idx_fin_tontines_tenant ON finance_tontines (tenant_id);
CREATE INDEX IF NOT EXISTS idx_fin_tontine_mem_tenant ON finance_tontine_members (tenant_id);
CREATE INDEX IF NOT EXISTS idx_fin_tontine_pay_tenant ON finance_tontine_payouts (tenant_id);

COMMENT ON TABLE finance_accounts IS 'Comptes bancaires (V236).';
COMMENT ON TABLE finance_donations IS 'Dons (V236).';
COMMENT ON TABLE finance_tontines IS 'Tontines (V236).';
COMMENT ON TABLE finance_tontine_members IS 'Membres de tontine (V236).';
COMMENT ON TABLE finance_tontine_payouts IS 'Versements de tontine (V236).';
