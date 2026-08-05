-- ============================================================
-- V22 : Suivi d'évangélisation (pipeline) + Système d'objectifs
-- ============================================================

-- ------------------------------------------------------------
-- Pipeline d'évangélisation : une ligne par âme (étape courante)
-- ------------------------------------------------------------
CREATE TABLE evangelism_track (
    id            UUID PRIMARY KEY,
    soul_id       UUID NOT NULL UNIQUE REFERENCES souls (id) ON DELETE CASCADE,
    etape         VARCHAR(40) NOT NULL,
    date_etape    DATE NOT NULL,
    note          TEXT,
    cree_par      UUID,
    cree_le       TIMESTAMP NOT NULL DEFAULT now(),
    maj_le        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_evangelism_etape CHECK (etape IN (
        'NOUVELLE_AME', 'PREMIER_CONTACT', 'VISITE', 'INVITATION',
        'PREMIER_CULTE', 'SUIVI', 'BAPTEME', 'DEPARTEMENT',
        'FAMILLE', 'DISCIPOLAT', 'LEADER'
    ))
);

-- Historique des franchissements d'étapes (chaque passage est tracé)
CREATE TABLE evangelism_stage_history (
    id          UUID PRIMARY KEY,
    track_id    UUID NOT NULL REFERENCES evangelism_track (id) ON DELETE CASCADE,
    etape       VARCHAR(40) NOT NULL,
    cree_par    UUID,
    cree_le     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_evangelism_track_soul ON evangelism_track (soul_id);
CREATE INDEX idx_evangelism_track_etape ON evangelism_track (etape);
CREATE INDEX idx_evangelism_history_track ON evangelism_stage_history (track_id);

-- ------------------------------------------------------------
-- Objectifs par rôle : cible mesurée automatiquement
-- ------------------------------------------------------------
CREATE TABLE objectives (
    id          UUID PRIMARY KEY,
    role        VARCHAR(40) NOT NULL,             -- PASTEUR | RESPONSABLE | CHEF_DE_FAMILLE | FAISEUR
    type        VARCHAR(40) NOT NULL,             -- VISITES | NOUVELLES_AMES | DISCIPLES_ACTIFS | EVANGELISATION | SUIVIS | PRESENCE
    cible       INTEGER NOT NULL,                 -- valeur à atteindre
    periode     VARCHAR(20) NOT NULL DEFAULT 'MENSUEL',  -- MENSUEL | TRIMESTRIEL | ANNUEL
    actif       BOOLEAN NOT NULL DEFAULT TRUE,
    cree_par    UUID,
    cree_le     TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_objective_role CHECK (role IN ('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')),
    CONSTRAINT ck_objective_type CHECK (type IN ('VISITES', 'NOUVELLES_AMES', 'DISCIPLES_ACTIFS', 'EVANGELISATION', 'SUIVIS', 'PRESENCE')),
    CONSTRAINT ck_objective_periode CHECK (periode IN ('MENSUEL', 'TRIMESTRIEL', 'ANNUEL'))
);

CREATE INDEX idx_objectives_role ON objectives (role);
CREATE INDEX idx_objectives_actif ON objectives (actif);

-- ------------------------------------------------------------
-- Objectifs par défaut (évitent une page vide dès l'installation)
-- ------------------------------------------------------------
INSERT INTO objectives (id, role, type, cible, periode) VALUES
  (gen_random_uuid(), 'PASTEUR', 'NOUVELLES_AMES', 5, 'MENSUEL'),
  (gen_random_uuid(), 'PASTEUR', 'DISCIPLES_ACTIFS', 50, 'MENSUEL'),
  (gen_random_uuid(), 'RESPONSABLE', 'VISITES', 10, 'MENSUEL'),
  (gen_random_uuid(), 'RESPONSABLE', 'PRESENCE', 80, 'MENSUEL'),
  (gen_random_uuid(), 'CHEF_DE_FAMILLE', 'SUIVIS', 8, 'MENSUEL'),
  (gen_random_uuid(), 'CHEF_DE_FAMILLE', 'EVANGELISATION', 1, 'MENSUEL'),
  (gen_random_uuid(), 'FAISEUR', 'VISITES', 6, 'MENSUEL'),
  (gen_random_uuid(), 'FAISEUR', 'SUIVIS', 6, 'MENSUEL');
