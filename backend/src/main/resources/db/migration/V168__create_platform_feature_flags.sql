-- Platform Feature Flags for Super Admin
CREATE TABLE IF NOT EXISTS platform_feature_flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_name VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    category VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_platform_feature_flags_category ON platform_feature_flags(category);
CREATE INDEX IF NOT EXISTS idx_platform_feature_flags_enabled ON platform_feature_flags(is_enabled);

-- Seed default feature flags
INSERT INTO platform_feature_flags (key_name, name, description, is_enabled, category) VALUES
('aiEnabled', 'Intelligence Artificielle', 'Fonctionnalités IA (sermons, conseils, etc.)', TRUE, 'AI'),
('mobileMoneyEnabled', 'Mobile Money', 'Paiements Mobile Money (Orange Money, MTN MoMo, etc.)', TRUE, 'PAYMENTS'),
('whatsappEnabled', 'WhatsApp Business', 'Notifications et communication via WhatsApp', TRUE, 'COMMUNICATION'),
('analyticsEnabled', 'Analytics Avancés', 'Tableaux de bord et rapports avancés', TRUE, 'ANALYTICS'),
('docsEnabled', 'Documentation', 'Accès à la documentation intégrée', TRUE, 'CORE')
ON CONFLICT (key_name) DO NOTHING;