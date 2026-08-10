-- V36__platform_modules_and_menus.sql
-- ============================================================
-- MODULES ACTIVABLES / DÉSACTIVABLES + MENUS CONFIGURABLES
-- L'administrateur active/désactive des modules et personnalise
-- les menus (libellé, icône, ordre, rôles, visibilité) sans code.
-- Le filtre de gating backend bloque les API des modules désactivés.
-- ============================================================

CREATE TABLE IF NOT EXISTS platform_modules (
    key VARCHAR(50) PRIMARY KEY,
    label VARCHAR(255) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    section VARCHAR(100) NOT NULL DEFAULT 'Général',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ordre INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS menu_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key VARCHAR(50) NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL,
    href VARCHAR(255) NOT NULL,
    icon VARCHAR(50),
    section VARCHAR(100) NOT NULL DEFAULT 'Général',
    ordre INTEGER NOT NULL DEFAULT 0,
    roles JSONB NOT NULL DEFAULT '[]'::jsonb,
    module_key VARCHAR(50) REFERENCES platform_modules(key),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_menu_entries_module ON menu_entries(module_key);
CREATE INDEX IF NOT EXISTS idx_menu_entries_enabled ON menu_entries(enabled);

-- ============================================================
-- SEED DES MODULES
-- ============================================================
INSERT INTO platform_modules (key, label, section, ordre) VALUES
    ('DASHBOARD', 'Tableaux de bord', 'Pilotage', 1),
    ('SEARCH', 'Recherche globale', 'Pilotage', 2),
    ('MAP', 'Cartographie', 'Pilotage', 3),
    ('SOULS', 'Âmes & disciples', 'Discipolat', 4),
    ('FAMILIES', 'Familles', 'Discipolat', 5),
    ('CRM_FAISEUR', 'CRM Faiseur', 'Discipolat', 6),
    ('EVANGELISM', 'Évangélisation', 'Discipolat', 7),
    ('PARALLEL_FOLLOWUPS', 'Suivis parallèles', 'Discipolat', 8),
    ('OBJECTIVES', 'Objectifs', 'Discipolat', 9),
    ('VISITS', 'Visites', 'Discipolat', 10),
    ('DEPARTMENTS', 'Départements', 'Structures', 11),
    ('REPORTS', 'Rapports', 'Structures', 12),
    ('PRAYERS', 'Prières', 'Vie de l''église', 13),
    ('EVENTS', 'Événements & programmes', 'Vie de l''église', 14),
    ('TRANSFERS', 'Transferts', 'Transferts', 15),
    ('DOCUMENTS', 'Documents', 'Outils', 16),
    ('ALERTS', 'Alertes', 'Outils', 17),
    ('MESSAGES', 'Messagerie', 'Outils', 18),
    ('EVALUATIONS', 'Évaluations', 'Engagement', 19),
    ('MEMBER_REQUESTS', 'Demandes membres', 'Engagement', 20),
    ('TRAININGS', 'Formations', 'Engagement', 21),
    ('BADGES', 'Badges', 'Engagement', 22),
    ('APPOINTMENTS', 'Rendez-vous', 'Engagement', 23),
    ('USERS', 'Utilisateurs', 'Administration', 24),
    ('AUDIT', 'Audit & historique', 'Administration', 25),
    ('PERMISSIONS', 'Rôles & permissions', 'Administration', 26),
    ('SETTINGS', 'Identité & paramètres', 'Administration', 27)
ON CONFLICT (key) DO NOTHING;

-- ============================================================
-- SEED DES MENUS (tous rôles)
-- ============================================================
INSERT INTO menu_entries (key, label, href, icon, section, ordre, roles, module_key) VALUES
    -- Pilotage
    ('dashboard', 'Tableau de bord', '/dashboard', 'LayoutDashboard', 'Pilotage', 1,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'DASHBOARD'),
    ('dashboard-pasteur', 'Pilotage Pasteur', '/dashboard/pasteur', 'Sparkles', 'Pilotage', 2,
     '["ADMIN","PASTEUR"]'::jsonb, 'DASHBOARD'),
    ('dashboard-responsable', 'Dashboard Responsable', '/dashboard/responsable', 'Building2', 'Pilotage', 3,
     '["ADMIN","PASTEUR","RESPONSABLE"]'::jsonb, 'DASHBOARD'),
    ('dashboard-chef', 'Dashboard Chef', '/dashboard/chef-famille', 'Users', 'Pilotage', 4,
     '["ADMIN","PASTEUR","CHEF_DE_FAMILLE"]'::jsonb, 'DASHBOARD'),
    ('dashboard-membre', 'Espace Membre', '/dashboard/membre', 'User', 'Pilotage', 5,
     '["MEMBRE"]'::jsonb, 'DASHBOARD'),
    ('search', 'Recherche', '/search', 'Search', 'Pilotage', 6,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'SEARCH'),
    ('map', 'Cartographie', '/map', 'Map', 'Pilotage', 7,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'MAP'),

    -- Discipolat
    ('souls', 'Âmes', '/souls', 'Heart', 'Discipolat', 1,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'SOULS'),
    ('families', 'Familles', '/families', 'Users', 'Discipolat', 2,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'FAMILIES'),
    ('families-compare', 'Comparer familles', '/families/compare', 'BarChart3', 'Discipolat', 3,
     '["ADMIN","PASTEUR"]'::jsonb, 'FAMILIES'),
    ('crm-faiseur', 'CRM Faiseur', '/crm/faiseur', 'HandHeart', 'Discipolat', 4,
     '["ADMIN","PASTEUR","FAISEUR"]'::jsonb, 'CRM_FAISEUR'),
    ('evangelism', 'Évangélisation', '/evangelism', 'Sprout', 'Discipolat', 5,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'EVANGELISM'),
    ('parallel-followups', 'Suivis parallèles', '/parallel-followups', 'Activity', 'Discipolat', 6,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'PARALLEL_FOLLOWUPS'),
    ('objectives', 'Objectifs', '/objectives', 'Target', 'Discipolat', 7,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'OBJECTIVES'),
    ('visits', 'Visites', '/visits', 'DoorOpen', 'Discipolat', 8,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'VISITS'),
    ('souls-retractions', 'Retraits', '/souls/retractions', 'AlertTriangle', 'Discipolat', 9,
     '["ADMIN","PASTEUR","RESPONSABLE"]'::jsonb, 'SOULS'),

    -- Structures & rapports
    ('departments', 'Départements', '/departments', 'Building2', 'Structures & rapports', 1,
     '["ADMIN","PASTEUR","RESPONSABLE"]'::jsonb, 'DEPARTMENTS'),
    ('reports', 'Rapports', '/reports', 'FileText', 'Structures & rapports', 2,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'REPORTS'),
    ('reports-maker', 'Rapport faiseur', '/reports/maker', 'FileText', 'Structures & rapports', 3,
     '["ADMIN","PASTEUR","FAISEUR"]'::jsonb, 'REPORTS'),
    ('reports-family', 'Rapport famille', '/reports/family', 'FileText', 'Structures & rapports', 4,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'REPORTS'),
    ('reports-urgent-aid', 'Aide urgente', '/reports/urgent-aid', 'AlertTriangle', 'Structures & rapports', 5,
     '["ADMIN","PASTEUR","RESPONSABLE"]'::jsonb, 'REPORTS'),

    -- Vie de l'église
    ('prayers', 'Prières', '/prayers', 'BookOpen', 'Vie de l''église', 1,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'PRAYERS'),
    ('prayers-spaces', 'Espaces prière', '/prayers/spaces', 'Shield', 'Vie de l''église', 2,
     '["ADMIN","PASTEUR"]'::jsonb, 'PRAYERS'),
    ('prayers-actions-de-grace', 'Actions de grâce', '/prayers/actions-de-grace', 'Heart', 'Vie de l''église', 3,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'PRAYERS'),
    ('events', 'Événements', '/events', 'Calendar', 'Vie de l''église', 4,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'EVENTS'),
    ('events-program', 'Programme hebdomadaire', '/events/program', 'Calendar', 'Vie de l''église', 5,
     '["ADMIN","PASTEUR"]'::jsonb, 'EVENTS'),
    ('events-statistics', 'Statistiques événements', '/events/statistics', 'BarChart3', 'Vie de l''église', 6,
     '["ADMIN","PASTEUR"]'::jsonb, 'EVENTS'),
    ('program-types', 'Types de programmes', '/programs', 'Calendar', 'Vie de l''église', 7,
     '["ADMIN","PASTEUR"]'::jsonb, 'EVENTS'),

    -- Transferts
    ('transfers', 'Demandes de transfert', '/transfers', 'ArrowLeftRight', 'Transferts', 1,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR","MEMBRE"]'::jsonb, 'TRANSFERS'),
    ('transfers-config', 'Configuration workflow', '/admin/transfers', 'Workflow', 'Transferts', 2,
     '["ADMIN","PASTEUR"]'::jsonb, 'TRANSFERS'),

    -- Engagement & outils
    ('evaluations', 'Évaluations', '/evaluations', 'Star', 'Engagement & outils', 1,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'EVALUATIONS'),
    ('member-requests', 'Demandes membres', '/members/requests', 'MessageSquare', 'Engagement & outils', 2,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE"]'::jsonb, 'MEMBER_REQUESTS'),
    ('documents', 'Documents', '/documents', 'FolderOpen', 'Engagement & outils', 3,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'DOCUMENTS'),
    ('alerts', 'Alertes', '/alerts', 'Bell', 'Engagement & outils', 4,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, 'ALERTS'),
    ('messages', 'Messagerie', '/messages', 'MessagesSquare', 'Engagement & outils', 5,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR","MEMBRE"]'::jsonb, 'MESSAGES'),
    ('trainings', 'Formations', '/trainings', 'GraduationCap', 'Engagement & outils', 6,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR","MEMBRE"]'::jsonb, 'TRAININGS'),
    ('badges', 'Badges', '/badges', 'Trophy', 'Engagement & outils', 7,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR","MEMBRE"]'::jsonb, 'BADGES'),
    ('appointments', 'Rendez-vous', '/appointments', 'CalendarClock', 'Engagement & outils', 8,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR","MEMBRE"]'::jsonb, 'APPOINTMENTS'),

    -- Administration
    ('settings', 'Identité & marque', '/admin/settings', 'Palette', 'Administration', 1,
     '["ADMIN"]'::jsonb, 'SETTINGS'),
    ('users', 'Utilisateurs', '/users', 'UserCog', 'Administration', 2,
     '["ADMIN","PASTEUR","RESPONSABLE"]'::jsonb, 'USERS'),
    ('audit', 'Audit', '/audit', 'Activity', 'Administration', 3,
     '["ADMIN","PASTEUR"]'::jsonb, 'AUDIT'),
    ('permissions', 'Permissions', '/permissions', 'Shield', 'Administration', 4,
     '["ADMIN"]'::jsonb, 'PERMISSIONS'),

    -- Administration plateforme (configuration sans code)
    ('modules', 'Modules', '/admin/modules', 'Boxes', 'Administration', 5,
     '["ADMIN"]'::jsonb, 'PERMISSIONS'),
    ('menus', 'Menus', '/admin/menus', 'Menu', 'Administration', 6,
     '["ADMIN"]'::jsonb, 'PERMISSIONS'),

    -- Profil
    ('profile', 'Profil', '/profile', 'User', 'Profil', 1,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR","MEMBRE"]'::jsonb, NULL)
ON CONFLICT (key) DO NOTHING;

COMMENT ON TABLE platform_modules IS 'Grands modules activables/désactivables par l''administrateur ; l''API des modules désactivés est bloquée côté serveur.';
COMMENT ON TABLE menu_entries IS 'Menus configurables : libellé, icône, section, ordre et rôles visibles, liés à un module.';
