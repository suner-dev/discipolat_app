-- V145__config_inheritance.sql
-- ============================================================
-- G1.7 - §53 : Héritage de configuration (DEFAULT / INHERITED / OVERRIDDEN)
-- Chaque nœud d'organisation peut définir sa source de configuration.
-- La résolution remonte la chaîne parente ; un OVERRIDDEN stoppe la remontée.
-- ============================================================

ALTER TABLE organization_nodes
    ADD COLUMN IF NOT EXISTS config_source VARCHAR(20) NOT NULL DEFAULT 'DEFAULT'
        CHECK (config_source IN ('DEFAULT', 'INHERITED', 'OVERRIDDEN'));

ALTER TABLE organization_nodes
    ADD COLUMN IF NOT EXISTS resolved_config_json JSONB;

CREATE INDEX IF NOT EXISTS idx_org_node_config_source
    ON organization_nodes(tenant_id, config_source);

COMMENT ON COLUMN organization_nodes.config_source IS
    'G1.7 §53 : DEFAULT (valeurs par défaut) | INHERITED (hérite du parent) | OVERRIDDEN (redéfinit et stoppe la remontée)';
COMMENT ON COLUMN organization_nodes.resolved_config_json IS
    'G1.7 §53 : configuration résolue mise en cache, invalidée par l''événement config-changed';
