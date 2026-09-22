-- ============================================================
-- V60 — Rappel automatique des evenements de departement
--       Delai (en jours) avant l'evenement pour notifier le
--       responsable. 0 = rappel desactive pour ce departement.
-- ============================================================

ALTER TABLE department_settings
    ADD COLUMN IF NOT EXISTS event_rappel_jours INTEGER NOT NULL DEFAULT 1;

COMMENT ON COLUMN department_settings.event_rappel_jours IS
    'Jours avant l''evenement pour envoyer le rappel au responsable (0 = desactive)';
