-- ============================================================
-- SPEC_ORGANISATION_MODULABLE_V3 §A — V224 : niveaux hiérarchiques
-- CONFIGURABLES par dénomination.
--
-- V2 figeait la hiérarchie dans l'enum OrganizationNodeType. V3 rend
-- chaque racine (dénomination) libre de définir SES niveaux (Région,
-- Zone, Campus…) : nom, ordre, parenté. L'enum `type` du nœud RESTE la
-- source de la logique transverse ; `level_id` n'apporte que le libellé
-- et l'ordre métier (repli sur `type` quand `level_id` est NULL).
--
-- D14 : migration MONTANTE uniquement (V>223). Rétrocompatible : toutes
-- les colonnes sont additives, `level_id` est NULLABLE (aucun nœud
-- existant n'est cassé). Backfill idempotent (ON CONFLICT DO NOTHING).
--
-- Cible PostgreSQL (Flyway désactivé sous H2/tests : le schéma de test
-- est généré depuis les entités — la parité est vérifiée par le gate PG).
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Référentiel de niveaux (scopé par RACINE, pas par tenant)
CREATE TABLE IF NOT EXISTS organization_levels (
    id              UUID PRIMARY KEY,
    root_tenant_id  UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    plural_name     VARCHAR(120),
    description     TEXT,
    depth_order     INTEGER NOT NULL,
    -- Sémantique interne : valeurs de l'enum OrganizationNodeType + 'CUSTOM'.
    semantic_type   VARCHAR(30) NOT NULL DEFAULT 'CUSTOM'
        CHECK (semantic_type IN ('ROOT_CHURCH','CAMPUS','SUB_CHURCH','ASSEMBLY','REGION','DISTRICT','DEPARTMENT','GROUP','CUSTOM')),
    parent_level_id UUID REFERENCES organization_levels(id) ON DELETE SET NULL,
    icon            VARCHAR(100),
    color           VARCHAR(7),
    is_branching    BOOLEAN NOT NULL DEFAULT TRUE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT uk_org_level_root_order UNIQUE (root_tenant_id, depth_order)
);
CREATE INDEX IF NOT EXISTS idx_org_level_root   ON organization_levels(root_tenant_id);
CREATE INDEX IF NOT EXISTS idx_org_level_parent ON organization_levels(parent_level_id);

-- 2) Lien nœud → niveau custom (NULLABLE = repli sur `type`)
ALTER TABLE organization_nodes ADD COLUMN IF NOT EXISTS level_id UUID
    REFERENCES organization_levels(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_org_node_level ON organization_nodes(level_id);

-- 3) Backfill — graine des 4 niveaux par défaut pour chaque racine existante,
--    moulée sur la sémantique V2. Parenté laissée NULL ici (l'ordre
--    `depth_order` suffit) ; l'éditeur interactif (service ensureDefaults)
--    rétablira les parents. Idempotent.
INSERT INTO organization_levels (id, root_tenant_id, name, plural_name, semantic_type, depth_order, is_branching, active, created_at)
SELECT gen_random_uuid(), r.root, 'Région', 'Régions', 'REGION', 1, TRUE, TRUE, now()
FROM (SELECT DISTINCT COALESCE(root_tenant_id, id) AS root FROM tenants) r
ON CONFLICT (root_tenant_id, depth_order) DO NOTHING;

INSERT INTO organization_levels (id, root_tenant_id, name, plural_name, semantic_type, depth_order, is_branching, active, created_at)
SELECT gen_random_uuid(), r.root, 'District', 'Districts', 'DISTRICT', 2, TRUE, TRUE, now()
FROM (SELECT DISTINCT COALESCE(root_tenant_id, id) AS root FROM tenants) r
ON CONFLICT (root_tenant_id, depth_order) DO NOTHING;

INSERT INTO organization_levels (id, root_tenant_id, name, plural_name, semantic_type, depth_order, is_branching, active, created_at)
SELECT gen_random_uuid(), r.root, 'Campus', 'Campus', 'CAMPUS', 3, TRUE, TRUE, now()
FROM (SELECT DISTINCT COALESCE(root_tenant_id, id) AS root FROM tenants) r
ON CONFLICT (root_tenant_id, depth_order) DO NOTHING;

INSERT INTO organization_levels (id, root_tenant_id, name, plural_name, semantic_type, depth_order, is_branching, active, created_at)
SELECT gen_random_uuid(), r.root, 'Groupe', 'Groupes', 'GROUP', 4, TRUE, TRUE, now()
FROM (SELECT DISTINCT COALESCE(root_tenant_id, id) AS root FROM tenants) r
ON CONFLICT (root_tenant_id, depth_order) DO NOTHING;

-- 4) Rattachement des nœuds existants à leur niveau (par sémantique égale).
UPDATE organization_nodes n
SET level_id = l.id
FROM tenants t
JOIN organization_levels l ON l.root_tenant_id = COALESCE(t.root_tenant_id, t.id)
WHERE n.tenant_id = t.id
  AND n.level_id IS NULL
  AND l.semantic_type = n.type;
