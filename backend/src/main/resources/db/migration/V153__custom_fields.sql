-- V153__custom_fields.sql
-- ============================================================
-- G2.4 - Custom Field Engine (19 types, validation backend, rendu generique)
-- Contrat : Annexe A §A.3 (custom_field_definition, custom_field_value)
-- Types: TEXT, LONG_TEXT, NUMBER, DECIMAL, BOOLEAN, DATE, DATETIME, TIME,
--        SELECT, MULTI_SELECT, USER, PERSON, DEPARTMENT, SPACE, TEAM,
--        FILE, IMAGE, URL, PHONE, EMAIL, CURRENCY
-- ============================================================

CREATE TABLE IF NOT EXISTS custom_field_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity_type VARCHAR(60) NOT NULL,
    space_id UUID REFERENCES spaces(id) ON DELETE CASCADE,
    field_code VARCHAR(60) NOT NULL,
    label VARCHAR(120) NOT NULL,
    field_type VARCHAR(30) NOT NULL CHECK (field_type IN
        ('TEXT', 'LONG_TEXT', 'NUMBER', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME', 'TIME',
         'SELECT', 'MULTI_SELECT', 'USER', 'PERSON', 'DEPARTMENT', 'SPACE', 'TEAM',
         'FILE', 'IMAGE', 'URL', 'PHONE', 'EMAIL', 'CURRENCY')),
    required BOOLEAN NOT NULL DEFAULT FALSE,
    options_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    validation_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    visibility_scope VARCHAR(30) NOT NULL DEFAULT 'PUBLIC' CHECK (visibility_scope IN ('PUBLIC', 'PRIVATE', 'ADMIN', 'PASTORAL')),
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_custom_field_def_scope UNIQUE (tenant_id, entity_type, COALESCE(space_id, '00000000-0000-0000-0000-000000000000'::uuid), field_code)
);

CREATE INDEX IF NOT EXISTS idx_cfd_tenant ON custom_field_definitions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cfd_entity ON custom_field_definitions(tenant_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_cfd_space ON custom_field_definitions(space_id);
CREATE INDEX IF NOT EXISTS idx_cfd_deleted ON custom_field_definitions(deleted_at);

COMMENT ON TABLE custom_field_definitions IS 'G2.4 : definitions de champs personnalises par entite/espace';
COMMENT ON COLUMN custom_field_definitions.entity_type IS 'Entite cible : PERSON, EVENT, ASSET, TASK, SPACE, ORGANIZATION_NODE, etc.';
COMMENT ON COLUMN custom_field_definitions.space_id IS 'NULL = definition au niveau tenant (heritable), sinon propre a l\'espace';
COMMENT ON COLUMN custom_field_definitions.options_json IS 'Pour SELECT/MULTI_SELECT: [{value, label, color}]. Pour USER/PERSON/DEPARTMENT/SPACE/TEAM: config de filtrage.';
COMMENT ON COLUMN custom_field_definitions.validation_json IS 'Regles: min/max/length, regex, custom validators par type';
COMMENT ON COLUMN custom_field_definitions.visibility_scope IS 'Qui peut voir ce champ: PUBLIC (tous), PRIVATE (proprietaire), ADMIN, PASTORAL';

CREATE TABLE IF NOT EXISTS custom_field_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    field_id UUID NOT NULL REFERENCES custom_field_definitions(id) ON DELETE CASCADE,
    entity_id UUID NOT NULL,
    value_json JSONB NOT NULL,
    updated_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cfv_entity ON custom_field_values(entity_id);
CREATE INDEX IF NOT EXISTS idx_cfv_field ON custom_field_values(field_id);
CREATE INDEX IF NOT EXISTS idx_cfv_tenant ON custom_field_values(tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_cfv_field_entity ON custom_field_values(field_id, entity_id);

COMMENT ON TABLE custom_field_values IS 'G2.4 : valeurs des champs personnalises (une ligne par champ par entite)';
COMMENT ON COLUMN custom_field_values.value_json IS 'Valeur typee selon field_type: string, number, boolean, array, object {url, name} pour FILE/IMAGE, etc.';

-- Trigger updated_at
DROP TRIGGER IF EXISTS update_custom_field_definitions_updated_at ON custom_field_definitions;
CREATE TRIGGER update_custom_field_definitions_updated_at
    BEFORE UPDATE ON custom_field_definitions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();