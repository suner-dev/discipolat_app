-- V203__event_recoit_perimetre_et_vocabulaire_produit.sql
--
-- alignment de l'entite `Event` sur la table VIVANTE `event` (V158), sans
-- supprimer quoi que ce soit. Voir docs/architecture/schema-events-drift.md.
--
-- ─────────────────────────────────────────────────────────────────────────────
-- POURQUOI CETTE MIGRATION EXISTE (constat, verifie sur PostgreSQL 16 reel)
-- ─────────────────────────────────────────────────────────────────────────────
-- Sur une base migree de zero jusqu'a V202 :
--
--   table      | latitude | date_debut | deleted
--   -----------+----------+------------+---------
--   events     |   NON    |    (absente)| (absente)   <- la table n'EXISTE PAS
--   legacy_events|  NON    |    oui     |   oui
--   event      |   oui    |    (absente)| (absente)
--
-- `Event.java` mappe `events` : la relation n'existe pas, donc tout le module
-- `/api/v1/events` repond 500 sur une base migree. C'est le constat de
-- docs/architecture/schema-events-drift.md.
--
-- Cette migration ne renomme rien et ne supprime rien : elle COMPLETE la table
-- vivante pour qu'elle porte tout ce que le code mappe deja. C'est la seule
-- correction qui laisse le module entier operationnel.
--
-- ─────────────────────────────────────────────────────────────────────────────
-- ARBITRAGE 1 — LE PERIMETRE (famille / departement / unite / portee)
-- ─────────────────────────────────────────────────────────────────────────────
-- Le plan de reecriture prevoyait de RETIRER `famille_id` au motif que « le
-- controle d'acces se resout par le perimetre tenant ». C'est faux, et le
-- faux est demontre par le code : `WorkspaceScopeService` scope les donnees
-- selon le ROLE ACTIF (`FAISEUR`, `CHEF_DE_FAMILLE`, `RESPONSABLE`) via
-- `canAccessFamily` / `canAccessDepartment`, et `EventService.canAccessEvent` /
-- `canManageEvent` en dependent pour lister, lire, modifier et supprimer un
-- evenement. Le filtre de tenant (`@Filter tenantFilter`) ne remplace pas ce
-- scope : il est lui aussi porte par `tenant_id`.
--
-- Sans ces colonnes, tout membre d'un tenant verrait tout evenement du tenant :
-- une regression d'autorisation INTRA-tenant, invisible pour un test
-- multi-tenant qui verifie seulement l'isolation entre tenants.
--
-- Arbitrage retenu : la table vivante recoit le perimetre. `ON DELETE SET NULL`
-- pour que la suppression d'une famille ne supprime pas son historique
-- d'evenements.
--
-- ─────────────────────────────────────────────────────────────────────────────
-- ARBITRAGE 2 — LE VOCABULAIRE (type / status)
-- ─────────────────────────────────────────────────────────────────────────────
-- `V158` a cree la table `event` avec un vocabulaire anglais (`SERVICE`,
-- `MEETING`, `TRAINING`, ... / `DRAFT`, `PUBLISHED`, ...) que rien dans le
-- produit ne parle : pas l'entite, pas les contrôleurs, pas les clients.
--
-- Le vocabulaire du produit, lui, est deja arrete par le projet lui-meme :
--   * `V42` cree les dictionnaires `EVENT_TYPE` (13 codes) et `EVENT_STATUS`
--     (4 codes) ;
--   * `V62` a deja reporte `EVENT_TYPE` sur la contrainte CHECK de la table
--     morte `events`, precisement parce que la CHECK de `V3` (9 codes) refusait
--     CULTE / ETUDE_BIBLIQUE / VEILLEE / PRIERE (500 a la creation) ;
--   * `frontend/src/types/index.ts` type `TypeEvenement` sur ces 13 codes, et
--     `mobile/.../event_model.dart` decode exactement les memes 13.
--
-- Ce n'est donc pas une decision nouvelle : c'est la decision existante que la
-- table vivante n'a jamais recue. On la lui porte.
--
-- `is_public` n'est PAS ajoute : `event.visibility` existe deja et fait
-- l'autorite. Ajouter un booleen parallel recreerait deux verites sur la meme
-- question. `EventResponse.publicEvent` devient une lecture de `visibility`.
--
-- `famille_id` est bien ajoute, contrairement a ce que prevoyait le plan : voir
-- l'arbitrage 1 ci-dessus.

-- ── 1. Perimetre ───────────────────────────────────────────────────────────
ALTER TABLE event
    ADD COLUMN IF NOT EXISTS famille_id          UUID REFERENCES families(id)          ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS department_id       UUID REFERENCES departments(id)       ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS organization_unit_id UUID REFERENCES organization_nodes(id) ON DELETE SET NULL,
    -- Portee de la ressource (G1.8 §54) : la colonne existe sur les deux autres
    -- entites qui la declarent (InventoryItem, DepartmentDocument). La defaut
    -- `TENANT_GLOBAL` est celle de l'entite.
    ADD COLUMN IF NOT EXISTS resource_scope      VARCHAR(20) NOT NULL DEFAULT 'TENANT_GLOBAL';

-- L'index `idx_event_tenant` existe (V158). On ajoute le perimetre, qui est ce
-- que les listes filtrent reellement : par famille, par departement.
CREATE INDEX IF NOT EXISTS idx_event_famille
    ON event (famille_id) WHERE deleted_at IS NULL AND famille_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_event_department
    ON event (department_id) WHERE deleted_at IS NULL AND department_id IS NOT NULL;

-- ── 2. Vocabulaire : on retire les contraintes de la table morte ───────────
-- Les CHECK inline de `V158` s'appellent `event_type_check` / `event_status_check`
-- (nommage automatique de PostgreSQL, verifie sur la base reelle).
ALTER TABLE event DROP CONSTRAINT IF EXISTS event_type_check;
ALTER TABLE event DROP CONSTRAINT IF EXISTS event_status_check;

-- Backfill AVANT de poser les nouvelles contraintes : sinon le UPDATE viole la
-- contrainte qui tient encore.
--
-- Le dictionnaire est l'INVERSE EXACT de celui que `V158` a lui-meme applique
-- lors de la migration des donnees (lignes 64-80 de V158). On ne l'invente pas :
--   V158 : SORTIE->MEETING  RETRAITE->RETREAT  EVANGELISATION->EVANGELISM
--          REUNION->MEETING  VISITE->MEETING   CONFERENCE->CONFERENCE
--          FORMATION->TRAINING  ANNIVERSAIRE->OTHER  ELSE->OTHER
--   ici  : l'inverse. Ce qui n'a pas d'equivalent (SERVICE, WEDDING, BAPTISM,
--          FUNERAL, OTHER, et tout le ELSE de V158) tombe sur AUTRE.
--
-- Meme principe pour le statut, inverse de :
--   V158 : PLANIFIE->DRAFT  EN_COURS->PUBLISHED  TERMINE->COMPLETED
--          ANNULE->CANCELLED  ELSE->DRAFT
UPDATE event SET type = 'CULTE'         WHERE type = 'SERVICE';
UPDATE event SET type = 'REUNION'       WHERE type = 'MEETING';
UPDATE event SET type = 'FORMATION'     WHERE type = 'TRAINING';
UPDATE event SET type = 'EVANGELISATION' WHERE type = 'EVANGELISM';
UPDATE event SET type = 'RETRAITE'      WHERE type = 'RETREAT';
UPDATE event SET type = 'AUTRE'         WHERE type IN ('WEDDING', 'BAPTISM', 'FUNERAL', 'OTHER');

UPDATE event SET status = 'PLANIFIE'    WHERE status = 'DRAFT';
UPDATE event SET status = 'EN_COURS'    WHERE status = 'PUBLISHED';
UPDATE event SET status = 'TERMINE'     WHERE status = 'COMPLETED';
UPDATE event SET status = 'ANNULE'      WHERE status = 'CANCELLED';
-- ARCHIVED : V158 le produisait pour rien (son CASE ne le mentionne pas et son
-- ELSE est DRAFT) ; un evenement archive est un evenement clos -> TERMINE.
UPDATE event SET status = 'TERMINE'     WHERE status = 'ARCHIVED';

ALTER TABLE event ADD CONSTRAINT event_type_check CHECK (type IN (
    'SORTIE', 'RETRAITE', 'EVANGELISATION', 'REUNION', 'VISITE',
    'CONFERENCE', 'FORMATION', 'ANNIVERSAIRE',
    'CULTE', 'ETUDE_BIBLIQUE', 'VEILLEE', 'PRIERE', 'AUTRE'
));

ALTER TABLE event ADD CONSTRAINT event_status_check CHECK (status IN (
    'PLANIFIE', 'EN_COURS', 'TERMINE', 'ANNULE'
));

-- Le defaut de la colonne suit le vocabulaire du produit (V158 avait pose
-- 'DRAFT', que plus rien n'ecrit ni ne lit).
ALTER TABLE event ALTER COLUMN status SET DEFAULT 'PLANIFIE';

-- ── 3. Geolocalisation : NUMERIC(9,6) -> double precision ──────────────────
-- `EventGeofence.evaluate(Double, Double, int, ...)` travaille en double, et
-- `EventResponse` expose des BigDecimal qu'il convertit deja. `double precision`
-- est le type natif d'une coordonnee (c'est ce que V26 pose sur `souls` et
-- `families`) ; NUMERIC(9,6) n'apportait qu'une echelle fixe sans usage.
-- Les bornes restent garanties par `ck_event_geofence`, recreee ci-dessous car
-- elle ne peut pas survivre a un changement de type sans dropping.
ALTER TABLE event DROP CONSTRAINT IF EXISTS ck_event_geofence;
ALTER TABLE event
    ALTER COLUMN latitude  TYPE double precision USING latitude::double precision,
    ALTER COLUMN longitude TYPE double precision USING longitude::double precision;

ALTER TABLE event ADD CONSTRAINT ck_event_geofence CHECK (
    (latitude IS NULL AND longitude IS NULL)
    OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
);

-- ── 4. Preuve du pointage : meme correction de type, meme raison ──────────
-- `EventRegistration` mappe en `Double` les quatre colonnes de preuveposees par
-- V201 en `NUMERIC`. Meme derive que ci-dessus, meme famille : des coordonnees
-- et des distances, que `EventGeofence` et `GeoDistance` manipulent en double
-- et que `EventRegistrationResponse` expose en `BigDecimal`. Le `NUMERIC(9,6)`
-- d'une coordonnee GPS n'apporte rien : il n'y a pas de raison de faire porter
-- une echelle fixe a une mesure.
ALTER TABLE event_registrations
    DROP CONSTRAINT IF EXISTS ck_registration_checkin;

ALTER TABLE event_registrations
    ALTER COLUMN checkin_latitude    TYPE double precision USING checkin_latitude::double precision,
    ALTER COLUMN checkin_longitude   TYPE double precision USING checkin_longitude::double precision,
    ALTER COLUMN checkin_accuracy_m  TYPE double precision USING checkin_accuracy_m::double precision,
    ALTER COLUMN checkin_distance_m  TYPE double precision USING checkin_distance_m::double precision;

ALTER TABLE event_registrations ADD CONSTRAINT ck_registration_checkin CHECK (
    (checkin_latitude IS NULL AND checkin_longitude IS NULL
        AND checkin_accuracy_m IS NULL AND checkin_distance_m IS NULL)
    OR (checkin_latitude BETWEEN -90 AND 90
        AND checkin_longitude BETWEEN -180 AND 180
        AND checkin_distance_m >= 0)
);

-- ── 5. `nb_inscrits` n'est PAS une colonne ─────────────────────────────────
-- Le compteur se calcule sur `event_registrations`, dont la cle etrangere
-- pointe bien vers `event` (verifie : `event_registrations_event_id_fkey`).
-- Une colonne `nb_inscrits` sur `event` serait un second compteur, desynchronise
-- de la seule source. `EventService` compte donc a la lecture.

-- ── 6. Tracabilite ─────────────────────────────────────────────────────────
COMMENT ON COLUMN event.famille_id IS
    'Perimetre famille (isolation par role actif). NULL = evenement d''eglise, '
    'visible de tous dans le tenant. Arbitrage D1, cf. entete de la migration.';
COMMENT ON COLUMN event.department_id IS
    'Perimetre departement (espace Responsable).';
COMMENT ON COLUMN event.organization_unit_id IS
    'Unite de l''organigramme, pour le scoping G1.8.';
COMMENT ON COLUMN event.type IS
    'Vocabulaire du produit : dictionnaire EVENT_TYPE (V42), 13 codes. '
    'Contrainte reprise de V62, qui l''avait deposee sur la table morte.';
COMMENT ON COLUMN event.status IS
    'Vocabulaire du produit : dictionnaire EVENT_STATUS (V42), 4 codes.';
COMMENT ON COLUMN event.visibility IS
    'Fait autorite sur la visibilite. Le DTO public `isPublic` en est une '
    'lecture (isPublic = visibility = ''PUBLIC''), pas une colonne.';
