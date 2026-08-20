-- V73__create_integration_config.sql
-- Table pour stocker les configurations d'intégrations externes (SMTP, stockage, JWT, rate-limiting)

CREATE TABLE integration_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category VARCHAR(50) NOT NULL UNIQUE,
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_integration_config_category ON integration_config(category);
