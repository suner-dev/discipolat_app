-- ============================================================
-- V57 — Parametrage du departement (seuils d'alertes) + equipes
--       temporaires liees a un evenement
-- ============================================================

-- Seuils configurables des alertes intelligentes (regles : absence
-- repetee, inactivite, tâches en retard). Une ligne par departement,
-- creee avec les valeurs par defaut au premier acces.
CREATE TABLE IF NOT EXISTS department_settings (
    department_id       UUID PRIMARY KEY REFERENCES departments(id),
    absence_seuil       INTEGER NOT NULL DEFAULT 2,
    absence_periode     INTEGER NOT NULL DEFAULT 3,
    inactivite_mois     INTEGER NOT NULL DEFAULT 3,
    tache_retard_alerte BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP
);

-- Rattachement optionnel d'une equipe (type EQUIPE_TEMPORAIRE) a un
-- evenement du departement (mission : equipes mobilisees par evenement).
ALTER TABLE department_teams ADD COLUMN IF NOT EXISTS event_id UUID REFERENCES events(id);

CREATE INDEX IF NOT EXISTS idx_department_teams_event ON department_teams(event_id);
