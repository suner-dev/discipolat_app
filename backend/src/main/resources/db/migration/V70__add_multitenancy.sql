-- V70__add_multitenancy.sql
-- ============================================================
-- MULTI-TENANCY: Add tenant isolation to all tables
-- ============================================================

-- 1. Create tenants table
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED', 'PENDING_SETUP')),
    plan VARCHAR(50) NOT NULL DEFAULT 'free',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create default tenant for existing data
INSERT INTO tenants (id, name, slug, status, plan)
VALUES ('00000000-0000-0000-0000-000000000001', 'Discipolat (Default)', 'default', 'ACTIVE', 'free')
ON CONFLICT (slug) DO NOTHING;

-- 3. Add tenant_id to all business tables (nullable first for data migration)
-- Core entities
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE souls ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE families ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE departments ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Reports
ALTER TABLE maker_reports ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE family_reports ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Followups & alerts
ALTER TABLE parallel_followups ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Audit & history
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE soul_history ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Events & programs
ALTER TABLE events ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE event_registrations ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE weekly_program_templates ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE culte_config ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Files & communications
ALTER TABLE files ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE entity_attachments ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE communications ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Soul details
ALTER TABLE soul_notes ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE soul_tags ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE soul_exits ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE soul_retraction_requests ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE soul_departments ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE soul_interactions ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE spiritual_score_history ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Discipline
ALTER TABLE soul_discipline_events ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Members
ALTER TABLE member_presences ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE member_requests ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE member_departments ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Messages & conversations
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE conversation_messages ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Evaluations
ALTER TABLE evaluations ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Evangelism
ALTER TABLE evangelism_track ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE evangelism_stage_history ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Objectives
ALTER TABLE objectives ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Visits
ALTER TABLE visits ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Prayers
ALTER TABLE prayers ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Appointments
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Badges
ALTER TABLE badges ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE user_badges ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Trainings
ALTER TABLE courses ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE course_modules ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE course_enrollments ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE module_completions ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE certificates ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Favorites
ALTER TABLE favorites ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Transfers
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE transfer_decisions ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE transfer_history ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE transfer_attachments ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE transfer_workflow_configs ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE transfer_workflow_steps ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Department management
ALTER TABLE department_teams ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_positions ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_assignments ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_tasks ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_activity ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_member_notes ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_announcements ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_announcement_members ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_member_objectives ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_member_reports ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_reports ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_checklists ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_checklist_items ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_equipment ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_documents ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_settings ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_event_attendance ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Platform / config
ALTER TABLE church_settings ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE platform_modules ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE menu_entries ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE custom_pages ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE dictionary_entries ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE custom_field_definitions ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE custom_field_values ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE platform_roles ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE permission_catalog ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE config_revisions ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Finances
ALTER TABLE finance_transactions ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE finance_budgets ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Dashboard
ALTER TABLE dashboard_metrics ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Role & department relations
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE user_departments ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE role_permissions ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE family_department_history ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE role_history ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE department_history ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE family_history ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE family_risk_history ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE family_chief_history ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE faiseur_transfers ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE report_corrections ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Program types
ALTER TABLE program_types ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE program_sub_types ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Auth tokens (linked to user, but add tenant for consistency)
ALTER TABLE activation_tokens ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);
ALTER TABLE password_reset_tokens ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- Feedbacks
ALTER TABLE feedbacks ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id);

-- 4. Backfill existing data with default tenant
UPDATE users SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE souls SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE families SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE departments SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE maker_reports SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE family_reports SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE parallel_followups SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE alerts SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE notifications SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE audit_logs SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE soul_history SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE events SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE event_registrations SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE weekly_program_templates SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE culte_config SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE files SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE entity_attachments SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE communications SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE soul_notes SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE soul_tags SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE soul_exits SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE soul_retraction_requests SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE soul_departments SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE soul_interactions SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE spiritual_score_history SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE soul_discipline_events SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE member_presences SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE member_requests SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE member_departments SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE conversations SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE conversation_messages SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE evaluations SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE evangelism_track SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE evangelism_stage_history SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE objectives SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE visits SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE prayers SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE appointments SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE badges SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE user_badges SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE courses SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE course_modules SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE quiz_questions SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE course_enrollments SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE module_completions SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE certificates SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE favorites SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE transfer_requests SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE transfer_decisions SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE transfer_history SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE transfer_attachments SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE transfer_workflow_configs SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE transfer_workflow_steps SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_teams SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_positions SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_assignments SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_tasks SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_activity SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_member_notes SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_announcements SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_announcement_members SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_member_objectives SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_member_reports SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_reports SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_checklists SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_checklist_items SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_equipment SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_documents SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_settings SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_event_attendance SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE church_settings SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE platform_modules SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE menu_entries SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE custom_pages SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE dictionary_entries SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE custom_field_definitions SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE custom_field_values SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE platform_roles SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE permission_catalog SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE config_revisions SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE finance_transactions SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE finance_budgets SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE dashboard_metrics SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE user_roles SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE user_departments SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE role_permissions SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE family_department_history SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE role_history SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE department_history SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE family_history SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE family_risk_history SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE family_chief_history SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE faiseur_transfers SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE report_corrections SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE program_types SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE program_sub_types SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE activation_tokens SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE password_reset_tokens SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE feedbacks SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;

-- 5. Make tenant_id NOT NULL (after data migration)
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE souls ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE families ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE departments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE maker_reports ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE family_reports ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE parallel_followups ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE alerts ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE notifications ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE soul_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE events ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE event_registrations ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE weekly_program_templates ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE culte_config ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE files ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE entity_attachments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE communications ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE soul_notes ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE soul_tags ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE soul_exits ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE soul_retraction_requests ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE soul_departments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE soul_interactions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE spiritual_score_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE soul_discipline_events ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE member_presences ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE member_requests ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE member_departments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE conversations ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE conversation_messages ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE evaluations ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE evangelism_track ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE evangelism_stage_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE objectives ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE visits ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE prayers ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE appointments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE badges ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE user_badges ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE courses ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE course_modules ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE quiz_questions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE course_enrollments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE module_completions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE certificates ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE favorites ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE transfer_requests ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE transfer_decisions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE transfer_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE transfer_attachments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE transfer_workflow_configs ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE transfer_workflow_steps ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_teams ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_positions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_assignments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_tasks ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_activity ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_member_notes ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_announcements ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_announcement_members ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_member_objectives ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_member_reports ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_reports ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_checklists ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_checklist_items ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_equipment ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_documents ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_settings ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_event_attendance ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE church_settings ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE platform_modules ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE menu_entries ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE custom_pages ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE dictionary_entries ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE custom_field_definitions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE custom_field_values ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE platform_roles ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE permission_catalog ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE config_revisions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE finance_transactions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE finance_budgets ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE dashboard_metrics ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE user_departments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE family_department_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE role_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE department_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE family_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE family_risk_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE family_chief_history ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE faiseur_transfers ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE report_corrections ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE program_types ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE program_sub_types ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE activation_tokens ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE password_reset_tokens ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE feedbacks ALTER COLUMN tenant_id SET NOT NULL;

-- 6. Add composite indexes for tenant-aware queries (performance)
CREATE INDEX IF NOT EXISTS idx_users_tenant ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_tenant_email ON users(tenant_id, email);
CREATE INDEX IF NOT EXISTS idx_souls_tenant ON souls(tenant_id);
CREATE INDEX IF NOT EXISTS idx_souls_tenant_faiseur ON souls(tenant_id, faiseur_id);
CREATE INDEX IF NOT EXISTS idx_souls_tenant_famille ON souls(tenant_id, famille_id);
CREATE INDEX IF NOT EXISTS idx_families_tenant ON families(tenant_id);
CREATE INDEX IF NOT EXISTS idx_departments_tenant ON departments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_events_tenant ON events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_maker_reports_tenant ON maker_reports(tenant_id);
CREATE INDEX IF NOT EXISTS idx_family_reports_tenant ON family_reports(tenant_id);
CREATE INDEX IF NOT EXISTS idx_notifications_tenant ON notifications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_dest ON notifications(tenant_id, destinataire_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant ON audit_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_alerts_tenant ON alerts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_soul_departments_tenant ON soul_departments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_department_teams_tenant ON department_teams(tenant_id);
CREATE INDEX IF NOT EXISTS idx_department_tasks_tenant ON department_tasks(tenant_id);
CREATE INDEX IF NOT EXISTS idx_prayers_tenant ON prayers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_files_tenant ON files(tenant_id);
CREATE INDEX IF NOT EXISTS idx_communications_tenant ON communications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_finance_transactions_tenant ON finance_transactions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_finance_budgets_tenant ON finance_budgets(tenant_id);
CREATE INDEX IF NOT EXISTS idx_transfer_requests_tenant ON transfer_requests(tenant_id);
CREATE INDEX IF NOT EXISTS idx_church_settings_tenant ON church_settings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_platform_modules_tenant ON platform_modules(tenant_id);
CREATE INDEX IF NOT EXISTS idx_menu_entries_tenant ON menu_entries(tenant_id);
CREATE INDEX IF NOT EXISTS idx_custom_pages_tenant ON custom_pages(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dictionary_entries_tenant ON dictionary_entries(tenant_id);
CREATE INDEX IF NOT EXISTS idx_custom_field_definitions_tenant ON custom_field_definitions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_custom_field_values_tenant ON custom_field_values(tenant_id);
CREATE INDEX IF NOT EXISTS idx_config_revisions_tenant ON config_revisions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dashboard_metrics_tenant ON dashboard_metrics(tenant_id);
CREATE INDEX IF NOT EXISTS idx_conversations_tenant ON conversations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_courses_tenant ON courses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_visits_tenant ON visits(tenant_id);
CREATE INDEX IF NOT EXISTS idx_appointments_tenant ON appointments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_tenant ON evaluations(tenant_id);

-- 7. Add unique constraint: email must be unique per tenant (not globally)
-- First drop the global unique constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
-- Add composite unique constraint
ALTER TABLE users ADD CONSTRAINT uk_users_tenant_email UNIQUE (tenant_id, email);
