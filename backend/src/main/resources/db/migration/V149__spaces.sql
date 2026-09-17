-- V149__spaces.sql
-- ============================================================
-- G2.6 — Espaces configurables unifiés (département = famille = sous-équipe)
-- Contrat : Annexe A §A.1 (space)
-- Un space vit dans une organization_unit (pas de fusion des deux objets).
-- Soft delete via deleted_at (§0.3 n°5) + historisation.
-- ============================================================

CREATE TABLE IF NOT EXISTS spaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    organization_unit_id UUID NOT NULL REFERENCES organization_nodes(id) ON DELETE CASCADE,
    space_type VARCHAR(30) NOT NULL CHECK (space_type IN ('DEPARTMENT', 'FAMILY', 'SUB_TEAM')),
    template_code VARCHAR(100),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(60),
    icon VARCHAR(100),
    color VARCHAR(7),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    visible_people_scope VARCHAR(20) DEFAULT 'CHURCH' CHECK (visible_people_scope IN ('CAMPUS', 'CHURCH')),
    configuration_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_space_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_space_tenant ON spaces(tenant_id);
CREATE INDEX IF NOT EXISTS idx_space_org_unit ON spaces(organization_unit_id);
CREATE INDEX IF NOT EXISTS idx_space_type ON spaces(space_type);
CREATE INDEX IF NOT EXISTS idx_space_tenant_type ON spaces(tenant_id, space_type);
CREATE INDEX IF NOT EXISTS idx_space_deleted ON spaces(deleted_at);

COMMENT ON TABLE spaces IS 'G2.6 : espace unifié (département = famille = sous-équipe), 100% configuration, aucun département codé en dur';
COMMENT ON COLUMN spaces.organization_unit_id IS 'Unité organisationnelle qui accueille l''espace (§A.1 : space et organization_unit restent liés sans fusion)';
COMMENT ON COLUMN spaces.configuration_json IS 'Configuration de l''espace (pages, boutons, widgets, dashboard, couleurs) — jamais du code';
COMMENT ON COLUMN spaces.deleted_at IS 'Soft delete : un espace archivé reste historisé';

-- Backfill : un espace DEPARTMENT pour chaque nœud organization_nodes de type DEPARTMENT
INSERT INTO spaces (tenant_id, organization_unit_id, space_type, name, code, icon, color, status, configuration_json)
SELECT n.tenant_id, n.id, 'DEPARTMENT', n.name, n.code, n.icon, n.color, 'ACTIVE', '{}'::jsonb
FROM organization_nodes n
WHERE n.type = 'DEPARTMENT'
  AND NOT EXISTS (
      SELECT 1 FROM spaces s WHERE s.organization_unit_id = n.id
  )
ON CONFLICT DO NOTHING;
