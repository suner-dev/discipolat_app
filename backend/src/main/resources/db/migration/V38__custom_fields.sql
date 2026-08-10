-- V38__custom_fields.sql
-- ============================================================
-- CHAMPS PERSONNALISABLES PAR LES ADMINISTRATEURS
-- Sans modifier le code, l'administrateur ajoute des champs aux
-- entités (âme, utilisateur, département, famille) avec un type,
-- une obligation, des rôles visibles/éditables et les valeurs stockées.
-- ============================================================

CREATE TABLE IF NOT EXISTS custom_field_definitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entite_type VARCHAR(50) NOT NULL
        CHECK (entite_type IN ('SOUL', 'USER', 'DEPARTMENT', 'FAMILY')),
    code VARCHAR(100) NOT NULL,
    label VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL
        CHECK (type IN ('TEXTE', 'NOMBRE', 'DATE', 'DATE_HEURE', 'BOOLEEN',
                        'SELECTION', 'SELECTION_MULTIPLE', 'FICHIER', 'IMAGE',
                        'TELEPHONE', 'EMAIL', 'URL', 'TEXTAREA')),
    obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    ordre INTEGER NOT NULL DEFAULT 0,
    options JSONB,
    placeholder VARCHAR(255),
    default_value VARCHAR(500),
    roles_lecture JSONB NOT NULL DEFAULT '[]'::jsonb,
    roles_ecriture JSONB NOT NULL DEFAULT '[]'::jsonb,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (entite_type, code)
);

CREATE TABLE IF NOT EXISTS custom_field_values (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entite_type VARCHAR(50) NOT NULL,
    entite_id UUID NOT NULL,
    field_id UUID NOT NULL REFERENCES custom_field_definitions(id),
    value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (entite_type, entite_id, field_id)
);

CREATE INDEX IF NOT EXISTS idx_cfv_entity ON custom_field_values(entite_type, entite_id);
CREATE INDEX IF NOT EXISTS idx_cfv_field ON custom_field_values(field_id);

COMMENT ON TABLE custom_field_definitions IS 'Définitions de champs personnalisés configurés par l''administrateur pour chaque entité métier.';
COMMENT ON TABLE custom_field_values IS 'Valeurs des champs personnalisés pour chaque entité.';