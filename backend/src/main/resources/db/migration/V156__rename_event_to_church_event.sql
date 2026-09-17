-- V156__rename_event_to_church_event.sql
-- ============================================================
-- Rename event -> church_event to avoid conflict with legacy events table
-- Update all foreign keys to reference church_event
-- ============================================================

-- Rename event table
ALTER TABLE IF EXISTS event RENAME TO church_event;

-- Rename indexes
ALTER INDEX IF EXISTS idx_event_tenant RENAME TO idx_church_event_tenant;
ALTER INDEX IF EXISTS idx_event_status RENAME TO idx_church_event_status;
ALTER INDEX IF EXISTS idx_event_start RENAME TO idx_church_event_start;
ALTER INDEX IF EXISTS idx_event_tenant_start RENAME TO idx_church_event_tenant_start;
ALTER INDEX IF EXISTS idx_event_deleted RENAME TO idx_church_event_deleted;

-- Update foreign keys in event_space
ALTER TABLE event_space RENAME CONSTRAINT event_space_event_id_fkey TO event_space_church_event_id_fkey;
ALTER TABLE event_space RENAME COLUMN event_id TO church_event_id;
ALTER INDEX IF EXISTS idx_es_event RENAME TO idx_es_church_event;

-- Update foreign keys in event_team
ALTER TABLE event_team RENAME CONSTRAINT event_team_event_id_fkey TO event_team_church_event_id_fkey;
ALTER TABLE event_team RENAME COLUMN event_id TO church_event_id;
ALTER INDEX IF EXISTS idx_et_event RENAME TO idx_et_church_event;

-- Update foreign keys in event_task
ALTER TABLE event_task RENAME CONSTRAINT event_task_event_id_fkey TO event_task_church_event_id_fkey;
ALTER TABLE event_task RENAME COLUMN event_id TO church_event_id;
ALTER INDEX IF EXISTS idx_etk_event RENAME TO idx_etk_church_event;

-- Update foreign keys in event_asset
ALTER TABLE event_asset RENAME CONSTRAINT event_asset_event_id_fkey TO event_asset_church_event_id_fkey;
ALTER TABLE event_asset RENAME COLUMN event_id TO church_event_id;
ALTER INDEX IF EXISTS idx_ea_event RENAME TO idx_ea_church_event;

-- Update foreign keys in event_expense
ALTER TABLE event_expense RENAME CONSTRAINT event_expense_event_id_fkey TO event_expense_church_event_id_fkey;
ALTER TABLE event_expense RENAME COLUMN event_id TO church_event_id;
ALTER INDEX IF EXISTS idx_ee_event RENAME TO idx_ee_church_event;

-- Update foreign keys in event_attendance
ALTER TABLE event_attendance RENAME CONSTRAINT event_attendance_event_id_fkey TO event_attendance_church_event_id_fkey;
ALTER TABLE event_attendance RENAME COLUMN event_id TO church_event_id;
ALTER INDEX IF EXISTS idx_ea_event RENAME TO idx_ea_church_event;

-- Update foreign keys in event_document
ALTER TABLE event_document RENAME CONSTRAINT event_document_event_id_fkey TO event_document_church_event_id_fkey;
ALTER TABLE event_document RENAME COLUMN event_id TO church_event_id;
ALTER INDEX IF EXISTS idx_ed_event RENAME TO idx_ed_church_event;

-- Update foreign keys in event_schedule
ALTER TABLE event_schedule RENAME CONSTRAINT event_schedule_event_id_fkey TO event_schedule_church_event_id_fkey;
ALTER TABLE event_schedule RENAME COLUMN event_id TO church_event_id;
ALTER INDEX IF EXISTS idx_esched_event RENAME TO idx_esched_church_event;

-- Update unique constraints
ALTER TABLE event_space RENAME CONSTRAINT uk_event_space TO uk_church_event_space;

COMMENT ON TABLE church_event IS 'G3.3 : Church OS événement central (nouveau moteur, table church_event pour éviter conflit avec legacy events)';