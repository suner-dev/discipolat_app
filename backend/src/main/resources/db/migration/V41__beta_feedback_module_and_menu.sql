-- V41__beta_feedback_module_and_menu.sql
-- ============================================================
-- MODULE & MENU "RETOURS TESTEURS" (bêta-testing)
-- Le panneau de gestion des retours testeurs est configurable comme
-- tout module de la plateforme : activable/désactivable et masquable
-- dans les menus par l'administration.
-- ============================================================

INSERT INTO platform_modules (key, label, description, icon, section, enabled, ordre)
VALUES ('FEEDBACK', 'Retours testeurs',
        'Collecte et gestion des retours des testeurs (bugs, suggestions, UX)',
        'MessageSquare', 'Administration', TRUE, 90)
ON CONFLICT (key) DO NOTHING;

INSERT INTO menu_entries (key, label, href, icon, section, ordre, roles, module_key, enabled)
VALUES ('admin-feedback', 'Retours testeurs', '/admin/feedback',
        'MessageSquare', 'Administration', 90, '["ADMIN","PASTEUR"]'::jsonb, 'FEEDBACK', TRUE)
ON CONFLICT (key) DO NOTHING;
