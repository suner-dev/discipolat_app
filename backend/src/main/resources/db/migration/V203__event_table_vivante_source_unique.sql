-- V203__event_table_vivante_source_unique.sql
-- ============================================================
-- ARBITRAGE D1 (orchestrateur, 2026-09-30) : « aligner le code sur la table
-- vivante `event` ». L'entité legacy Event — donc /api/v1/events, l'étape
-- FIRST_EVENT du wizard, la prédiction de charge et les clients mobiles
-- recâblés sur ce contrat — est repointée sur `event` (V158, enrichie par
-- V200/V201/V202). `events` (rétablie par V194) devient redondante : ses
-- lignes sont déplacées vers `event`, la table n'est PAS supprimée.
--
-- Cette migration ajoute à `event` ce que l'inventaire du « plan retenu »
-- (docs/architecture/schema-events-drift.md) ne couvrait pas, sans quoi le
-- port serait un relâchement d'accès :
--   1. COLONNES D'ISOLATION : famille_id, department_id, resource_scope,
--      organization_unit_id. Sans elles, canAccessEvent/canManageEvent de
--      EventService et les six requêtes dérivées d'EventRepository ne
--      peuvent plus filtrer par espace métier : un CHEF_DE_FAMILLE verrait
--      tous les événements du tenant. `famille_id` est ici un amendement
--      motivé à l'inventaire B (« retirer ») : le contrat §3 (figé, R2)
--      l'expose dans CreateEventRequest/EventResponse, et le retirer
--      casserait l'isolation, pas seulement l'affichage.
--   2. CHAMPs DU MODÈLE VIVANT absorbés pour qu'UNE SEULE entité mappe
--      `event` : timezone, is_recurring, recurrence_rule, created_by
--      existent déjà en base (V158) — la fusion Event/ChurchEvent côté Java
--      ne demande pas de colonne nouvelle.
--   3. VOCABULAIRE : les CHECK de `event` (anglais, V158) sont ÉLARGIS à
--      l'union FR (contrat §3, V3/V62/dictionnaire V42) ∪ EN (Church OS),
--      et non remplacés. Verrouiller l'anglais imposerait une conversion
--      AVEC PERTE : REUNION/SORTIE/VISITE → MEETING n'est pas inversible
--      (V158 l'a déjà perdu une fois). Aucun des deux vocabulaires n'est
--      « une deuxième vérité » : ce sont les deux lexiques du même fait.
--   4. DÉPLACEMENT des lignes `events` → `event` (mêmes id, mapping sans
--      perte : nb_inscrits n'est pas déplacé, c'est un compteur calculé sur
--      event_registrations ; is_public devient visibility). Fail-closed :
--      si une ligne events a un tenant_id absent de tenants, la FK bloque
--      et la migration échoue avec message explicite — pas de fusion
--      silencieuse de données divergentes.
--
-- Patron d'idempotence : ALTER TABLE IF EXISTS + ADD COLUMN IF NOT EXISTS +
-- DO blocks sur pg_constraint (V193/V194). Aucune migration appliquée
-- n'est modifiée (G-A.3).
-- ============================================================

-- 1. Colonnes d'isolation + suppression logique alignée sur le modèle vivant
ALTER TABLE event
    ADD COLUMN IF NOT EXISTS famille_id          UUID,
    ADD COLUMN IF NOT EXISTS department_id       UUID,
    ADD COLUMN IF NOT EXISTS resource_scope      VARCHAR(20) NOT NULL DEFAULT 'TENANT_GLOBAL',
    ADD COLUMN IF NOT EXISTS organization_unit_id UUID;

DO $v203$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conrelid = 'event'::regclass AND conname = 'fk_event_famille') THEN
        ALTER TABLE event ADD CONSTRAINT fk_event_famille
            FOREIGN KEY (famille_id) REFERENCES families(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conrelid = 'event'::regclass AND conname = 'fk_event_department') THEN
        ALTER TABLE event ADD CONSTRAINT fk_event_department
            FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL;
    END IF;
END
$v203$;

CREATE INDEX IF NOT EXISTS idx_event_famille    ON event (famille_id) WHERE famille_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_event_department ON event (department_id) WHERE department_id IS NOT NULL;

-- 2. Vocabulaires : union FR (contrat §3) ∪ EN (V158), par colonne et par sens
DO $v203$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint
               WHERE conrelid = 'event'::regclass AND conname = 'event_status_check') THEN
        ALTER TABLE event DROP CONSTRAINT event_status_check;
    END IF;
    ALTER TABLE event ADD CONSTRAINT event_status_check CHECK (status IN (
        'DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED', 'ARCHIVED',
        'PLANIFIE', 'EN_COURS', 'TERMINE', 'ANNULE'
    ));

    IF EXISTS (SELECT 1 FROM pg_constraint
               WHERE conrelid = 'event'::regclass AND conname = 'event_type_check') THEN
        ALTER TABLE event DROP CONSTRAINT event_type_check;
    END IF;
    ALTER TABLE event ADD CONSTRAINT event_type_check CHECK (type IN (
        'SERVICE', 'MEETING', 'TRAINING', 'EVANGELISM', 'CONFERENCE',
        'RETREAT', 'WEDDING', 'BAPTISM', 'FUNERAL', 'OTHER',
        'SORTIE', 'RETRAITE', 'EVANGELISATION', 'REUNION', 'VISITE',
        'FORMATION', 'ANNIVERSAIRE', 'CULTE', 'ETUDE_BIBLIQUE', 'VEILLEE',
        'PRIERE', 'AUTRE'
    ));
END
$v203$;

-- 3. Déplacement events -> event (mêmes id ; mapping conforme aux décisions
--    V202 : is_public -> visibility, deleted -> deleted_at, vocabulaires
--    conservés tels quels — les CHECK élargis les acceptent).
DO $v203$
DECLARE
    n bigint;
BEGIN
    IF to_regclass('public.events') IS NOT NULL THEN
        INSERT INTO event (id, tenant_id, title, description, lieu, type, status,
                           start_at, end_at, organizer_id, created_at, updated_at,
                           deleted_at, visibility, famille_id, department_id,
                           resource_scope, organization_unit_id,
                           image_url, tags, requires_registration, has_checkin,
                           stream_id, limite_places, compte_rendu,
                           latitude, longitude, geofence_radius_m)
        SELECT e.id, e.tenant_id, e.titre, e.description, e.lieu, e.type_evenement, e.statut,
               e.date_debut, e.date_fin, e.organisateur_id, e.created_at, e.updated_at,
               CASE WHEN e.deleted THEN COALESCE(e.deleted_at, NOW()) ELSE NULL END,
               CASE WHEN e.is_public THEN 'PUBLIC' ELSE 'CHURCH' END,
               e.famille_id, e.department_id,
               COALESCE(e.resource_scope, 'TENANT_GLOBAL'), e.organization_unit_id,
               e.image_url, COALESCE(e.tags, '{}'),
               COALESCE(e.requires_registration, TRUE), COALESCE(e.has_checkin, TRUE),
               e.stream_id, e.limite_places, e.compte_rendu,
               e.latitude, e.longitude, COALESCE(e.geofence_radius_m, 200)
        FROM events e
        WHERE NOT EXISTS (SELECT 1 FROM event ev WHERE ev.id = e.id)
          AND EXISTS (SELECT 1 FROM tenants t WHERE t.id = e.tenant_id);

        GET DIAGNOSTICS n = ROW_COUNT;
        RAISE NOTICE 'V203 : % ligne(s) de « events » déplacée(s) vers « event »', n;

        -- Solde de contrôle : toute ligne events non déplacée est une ligne
        -- orpheline (tenant absent) — fusion silencieuse refusée.
        IF EXISTS (SELECT 1 FROM events e
                   WHERE NOT EXISTS (SELECT 1 FROM event ev WHERE ev.id = e.id)) THEN
            RAISE EXCEPTION
                'V203 FAIL-CLOSED : des lignes de « events » n''ont pu etre deplacees vers « event » (tenant inexistant ou conflit). Resoudre manuellement, puis rejouer.';
        END IF;
    END IF;
END
$v203$;

COMMENT ON TABLE event IS
    'Source unique des évenements (arbitrage D1, V203) : modèle vivant Church OS (V158)
     + colonnes d''isolation legacy (famille_id, department_id, resource_scope,
     organization_unit_id) + options contrat (V200/V202). Vocabulaires FR ∪ EN acceptés
     par les CHECK. La table « events » (V194) est héritée, plus mappée.';
COMMENT ON COLUMN event.famille_id IS
    'Isolation par espace métier (chef de famille). NULL = événement d''église, visible de tous dans le tenant.';
COMMENT ON COLUMN event.department_id IS
    'Isolation par département. NULL = pas de rattachement départemental.';
COMMENT ON COLUMN event.resource_scope IS
    'Portée ResourceScope du contrat plateforme : TENANT_GLOBAL par défaut.';
COMMENT ON COLUMN event.visibility IS
    'Autorité de visibilité (PRIVATE/TEAM/CHURCH/PUBLIC). is_public du DTO en est la lecture (== ''PUBLIC'').';
DO $v203$
BEGIN
    IF to_regclass('public.events') IS NOT NULL THEN
        COMMENT ON TABLE events IS
            'HÉRITÉE (V194, legs de V158) — vidée par V203, plus aucune entité ne la mappe. Ne plus écrire ici.';
    END IF;
END
$v203$;
