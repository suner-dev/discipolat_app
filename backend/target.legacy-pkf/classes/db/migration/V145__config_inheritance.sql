-- V145__config_inheritance.sql
-- ============================================================
-- G1.7 - §53 : Heritage de configuration (DEFAULT / INHERITED / OVERRIDDEN)
-- Chaque nœud d'organisation peut definir sa source de configuration.
-- La resolution remonte la chaine parente ; un OVERRIDDEN stoppe la remontee.
-- ============================================================

ALTER TABLE organization_nodes
    ADD COLUMN IF NOT EXISTS config_source VARCHAR(20) NOT NULL DEFAULT 'DEFAULT'
        CHECK (config_source IN ('DEFAULT', 'INHERITED', 'OVERRIDDEN'));

ALTER TABLE organization_nodes
    ADD COLUMN IF NOT EXISTS resolved_config_json JSONB;

CREATE INDEX IF NOT EXISTS idx_org_node_config_source
    ON organization_nodes(tenant_id, config_source);

COMMENT ON COLUMN organization_nodes.config_source IS
    'G1.7 §53 : DEFAULT (valeurs par defaut) | INHERITED (herite du parent) | OVERRIDDEN (redefinit et stoppe la remontee)';
COMMENT ON COLUMN organization_nodes.resolved_config_json IS
    'G1.7 §53 : configuration resolue mise en cache, invalidee par l''evenement config-changed';
