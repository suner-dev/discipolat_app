-- V46__notifications_module_and_menu.sql
-- ============================================================
-- MODULE + MENU « NOTIFICATIONS »
-- Le centre de notifications (/notifications) devient un module
-- activable/desactivable et un menu configurable, coherent avec
-- le reste de la plateforme (Administration → Modules / Menus).
-- ============================================================

-- Decale les modules « Outils » suivants pour inserer Notifications apres Alertes
UPDATE platform_modules SET ordre = ordre + 1 WHERE section = 'Outils' AND ordre >= 18;

INSERT INTO platform_modules (key, label, description, icon, section, ordre, enabled) VALUES
    ('NOTIFICATIONS', 'Notifications', 'Centre de notifications (rapports, absences, transferts, prieres…).', 'BellRing', 'Outils', 18, TRUE)
ON CONFLICT (key) DO NOTHING;

-- Decale les menus « Engagement & outils » suivants pour inserer Notifications avant Alertes
UPDATE menu_entries SET ordre = ordre + 1 WHERE section = 'Engagement & outils' AND ordre >= 4;

INSERT INTO menu_entries (key, label, href, icon, section, ordre, roles, module_key) VALUES
    ('notifications', 'Notifications', '/notifications', 'BellRing', 'Engagement & outils', 4,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR","MEMBRE"]'::jsonb, 'NOTIFICATIONS')
ON CONFLICT (key) DO NOTHING;
