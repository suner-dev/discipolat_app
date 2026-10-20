-- V146__resource_scope_global_local.sql
-- ============================================================
-- G1.8 - §54 : Ressources GLOBAL / LOCAL
-- Distingue les ressources partagees (TENANT_GLOBAL) des ressources
-- propres a une unite d'organisation (ORGANIZATION_LOCAL / UNIT_LOCAL).
-- organization_unit_id NULL = ressource globale du tenant.
-- Filtrage systematique : resource_scope + organization_unit_id (jamais le scope seul).
-- ============================================================

DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['inventory_items','events','department_documents','entity_attachments','form_templates']
    LOOP
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = t) THEN
            EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS resource_scope VARCHAR(20) NOT NULL DEFAULT ''TENANT_GLOBAL''', t);
            EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS organization_unit_id UUID', t);
            EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_scope_unit ON %I(tenant_id, resource_scope, organization_unit_id)', t, t);
        END IF;
    END LOOP;
END $$;

COMMENT ON COLUMN inventory_items.resource_scope IS 'G1.8 §54 : TENANT_GLOBAL | ORGANIZATION_LOCAL | UNIT_LOCAL';
