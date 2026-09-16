-- V147__organization_unit_generalized_hierarchy.sql
-- ============================================================
-- G2.1 — OrganizationUnit généralisée & hiérarchie infinie
-- Complète les champs manquants sur organization_nodes pour arbre universel
-- ============================================================

ALTER TABLE organization_nodes
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS icon VARCHAR(100),
    ADD COLUMN IF NOT EXISTS color VARCHAR(7),
    ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN organization_nodes.description IS 'G2.1 : Description de l\'unité organisationnelle';
COMMENT ON COLUMN organization_nodes.icon IS 'G2.1 : Icône (ex: lucide-react name, emoji, ou classe CSS)';
COMMENT ON COLUMN organization_nodes.color IS 'G2.1 : Couleur hexadécimale pour UI (#RRGGBB)';
COMMENT ON COLUMN organization_nodes.sort_order IS 'G2.1 : Ordre d\'affichage parmi les frères';

-- Les index (tenant_id, parent_id) et (tenant_id, type) existent déjà via V135:
-- idx_org_node_tenant, idx_org_node_parent, idx_org_node_type, idx_org_node_tenant_type