-- ============================================================
-- V60 — Rappel automatique des événements de département
--       Délai (en jours) avant l'événement pour notifier le
--       responsable. 0 = rappel désactivé pour ce département.
-- ============================================================

ALTER TABLE department_settings
    ADD COLUMN IF NOT EXISTS event_rappel_jours INTEGER NOT NULL DEFAULT 1;

COMMENT ON COLUMN department_settings.event_rappel_jours IS
    'Jours avant l''événement pour envoyer le rappel au responsable (0 = désactivé)';
