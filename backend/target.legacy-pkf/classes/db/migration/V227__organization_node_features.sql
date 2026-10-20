-- ============================================================
-- SPEC_ORGANISATION_MODULABLE_V3 §D — V227 : MODULES par NŒUD.
--
-- `TenantFeature` = tenant ; `SpaceModule` = space. Rien n'accordait un
-- module à un `OrganizationNode` (un campus précis). V3-D : chaque
-- église/campus CHOISIT ses modules, INDÉPENDANT par défaut — activer
-- un module sur la racine n'active PAS les enfants.
--
-- D14 : migration MONTANTE. Table additive, rétrocompatible.
-- Cible PostgreSQL (parité validée par le gate PG).
-- ============================================================

CREATE TABLE IF NOT EXISTS organization_node_features (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    node_id            UUID NOT NULL REFERENCES organization_nodes(id) ON DELETE CASCADE,
    module_code        VARCHAR(50) NOT NULL,       -- référence module_definition.code
    enabled            BOOLEAN NOT NULL DEFAULT TRUE,
    configuration_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    limits_json        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ,
    CONSTRAINT uk_node_feature UNIQUE (node_id, module_code)
);
CREATE INDEX IF NOT EXISTS idx_node_feature_tenant ON organization_node_features(tenant_id, module_code);
CREATE INDEX IF NOT EXISTS idx_node_feature_node ON organization_node_features(node_id);
