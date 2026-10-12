-- V252__saas_momo_subscription_retry_fields.sql
-- ============================================================
-- TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T6.3 (partie 3/3)
-- Champs de retentative pour les abonnements MoMo récurrents
-- ============================================================

ALTER TABLE saas_momo_subscriptions
    ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_retry_at TIMESTAMPTZ;

COMMENT ON COLUMN saas_momo_subscriptions.retry_count IS
    'Nombre de tentatives de prélèvement échouées consécutives.';

COMMENT ON COLUMN saas_momo_subscriptions.last_retry_at IS
    'Date de la dernière tentative de prélèvement (échec ou succès).';