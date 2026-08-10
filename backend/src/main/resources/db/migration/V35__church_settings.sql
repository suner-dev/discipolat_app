-- V35__church_settings.sql
-- ============================================================
-- PARAMÈTRES D'IDENTITÉ ET DE MARQUE DE L'ÉGLISE
-- Configuration sans code : l'administrateur personnalise le nom,
-- le slogan, le logo, les couleurs, la typographie et les contacts
-- via l'interface d'administration. Le thème est appliqué dynamiquement.
-- ============================================================

CREATE TABLE IF NOT EXISTS church_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    church_name VARCHAR(255) NOT NULL DEFAULT 'Discipolat',
    platform_name VARCHAR(255) NOT NULL DEFAULT 'Discipolat',
    slogan VARCHAR(255),
    description TEXT,
    logo_url VARCHAR(500),
    favicon_url VARCHAR(500),
    banner_url VARCHAR(500),
    primary_color VARCHAR(20) NOT NULL DEFAULT '#16a34a',
    accent_color VARCHAR(20) NOT NULL DEFAULT '#f59e0b',
    button_color VARCHAR(20) NOT NULL DEFAULT '#16a34a',
    font_family VARCHAR(100) NOT NULL DEFAULT 'Inter',
    allow_dark_mode BOOLEAN NOT NULL DEFAULT TRUE,
    address VARCHAR(500),
    phone VARCHAR(50),
    email VARCHAR(255),
    website VARCHAR(255),
    social_links JSONB NOT NULL DEFAULT '{}'::jsonb,
    contact_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Ligne par défaut (id stable : settings-0001)
INSERT INTO church_settings (id, church_name, platform_name, slogan, description,
                             primary_color, accent_color, button_color)
VALUES ('00000000-0000-0000-0000-00000000c001', 'Discipolat', 'Discipolat',
        'Former des disciples de Jésus-Christ',
        'Plateforme de gestion du discipolat',
        '#16a34a', '#f59e0b', '#16a34a');

COMMENT ON TABLE church_settings IS 'Identité & marque de l''église (nom, logo, couleurs, contacts) — configurable sans code par l''administrateur.';
