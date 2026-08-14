-- ============================================================
-- V57 — Paramétrage du département (seuils d'alertes) + équipes
--       temporaires liées à un événement
-- ============================================================

-- Seuils configurables des alertes intelligentes (règles : absence
-- répétée, inactivité, tâches en retard). Une ligne par département,
-- créée avec les valeurs par défaut au premier accès.
CREATE TABLE IF NOT EXISTS department_settings (
    department_id       UUID PRIMARY KEY REFERENCES departments(id),
    absence_seuil       INTEGER NOT NULL DEFAULT 2,
    absence_periode     INTEGER NOT NULL DEFAULT 3,
    inactivite_mois     INTEGER NOT NULL DEFAULT 3,
    tache_retard_alerte BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP
);

-- Rattachement optionnel d'une équipe (type EQUIPE_TEMPORAIRE) à un
-- événement du département (mission : équipes mobilisées par événement).
ALTER TABLE department_teams ADD COLUMN IF NOT EXISTS event_id UUID REFERENCES events(id);

CREATE INDEX IF NOT EXISTS idx_department_teams_event ON department_teams(event_id);
