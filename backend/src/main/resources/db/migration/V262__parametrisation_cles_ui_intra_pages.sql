-- V262__parametrisation_cles_ui_intra_pages.sql
-- ============================================================
-- PARAMÉTRISATION PAR ÉGLISE — Phase 4/5 (tranche résiduelle :
-- gardes INTRA-PAGES web et mobile : boutons, sections, sélecteurs.
-- Matrice = parité EXACTE du littéral legacy remplacé (règle
-- additive : aucune conversion ne large ni ne rétrécit l'accès
-- au minting). Les sites dont le littéral était déjà ≡ à une clé
-- serveur existante (USERS_EXECUTE, FAMILIES_EXECUTE_CREATE,
-- DASHBOARD_EXECUTE, etc.) réutilisent cette clé — aucun mint.
-- Une église peut retirer/ajouter ces capacités par rôle
-- (surcouche tenant V258) ; les pages suivent les clés SERVIES
-- (GET /me/permissions, served-first via useServedAccess côté
-- web et AuthState.canServed côté mobile). Cible PostgreSQL
-- (Flyway coupé sous H2/test : replay import.sql par
-- gen_h2_seed.py). Généré par .e2e-tmp/param/p4/gen_v262.py.
-- ============================================================

-- 1) Catalogue des permissions UI (globales, système).
INSERT INTO permissions (id, tenant_id, key, label, description, scope, category, system, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, p.key, p.label, p.label, 'TENANT', p.category, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('UI_DOCUMENTS_DELETEANY_ACCESS', 'Suppression de tout document (hors auteur)', 'UI'),
    ('UI_EVALUATIONS_ADMIN_VIEW_ACCESS', 'Vue administrateur des évaluations', 'UI'),
    ('UI_EVENTS_CONSOLIDATED_ACCESS', 'Vue consolidée des événements par famille', 'UI'),
    ('UI_FAMILIES_ADDSOUL_ACCESS', 'Ajout de nouvelle âme depuis une famille', 'UI'),
    ('UI_FAMILIES_COMPARE_ACCESS', 'Comparaison de familles', 'UI'),
    ('UI_FAMILIES_VIEWFAISEURPERF_ACCESS', 'Performance des faiseurs de la famille', 'UI'),
    ('UI_FILES_PICKER_ACCESS', 'Sélecteur de pièces jointes (module Fichiers)', 'UI'),
    ('UI_PASSPORT_ISSUE_ACCESS', 'Émission de passeport spirituel (UI web)', 'UI'),
    ('UI_PASSPORT_QR_VERIFY_ACCESS', 'QR de vérification du passeport (mobile)', 'UI'),
    ('UI_REPORTS_EXPORT_ACCESS', 'Export des rapports consolidés', 'UI'),
    ('UI_VISITS_LEADER_UPCOMING_ACCESS', 'Accès bloc équipe (visites à venir)', 'UI'),
    ('UI_WEEKLYPROGRAM_TEMPLATES_ACCESS', 'Gestion des templates de programme hebdomadaire', 'UI')
) AS p(key, label, category)
WHERE NOT EXISTS (SELECT 1 FROM permissions po WHERE po.key = p.key);

-- 2) Matrice par défaut : parité exacte des littéraux legacy intra-pages.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('PASTEUR', 'UI_DOCUMENTS_DELETEANY_ACCESS'),
    ('ADMIN', 'UI_EVALUATIONS_ADMIN_VIEW_ACCESS'),
    ('PASTEUR', 'UI_EVALUATIONS_ADMIN_VIEW_ACCESS'),
    ('ADMIN', 'UI_EVENTS_CONSOLIDATED_ACCESS'),
    ('PASTEUR', 'UI_EVENTS_CONSOLIDATED_ACCESS'),
    ('ADMIN', 'UI_FAMILIES_ADDSOUL_ACCESS'),
    ('CHEF_DE_FAMILLE', 'UI_FAMILIES_ADDSOUL_ACCESS'),
    ('PASTEUR', 'UI_FAMILIES_ADDSOUL_ACCESS'),
    ('ADMIN', 'UI_FAMILIES_COMPARE_ACCESS'),
    ('PASTEUR', 'UI_FAMILIES_COMPARE_ACCESS'),
    ('ADMIN', 'UI_FAMILIES_VIEWFAISEURPERF_ACCESS'),
    ('CHEF_DE_FAMILLE', 'UI_FAMILIES_VIEWFAISEURPERF_ACCESS'),
    ('FAISEUR', 'UI_FAMILIES_VIEWFAISEURPERF_ACCESS'),
    ('PASTEUR', 'UI_FAMILIES_VIEWFAISEURPERF_ACCESS'),
    ('ADMIN', 'UI_FILES_PICKER_ACCESS'),
    ('CHEF_DE_FAMILLE', 'UI_FILES_PICKER_ACCESS'),
    ('FAISEUR', 'UI_FILES_PICKER_ACCESS'),
    ('PASTEUR', 'UI_FILES_PICKER_ACCESS'),
    ('RESPONSABLE', 'UI_FILES_PICKER_ACCESS'),
    ('ADMIN', 'UI_PASSPORT_ISSUE_ACCESS'),
    ('PASTEUR', 'UI_PASSPORT_ISSUE_ACCESS'),
    ('RESPONSABLE', 'UI_PASSPORT_ISSUE_ACCESS'),
    ('ADMIN', 'UI_PASSPORT_QR_VERIFY_ACCESS'),
    ('FAISEUR', 'UI_PASSPORT_QR_VERIFY_ACCESS'),
    ('PASTEUR', 'UI_PASSPORT_QR_VERIFY_ACCESS'),
    ('ADMIN', 'UI_REPORTS_EXPORT_ACCESS'),
    ('PASTEUR', 'UI_REPORTS_EXPORT_ACCESS'),
    ('ADMIN', 'UI_VISITS_LEADER_UPCOMING_ACCESS'),
    ('CHEF_DE_FAMILLE', 'UI_VISITS_LEADER_UPCOMING_ACCESS'),
    ('PASTEUR', 'UI_VISITS_LEADER_UPCOMING_ACCESS'),
    ('RESPONSABLE', 'UI_VISITS_LEADER_UPCOMING_ACCESS'),
    ('PASTEUR', 'UI_WEEKLYPROGRAM_TEMPLATES_ACCESS')
) AS m(role_key, perm_key)
JOIN roles r ON r.key = m.role_key AND r.tenant_id IS NULL
JOIN permissions p ON p.key = m.perm_key
ON CONFLICT DO NOTHING;

-- 3) Répercussion des nouvelles clés globales vers les surcouches
--    locales homonymes (miroir exact du backfill V259 §5 / V260 §2 / V261 §3).
--    Additif : une église garde le pouvoir de RETIRER en local ensuite.
INSERT INTO role_permissions (role_id, permission_id)
SELECT local.id, rp.permission_id
FROM roles g
JOIN roles local ON local.key = g.key AND local.tenant_id IS NOT NULL
JOIN role_permissions rp ON rp.role_id = g.id
WHERE g.tenant_id IS NULL
ON CONFLICT DO NOTHING;
