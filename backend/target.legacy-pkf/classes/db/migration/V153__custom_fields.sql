-- V153__custom_fields.sql
-- ============================================================
-- G2.4 - Custom Field Engine
-- NOTE : les tables custom_field_definitions / custom_field_values
-- existent deja depuis V38 (schema legacy francais) et sont mapgees
-- par les entites JPA CustomFieldDefinition / CustomFieldValue
-- (colonnes entite_type, code, type, obligatoire, ordre, options,
--  placeholder, default_value, roles_lecture, roles_ecriture, actif).
-- V70 a deja ajoute tenant_id (NOT NULL) + index de tenant.
-- Cette migration est donc DEFENSIVE : elle ne redefinit pas le schema
-- (CREATE TABLE IF NOT EXISTS serait un no-op sur DB fraiche), elle
-- garantit uniquement les complements necessaires au runtime.
-- ============================================================

-- Garantir la colonne tenant_id (deja ajoutee par V70 sur DB historiques)
ALTER TABLE custom_field_definitions ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE custom_field_values ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

UPDATE custom_field_definitions SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE custom_field_values SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;

ALTER TABLE custom_field_definitions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE custom_field_values ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_custom_field_definitions_tenant ON custom_field_definitions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_custom_field_values_tenant ON custom_field_values(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cfv_entity ON custom_field_values(entite_type, entite_id);
CREATE INDEX IF NOT EXISTS idx_cfv_field ON custom_field_values(field_id);

-- Trigger updated_at (fonction update_updated_at_column definie par V70+)
DROP TRIGGER IF EXISTS update_custom_field_definitions_updated_at ON custom_field_definitions;
CREATE TRIGGER update_custom_field_definitions_updated_at
    BEFORE UPDATE ON custom_field_definitions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE custom_field_definitions IS 'G2.4 : definitions de champs personnalises (schema legacy V38, multi-tenant V70)';
COMMENT ON TABLE custom_field_values IS 'G2.4 : valeurs des champs personnalises (schema legacy V38, multi-tenant V70)';