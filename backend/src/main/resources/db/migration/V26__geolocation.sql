-- ============================================================
-- V26 : Cartographie — coordonnées GPS + zone géographique
-- ============================================================

ALTER TABLE souls ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE souls ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE souls ADD COLUMN zone VARCHAR(120);

ALTER TABLE families ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE families ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE families ADD COLUMN zone VARCHAR(120);

-- Coordonnées de démonstration : répartition autour de Kinshasa
-- (latitude 0 à -5.5, longitude 15 à 19) selon un hachage stable de l'id.
UPDATE souls SET
    latitude  = -4.3217 + ((('x' || substr(md5(id::text), 1, 8))::bit(32)::int % 5500) / 1000.0),
    longitude = 15.3121 + ((('x' || substr(md5(id::text), 9, 8))::bit(32)::int % 3500) / 1000.0),
    zone      = CASE
        WHEN (('x' || substr(md5(id::text), 1, 8))::bit(32)::int % 4) = 0 THEN 'Kinshasa - Gombe'
        WHEN (('x' || substr(md5(id::text), 1, 8))::bit(32)::int % 4) = 1 THEN 'Kinshasa - Limete'
        WHEN (('x' || substr(md5(id::text), 1, 8))::bit(32)::int % 4) = 2 THEN 'Kinshasa - N''Djili'
        ELSE 'Kinshasa - Matete'
    END
WHERE deleted = false;

UPDATE families SET
    latitude  = -4.3217 + ((('x' || substr(md5(id::text), 1, 8))::bit(32)::int % 4000) / 1000.0),
    longitude = 15.3121 + ((('x' || substr(md5(id::text), 9, 8))::bit(32)::int % 3000) / 1000.0),
    zone      = CASE
        WHEN (('x' || substr(md5(id::text), 1, 8))::bit(32)::int % 3) = 0 THEN 'Kinshasa - Centre'
        WHEN (('x' || substr(md5(id::text), 1, 8))::bit(32)::int % 3) = 1 THEN 'Kinshasa - Est'
        ELSE 'Kinshasa - Ouest'
    END
WHERE deleted = false;

CREATE INDEX idx_souls_geo ON souls (latitude, longitude);
CREATE INDEX idx_families_geo ON families (latitude, longitude);
CREATE INDEX idx_souls_zone ON souls (zone);
