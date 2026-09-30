-- V201__geolocalisation_evenement.sql
--
-- Moteur de géolocalisation : l'événement porte les coordonnées de son lieu et
-- un rayon d'effet ; le pointage enregistre la position MESURÉE, sa précision et
-- la distance calculée, ce qui rend le pointage contestable (audit).
--
-- Décision de conception, et pourquoi elle est serveur :
-- une position envoyée par le client est falsifiable. Un périmètre vérifié dans
-- l'application mobile ne protège rien. Le serveur possède donc les coordonnées,
-- le rayon et le calcul (Haversine) ; le client envoie sa position et n'affiche
-- qu'une pré-visualisation. La vérification client reste une simplecription de
-- confort (éviter un aller-retour inutile), jamais une autorisation.

-- ── 1. Lieu et périmètre de l'événement ─────────────────────���───────────
ALTER TABLE IF EXISTS events
    ADD COLUMN IF NOT EXISTS latitude          NUMERIC(9, 6),
    ADD COLUMN IF NOT EXISTS longitude         NUMERIC(9, 6),
    ADD COLUMN IF NOT EXISTS geofence_radius_m INTEGER NOT NULL DEFAULT 200;

-- Contrainte de cohérence : un rayon seul n'a pas de sens, et des coordonnées
-- hors bornes seraient acceptées silencieusement.
ALTER TABLE IF EXISTS events
    DROP CONSTRAINT IF EXISTS ck_events_geofence;

ALTER TABLE IF EXISTS events
    ADD CONSTRAINT ck_events_geofence CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
    );

ALTER TABLE IF EXISTS events
    ADD CONSTRAINT ck_events_geofence_radius CHECK (geofence_radius_m BETWEEN 10 AND 5000);

-- `COMMENT ON` n'a pas de variante conditionnelle : sur une table absente il
-- echoue. D'ou le bloc, meme logique que pour les index.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'public' AND table_name = 'events') THEN
        COMMENT ON COLUMN events.latitude IS
            'Latitude du lieu (degres, WGS84). NULL = pas de geolocalisation configuree.';
        COMMENT ON COLUMN events.geofence_radius_m IS
            'Rayon d''effet en metres (10 a 5000). Applique par le serveur au pointage.';
    END IF;
END $$;

-- Recherche des événements pointables dans une zone (usage pastoral).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'public' AND table_name = 'events') THEN
        CREATE INDEX IF NOT EXISTS idx_events_geofence
            ON events (latitude, longitude)
            WHERE latitude IS NOT NULL AND deleted = false;
    ELSE
        RAISE NOTICE 'V201: table `events` absente — géolocalisation non appliquée. '
                     'Dérive entité/schema ouverte (cf. V200).';
    END IF;
END $$;

-- ── 2. Preuve du pointage ───────────────────────────────────────────────
ALTER TABLE event_registrations
    ADD COLUMN IF NOT EXISTS checkin_latitude    NUMERIC(9, 6),
    ADD COLUMN IF NOT EXISTS checkin_longitude   NUMERIC(9, 6),
    ADD COLUMN IF NOT EXISTS checkin_accuracy_m  NUMERIC(7, 1),
    ADD COLUMN IF NOT EXISTS checkin_distance_m  NUMERIC(9, 1);

COMMENT ON COLUMN event_registrations.checkin_distance_m IS
    'Distance mesuree (m) entre le participant et le lieu de l''evenement.';
COMMENT ON COLUMN event_registrations.checkin_accuracy_m IS
    'Precision GPS declaree par l''appareil (m). Sert a trancher une contestation.';

ALTER TABLE event_registrations
    DROP CONSTRAINT IF EXISTS ck_registration_checkin;

ALTER TABLE event_registrations
    ADD CONSTRAINT ck_registration_checkin CHECK (
        (checkin_latitude IS NULL AND checkin_longitude IS NULL
            AND checkin_accuracy_m IS NULL AND checkin_distance_m IS NULL)
        OR (checkin_latitude BETWEEN -90 AND 90
            AND checkin_longitude BETWEEN -180 AND 180
            AND checkin_distance_m >= 0)
    );
