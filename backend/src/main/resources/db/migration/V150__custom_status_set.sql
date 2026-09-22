-- V150__custom_status_set.sql
-- ============================================================
-- G2.7 — Statuts configurables par espace (contrat Annexe A §A.3)
-- Un statut est une configuration, jamais un enum fige.
-- Heritage §G1.7 : space_id NULL = jeu defini au niveau du tenant.
-- ============================================================

CREATE TABLE IF NOT EXISTS custom_status_set (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity_type VARCHAR(60) NOT NULL,
    space_id UUID REFERENCES spaces(id) ON DELETE CASCADE,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(120) NOT NULL,
    color VARCHAR(7),
    icon VARCHAR(100),
    display_order INTEGER NOT NULL DEFAULT 0,
    initial_status BOOLEAN NOT NULL DEFAULT FALSE,
    final_status BOOLEAN NOT NULL DEFAULT FALSE,
    allowed_transitions_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_custom_status_tenant ON custom_status_set(tenant_id);
CREATE INDEX IF NOT EXISTS idx_custom_status_entity ON custom_status_set(tenant_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_custom_status_space ON custom_status_set(space_id);

-- Un code unique par (tenant, entity_type, space). NULL space_id = niveau tenant :
-- un index partiel garantit l'unicite des jeux herites.
CREATE UNIQUE INDEX IF NOT EXISTS uk_custom_status_tenant_scope
    ON custom_status_set (tenant_id, entity_type, COALESCE(space_id, '00000000-0000-0000-0000-000000000000'::uuid), code)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE custom_status_set IS 'G2.7 : statuts configurables par espace avec transitions controlees';
COMMENT ON COLUMN custom_status_set.space_id IS 'Espace proprietaire ; NULL = jeu herite au niveau du tenant (heritage G1.7)';
COMMENT ON COLUMN custom_status_set.allowed_transitions_json IS 'Codes des statuts cibles autorises depuis celui-ci ; toute autre transition est refusee par le backend';

-- Exemple de jeu tenant par defaut pour les tâches (fallback des espaces qui n''en definissent pas)
INSERT INTO custom_status_set (tenant_id, entity_type, space_id, code, name, color, icon, display_order, initial_status, final_status, allowed_transitions_json)
SELECT t.id, 'TASK', NULL, 'TODO', 'A faire', '#94a3b8', 'Circle', 0, TRUE, FALSE, '["IN_PROGRESS"]'::jsonb
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM custom_status_set s WHERE s.tenant_id = t.id AND s.entity_type = 'TASK' AND s.code = 'TODO'
);

INSERT INTO custom_status_set (tenant_id, entity_type, space_id, code, name, color, icon, display_order, initial_status, final_status, allowed_transitions_json)
SELECT t.id, 'TASK', NULL, 'IN_PROGRESS', 'En cours', '#3b82f6', 'Loader', 1, FALSE, FALSE, '["DONE"]'::jsonb
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM custom_status_set s WHERE s.tenant_id = t.id AND s.entity_type = 'TASK' AND s.code = 'IN_PROGRESS'
);

INSERT INTO custom_status_set (tenant_id, entity_type, space_id, code, name, color, icon, display_order, initial_status, final_status, allowed_transitions_json)
SELECT t.id, 'TASK', NULL, 'DONE', 'Termine', '#22c55e', 'CheckCircle', 2, FALSE, TRUE, '[]'::jsonb
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM custom_status_set s WHERE s.tenant_id = t.id AND s.entity_type = 'TASK' AND s.code = 'DONE'
);
