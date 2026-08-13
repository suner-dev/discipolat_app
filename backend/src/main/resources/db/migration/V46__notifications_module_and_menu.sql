-- V46__notifications_module_and_menu.sql
-- ============================================================
-- MODULE + MENU « NOTIFICATIONS »
-- Le centre de notifications (/notifications) devient un module
-- activable/désactivable et un menu configurable, cohérent avec
-- le reste de la plateforme (Administration → Modules / Menus).
-- ============================================================

-- Décale les modules « Outils » suivants pour insérer Notifications après Alertes
UPDATE platform_modules SET ordre = ordre + 1 WHERE section = 'Outils' AND ordre >= 18;

INSERT INTO platform_modules (key, label, description, icon, section, ordre, enabled) VALUES
    ('NOTIFICATIONS', 'Notifications', 'Centre de notifications (rapports, absences, transferts, prières…).', 'BellRing', 'Outils', 18, TRUE)
ON CONFLICT (key) DO NOTHING;

-- Décale les menus « Engagement & outils » suivants pour insérer Notifications avant Alertes
UPDATE menu_entries SET ordre = ordre + 1 WHERE section = 'Engagement & outils' AND ordre >= 4;

INSERT INTO menu_entries (key, label, href, icon, section, ordre, roles, module_key) VALUES
    ('notifications', 'Notifications', '/notifications', 'BellRing', 'Engagement & outils', 4,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR","MEMBRE"]'::jsonb, 'NOTIFICATIONS')
ON CONFLICT (key) DO NOTHING;
