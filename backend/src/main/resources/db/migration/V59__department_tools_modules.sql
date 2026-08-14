-- ============================================================
-- V59 — Sous-modules du DMS activables/désactivables par l'admin
-- Rapports de département, checklists, inventaire et documentation
-- sont désormais des modules indépendants (l'API est bloquée côté
-- serveur via ModuleGateFilter lorsque désactivés).
-- ============================================================

INSERT INTO platform_modules (key, label, description, icon, section, enabled, ordre) VALUES
    ('DEPT_REPORTS', 'Rapports de département',
     'Synthèses sauvegardées (hebdo, mensuel, événement, incident…) et export CSV.',
     'FileText', 'Structures', TRUE, 28),
    ('DEPT_CHECKLISTS', 'Checklists de département',
     'Listes de contrôle pour tâches, événements, équipes et membres.',
     'ListChecks', 'Structures', TRUE, 29),
    ('DEPT_INVENTORY', 'Inventaire matériel',
     'Matériel du département : quantité, état, responsable, localisation.',
     'Boxes', 'Structures', TRUE, 30),
    ('DEPT_DOCUMENTS', 'Documentation du département',
     'Procédures, guides, formulaires, comptes rendus et ressources.',
     'BookOpen', 'Structures', TRUE, 31)
ON CONFLICT (key) DO NOTHING;
