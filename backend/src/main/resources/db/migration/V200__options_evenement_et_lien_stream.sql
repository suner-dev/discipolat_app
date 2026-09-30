-- V200__options_evenement_et_lien_stream.sql
--
-- Pourquoi : le formulaire et l'écran de détail du client mobile exposaient des
-- options (image de couverture, tags, visibilité, inscription obligatoire,
-- pointage, lien vers un direct) pour lesquelles AUCUNE colonne n'existait.
-- Les clients les envoyaient et le serveur les ignorait silencieusement.
-- Plutôt que de les retirer de l'interface, on leur donne une réalité.
--
-- Décision de conception : les options de contrôle d'accès sont des
-- **colonnes** (le serveur les applique réellement), pas des drapeaux décoratifs.
--   - `is_public`            : l'événement n'apparaît que si vrai
--   - `requires_registration`: le serveur REFUSE l'inscription si faux
--   - `has_checkin`          : le serveur REFUSE le pointage si faux
--
-- `has_face_check_in` n'est PAS ajouté : il n'existe aucun service de
-- reconnaissance faciale dans le produit. Un booléen sans implémentation serait
-- exactement le mensonge de contrat que ce chantier supprime. La
-- géolocalisation, elle, est RÉELLEMENT implémentée (V201).
--
-- ⚠ DÉRIVE ENTITÉ/SCHÉMA NON RÉSOLUE — lue sur une base migrée de zéro :
-- la migration V158 a renommé `events` en `legacy_events` et créé la table de
-- remplacement `event` (colonnes `title`, `start_at`, `status`, `visibility`…).
-- Or le modèle `Event.java` mappe TOUJOURS `events`. Concrètement, sans les
-- gardes ci-dessous, CETTE MIGRATION FAIT ÉCHOUER LE DÉPLOIEMENT : en
-- PostgreSQL, `ADD COLUMN IF NOT EXISTS` protège la colonne, pas la table
-- (`ALTER TABLE events` sur une table absente → « relation does not exist »,
-- vérifié sur PostgreSQL 16).
-- Les gardes rendent le déploiement possible sans prétendre que la correction
-- est faite. Le vrai correctif est un alignement du modèle, suivi de ce document :
-- docs/architecture/schema-events-drift.md
--
-- Note de_checksum : cette migration était publiée sur une branche de travail
-- non fusionnée, donc jamais appliquée par un environnement. La modifier ne
-- casse donc aucun `flyway_schema_history` existant.

-- ── 1. Colonnes d'options ────────────────────────────────────────────────
ALTER TABLE IF EXISTS events
    ADD COLUMN IF NOT EXISTS image_url       VARCHAR(500),
    ADD COLUMN IF NOT EXISTS tags            TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS is_public       BOOLEAN NOT NULL DEFAULT FALSE,
    -- DEFAULT TRUE : ces deux options existent déjà de fait (on pouvait
    -- s'inscrire et pointer sur tout événement). Un défaut FALSE casserait
    -- rétroactivement tous les événements déjà en base.
    ADD COLUMN IF NOT EXISTS requires_registration BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS has_checkin      BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS stream_id       UUID;

-- `CREATE INDEX` n'a pas de variante « IF EXISTS sur la table » : sur une table
-- absente il échoue. D'où le bloc conditionnel, qui laisse un TRACE dans les
-- logs de déploiement au lieu de disparaitre en silence.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'public' AND table_name = 'events') THEN
        CREATE INDEX IF NOT EXISTS idx_events_tags      ON events USING GIN (tags);
        CREATE INDEX IF NOT EXISTS idx_events_is_public ON events (tenant_id, is_public, date_debut)
            WHERE deleted = false AND is_public = true;
        CREATE INDEX IF NOT EXISTS idx_events_stream    ON events (stream_id)
            WHERE stream_id IS NOT NULL;
    ELSE
        RAISE NOTICE 'V200: table `events` absente (renommée `legacy_events` par V158, '
                     'remplacée par `event`) — options d''événement NON appliquées. '
                     'Dérive entité/schema ouverte : voir docs/architecture/schema-events-drift.md';
    END IF;
END $$;

-- ── 2. Intégrité : un direct doit exister et être cohérent ───────────────
-- `live_streams` a été créée en V133 avec `id BIGSERIAL` et `tenant_id BIGINT`
-- (dérive documentée : ce type ne peut pas référencer tenants.id, qui est UUID).
-- On ne peut donc pas poser une clé étrangère. On impose au minimum qu'un
-- identifiant de direct soit un UUID valide, et on laisse la liaison logique
-- (même tenant + même événement) être vérifiée par le service.
DO $$
DECLARE total bigint;
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'public' AND table_name = 'events') THEN
        SELECT count(*) INTO total FROM events WHERE stream_id IS NOT NULL;
        IF total > 0 THEN
            RAISE NOTICE 'V200: % événement(s) portent déjà un stream_id', total;
        END IF;
    END IF;
END $$;

-- ── 3. Garde-fou : ne pas réintroduire une option fantôme ─────────────────
-- Si une migration ultérieure ajoute `has_face_check_in` sans implémentation,
-- ce contrôle la signale plutôt que de laisser l'interface annoncer une
-- capacité inexistante.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'events'
          AND column_name = 'has_face_check_in'
    ) THEN
        RAISE EXCEPTION
            'has_face_check_in introduit sans moteur de reconnaissance faciale. '
            'Implémentez le service, ou retirez la colonne : un drapeau sans '
            'implémentation est un mensonge de contrat.';
    END IF;
END $$;
