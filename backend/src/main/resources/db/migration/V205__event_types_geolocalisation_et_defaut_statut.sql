-- V205__event_types_geolocalisation_et_defaut_statut.sql
--
-- Ce que V203 (autre branche) ne fait pas. Trois points, tous deux derives
-- entites/schema du meme campagne.
--
-- ⚠ NUMEROTATION : `V203` et `V204` sont deja pris par la branche de l'agent A
-- (V203 = colonnes de perimetre + vocabulaire elargi + deplacement
-- `events` -> `event` ; V204 = unicite `families` par tenant, arbitrage D4).
-- Cette migration ne repete donc AUCUNE de ces colonnes ni aucun de ces index
-- ni ces CHECK : elle ne fait que ce qui reste, et qui n'est pas fait ailleurs.
--
-- ─────────────────────────────────────────────────────────────────────────────
-- 1. LES TYPES DE LA GEOLOCALISATION
-- ─────────────────────────────────────────────────────────────────────────────
-- V201 a depose la latitude/longitude de l'evenement et la preuve du pointage
-- en `NUMERIC`. Les entites les mappe en `Double` :
--
--   * `EventGeofence.evaluate(Double, Double, int, …)` calcule en double ;
--   * `GeoDistance.betweenMeters(…)` aussi ;
--   * `EventRegistrationResponse` expose des `BigDecimal` qu'il convertit deja.
--
-- Un `NUMERIC(9,6)` n'apporte qu'une echelle fixe a une mesure GPS, et il
-- diverge du type que le code manipule. C'est la meme raison qui a fait poser
-- `DOUBLE PRECISION` sur `souls` et `families` (V26) : la coordonnee d'un lieu
-- est une mesure, pas une quantite a echelle fixe.
--
-- Sans cette correction, `ddl-auto: validate` refuse de demarrer et
-- `ddl-auto: update` altere la colonne a chaque deploiement. Les bornes restent
-- garanties : les CHECK sont recrees a l'identique juste apres le changement de
-- type, qu'un `ALTER COLUMN … TYPE` invaliderait sinon.
--
-- Le meme ecart existait sur la PREUVE du pointage (`checkin_*`), qui est la
-- colonne que l'on conteste en cas de litige : elle doit etre aussi fiable que
-- le type ne le pretend pas.

-- ── Evenement : latitude / longitude ────────────────────────────────────────
-- La contrainte est droppee puis recreee : un changement de type invalide les
-- CHECK qui s'appuient sur la colonne.
ALTER TABLE event DROP CONSTRAINT IF EXISTS ck_event_geofence;

ALTER TABLE event
    ALTER COLUMN latitude  TYPE double precision USING latitude::double precision,
    ALTER COLUMN longitude TYPE double precision USING longitude::double precision;

ALTER TABLE event ADD CONSTRAINT ck_event_geofence CHECK (
    (latitude IS NULL AND longitude IS NULL)
    OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
);

-- ── Preuve du pointage : les quatre colonnes de V201 ────────────────────────
ALTER TABLE event_registrations DROP CONSTRAINT IF EXISTS ck_registration_checkin;

ALTER TABLE event_registrations
    ALTER COLUMN checkin_latitude   TYPE double precision USING checkin_latitude::double precision,
    ALTER COLUMN checkin_longitude  TYPE double precision USING checkin_longitude::double precision,
    ALTER COLUMN checkin_accuracy_m TYPE double precision USING checkin_accuracy_m::double precision,
    ALTER COLUMN checkin_distance_m TYPE double precision USING checkin_distance_m::double precision;

ALTER TABLE event_registrations ADD CONSTRAINT ck_registration_checkin CHECK (
    (checkin_latitude IS NULL AND checkin_longitude IS NULL
        AND checkin_accuracy_m IS NULL AND checkin_distance_m IS NULL)
    OR (checkin_latitude BETWEEN -90 AND 90
        AND checkin_longitude BETWEEN -180 AND 180
        AND checkin_distance_m >= 0)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. LE VOCABULAIRE : CHECK elargi a l'union des deux lexiques
-- ─────────────────────────────────────────────────────────────────────────────
-- ⚠ Cette section FAIT DOUBLE EMPLOI avec la V203 de l'agent A, qui elargit les
-- memes contraintes. Elle est volontairement **idempotente** (DROP IF EXISTS puis
-- ADD) et laisse la meme liste, donc :
--
--   * sur cette branche seule, elle est indispensable — sans elle, la table
--     vivante refuse encore le vocabulaire du produit, et rien ne pourrait etre
--     prouve sur ce module (le gate echouerait, non par defaut de code mais
--     parce que l'autre branche n'est pas la) ;
--   * apres fusion, la V203 l'a deja posee : la re-poser a l'identique est un
--     no-op. Aucune migration deja appliquee n'est modifiee (G-A.3).
--
-- L'arbitrage, pour le tracabilite : les DEUX lexiques restent acceptes.
-- L'entite ecrit toujours le vocabulaire du produit (dictionnaire
-- EVENT_TYPE / EVENT_STATUS, V42 et V62). Le lexique Church OS de V158 survit
-- parce que `POST /api/v1/church-events` accepte encore un corps libre, et
-- surtout parce que le remplacement serait PERTEUX : `REUNION` et `VISITE`
-- convergent tous deux vers `MEETING`, donc l'aller-retour n'est pas
-- reversible. On ne pretend donc pas a un seul lexique tant que la conversion
-- n'est pas lossy et subie par tous les ecrivains.

ALTER TABLE event DROP CONSTRAINT IF EXISTS event_type_check;
ALTER TABLE event ADD CONSTRAINT event_type_check CHECK (type IN (
    -- vocabulaire du produit (V42 / V62)
    'SORTIE', 'RETRAITE', 'EVANGELISATION', 'REUNION', 'VISITE',
    'CONFERENCE', 'FORMATION', 'ANNIVERSAIRE', 'CULTE', 'ETUDE_BIBLIQUE',
    'VEILLEE', 'PRIERE', 'AUTRE',
    -- vocabulaire Church OS (V158), tolere pendant la transition
    'SERVICE', 'MEETING', 'TRAINING', 'EVANGELISM', 'CONFERENCE', 'RETREAT',
    'WEDDING', 'BAPTISM', 'FUNERAL', 'OTHER'
));

ALTER TABLE event DROP CONSTRAINT IF EXISTS event_status_check;
ALTER TABLE event ADD CONSTRAINT event_status_check CHECK (status IN (
    'PLANIFIE', 'EN_COURS', 'TERMINE', 'ANNULE',
    'DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED', 'ARCHIVED'
));

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. LE DEFAUT DE `status`
-- ─────────────────────────────────────────────────────────────────────────────
-- V158 a pose `DEFAULT 'DRAFT'`, qui appartient au vocabulaire Church OS.
-- L'entite `Event` porte `DEFAULT 'PLANIFIE'`, qui appartient au vocabulaire du
-- produit (dictionnaire `EVENT_STATUS`, V42). Les DEUX sont acceptes par les
-- CHECK elargis de V203 — l'union etant assumee — mais un defaut de colonne
-- n'est pas un detail : il s'applique a toute insertion qui ne dit rien, donc
-- notamment aux requetes SQL brutes. Laisser `DRAFT` rendrait la base et le
-- code d'accord sur le defaut et desaccords sur l'ecriture, ce qui est
-- exactement la derive que ce chantier combat.
ALTER TABLE event ALTER COLUMN status SET DEFAULT 'PLANIFIE';

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. CE QUI N'EST PAS UNE COLONNE, ET POURQUOI
-- ─────────────────────────────────────────────────────────────────────────────
-- Aucune de ces trois colonnes n'est ajoutee, et c'est un arbitrage :
--
--  * `nb_inscrits` : un COMPTEUR, pas un etat. Il se calcule sur
--    `event_registrations`, dont la cle etrangere pointe vers `event` (verifie
--    sur la base reelle). Une colonne serait un second compteur, a
--    resynchroniser a chaque inscription, desinscription et pointage — donc une
--    seconde verite sur une question a laquelle la base repond deja.
--
--  * `is_public` : `visibility` existe, est contrainte, et fait autorite. Un
--    booleen parallel rendrait la question « cet evenement est-il public ? »
--    ambigue. Le DTO public `isPublic` est une LECTURE de `visibility`.
--
--  * `deleted` : la table vivante est en suppression logique horodatee
--    (`deleted_at`, V158). On sait quand un evenement a ete supprime, ce qu'un
--    booleen ne disait pas.
--
-- Ces trois points sont invariants de code, pas de schema : ils sont verifies
-- par `EventTableContractTest` (Testcontainers, PostgreSQL 16 migre de zero),
-- qui echoue si l'une de ces colonnes reapparait.

COMMENT ON COLUMN event.latitude IS
    'Latitude du lieu (WGS84, double precision). NULL = pas de geolocalisation '
    'configuree, et le serveur refuse alors tout pointage (fail-closed).';
COMMENT ON COLUMN event.longitude IS
    'Longitude du lieu (WGS84, double precision).';
COMMENT ON COLUMN event.status IS
    'Vocabulaire du produit (dictionnaire EVENT_STATUS, V42) UNION vocabulaire '
    'Church OS de V158 : les deux lexiques restent acceptes pendant la '
    'transition, parce que REUNION et VISITE convergent tous deux vers MEETING '
    'et qu''un remplacement serait perteux. L''entite ecrit toujours le '
    'vocabulaire du produit.';
COMMENT ON COLUMN event.visibility IS
    'Fait autorite sur la visibilite. Le DTO public `isPublic` en est une '
    'lecture (isPublic = visibility = ''PUBLIC''), pas une colonne.';
COMMENT ON COLUMN event_registrations.checkin_distance_m IS
    'Distance mesuree (m) entre le participant et le lieu de l''evenement. '
    'Double precision : c''est la mesure qu''une contestation arbitre.';
