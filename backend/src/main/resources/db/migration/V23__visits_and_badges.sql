-- ============================================================
-- V23 : Gestion des visites + Badges & Gamification
-- ============================================================

-- ------------------------------------------------------------
-- Visites : planification, compte rendu, objectifs, suivi
-- ------------------------------------------------------------
CREATE TABLE visits (
    id            UUID PRIMARY KEY,
    soul_id       UUID NOT NULL REFERENCES souls (id) ON DELETE CASCADE,
    visiteur_id   UUID NOT NULL,                  -- faiseur / responsable qui visite
    date_prevue   DATE NOT NULL,
    date_realisee DATE,
    statut        VARCHAR(20) NOT NULL DEFAULT 'PLANIFIEE',
    motif         VARCHAR(40),
    objectif      TEXT,
    compte_rendu  TEXT,
    photo_url     TEXT,
    present       BOOLEAN,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_visit_statut CHECK (statut IN ('PLANIFIEE', 'REALISEE', 'ANNULEE', 'REPORTEE'))
);

CREATE INDEX idx_visits_soul ON visits (soul_id);
CREATE INDEX idx_visits_visiteur ON visits (visiteur_id);
CREATE INDEX idx_visits_statut ON visits (statut);

-- ------------------------------------------------------------
-- Badges : catalogue + attributions par utilisateur
-- ------------------------------------------------------------
CREATE TABLE badges (
    id            UUID PRIMARY KEY,
    code          VARCHAR(50) NOT NULL UNIQUE,    -- ex: VISITE_10, PRESENCE_4SEMAINES
    nom           VARCHAR(80) NOT NULL,
    description   TEXT,
    icone         VARCHAR(30),
    niveau        VARCHAR(20) NOT NULL DEFAULT 'BRONZE',  -- BRONZE | ARGENT | OR | DIAMANT
    critere       VARCHAR(20) NOT NULL,           -- VISITES | PRESENCE | EVANGELISATION | INTERACTIONS | FIDELITE
    seuil         INTEGER NOT NULL,               -- valeur déclenchant le badge
    actif         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_badge_niveau CHECK (niveau IN ('BRONZE', 'ARGENT', 'OR', 'DIAMANT')),
    CONSTRAINT ck_badge_critere CHECK (critere IN ('VISITES', 'PRESENCE', 'EVANGELISATION', 'INTERACTIONS', 'FIDELITE'))
);

-- Attributions : un utilisateur peut gagner chaque badge une seule fois
CREATE TABLE user_badges (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    badge_id    UUID NOT NULL REFERENCES badges (id) ON DELETE CASCADE,
    earned_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_badge UNIQUE (user_id, badge_id)
);

CREATE INDEX idx_user_badges_user ON user_badges (user_id);

-- ------------------------------------------------------------
-- Badges de base (données de référence)
-- ------------------------------------------------------------
INSERT INTO badges (id, code, nom, description, icone, niveau, critere, seuil) VALUES
  (gen_random_uuid(), 'VISITE_1',      'Première visite',          'Effectuer sa première visite pastorale',       'DoorOpen',   'BRONZE',  'VISITES', 1),
  (gen_random_uuid(), 'VISITE_5',      'Visiteur régulier',        'Réaliser 5 visites de suivi',                  'Footprints', 'ARGENT',  'VISITES', 5),
  (gen_random_uuid(), 'VISITE_10',     'Ambassadeur',              'Réaliser 10 visites de suivi',                 'Award',      'OR',      'VISITES', 10),
  (gen_random_uuid(), 'CONTACT_20',    'Bâtisseur de relations',   'Enregistrer 20 interactions de suivi',         'Handshake',  'BRONZE',  'INTERACTIONS', 20),
  (gen_random_uuid(), 'CONTACT_50',    'Pasteur de proximité',     'Enregistrer 50 interactions de suivi',         'Heart',      'DIAMANT', 'INTERACTIONS', 50),
  (gen_random_uuid(), 'EVANGELISME_1','Graine semée',              'Faire passer une âme à l''étape Baptême',      'Sprout',     'ARGENT',  'EVANGELISATION', 1),
  (gen_random_uuid(), 'EVANGELISME_3','Moissonneur',               '3 âmes baptisées dans le pipeline',            'Church',     'OR',      'EVANGELISATION', 3),
  (gen_random_uuid(), 'FIDELITE_1',   'Serviteur fidèle',          '1 an de service continu',                      'Crown',      'DIAMANT', 'FIDELITE', 1);
