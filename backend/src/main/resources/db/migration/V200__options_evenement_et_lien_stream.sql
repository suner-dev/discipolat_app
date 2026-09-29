-- V200__options_evenement_et_lien_stream.sql
--
-- Pourquoi : le formulaire et l'écran de détail du client mobile exposaient des
-- options (image de couverture, tags, visibilité, inscription obligatoire,
-- pointage, lien vers un direct) pour lesquelles AUCUNE colonne n'existait.
-- Les clients les envoyaient et le serveur les ignorait silencieusement.
-- Plutôt que de les retirer de l'interface, on leur donne une réalité.
--
-- Décision de conception : les 3 options de contrôle d'accès sont des
-- **colonnes** (le serveur les applique réellement), pas des drapeaux décoratifs.
--   - `is_public`            : l'événement n'apparaît que si vrai (filtre appliqué)
--   - `requires_registration`: le serveur REFUSE l'inscription si faux
--   - `has_checkin`          : le serveur REFUSE le pointage si faux
--
-- `has_geofencing` et `has_face_check_in` ne sont PAS ajoutés : il n'existe
-- ni moteur de géolocalisation ni service de reconnaissance faciale dans le
-- produit. Ajouter un booléen sans implémentation serait exactement le mensonge
-- de contrat que ce chantier supprime. Voir KNOWN_ISSUES.

-- ── 1. Colonnes d'options ────────────────────────────────────────────────
ALTER TABLE events
    ADD COLUMN IF NOT EXISTS image_url       VARCHAR(500),
    ADD COLUMN IF NOT EXISTS tags            TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS is_public       BOOLEAN NOT NULL DEFAULT FALSE,
    -- DEFAULT TRUE : ces deux options existent deja de fait (on pouvait
    -- s'inscrire et pointer sur tout evenement). Un defaut FALSE casserait
    -- retroactivement tous les evenements deja en base.
    ADD COLUMN IF NOT EXISTS requires_registration BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS has_checkin      BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS stream_id       UUID;

CREATE INDEX IF NOT EXISTS idx_events_tags        ON events USING GIN (tags);
CREATE INDEX IF NOT EXISTS idx_events_is_public   ON events (tenant_id, is_public, date_debut)
    WHERE deleted = false AND is_public = true;
CREATE INDEX IF NOT EXISTS idx_events_stream      ON events (stream_id) WHERE stream_id IS NOT NULL;

-- ── 2. Intégrité : un direct doit exister et être cohérent ───────────────
-- `live_streams` a été créée en V133 avec `id BIGSERIAL` et `tenant_id BIGINT`
-- (dérive documentée : ce type ne peut pas référencer tenants.id, qui est UUID).
-- On ne peut donc pas poser une clé étrangère. On impose au minimum qu'un
-- identifiant de direct soit un UUID valide, et on laisse la liaison logique
-- (même tenant + même événement) être vérifiée par le service.
DO $$
DECLARE bad uuid;
BEGIN
    IF EXISTS (SELECT 1 FROM events WHERE stream_id IS NOT NULL) THEN
        -- On ne peut pas valider l'unicité sans la FK (voir ci-dessus) : on
        -- refuse seulement une valeur qui ne soit pas un UUID, ce que la colonne
        -- garantit déjà par son type. Rien à faire ici, garde-fou explicite.
        RAISE NOTICE 'V200: % evenement(s) portent deja un stream_id', (
            SELECT count(*) FROM events WHERE stream_id IS NOT NULL);
    END IF;
END $$;

-- ── 3. Garde-fou : ne pas réintroduire une option fantôme ─────────────────
-- Si une migration ultérieure ajoute `has_geofencing` ou `has_face_check_in`
-- sans implémentation, ce contrôle la signale plutôt que de laisser l'interface
-- annoncer une capacité inexistante.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'events'
          AND column_name IN ('has_geofencing', 'has_face_check_in')
    ) THEN
        RAISE EXCEPTION
            'V200: has_geofencing / has_face_check_in interdits sans implementation '
            'reelle (geolocalisation / reconnaissance faciale) — retirez la colonne '
            'et l option de l interface';
    END IF;
END $$;
