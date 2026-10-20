-- V272__archives_parcours_complet.sql
-- ============================================================
-- ARCHIVES D'ÉVÉNEMENTS — intégrité, parité de droits, liste
-- paginée. ADDITIF : aucun droit retiré, aucune table ni colonne
-- supprimée, aucun comportement de lecture existant modifié.
--
-- Audit (oct. 2026) relevé sur V206/V258/V271 :
--  (1) « parité exacte » V271 fausse : EVENTS_EXECUTE_R6A2 (garde
--      serveur de GET /church-events/archives) est détenu par SIX
--      rôles, UI_ARCHIVES_ACCESS seulement par QUATRE —
--      DEPARTMENT_LEADER et FAMILY_LEADER pouvaient lire l'API mais
--      l'écran leur était caché ;
--  (2) aucun UNIQUE (event_id, version) : le « jamais d'écrasement »
--      n'était pas soutenu par la base ;
--  (3) la liste servait snapshot_json ENTIER (aucune pagination) :
--      la consultation chargeait des mégaoctets par écran ;
--  (4) aucune politique de rétention ne pouvait cibler les archives.
--
-- Décisions d'architecture consignées ici (et non implicites) :
--  A) INTÉGRITÉ RÉFÉRENTIELLE — pas de clé étrangère sur
--     event_archive(event_id, tenant_id, space_id, archived_by).
--     Le cycle de vie du modèle vivant est le SOFT-DELETE
--     (event.deleted_at, V158/V203) : la purge de conformité
--     (ComplianceManagerService) et la rétention peuvent légitimement
--     effacer des lignes sans qu'une FK vienne en interdire la
--     suppression ou propager un effacement en cascade. L'unicité
--     (event_id, version) ci-dessous garantit la version ; la
--     portée tenant est garantie au niveau applicatif (toutes les
--     requêtes du repository filtrent par tenant_id).
--  B) IMMUITÉ — la base n'offre aucun UPDATE/DELETE applicatif sur
--     cette table : la seule écriture passante est l'INSERT du
--     service de clôture. La « rétention » (§4) est la UNIQUE porte
--     de purge, et elle exige une politique explicitement créée par
--     l'église — aucune purge n'est active par défaut.
-- ============================================================

-- 1) PARITÉ DE DROITS — UI_ARCHIVES_ACCESS aligné sur les vrais
--    détenteurs de la garde serveur EVENTS_EXECUTE_R6A2 (V258 l.548-553 :
--    ADMIN, CHEF_DE_FAMILLE, DEPARTMENT_LEADER, FAMILY_LEADER, PASTEUR,
--    RESPONSABLE). ADDITIF : les quatre rôles déjà servis conservent la
--    clé ; on AJOUTE les deux rôles legacy qui tenaient déjà l'API.
--    Contrainte outillage : pas d'apostrophe SQL échappée dans les
--    libellés (en-tête V270), sinon le parseur de seed perd la clé.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('DEPARTMENT_LEADER', 'UI_ARCHIVES_ACCESS'),
    ('FAMILY_LEADER', 'UI_ARCHIVES_ACCESS')
) AS m(role_key, perm_key)
JOIN roles r ON r.key = m.role_key AND r.tenant_id IS NULL
JOIN permissions p ON p.key = m.perm_key
ON CONFLICT DO NOTHING;

-- 1-bis) Répercussion vers les surcouches locales homonymes (miroir
--    V271 §3 / V259 §5) : additif, une église peut retirer en local.
INSERT INTO role_permissions (role_id, permission_id)
SELECT local.id, rp.permission_id
FROM roles g
JOIN roles local ON local.key = g.key AND local.tenant_id IS NOT NULL
JOIN role_permissions rp ON rp.role_id = g.id
WHERE g.tenant_id IS NULL
  AND rp.permission_id IN (SELECT id FROM permissions WHERE key = 'UI_ARCHIVES_ACCESS')
ON CONFLICT DO NOTHING;

-- 1-ter) Entrée de menu « archives » : matrice élargie aux deux rôles.
--    `roles` est un jsonb de libellés de rôles ; concaténation sans
--    duplication, idempotente.
UPDATE menu_entries
SET roles = CASE
        WHEN roles ? 'DEPARTMENT_LEADER' AND roles ? 'FAMILY_LEADER' THEN roles
        WHEN roles ? 'DEPARTMENT_LEADER' THEN roles || '["FAMILY_LEADER"]'::jsonb
        WHEN roles ? 'FAMILY_LEADER' THEN roles || '["DEPARTMENT_LEADER"]'::jsonb
        ELSE roles || '["DEPARTMENT_LEADER","FAMILY_LEADER"]'::jsonb
    END
WHERE key = 'archives'
  AND (NOT roles ? 'DEPARTMENT_LEADER' OR NOT roles ? 'FAMILY_LEADER');

-- 2) INTÉGRITÉ VERSION — plus d'une version par (événement, numéro),
--    quel que soit le chemin d'écriture. Le service pose en plus un
--    verrou pessimiste sur la ligne `event` (findForUpdate) : cette
--    contrainte est le DERNIER rempart, pas la seule défense.
DO $$
DECLARE
    dup_count bigint;
BEGIN
    SELECT COUNT(*) INTO dup_count FROM (
        SELECT event_id, version FROM event_archive
        GROUP BY event_id, version HAVING COUNT(*) > 1
    ) d;
    IF dup_count > 0 THEN
        RAISE EXCEPTION USING
            MESSAGE = 'V272 refuse de s''appliquer : ' || dup_count
                || ' couple(s) (event_id, version) en doublon dans event_archive. '
                || 'Des clôtures concurrentes ont contourne le service avant cette migration. '
                || 'Resolution requise : creer une version de rattrapage par evenement concerne, '
                || 'puis relancer la migration. Aucune ligne n''est supprimee ni modifiee.',
            HINT = 'Rapport : SELECT event_id, version, COUNT(*) FROM event_archive GROUP BY event_id, version HAVING COUNT(*) > 1;';
    END IF;
    CREATE UNIQUE INDEX IF NOT EXISTS uq_event_archive_event_version
        ON event_archive (event_id, version);
END $$;

-- 3) COLONNE SCALAIRE — nombre de présences figé au clôturage.
--    Avant : la liste devait déserialiser snapshot_json POUR CHAQUE
--    ligne juste pour afficher un compteur. La colonne est renseignée
--    à l'écriture (ChurchEventService.archiveEvent) et backfillée ici
--    pour les lignes existantes (aucune donnée perdue : la valeur est
--    extraite du snapshot, une valeur absente compte 0 — exactement
--    ce que l'écran affichait).
ALTER TABLE event_archive ADD COLUMN IF NOT EXISTS attendance_count INTEGER;

UPDATE event_archive
SET attendance_count = COALESCE((snapshot_json ->> 'attendanceCount')::integer, 0)
WHERE attendance_count IS NULL;

ALTER TABLE event_archive ALTER COLUMN attendance_count SET DEFAULT 0;
ALTER TABLE event_archive ALTER COLUMN attendance_count SET NOT NULL;

-- 4) RÉTENTION (RGPD art. 5-1-e) — la table devient PURGEABLE de façon
--    ciblée via une politique créée explicitement par l'église
--    (dataType = 'EventArchive', ComplianceManagerService). AUCUNE purge
--    n'est active par défaut ; la purge exporte avant toute suppression.
--    Le référentiel reste sans clé étrangère, cf. décision (A) en tête.

