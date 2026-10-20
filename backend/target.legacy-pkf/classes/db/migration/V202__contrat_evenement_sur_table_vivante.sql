-- V202__contrat_evenement_sur_table_vivante.sql
--
-- Reçoit sur la table VIVANTE `event` (créée par V158) les colonnes du contrat
-- que les clients exposent déjà, et que seule la table morte `events`
-- (renommée `legacy_events`) portait jusqu'ici.
--
-- Arbitrages explicites, voir docs/architecture/schema-events-drift.md :
--
--  * `is_public` N'EST PAS ajouté : `event.visibility` existe déjà et est
--    contraint à ('PRIVATE','TEAM','CHURCH','PUBLIC'). Ajouter un booléen
--    parallèle créerait DEUX vérités sur la même question — c'est
--   .visibility = 'PUBLIC' qui fait la autorité, pas une colonne de plus.
--    Le DTO `isPublic` devient une lecture de `visibility`.
--
--  * `famille_id` N'EST PAS ajouté : `event` est un modèle paroissial et
--    multi-tenant (V158 a délibérément déplacé les données de l'ancien modèle
--    familial vers celui-ci). Un événement rattaché à une famille est un autre
--    objet métier ; on ne le ressuscite pas sous forme de colonne.
--
--  * `nb_inscrits` N'EST PAS ajouté : c'est un compteur, il se calcule sur
--    `event_registrations` (dont la FK pointe bien vers `event`).
--
--  * `statut` n'est pas dupliqué : `event.status` existe et impose son
--    vocabulaire. L'alignement se joue à la frontière du DTO, pas en base.
--
-- `stream_id` reste un UUID sans clé étrangère : `live_streams.id` est
-- BIGSERIAL (dérive V133, préexistante et documentée).

ALTER TABLE event
    ADD COLUMN IF NOT EXISTS image_url             VARCHAR(500),
    ADD COLUMN IF NOT EXISTS tags                  TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS requires_registration BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS has_checkin           BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS stream_id             UUID,
    ADD COLUMN IF NOT EXISTS limite_places         INTEGER,
    ADD COLUMN IF NOT EXISTS compte_rendu          TEXT,
    -- Moteur de géolocalisation (V201 avait visé la table morte)
    ADD COLUMN IF NOT EXISTS latitude              NUMERIC(9, 6),
    ADD COLUMN IF NOT EXISTS longitude             NUMERIC(9, 6),
    ADD COLUMN IF NOT EXISTS geofence_radius_m     INTEGER NOT NULL DEFAULT 200;

-- Contraintes de cohérence : un rayon seul n'a pas de sens, des coordonnées
-- hors bornes seraient acceptées en silence, et une capacité négative
-- produirait un formulaire qui n'explique jamais pourquoi l'inscription échoue.
ALTER TABLE event DROP CONSTRAINT IF EXISTS ck_event_geofence;
ALTER TABLE event ADD CONSTRAINT ck_event_geofence CHECK (
    (latitude IS NULL AND longitude IS NULL)
    OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
);

ALTER TABLE event DROP CONSTRAINT IF EXISTS ck_event_geofence_radius;
ALTER TABLE event ADD CONSTRAINT ck_event_geofence_radius
    CHECK (geofence_radius_m BETWEEN 10 AND 5000);

ALTER TABLE event DROP CONSTRAINT IF EXISTS ck_event_limite_places;
ALTER TABLE event ADD CONSTRAINT ck_event_limite_places
    CHECK (limite_places IS NULL OR limite_places > 0);

COMMENT ON COLUMN event.tags IS
    'Étiquettes (recherche par tag, index GIN).';
COMMENT ON COLUMN event.limite_places IS
    'Capacité maximale. NULL = pas de limite. Compteur dérivé : nb_inscrits '
    'vient de event_registrations, pas d''une colonne.';
COMMENT ON COLUMN event.requires_registration IS
    'Défaut TRUE : le comportement d''avant (inscription libre).';
COMMENT ON COLUMN event.has_checkin IS
    'Défaut TRUE : le comportement d''avant (pointage possible partout).';
COMMENT ON COLUMN event.latitude IS
    'Latitude du lieu (WGS84). NULL = pas de géolocalisation configurée, et le '
    'serveur refuse alors tout pointage (fail-closed).';

-- Recherche par étiquette : c'est le seul index qui ait du sens sans `pg_trgm`.
CREATE INDEX IF NOT EXISTS idx_event_tags ON event USING GIN (tags);

-- Événements publics, pour l'affichage public. `visibility` est l'autorité.
CREATE INDEX IF NOT EXISTS idx_event_public
    ON event (tenant_id, start_at DESC)
    WHERE visibility = 'PUBLIC' AND deleted_at IS NULL;

-- Recherche des événements pointables dans une zone.
CREATE INDEX IF NOT EXISTS idx_event_geofence
    ON event (latitude, longitude)
    WHERE latitude IS NOT NULL AND deleted_at IS NULL;
