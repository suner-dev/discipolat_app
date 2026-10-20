-- V6__additional_enhancements.sql
-- US-06, US-28, US-58 enhancements

-- ============================================================
-- US-06: Unique constraint on family name
-- ============================================================
ALTER TABLE families ADD CONSTRAINT uk_families_nom UNIQUE (nom);

-- ============================================================
-- US-28: Multi-select absence reasons (JSONB instead of single enum)
-- ============================================================
ALTER TABLE maker_reports ADD COLUMN IF NOT EXISTS absences_multi JSONB;

-- ============================================================
-- US-58: Permission matrix
-- ============================================================
CREATE TABLE IF NOT EXISTS role_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role VARCHAR(50) NOT NULL,
    permission VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role, permission)
);

-- Default permissions for each role
INSERT INTO role_permissions (role, permission, enabled) VALUES
    ('ADMIN', 'USER_CREATE', true),
    ('ADMIN', 'USER_READ', true),
    ('ADMIN', 'USER_UPDATE', true),
    ('ADMIN', 'USER_DELETE', true),
    ('ADMIN', 'FAMILY_CREATE', true),
    ('ADMIN', 'FAMILY_READ', true),
    ('ADMIN', 'FAMILY_UPDATE', true),
    ('ADMIN', 'FAMILY_DELETE', true),
    ('ADMIN', 'REPORT_CORRECT', true),
    ('ADMIN', 'REPORT_EXPORT', true),
    ('ADMIN', 'AUDIT_READ', true),
    ('ADMIN', 'BULK_IMPORT', true),
    ('ADMIN', 'PERMISSION_MANAGE', true),
    ('PASTEUR', 'USER_CREATE', true),
    ('PASTEUR', 'USER_READ', true),
    ('PASTEUR', 'USER_UPDATE', true),
    ('PASTEUR', 'FAMILY_CREATE', true),
    ('PASTEUR', 'FAMILY_READ', true),
    ('PASTEUR', 'FAMILY_UPDATE', true),
    ('PASTEUR', 'FAMILY_DELETE', true),
    ('PASTEUR', 'REPORT_READ', true),
    ('PASTEUR', 'REPORT_EXPORT', true),
    ('PASTEUR', 'AUDIT_READ', true),
    ('PASTEUR', 'BULK_IMPORT', true),
    ('RESPONSABLE', 'USER_READ', true),
    ('RESPONSABLE', 'FAMILY_READ', true),
    ('RESPONSABLE', 'REPORT_READ', true),
    ('RESPONSABLE', 'REPORT_EXPORT', true),
    ('FAISEUR', 'USER_READ', true),
    ('FAISEUR', 'SOUL_CREATE', true),
    ('FAISEUR', 'SOUL_UPDATE', true),
    ('FAISEUR', 'REPORT_CREATE', true),
    ('FAISEUR', 'REPORT_READ', true)
ON CONFLICT (role, permission) DO NOTHING;
