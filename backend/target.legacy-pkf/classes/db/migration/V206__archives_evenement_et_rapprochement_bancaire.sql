-- ============================================================
-- PORT Develop1 → main (lot « restauration des capacités absentes »).
-- Source: Develop1 V169__event_archive_and_bank_reconciliation.sql,
-- renumérotée car main occupe déjà V164/V165/V167/V169 avec d'autres
-- contenus (collision de versions Flyway = validate() refuse le démarrage).
--
-- §G3.4 / §G6.4 — tables des endpoints réels portés dans la même passe :
--   * event_archive                  → POST /church-events/{id}/archive + GET /archives
--   * finance_bank_statement_line    → /finances/reconciliation/*
-- En tests, Hibernate génère le schéma ; cette migration aligne la PRODUCTION.
-- ============================================================

-- ========== ARCHIVES D'ÉVÉNEMENT (§G3.4 / §G6.4) ==========
CREATE TABLE IF NOT EXISTS event_archive (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    event_id UUID NOT NULL,
    event_title VARCHAR(255) NOT NULL,
    event_start_at TIMESTAMPTZ NOT NULL,
    event_year INT NOT NULL,
    event_month INT NOT NULL,
    space_id UUID,
    version INT NOT NULL DEFAULT 1,
    snapshot_json JSONB NOT NULL,
    archived_by UUID,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_event_archive_tenant ON event_archive (tenant_id);
CREATE INDEX IF NOT EXISTS idx_event_archive_event ON event_archive (event_id);
CREATE INDEX IF NOT EXISTS idx_event_archive_year_month ON event_archive (tenant_id, event_year, event_month);

-- ========== RAPPROCHEMENT BANCAIRE (§G6.4) ==========
CREATE TABLE IF NOT EXISTS finance_bank_statement_line (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    date_transaction DATE NOT NULL,
    montant NUMERIC(14, 2) NOT NULL,
    description VARCHAR(500),
    reference VARCHAR(120),
    matched_transaction_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'UNMATCHED',
    external_key VARCHAR(160),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_fbsl_tenant_status ON finance_bank_statement_line (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_fbsl_transaction ON finance_bank_statement_line (matched_transaction_id);
