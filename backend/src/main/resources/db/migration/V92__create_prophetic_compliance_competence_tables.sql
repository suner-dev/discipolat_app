-- V92: Create tables for new modules
-- Prophetic Journal, Compliance Manager, Member Competence Matching

-- Prophetic Journal
CREATE TABLE IF NOT EXISTS prophetic_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    author_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    tags VARCHAR(500),
    scope VARCHAR(100),
    related_soul_id UUID,
    related_family_id UUID,
    related_department_id UUID,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_prophetic_entries_tenant ON prophetic_entries(tenant_id);
CREATE INDEX idx_prophetic_entries_author ON prophetic_entries(author_id);
CREATE INDEX idx_prophetic_entries_type ON prophetic_entries(type);

-- Member Competences
CREATE TABLE IF NOT EXISTS member_competences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    competence_name VARCHAR(255) NOT NULL,
    category VARCHAR(50),
    level INTEGER NOT NULL DEFAULT 1,
    interest_level INTEGER NOT NULL DEFAULT 1,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_member_competences_tenant ON member_competences(tenant_id);
CREATE INDEX idx_member_competences_user ON member_competences(user_id);
CREATE INDEX idx_member_competences_category ON member_competences(category);
CREATE INDEX idx_member_competences_name ON member_competences(competence_name);
