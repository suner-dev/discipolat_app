-- V15: Performance Optimizations & Security Enhancements
-- Adds missing composite indexes and security-related columns

-- ============================================================
-- COMPOSITE INDEXES for common query patterns
-- ============================================================

-- Maker reports: most queries filter by (faiseur_id, semaine)
CREATE INDEX IF NOT EXISTS idx_maker_reports_faiseur_semaine
    ON maker_reports(faiseur_id, semaine);

-- Maker reports: also (ame_id, semaine) for individual disciple lookups
CREATE INDEX IF NOT EXISTS idx_maker_reports_ame_semaine
    ON maker_reports(ame_id, semaine);

-- Family reports: (famille_id, semaine)
CREATE INDEX IF NOT EXISTS idx_family_reports_famille_semaine
    ON family_reports(famille_id, semaine);

-- Soul history: timeline ordering by (ame_id, created_at)
CREATE INDEX IF NOT EXISTS idx_soul_history_ame_created
    ON soul_history(ame_id, created_at DESC);

-- Souls: filter by faiseur + statut (disciples listing)
CREATE INDEX IF NOT EXISTS idx_souls_faiseur_statut
    ON souls(faiseur_id, statut);

-- Souls: filter by famille + statut
CREATE INDEX IF NOT EXISTS idx_souls_famille_statut
    ON souls(famille_id, statut);

-- Notifications: unread count queries
CREATE INDEX IF NOT EXISTS idx_notifications_destinataire_lu
    ON notifications(destinataire_id, lu);

-- User roles: role lookup
CREATE INDEX IF NOT EXISTS idx_user_roles_user_role
    ON user_roles(user_id, role);

-- Soul notes: latest first for feed queries
CREATE INDEX IF NOT EXISTS idx_soul_notes_ame_date
    ON soul_notes(ame_id, created_at DESC);

-- Evaluations: score lookups by user (column is evalue_id per V11)
CREATE INDEX IF NOT EXISTS idx_evaluations_evalue_created
    ON evaluations(evalue_id, created_at DESC);

-- Dashboard metrics: type + periode
CREATE INDEX IF NOT EXISTS idx_dashboard_metrics_type_periode
    ON dashboard_metrics(type_metrique, periode);

-- Parallel followups: active lookups
CREATE INDEX IF NOT EXISTS idx_parallel_followups_ame_statut
    ON parallel_followups(ame_id, statut);

-- ============================================================
-- SECURITY ENHANCEMENTS
-- ============================================================

-- Add last_login_at for security auditing (if not exists)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

-- Add password_changed_at for password rotation tracking
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add refresh_token column to support explicit token revocation
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS refresh_token_hash VARCHAR(255);

-- ============================================================
-- PERFORMANCE: ANALYZE TABLES
-- ============================================================
ANALYZE;
