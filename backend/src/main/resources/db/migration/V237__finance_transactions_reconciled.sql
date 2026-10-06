-- V237__finance_transactions_reconciled.sql
-- ============================================================
-- Ajout du champ `reconciled` à finance_transactions (V236/V237).
-- Le endpoint mobile POST /finances/transactions/{id}/reconcile
-- marque une transaction comme rapprochée.
-- ============================================================

ALTER TABLE finance_transactions
    ADD COLUMN IF NOT EXISTS reconciled BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN finance_transactions.reconciled IS 'Transaction rapprochée (V237).';
