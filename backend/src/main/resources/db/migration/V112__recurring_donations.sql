-- RecurringDonation — dons récurrents (dîme mensuelle, offrande hebdomadaire…).
-- Table ajoutée par migration : elle reposait auparavant uniquement sur JPA,
-- ce qui cassait le démarrage en dev (ddl-auto=validate) et toute requête en prod.

CREATE TABLE IF NOT EXISTS recurring_donations (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID NOT NULL,
    user_id            UUID NOT NULL,
    operator           VARCHAR(30) NOT NULL,
    phone_number       VARCHAR(30),
    amount             NUMERIC(12,2) NOT NULL,
    currency           VARCHAR(10) NOT NULL DEFAULT 'XOF',
    purpose            VARCHAR(30) NOT NULL DEFAULT 'DIME',
    frequency          VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    next_donation_date DATE,
    total_donated      NUMERIC(12,2) NOT NULL DEFAULT 0,
    donation_count     INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_recurring_donations_tenant
    ON recurring_donations (tenant_id);
CREATE INDEX IF NOT EXISTS idx_recurring_donations_user
    ON recurring_donations (tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_recurring_donations_next
    ON recurring_donations (tenant_id, active, next_donation_date);
