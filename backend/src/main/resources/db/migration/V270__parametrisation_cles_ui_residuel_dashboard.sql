-- V270__parametrisation_cles_ui_residuel_dashboard.sql
-- ============================================================
-- PARAMÉTRISATION PAR ÉGLISE — Tranche résiduelle (audit profond)
-- ------------------------------------------------------------
-- 1) `DressCodePage` consommait `DRESS_CODE_CREATE|UPDATE|ARCHIVE`, clés
--    ABSENTES du catalogue servi. `TenantContext.hasPermission` est un match
--    strict (normalisé en majuscules) sur `GET /me/permissions` : ces trois
--    boutons n'ont donc JAMAIS été rendus, pour aucun rôle. La page est
--    rebranchée sur les clés réellement gardées par `DressCodeController` —
--    `DRESSCODE_EXECUTE` (création / modification / archivage) et
--    `DRESSCODE_EXECUTE_DELETE` (suppression). Matrice V258 inchangée, donc
--    parité EXACTE avec l'ancien littéral serveur
--    `hasAnyRole('TENANT_OWNER','TENANT_ADMIN','PASTEUR','RESPONSABLE')`.
--    Aucune clé n'est ajoutée pour ce correctif.
--
-- 2) Sept gardes d'UI étaient encore figées sur un littéral de rôle actif,
--    donc non paramétrables par église (aucune église ne pouvait les retirer
--    ni les accorder à un rôle personnalisé) :
--      - `MemberDashboardPage.canManage`            (MEMBRE)
--      - `ResponsableDashboardPage.canManage`        (RESPONSABLE)
--      - `ChefFamilleDashboardPage.canManage`        (CHEF_DE_FAMILLE)
--      - `DashboardPage` bloc « Vue Responsable »     (RESPONSABLE)
--      - `DashboardPage` bloc « Mes âmes »            (FAISEUR)
--      - `MemberRequestsPage` vue « Toute l'église »  (ADMIN, PASTEUR)
--      - `reports_screen.dart` (mobile)              portée départements (ADMIN, PASTEUR)
--    Chaque clé reçoit la matrice EXACTE du littéral qu'elle remplace : aucune
--    conversion n'élargit ni ne rétrécit l'accès à l'installation. Le ratchet
--    client (`parametrisationGate.test.ts` et
--    `served_permissions_parity_test.dart`) interdit ensuite toute nouvelle
--    garde à littéral et toute clé consommée hors catalogue.
--
-- Règle additive : `WHERE NOT EXISTS` sur le catalogue et `ON CONFLICT DO
-- NOTHING` sur la matrice ; une église garde le pouvoir de retirer la clé en
-- surcouche locale juste après.
--
-- ⚠ CONTRAINTE OUTILLAGE (import.sql) : les libellés de ce bloc Évitent
-- l'apostrophe. Le générateur de seed H2 (`gen_h2_seed.py`) et le verrou
-- `TestPermissionMatrixSeedGuardTest` analysent les tuples par
-- `\(\s*'([A-Z0-9_]+)'\s*,\s*'([^']+)'…\)` : un libellé contenant `''`
-- (apostrophe SQL échappée) casse la reconnaissance, et la clé disparaît
-- SILENCIEUSEMENT du seed H2 — donc des tests d'intégration greeneroient à
-- tort sur des gardes @authz.can. Règle : pas d'apostrophe dans un libellé
-- de ce genre de bloc (le générateur du seed n'est pas échappe-aware).
-- ============================================================

-- 1) Catalogue des permissions UI (globales, système).
INSERT INTO permissions (id, tenant_id, key, label, description, scope, category, system, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, p.key, p.label, p.label, 'TENANT', p.category, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('UI_DASHBOARD_MEMBRE_MANAGE_ACCESS', 'Édition de son profil depuis le tableau de bord membre', 'UI'),
    ('UI_DASHBOARD_RESPONSABLE_MANAGE_ACCESS', 'Actions de gestion depuis le tableau de bord responsable', 'UI'),
    ('UI_DASHBOARD_CHEF_FAMILLE_MANAGE_ACCESS', 'Actions de gestion depuis le tableau de bord chef de famille', 'UI'),
    ('UI_DASHBOARD_RESPONSABLE_SECTION_ACCESS', 'Bloc « Vue Responsable » du tableau de bord', 'UI'),
    ('UI_DASHBOARD_FAISER_SECTION_ACCESS', 'Bloc « Mes âmes » du tableau de bord', 'UI'),
    ('UI_MEMBER_REQUESTS_GLOBAL_SCOPE_ACCESS', 'Vue globale des demandes de membres', 'UI'),
    ('UI_REPORTS_DEPARTMENTS_GLOBAL_SCOPE_ACCESS', 'Portée départements complète dans les rapports', 'UI')
) AS p(key, label, category)
WHERE NOT EXISTS (SELECT 1 FROM permissions po WHERE po.key = p.key);

-- 2) Matrice par défaut : parité exacte des littéraux legacy remplacés.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('MEMBRE', 'UI_DASHBOARD_MEMBRE_MANAGE_ACCESS'),
    ('RESPONSABLE', 'UI_DASHBOARD_RESPONSABLE_MANAGE_ACCESS'),
    ('CHEF_DE_FAMILLE', 'UI_DASHBOARD_CHEF_FAMILLE_MANAGE_ACCESS'),
    ('RESPONSABLE', 'UI_DASHBOARD_RESPONSABLE_SECTION_ACCESS'),
    ('FAISEUR', 'UI_DASHBOARD_FAISER_SECTION_ACCESS'),
    ('ADMIN', 'UI_MEMBER_REQUESTS_GLOBAL_SCOPE_ACCESS'),
    ('PASTEUR', 'UI_MEMBER_REQUESTS_GLOBAL_SCOPE_ACCESS'),
    ('ADMIN', 'UI_REPORTS_DEPARTMENTS_GLOBAL_SCOPE_ACCESS'),
    ('PASTEUR', 'UI_REPORTS_DEPARTMENTS_GLOBAL_SCOPE_ACCESS')
) AS m(role_key, perm_key)
JOIN roles r ON r.key = m.role_key AND r.tenant_id IS NULL
JOIN permissions p ON p.key = m.perm_key
ON CONFLICT DO NOTHING;

-- 3) Répercussion des nouvelles clés globales vers les surcouches
--    locales homonymes (miroir exact du backfill V259 §5 / V261 §3 / V262 §3).
--    Additif : une église garde le pouvoir de RETIRER en local ensuite.
INSERT INTO role_permissions (role_id, permission_id)
SELECT local.id, rp.permission_id
FROM roles g
JOIN roles local ON local.key = g.key AND local.tenant_id IS NOT NULL
JOIN role_permissions rp ON rp.role_id = g.id
WHERE g.tenant_id IS NULL
ON CONFLICT DO NOTHING;
