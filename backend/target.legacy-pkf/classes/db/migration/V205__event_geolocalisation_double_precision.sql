-- V205__event_geolocalisation_double_precision.sql
-- ============================================================
-- DÉRIVE DE TYPE corrigée par le gate de contrat EventTableContractTest
-- (reportée dans la lignée canonique, 2026-09-30).
--
-- V201/V202 ont déposé les coordonnées geografiques en NUMERIC(9,6) (et les
-- preuves de pointage en NUMERIC), alors que :
--   * Event.latitude / Event.longitude sont mappees `Double` ;
--   * EventRegistration.checkinLatitude / checkinLongitude /
--     checkinAccuracyM / checkinDistanceM sont mappees `Double` ;
--   * EventGeofence calcule la distance en `double`.
--
-- Conséquence : le profil `dev`/`prod` (spring.jpa.hibernate.ddl-auto:
-- validate) refuse de démarrer sur une base réellement migree — « wrong
-- column type: found numeric, expected float8 » — pendant que la suite H2
-- (`create-drop`, schéma fabriqué par Hibernate) ne voit jamais l'écart,
-- précisément l'angle mort que le gate dénonce. Le gate rejouait la chaîne
-- Flyway complète puis validait le type colonne par colonne ; il tombait
-- juste sur ces six colonnes.
--
-- Réparation : convertir la base vers `double precision`, le type que
-- déclarent l'entité et le service. C'est la base qui suit le contrat, pas
-- l'inverse — on n'affaiblit pas le gate pour le faire passer.
--
-- Patron : DO block idempotent (invariant si la colonne est déjà float8),
-- conversion explicite USING ::double precision.
-- ============================================================

DO $v205$
DECLARE
    r RECORD;
BEGIN
    -- (table, colonne) dont le type déclaré par l'entité est `Double`
    -- mais que V201/V202 avaient posées en NUMERIC.
    FOR r IN
        SELECT * FROM (VALUES
            ('event',               'latitude'),
            ('event',               'longitude'),
            ('event_registrations', 'checkin_latitude'),
            ('event_registrations', 'checkin_longitude'),
            ('event_registrations', 'checkin_accuracy_m'),
            ('event_registrations', 'checkin_distance_m')
        ) AS t(tbl, col)
    LOOP
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = r.tbl
              AND column_name = r.col
              AND data_type <> 'double precision'
        ) THEN
            EXECUTE format(
                'ALTER TABLE %I ALTER COLUMN %I TYPE double precision USING %I::double precision',
                r.tbl, r.col, r.col);
            RAISE NOTICE 'V205 : %.% converti en double precision', r.tbl, r.col;
        END IF;
    END LOOP;
END
$v205$;

COMMENT ON COLUMN event.latitude IS
    'Latitude WGS84 (double precision, alignée sur Event.latitude depuis V205). NULL = pas de géolocalisation.';
COMMENT ON COLUMN event.longitude IS
    'Longitude WGS84 (double precision, alignée sur Event.longitude depuis V205).';
