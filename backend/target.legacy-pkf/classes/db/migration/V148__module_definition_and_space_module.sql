-- V148__module_definition_and_space_module.sql
-- ============================================================
-- G2.2 — Module Engine & catalogue de modules
-- module_definition : catalogue global de tous les modules (~130)
-- space_module : relation polymorphe espace ↔ module (remplace department_module)
-- ============================================================

-- 1. module_definition : catalogue global
CREATE TABLE IF NOT EXISTS module_definition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL DEFAULT 'GENERAL',
    version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    icon VARCHAR(100),
    source VARCHAR(20) NOT NULL DEFAULT 'CORE' CHECK (source IN ('CORE', 'EXISTING', 'ENGINE')),
    display_order INTEGER NOT NULL DEFAULT 0,
    features_json JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_module_def_code ON module_definition(code);
CREATE INDEX IF NOT EXISTS idx_module_def_category ON module_definition(category);
CREATE INDEX IF NOT EXISTS idx_module_def_source ON module_definition(source);
CREATE INDEX IF NOT EXISTS idx_module_def_enabled ON module_definition(enabled);

COMMENT ON TABLE module_definition IS 'G2.2 : Catalogue global des modules (CORE | EXISTING | ENGINE)';
COMMENT ON COLUMN module_definition.source IS 'CORE = modules noyau (people, org, events...), EXISTING = modules legacy (~130), ENGINE = nouveaux moteurs (config, workflow, custom_fields...)';

-- 2. space_module : relation polymorphe espace ↔ module
-- Remplace l''ancien department_module, applicable a toute organization_unit
CREATE TABLE IF NOT EXISTS space_module (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    space_id UUID NOT NULL REFERENCES organization_nodes(id) ON DELETE CASCADE,
    module_code VARCHAR(50) NOT NULL REFERENCES module_definition(code) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configuration_json JSONB DEFAULT '{}'::jsonb,
    limits_json JSONB DEFAULT '{}'::jsonb,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, space_id, module_code)
);

CREATE INDEX IF NOT EXISTS idx_space_module_tenant ON space_module(tenant_id);
CREATE INDEX IF NOT EXISTS idx_space_module_space ON space_module(space_id);
CREATE INDEX IF NOT EXISTS idx_space_module_module ON space_module(module_code);
CREATE INDEX IF NOT EXISTS idx_space_module_enabled ON space_module(enabled);

COMMENT ON TABLE space_module IS 'G2.2 : Modules actives par espace (organization_unit), remplace department_module';
COMMENT ON COLUMN space_module.space_id IS 'ID de l''unite organisationnelle (department, family, campus, ministry, team, cell, group, etc.)';
COMMENT ON COLUMN space_module.configuration_json IS 'Configuration specifique au module pour cet espace';
COMMENT ON COLUMN space_module.limits_json IS 'Limites/quotas specifiques au module pour cet espace';

-- 3. Seed : CORE modules (built-in platform features, order matters)
INSERT INTO module_definition (code, name, description, category, version, enabled, icon, source, display_order, features_json) VALUES
('people', 'People', 'Gestion des membres et âmes', 'PEOPLE', '1.0.0', true, 'Users', 'CORE', 1, '{}'),
('org', 'Organization', 'Hierarchie organisationnelle', 'ORGANIZATION', '1.0.0', true, 'Building2', 'CORE', 2, '{}'),
('events', 'Events', 'Evenements et programmes', 'EVENTS', '1.0.0', true, 'Calendar', 'CORE', 3, '{}'),
('notifications', 'Notifications', 'Centre de notifications', 'PLATFORM', '1.0.0', true, 'Bell', 'CORE', 4, '{}'),
('audit', 'Audit', 'Journal d''audit et historique', 'ADMIN', '1.0.0', true, 'Activity', 'CORE', 5, '{}'),
('dashboard', 'Dashboard', 'Tableaux de bord', 'PILOTAGE', '1.0.0', true, 'LayoutDashboard', 'CORE', 6, '{}'),
('search', 'Search', 'Recherche globale', 'PILOTAGE', '1.0.0', true, 'Search', 'CORE', 7, '{}'),
('settings', 'Settings', 'Parametres et branding', 'ADMIN', '1.0.0', true, 'Settings', 'CORE', 8, '{}'),
('roles', 'Roles & Permissions', 'RBAC et permissions scopees', 'ADMIN', '1.0.0', true, 'Shield', 'CORE', 9, '{}'),
('memberships', 'Memberships', 'Appartenance aux espaces', 'PEOPLE', '1.0.0', true, 'Users', 'CORE', 10, '{}')
ON CONFLICT (code) DO NOTHING;

-- 4. Seed : ENGINE modules (nouveaux moteurs Church OS)
INSERT INTO module_definition (code, name, description, category, version, enabled, icon, source, display_order, features_json) VALUES
('config', 'Configuration Engine', 'Moteur de configuration (templates, custom fields, workflows, statuts)', 'CONFIG', '1.0.0', true, 'Sliders', 'ENGINE', 20, '{}'),
('workflow', 'Workflow Engine', 'Moteur de workflows configurables avec approbations et escalade', 'CONFIG', '1.0.0', true, 'GitBranch', 'ENGINE', 21, '{}'),
('custom_fields', 'Custom Fields Engine', 'Champs personnalises sans migration', 'CONFIG', '1.0.0', true, 'Database', 'ENGINE', 22, '{}'),
('dress_code', 'Dress Code Engine', 'Gestion du dress code et patrimoine evenementiel', 'EVENTS', '1.0.0', true, 'Shirt', 'ENGINE', 30, '{}'),
('health', 'Health Engine', 'Sante / Infirmerie (patients, consultations, pharmacie, campagnes, kits)', 'HEALTH', '1.0.0', true, 'HeartPulse', 'ENGINE', 35, '{}'),
('family', 'Family Engine', 'Espace FAMILY - suivi des âmes, visites, receptions', 'PEOPLE', '1.0.0', true, 'Users', 'ENGINE', 40, '{}'),
('media', 'Media Engine', 'Sermons, streaming, mediatheque', 'MEDIA', '1.0.0', true, 'Video', 'ENGINE', 45, '{}'),
('finance', 'Finance Engine', 'Finances, paiements, budgets, tontines', 'FINANCE', '1.0.0', true, 'DollarSign', 'ENGINE', 50, '{}'),
('discipleship', 'Discipleship Engine', 'Parcours de discipleship configurables', 'DISCIPLESHIP', '1.0.0', true, 'BookOpen', 'ENGINE', 55, '{}'),
('pastoral', 'Pastoral Engine', 'Soins pastoraux confidentiels', 'PASTORAL', '1.0.0', true, 'Shield', 'ENGINE', 60, '{}'),
('prayer', 'Prayer Engine', 'Programmes de priere, creneaux, requetes', 'PRAYER', '1.0.0', true, 'Heart', 'ENGINE', 65, '{}'),
('asset', 'Asset Engine', 'Materiel, checkout, maintenance, TCO, QR', 'ASSETS', '1.0.0', true, 'Package', 'ENGINE', 70, '{}'),
('inventory', 'Inventaire', 'Gestion inventaire et stock', 'ASSETS', '1.0.0', true, 'Package', 'ENGINE', 71, '{}'),
('transfers', 'Transferts', 'Workflows de transfert', 'PEOPLE', '1.0.0', true, 'ArrowLeftRight', 'ENGINE', 75, '{}'),
('forms', 'Formulaires', 'Formulaires dynamiques', 'CONFIG', '1.0.0', true, 'FileText', 'ENGINE', 23, '{}'),
('academy', 'Academie', 'Formation et e-learning', 'DISCIPLESHIP', '1.0.0', true, 'GraduationCap', 'ENGINE', 56, '{}'),
('visit', 'Visites pastorales', 'Visites pastorales historisees', 'PASTORAL', '1.0.0', true, 'DoorOpen', 'ENGINE', 61, '{}'),
('parallel_followups', 'Suivis paralleles', 'Suivis paralleles multi-acteurs', 'DISCIPLESHIP', '1.0.0', true, 'Activity', 'ENGINE', 57, '{}'),
('spiritual_journal', 'Journal spirituel', 'Journal spirituel personnel', 'DISCIPLESHIP', '1.0.0', true, 'BookOpen', 'ENGINE', 58, '{}'),
('reverse_mentoring', 'Mentorat inverse', 'Mentorat inverse senior↔junior', 'DISCIPLESHIP', '1.0.0', true, 'Users', 'ENGINE', 59, '{}'),
('succession', 'Succession', 'Plans de succession', 'ADMIN', '1.0.0', true, 'Users', 'ENGINE', 80, '{}'),
('kpi', 'KPI', 'Indicateurs de performance', 'PILOTAGE', '1.0.0', true, 'BarChart3', 'ENGINE', 81, '{}'),
('department_kpi', 'KPI Departement', 'KPI par departement', 'PILOTAGE', '1.0.0', true, 'BarChart3', 'ENGINE', 82, '{}'),
('ai', 'IA', 'Intelligence artificielle Discipolat', 'AI', '1.0.0', true, 'Brain', 'ENGINE', 90, '{}'),
('ai_predictions', 'Predictions IA', 'Predictions de presence et engagement', 'AI', '1.0.0', true, 'Brain', 'ENGINE', 91, '{}'),
('ai_chat', 'Chat IA', 'Chat avec assistant IA', 'AI', '1.0.0', true, 'MessageSquare', 'ENGINE', 92, '{}'),
('ai_visit_notes', 'Notes IA', 'Notes de visite IA', 'AI', '1.0.0', true, 'FileText', 'ENGINE', 93, '{}'),
('evaluations', 'Evaluations', 'Evaluations et feedback', 'ENGAGEMENT', '1.0.0', true, 'Star', 'ENGINE', 100, '{}'),
('trainings', 'Formations', 'Formations et cursus', 'ENGAGEMENT', '1.0.0', true, 'GraduationCap', 'ENGINE', 101, '{}'),
('badges', 'Badges', 'Badges et recompenses', 'ENGAGEMENT', '1.0.0', true, 'Trophy', 'ENGINE', 102, '{}'),
('appointments', 'Rendez-vous', 'Gestion rendez-vous', 'ENGAGEMENT', '1.0.0', true, 'CalendarClock', 'ENGINE', 103, '{}'),
('dev_plan', 'Plans de developpement', 'Plans de developpement personnel', 'DISCIPLESHIP', '1.0.0', true, 'Target', 'ENGINE', 62, '{}'),
('rewards', 'Recompenses', 'Badges et certificats', 'ENGAGEMENT', '1.0.0', true, 'Award', 'ENGINE', 104, '{}'),
('emergency_aid', 'Aide d''urgence', 'Aide d''urgence', 'PASTORAL', '1.0.0', true, 'AlertTriangle', 'ENGINE', 63, '{}'),
('bible_reading', 'Lecture biblique', 'Programme de lecture biblique', 'DISCIPLESHIP', '1.0.0', true, 'BookOpen', 'ENGINE', 64, '{}'),
('face_recognition', 'Reconnaissance faciale', 'Reconnaissance faciale pour presence', 'PEOPLE', '1.0.0', true, 'UserCheck', 'ENGINE', 76, '{}'),
('skills', 'Competences', 'Competences et matching', 'PEOPLE', '1.0.0', true, 'Target', 'ENGINE', 77, '{}'),
('quest', 'Quetes', 'Systeme de quetes gamifie', 'ENGAGEMENT', '1.0.0', true, 'Sword', 'ENGINE', 105, '{}'),
('tracking', 'Tracking', 'Suivi maker/disciple', 'DISCIPLESHIP', '1.0.0', true, 'MapPin', 'ENGINE', 66, '{}'),
('referrals', 'Parrainage', 'Systeme de parrainage', 'PEOPLE', '1.0.0', true, 'UserPlus', 'ENGINE', 78, '{}'),
('sermons', 'Sermons', 'Gestion sermons et predications', 'MEDIA', '1.0.0', true, 'BookOpen', 'ENGINE', 46, '{}'),
('encouragements', 'Encouragements', 'Encouragements entre membres', 'DISCIPLESHIP', '1.0.0', true, 'Heart', 'ENGINE', 67, '{}'),
('voice_reports', 'Rapports vocaux', 'Rapports vocaux terrain', 'PILOTAGE', '1.0.0', true, 'Mic', 'ENGINE', 83, '{}'),
('family_meetings', 'Reunions familiales', 'Reunions familiales', 'PEOPLE', '1.0.0', true, 'Users', 'ENGINE', 41, '{}'),
('group_messages', 'Messages de groupe', 'Messages de groupe par espace', 'PLATFORM', '1.0.0', true, 'MessageSquare', 'ENGINE', 110, '{}'),
('executive_insights', 'Insights executifs', 'Insights executifs IA', 'AI', '1.0.0', true, 'BarChart3', 'ENGINE', 94, '{}'),
('currency', 'Devises', 'Gestion devises multi-region', 'FINANCE', '1.0.0', true, 'Currency', 'ENGINE', 51, '{}'),
('automation', 'Automatisation', 'Regles d''automatisation', 'CONFIG', '1.0.0', true, 'Zap', 'ENGINE', 24, '{}'),
('spiritual_challenges', 'Defis spirituels', 'Defis spirituels communautaires', 'DISCIPLESHIP', '1.0.0', true, 'Target', 'ENGINE', 68, '{}'),
('discipline', 'Discipline', 'Evenements de discipline', 'PASTORAL', '1.0.0', true, 'Gavel', 'ENGINE', 69, '{}'),
('visitor', 'Visiteurs', 'Gestion visiteurs', 'PEOPLE', '1.0.0', true, 'UserPlus', 'ENGINE', 79, '{}'),
('compliance', 'Conformite', 'Conformite RGPD', 'ADMIN', '1.0.0', true, 'Shield', 'ENGINE', 84, '{}'),
('backup', 'Sauvegardes', 'Sauvegardes et restauration', 'ADMIN', '1.0.0', true, 'Database', 'ENGINE', 85, '{}'),
('marketplace', 'Marketplace', 'Marketplace interne', 'PLATFORM', '1.0.0', true, 'Store', 'ENGINE', 111, '{}'),
('streaming', 'Streaming', 'Streaming video', 'MEDIA', '1.0.0', true, 'Video', 'ENGINE', 47, '{}'),
('community', 'Communaute', 'Posts et interactions communautaires', 'DISCIPLESHIP', '1.0.0', true, 'Users', 'ENGINE', 112, '{}'),
('import_export', 'Import/Export', 'Migration et import/export de donnees', 'ADMIN', '1.0.0', true, 'ArrowUpDown', 'ENGINE', 86, '{}'),
('demo', 'Demo', 'Demandes de demo', 'ADMIN', '1.0.0', true, 'Monitor', 'ENGINE', 87, '{}')
ON CONFLICT (code) DO NOTHING;

-- 5. Seed : EXISTING modules (legacy platform_modules declares comme EXISTING pour compatibilite)
-- Ces modules ont des codecs UPPERCASE pour distinguer des modules ENGINE
INSERT INTO module_definition (code, name, description, category, version, enabled, icon, source, display_order, features_json) VALUES
('MAP', 'Cartographie', 'Cartographie des membres et eglises', 'PILOTAGE', '1.0.0', true, 'Map', 'EXISTING', 200, '{}'),
('SOULS', 'Âmes & disciples', 'Gestion des âmes (legacy)', 'PEOPLE', '1.0.0', true, 'Heart', 'EXISTING', 201, '{}'),
('FAMILIES', 'Familles', 'Gestion des familles (legacy)', 'PEOPLE', '1.0.0', true, 'Users', 'EXISTING', 202, '{}'),
('CRM_FAISEUR', 'CRM Faiseur', 'CRM Faiseur - suivi discipulaire (legacy)', 'DISCIPLESHIP', '1.0.0', true, 'UserCog', 'EXISTING', 203, '{}'),
('EVANGELISM', 'Evangelisation', 'Module d''evangelisation (legacy)', 'DISCIPLESHIP', '1.0.0', true, 'Sprout', 'EXISTING', 204, '{}'),
('PARALLEL_FOLLOWUPS', 'Suivis paralleles', 'Suivis paralleles (legacy)', 'DISCIPLESHIP', '1.0.0', true, 'Activity', 'EXISTING', 205, '{}'),
('OBJECTIVES', 'Objectifs', 'Objectifs SMART (legacy)', 'PILOTAGE', '1.0.0', true, 'Target', 'EXISTING', 206, '{}'),
('VISITS', 'Visites', 'Visites pastorales (legacy)', 'PASTORAL', '1.0.0', true, 'DoorOpen', 'EXISTING', 207, '{}'),
('DEPARTMENTS', 'Departements', 'Departements (legacy)', 'ORGANIZATION', '1.0.0', true, 'Building2', 'EXISTING', 208, '{}'),
('REPORTS', 'Rapports', 'Rapports (legacy)', 'PILOTAGE', '1.0.0', true, 'FileText', 'EXISTING', 209, '{}'),
('PRAYERS', 'Prieres', 'Prieres (legacy)', 'PRAYER', '1.0.0', true, 'BookOpen', 'EXISTING', 210, '{}'),
('DOCUMENTS', 'Documents', 'Documents (legacy)', 'PLATFORM', '1.0.0', true, 'FolderOpen', 'EXISTING', 211, '{}'),
('ALERTS', 'Alertes', 'Alertes (legacy)', 'PLATFORM', '1.0.0', true, 'Bell', 'EXISTING', 212, '{}'),
('MESSAGES', 'Messagerie', 'Messagerie (legacy)', 'PLATFORM', '1.0.0', true, 'MessageSquare', 'EXISTING', 213, '{}'),
('MEMBER_REQUESTS', 'Demandes membres', 'Demandes membres (legacy)', 'PEOPLE', '1.0.0', true, 'MessageSquare', 'EXISTING', 214, '{}'),
('USERS', 'Utilisateurs', 'Utilisateurs (legacy)', 'ADMIN', '1.0.0', true, 'UserCog', 'EXISTING', 215, '{}'),
('AUDIT', 'Audit', 'Audit & historique (legacy)', 'ADMIN', '1.0.0', true, 'Activity', 'EXISTING', 216, '{}'),
('PERMISSIONS', 'Roles & permissions', 'Roles & permissions (legacy)', 'ADMIN', '1.0.0', true, 'Shield', 'EXISTING', 217, '{}'),
('SETTINGS', 'Identite & parametres', 'Identite & parametres (legacy)', 'ADMIN', '1.0.0', true, 'Settings', 'EXISTING', 218, '{}'),
('whatsapp', 'WhatsApp', 'Integration WhatsApp/USSD', 'PLATFORM', '1.0.0', true, 'MessageSquare', 'EXISTING', 220, '{}'),
('network', 'Reseau', 'Carte du reseau d''eglises', 'ORGANIZATION', '1.0.0', true, 'Globe', 'EXISTING', 221, '{}'),
('voice', 'Voix', 'Rapports vocaux (legacy)', 'PILOTAGE', '1.0.0', true, 'Mic', 'EXISTING', 222, '{}'),
('mobile_offline', 'Mobile Offline', 'Mode hors-ligne mobile', 'PLATFORM', '1.0.0', true, 'Smartphone', 'EXISTING', 223, '{}'),
('usss', 'USSD', 'Portail USSD basse connexion', 'PLATFORM', '1.0.0', true, 'Phone', 'EXISTING', 224, '{}'),
('beta', 'Beta Testing', 'Gestion beta testing', 'ADMIN', '1.0.0', true, 'FlaskConical', 'EXISTING', 225, '{}'),
('dictionaries', 'Dictionnaires', 'Dictionnaires de donnees', 'ADMIN', '1.0.0', true, 'BookOpen', 'EXISTING', 226, '{}')
ON CONFLICT (code) DO NOTHING;
