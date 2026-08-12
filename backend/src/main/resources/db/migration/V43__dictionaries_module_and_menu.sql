-- V43__dictionaries_module_and_menu.sql
-- ============================================================
-- MODULE & MENU "DICTIONNAIRES" (référentiels configurables)
-- L'administrateur adapte les listes de l'église (types d'événements,
-- statuts, raisons d'absence, catégories…) sans modifier le code.
-- Comme tout module, il est activable/désactivable et masquable.
-- ============================================================

INSERT INTO platform_modules (key, label, description, icon, section, enabled, ordre)
VALUES ('DICTIONARIES', 'Dictionnaires',
        'Référentiels configurables : types d''événements, statuts, raisons d''absence, catégories…',
        'BookOpen', 'Administration', TRUE, 91)
ON CONFLICT (key) DO NOTHING;

INSERT INTO menu_entries (key, label, href, icon, section, ordre, roles, module_key, enabled)
VALUES ('admin-dictionaries', 'Dictionnaires', '/admin/dictionaries',
        'BookOpen', 'Administration', 91, '["ADMIN"]'::jsonb, 'DICTIONARIES', TRUE)
ON CONFLICT (key) DO NOTHING;
