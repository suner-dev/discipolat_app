-- ============================================================
-- V59 — Sous-modules du DMS activables/desactivables par l'admin
-- Rapports de departement, checklists, inventaire et documentation
-- sont desormais des modules independants (l'API est bloquee cote
-- serveur via ModuleGateFilter lorsque desactives).
-- ============================================================

INSERT INTO platform_modules (key, label, description, icon, section, enabled, ordre) VALUES
    ('DEPT_REPORTS', 'Rapports de departement',
     'Syntheses sauvegardees (hebdo, mensuel, evenement, incident…) et export CSV.',
     'FileText', 'Structures', TRUE, 28),
    ('DEPT_CHECKLISTS', 'Checklists de departement',
     'Listes de controle pour tâches, evenements, equipes et membres.',
     'ListChecks', 'Structures', TRUE, 29),
    ('DEPT_INVENTORY', 'Inventaire materiel',
     'Materiel du departement : quantite, etat, responsable, localisation.',
     'Boxes', 'Structures', TRUE, 30),
    ('DEPT_DOCUMENTS', 'Documentation du departement',
     'Procedures, guides, formulaires, comptes rendus et ressources.',
     'BookOpen', 'Structures', TRUE, 31)
ON CONFLICT (key) DO NOTHING;
