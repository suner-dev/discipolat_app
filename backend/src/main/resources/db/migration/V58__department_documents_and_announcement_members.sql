-- ============================================================
-- V58 — Documentation du département (procédures, guides,
--       formulaires, comptes rendus, ressources) + annonces
--       ciblées à certains membres
-- ============================================================

-- Documents du département. type :
-- PROCEDURE | GUIDE | DOCUMENT | FORMULAIRE | COMPTE_RENDU | RESSOURCE
-- statut : ACTIF | ARCHIVE
CREATE TABLE IF NOT EXISTS department_documents (
    id            UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    titre         VARCHAR(255) NOT NULL,
    type          VARCHAR(30) NOT NULL,
    description   TEXT,
    url           VARCHAR(1000),
    statut        VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
    created_by    UUID,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dept_documents_dept ON department_documents(department_id);
CREATE INDEX IF NOT EXISTS idx_dept_documents_type ON department_documents(department_id, type);

-- Cibles « certains membres » des annonces du département.
CREATE TABLE IF NOT EXISTS department_announcement_members (
    announcement_id UUID NOT NULL REFERENCES department_announcements(id) ON DELETE CASCADE,
    member_id       UUID NOT NULL,
    PRIMARY KEY (announcement_id, member_id)
);

CREATE INDEX IF NOT EXISTS idx_dept_announcement_members ON department_announcement_members(announcement_id);
