-- V71__notification_templates.sql
-- ============================================================
-- MODÈLES DE NOTIFICATION CONFIGURABLES (centre de configuration admin)
-- ============================================================
CREATE TABLE IF NOT EXISTS notification_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    event VARCHAR(60) NOT NULL,
    titre VARCHAR(255),
    message TEXT,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_template_tenant_event UNIQUE (tenant_id, event)
);

-- Canaux de diffusion d'un modèle
CREATE TABLE IF NOT EXISTS notification_template_channels (
    template_id UUID NOT NULL REFERENCES notification_templates(id) ON DELETE CASCADE,
    canal VARCHAR(20) NOT NULL
);

-- Rôles destinataires recommandés d'un modèle
CREATE TABLE IF NOT EXISTS notification_template_roles (
    template_id UUID NOT NULL REFERENCES notification_templates(id) ON DELETE CASCADE,
    role VARCHAR(40) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notification_templates_tenant ON notification_templates(tenant_id);
CREATE INDEX IF NOT EXISTS idx_notification_templates_tenant_event ON notification_templates(tenant_id, event, actif);
