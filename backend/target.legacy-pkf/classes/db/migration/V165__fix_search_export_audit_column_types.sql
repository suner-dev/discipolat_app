-- V165__fix_search_export_audit_column_types.sql
-- Fix column types for H2 compatibility: change TEXT[] to JSONB

-- search_audit: entity_types TEXT[] -> JSONB
ALTER TABLE search_audit ALTER COLUMN entity_types TYPE JSONB USING array_to_json(entity_types)::jsonb;

-- export_audit: columns_exported TEXT[] -> JSONB
ALTER TABLE export_audit ALTER COLUMN columns_exported TYPE JSONB USING array_to_json(columns_exported)::jsonb;