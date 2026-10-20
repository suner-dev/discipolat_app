-- V141__create_tenant_settings_table.sql
-- ============================================================
-- TENANT SETTINGS & BRANDING (G1.2 - §27-28)
-- Table complete pour configuration tenant avec branding dynamique
-- ============================================================

-- 1. Table tenant_settings (configuration complete par tenant)
CREATE TABLE tenant_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    
    -- Identite commerciale
    business_name VARCHAR(255),              -- Nom commercial affiche partout (remplace name si defini)
    slogan VARCHAR(500),                     -- Slogan/accroche
    legal_name VARCHAR(255),                 -- Nom legal
    description TEXT,                        -- Description tenant
    
    -- Branding visuel
    logo_url VARCHAR(1000),                  -- Logo principal
    logo_dark_url VARCHAR(1000),             -- Logo mode sombre
    cover_url VARCHAR(1000),                 -- Image de couverture/banniere
    favicon_url VARCHAR(1000),               -- Favicon
    
    -- Couleurs (tokens CSS --brand-*)
    primary_color VARCHAR(7) DEFAULT '#6366F1',
    secondary_color VARCHAR(7) DEFAULT '#8B5CF6',
    accent_color VARCHAR(7) DEFAULT '#EC4899',
    surface_color VARCHAR(7) DEFAULT '#FFFFFF',
    background_color VARCHAR(7) DEFAULT '#F8FAFC',
    text_primary_color VARCHAR(7) DEFAULT '#1E293B',
    text_secondary_color VARCHAR(7) DEFAULT '#64748B',
    success_color VARCHAR(7) DEFAULT '#10B981',
    warning_color VARCHAR(7) DEFAULT '#F59E0B',
    error_color VARCHAR(7) DEFAULT '#EF4444',
    info_color VARCHAR(7) DEFAULT '#3B82F6',
    
    -- Polices
    primary_font VARCHAR(100) DEFAULT 'Inter',
    secondary_font VARCHAR(100) DEFAULT 'Inter',
    heading_font VARCHAR(100) DEFAULT 'Inter',
    mono_font VARCHAR(100) DEFAULT 'JetBrains Mono',
    
    -- Localisation
    locale VARCHAR(10) DEFAULT 'fr',         -- Langue par defaut
    supported_locales JSONB DEFAULT '["fr", "en"]'::jsonb,
    timezone VARCHAR(64) DEFAULT 'Africa/Douala',
    country VARCHAR(2) DEFAULT 'CM',
    city VARCHAR(100),
    currency VARCHAR(3) DEFAULT 'XAF',
    date_format VARCHAR(20) DEFAULT 'dd/MM/yyyy',
    time_format VARCHAR(10) DEFAULT 'HH:mm',
    datetime_format VARCHAR(30) DEFAULT 'dd/MM/yyyy HH:mm',
    phone_country_code VARCHAR(10) DEFAULT '+237',
    week_start_day INTEGER DEFAULT 1,        -- 1=Lundi, 7=Dimanche
    
    -- Contact
    email VARCHAR(255),                      -- Email contact principal
    phone VARCHAR(50),                       -- Telephone contact
    website VARCHAR(255),                    -- Site web
    address TEXT,                            -- Adresse complete
    opening_hours JSONB DEFAULT '{}'::jsonb, -- Horaires d'ouverture par jour
    working_days JSONB DEFAULT '["Monday","Tuesday","Wednesday","Thursday","Friday"]'::jsonb,
    
    -- Textes d'invitation / communication
    invitation_email_subject VARCHAR(255) DEFAULT 'Invitation a rejoindre {{tenant_name}} sur Discipolat',
    invitation_email_body TEXT DEFAULT 'Bonjour,<br><br>Vous etes invite a rejoindre <strong>{{tenant_name}}</strong> sur Discipolat. Cliquez sur le lien ci-dessous pour accepter :<br><br><a href="{{invitation_link}}">{{invitation_link}}</a><br><br>Ce lien expire dans 72h.<br><br>Cordialement,<br>L''equipe {{tenant_name}}',
    welcome_email_subject VARCHAR(255) DEFAULT 'Bienvenue sur {{tenant_name}} !',
    welcome_email_body TEXT DEFAULT 'Bonjour {{first_name}},<br><br>Bienvenue dans <strong>{{tenant_name}}</strong> ! Votre compte a ete cree avec succes.<br><br>Connectez-vous ici : <a href="{{login_url}}">{{login_url}}</a><br><br>A bientot,<br>L''equipe {{tenant_name}}',
    footer_text TEXT DEFAULT '© {{year}} {{tenant_name}}. Tous droits reserves.',
    footer_links JSONB DEFAULT '[]'::jsonb,  -- Liens pied de page [{"label":"Mentions legales","url":"..."}]
    
    -- Feature flags / Toggles modules & canaux (activables par tenant)
    low_band_enabled BOOLEAN DEFAULT FALSE,        -- WhatsApp/USSD
    public_directory_enabled BOOLEAN DEFAULT FALSE, -- Annuaire public "Eglises sur Discipolat"
    legacy_migration_enabled BOOLEAN DEFAULT FALSE, -- Migration donnees legacy
    offline_mode VARCHAR(20) DEFAULT 'LECTURE' CHECK (offline_mode IN ('LECTURE', 'FIELD_OPS', 'FULL')),
    analytics_enabled BOOLEAN DEFAULT TRUE,
    ai_features_enabled BOOLEAN DEFAULT TRUE,
    chat_enabled BOOLEAN DEFAULT TRUE,
    academy_enabled BOOLEAN DEFAULT FALSE,
    marketplace_enabled BOOLEAN DEFAULT FALSE,
    api_access_enabled BOOLEAN DEFAULT FALSE,
    custom_domain_enabled BOOLEAN DEFAULT FALSE,
    sso_enabled BOOLEAN DEFAULT FALSE,
    two_factor_required BOOLEAN DEFAULT FALSE,
    password_policy_enabled BOOLEAN DEFAULT TRUE,
    session_timeout_minutes INTEGER DEFAULT 60,
    max_failed_login_attempts INTEGER DEFAULT 5,
    lockout_duration_minutes INTEGER DEFAULT 30,
    
    -- Configuration avancee (JSON flexible)
    ui_config JSONB DEFAULT '{}'::jsonb,        -- Config UI : theme, layout, widgets par defaut
    notification_rules JSONB DEFAULT '{}'::jsonb, -- Regles notification par evenement
    integration_config JSONB DEFAULT '{}'::jsonb, -- Config integrations (Stripe, MoMo, SMTP, etc.)
    custom_css TEXT,                            -- CSS personnalise (injecte dans head)
    custom_head_html TEXT,                      -- HTML injecte dans <head>
    
    -- Metadonnees
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1,
    
    CONSTRAINT uk_tenant_settings_tenant UNIQUE (tenant_id)
);

CREATE INDEX idx_tenant_settings_tenant ON tenant_settings(tenant_id);
CREATE INDEX idx_tenant_settings_business_name ON tenant_settings(business_name);

-- 2. Trigger updated_at
DROP TRIGGER IF EXISTS update_tenant_settings_updated_at ON tenant_settings;
CREATE TRIGGER update_tenant_settings_updated_at BEFORE UPDATE ON tenant_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 3. Creer tenant_settings par defaut pour tous les tenants existants
INSERT INTO tenant_settings (
    tenant_id,
    business_name,
    primary_color,
    secondary_color,
    accent_color,
    surface_color,
    background_color,
    text_primary_color,
    text_secondary_color,
    success_color,
    warning_color,
    error_color,
    info_color,
    primary_font,
    secondary_font,
    heading_font,
    mono_font,
    locale,
    timezone,
    country,
    currency,
    date_format,
    phone_country_code,
    created_by,
    updated_by
)
SELECT 
    id,
    NULL,
    '#6366F1',
    '#8B5CF6',
    '#EC4899',
    '#FFFFFF',
    '#F8FAFC',
    '#1E293B',
    '#64748B',
    '#10B981',
    '#F59E0B',
    '#EF4444',
    '#3B82F6',
    'Inter',
    'Inter',
    'Inter',
    'JetBrains Mono',
    'fr',
    'Africa/Douala',
    'CM',
    'XAF',
    'dd/MM/yyyy',
    '+237',
    (SELECT id FROM users WHERE tenant_id = tenants.id LIMIT 1),
    (SELECT id FROM users WHERE tenant_id = tenants.id LIMIT 1)
FROM tenants
WHERE id NOT IN (SELECT tenant_id FROM tenant_settings WHERE tenant_id IS NOT NULL)
ON CONFLICT (tenant_id) DO NOTHING;

-- 4. Vue pour branding resolu (avec heritage eventuel futur)
CREATE OR REPLACE VIEW tenant_branding_resolved AS
SELECT 
    ts.tenant_id,
    COALESCE(ts.business_name, t.name) AS display_name,
    ts.slogan,
    ts.logo_url,
    ts.logo_dark_url,
    ts.cover_url,
    ts.favicon_url,
    ts.primary_color,
    ts.secondary_color,
    ts.accent_color,
    ts.surface_color,
    ts.background_color,
    ts.text_primary_color,
    ts.text_secondary_color,
    ts.success_color,
    ts.warning_color,
    ts.error_color,
    ts.info_color,
    ts.primary_font,
    ts.secondary_font,
    ts.heading_font,
    ts.mono_font,
    ts.custom_css,
    ts.custom_head_html
FROM tenant_settings ts
JOIN tenants t ON t.id = ts.tenant_id;

-- 5. Fonction pour generer les variables CSS --brand-*
CREATE OR REPLACE FUNCTION generate_branding_css(tenant_uuid UUID) RETURNS TEXT AS $$
DECLARE
    ts RECORD;
    css TEXT := '';
BEGIN
    SELECT * INTO ts FROM tenant_settings WHERE tenant_id = tenant_uuid;
    IF ts IS NULL THEN
        RETURN '';
    END IF;
    
    css := css || ':root {';
    css := css || '  --brand-primary: ' || COALESCE(ts.primary_color, '#6366F1') || ';';
    css := css || '  --brand-secondary: ' || COALESCE(ts.secondary_color, '#8B5CF6') || ';';
    css := css || '  --brand-accent: ' || COALESCE(ts.accent_color, '#EC4899') || ';';
    css := css || '  --brand-surface: ' || COALESCE(ts.surface_color, '#FFFFFF') || ';';
    css := css || '  --brand-background: ' || COALESCE(ts.background_color, '#F8FAFC') || ';';
    css := css || '  --brand-text-primary: ' || COALESCE(ts.text_primary_color, '#1E293B') || ';';
    css := css || '  --brand-text-secondary: ' || COALESCE(ts.text_secondary_color, '#64748B') || ';';
    css := css || '  --brand-success: ' || COALESCE(ts.success_color, '#10B981') || ';';
    css := css || '  --brand-warning: ' || COALESCE(ts.warning_color, '#F59E0B') || ';';
    css := css || '  --brand-error: ' || COALESCE(ts.error_color, '#EF4444') || ';';
    css := css || '  --brand-info: ' || COALESCE(ts.info_color, '#3B82F6') || ';';
    css := css || '  --brand-font-primary: "' || COALESCE(ts.primary_font, 'Inter') || '", sans-serif;';
    css := css || '  --brand-font-secondary: "' || COALESCE(ts.secondary_font, 'Inter') || '", sans-serif;';
    css := css || '  --brand-font-heading: "' || COALESCE(ts.heading_font, 'Inter') || '", sans-serif;';
    css := css || '  --brand-font-mono: "' || COALESCE(ts.mono_font, 'JetBrains Mono') || '", monospace;';
    css := css || '  --brand-logo-url: url("' || COALESCE(ts.logo_url, '') || '");';
    css := css || '  --brand-logo-dark-url: url("' || COALESCE(ts.logo_dark_url, '') || '");';
    css := css || '  --brand-cover-url: url("' || COALESCE(ts.cover_url, '') || '");';
    css := css || '}';
    css := css || '@media (prefers-color-scheme: dark) {';
    css := css || '  :root { --brand-logo-url: url("' || COALESCE(ts.logo_dark_url, ts.logo_url, '') || '"); }';
    css := css || '}';
    IF ts.custom_css IS NOT NULL AND ts.custom_css != '' THEN
        css := css || ts.custom_css;
    END IF;
    RETURN css;
END;
$$ LANGUAGE plpgsql;