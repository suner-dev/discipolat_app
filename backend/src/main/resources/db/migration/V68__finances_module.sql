-- V68 : Outil metier FINANCES (module activable)
-- ============================================================
-- Gestion des recettes, depenses, transactions et budget de
-- l'eglise. Le module est activable/desactivable comme les autres
-- (ModuleGateFilter : /api/v1/finances → FINANCES) et le menu
-- est pilote par la configuration plateforme.
-- ============================================================

CREATE TABLE IF NOT EXISTS finance_transactions (
    id                UUID PRIMARY KEY,
    type              VARCHAR(20) NOT NULL CHECK (type IN ('RECETTE', 'DEPENSE')),
    categorie         VARCHAR(50) NOT NULL,
    montant           NUMERIC(14, 2) NOT NULL CHECK (montant >= 0),
    description       VARCHAR(500),
    date_transaction  DATE NOT NULL,
    created_by        UUID,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP,
    deleted           BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_finance_transactions_date ON finance_transactions(date_transaction);
CREATE INDEX IF NOT EXISTS idx_finance_transactions_type ON finance_transactions(type);

CREATE TABLE IF NOT EXISTS finance_budgets (
    id          UUID PRIMARY KEY,
    categorie   VARCHAR(50) NOT NULL,
    annee       INT NOT NULL CHECK (annee >= 2000 AND annee <= 2100),
    montant     NUMERIC(14, 2) NOT NULL CHECK (montant >= 0),
    created_by  UUID,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (categorie, annee)
);

COMMENT ON TABLE finance_transactions IS 'Transactions financieres de l''eglise (recettes / depenses).';
COMMENT ON TABLE finance_budgets IS 'Budget annuel par categorie de depense.';

-- Module activable + menu (Administration plateforme).
INSERT INTO platform_modules (key, label, description, icon, section, enabled, ordre)
VALUES ('FINANCES', 'Finances',
        'Gestion des recettes, depenses et budget de l''eglise',
        'Wallet', 'Engagement & outils', TRUE, 22)
ON CONFLICT (key) DO NOTHING;

INSERT INTO menu_entries (key, label, href, icon, section, ordre, roles, module_key, enabled)
VALUES ('finances', 'Finances', '/finances',
        'Wallet', 'Engagement & outils', 9, '["ADMIN","PASTEUR"]'::jsonb, 'FINANCES', TRUE)
ON CONFLICT (key) DO NOTHING;
