-- V40__beta_testing_and_feedback.sql
-- ============================================================
-- BETA-TESTING : RETOURS DES TESTEURS
-- Collecte des retours (bugs, suggestions, UX, performance...)
-- envoyes par les testeurs via le widget integre a l'application.
-- Chaque retour est rattache a l'utilisateur connecte (created_by),
-- sans stocker d'autres donnees personnelles que le strict necessaire.
-- ============================================================

CREATE TABLE IF NOT EXISTS feedbacks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MOYENNE',
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    page_url VARCHAR(500),
    user_agent VARCHAR(500),
    browser VARCHAR(200),
    device VARCHAR(200),
    os VARCHAR(200),
    app_version VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'NOUVEAU',
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_feedbacks_status ON feedbacks (status);
CREATE INDEX IF NOT EXISTS idx_feedbacks_created_at ON feedbacks (created_at);

COMMENT ON TABLE feedbacks IS 'Retours des testeurs (bugs, suggestions, UX) — collectes via le widget de feedback du beta-testing.';
