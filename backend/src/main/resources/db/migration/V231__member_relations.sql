-- V231__member_relations.sql
-- ============================================================
-- RELATIONS PERSONNELLES D'ENCADREMENT (« Mon encadrement »)
-- Plan : docs/PLAN_HIERARCHIE_RELATIONS_MEMBRES.md
--
-- Arête dirigée from (membre) -> to (autorité déclarée : pasteur,
-- supérieur, responsable, mentor, parrain…). Le membre déclare
-- lui-même ses encadrants ENREGISTRÉS selon le paramétrage de son
-- église (dictionnaire MEMBER_RELATION_TYPE, copie par tenant — V193).
-- Déclaration immédiate (statut ACTIVE) + notification chez le
-- supérieur (côté service). Fin de relation = statut REVOKED, jamais
-- de purge (traçabilité, même discipline que MemberRoleAssignment V226).
--
-- Unicité « un seul ACTIVE par (from, to, type) » : vérifiée au
-- service (MemberRelationService.declare) et NON par un index partiel
-- PostgreSQL — le profil de test H2 (ddl-auto, Flyway désactivé) ne
-- connaît pas les index partiels et la règle doit être identique sur
-- les deux dialectes (convention dialect-independent du projet). Un
-- index composé reste posé pour les lectures « mes encadrants » et
-- « ses membres », qui sont les chemins chauds.
--
-- Conventions alignées sur V222..V230 : TIMESTAMPTZ, now(), FK
-- explicites, CHECK sur les énumérations, ON DELETE explicite.
-- Migration MONTANTE : table additive, aucun impact sur l'existant.
-- ============================================================

CREATE TABLE IF NOT EXISTS member_relations (
    id             UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    from_user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    relation_type  VARCHAR(50) NOT NULL,
    statut         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (statut IN ('ACTIVE', 'REVOKED')),
    note           TEXT,
    declared_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ,
    ended_at       TIMESTAMPTZ,
    -- Cohérence temporelle : une relation terminée porte une date de fin.
    CONSTRAINT ck_member_relations_ended CHECK (
        (statut = 'ACTIVE' AND ended_at IS NULL) OR statut = 'REVOKED'
    )
);

CREATE INDEX IF NOT EXISTS idx_member_relations_tenant ON member_relations (tenant_id);
CREATE INDEX IF NOT EXISTS idx_member_relations_from   ON member_relations (from_user_id, statut);
CREATE INDEX IF NOT EXISTS idx_member_relations_to     ON member_relations (to_user_id, statut);
-- Doublon (tenant, from, to, type, statut) + tri « ses membres » par date.
CREATE INDEX IF NOT EXISTS idx_member_relations_active  ON member_relations
    (tenant_id, from_user_id, to_user_id, relation_type, statut);
CREATE INDEX IF NOT EXISTS idx_member_relations_feed    ON member_relations
    (tenant_id, to_user_id, statut, created_at DESC);

COMMENT ON TABLE member_relations IS
    'Rattachements personnels déclaratifs membre x autorité (hiérarchie multi-branches, feature Mon encadrement — V231).';

-- ============================================================
-- SEED DU DICTIONNAIRE MEMBER_RELATION_TYPE (le « paramétrage de
-- l'église » : chaque tenant possède SA copie éditable — V70 + V193).
-- Codes par défaut ; l'admin renomme / ajoute / DÉSACTIVE via
-- /admin/dictionaries. Un code désactivé est REJETÉ à la déclaration
-- (MemberRelationService.typeCatalog) — pas seulement masqué.
-- Garde NOT EXISTS = ré-exécution sans danger.
-- ============================================================

INSERT INTO dictionary_entries (id, tenant_id, dict_key, code, label, color, ordre, actif, is_default, created_at, updated_at)
SELECT uuid_generate_v4(), t.id, seed.dict_key, seed.code, seed.label, seed.color, seed.ordre, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
CROSS JOIN (VALUES
    ('MEMBER_RELATION_TYPE', 'PASTEUR',     'Mon pasteur',        '#a855f7', 1),
    ('MEMBER_RELATION_TYPE', 'SUPERIEUR',   'Mon supérieur',      '#3b82f6', 2),
    ('MEMBER_RELATION_TYPE', 'RESPONSABLE', 'Mon responsable',    '#f59e0b', 3),
    ('MEMBER_RELATION_TYPE', 'MENTOR',      'Mon mentor',         '#22c55e', 4),
    ('MEMBER_RELATION_TYPE', 'PARRAIN',     'Mon parrain / père (mère) spirituel(le)', '#ec4899', 5)
) AS seed(dict_key, code, label, color, ordre)
WHERE NOT EXISTS (
    SELECT 1 FROM dictionary_entries d
    WHERE d.tenant_id = t.id AND d.dict_key = seed.dict_key AND d.code = seed.code
);
