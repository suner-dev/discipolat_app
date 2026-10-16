-- V259__parametrisation_permissions_suite_phase3.sql
-- ============================================================
-- PARAMÉTRISATION PAR ÉGLISE — Phase 3 : complément du catalogue V258.
--  (1) rôles d'autorité globaux manquants dont les paires V258 étaient
--      mortes (JOIN roles ... tenant_id IS NULL sans ligne globale) ;
--  (2) permissions pour les 21 gardes hasRole() sans clé catalogue ;
--  (3) matrice par défaut à équivalence STRICTE (rôles = littéraux) ;
--  (4) réactivation des paires V258 des rôles (1) ;
--  (5) héritage one-shot global -> surcouche locale homonyme (la
--      résolution du pont fait findByTenantIdAndKey(...).or(global) : la
--      surcouche PRIME et redefinit — sans ce backfill, la conversion
--      d'un endpoint RÉTRÉCIRAIT l'accès des églises ayant une surcouche.
-- Purement ADDITIF : aucun DELETE, aucune modification de ligne existante.
-- Cible PostgreSQL (Flyway désactivé sous H2/tests).
-- ============================================================

-- 1) Rôles d'autorité absents en rôles système globaux (paires V258 mortes).
INSERT INTO roles (id, tenant_id, key, label, description, system, priority, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, k.key, k.label, k.label, TRUE, k.priority, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('CAMPUS_ADMIN', 'Administrateur Campus', 690),
    ('SUB_CHURCH_ADMIN', 'Administrateur Sous-Eglise', 680),
    ('REGION_ADMIN', 'Administrateur Region', 750),
    ('PASTOR_PRINCIPAL', 'Pasteur Principal', 860)
) AS k(key, label, priority)
WHERE NOT EXISTS (SELECT 1 FROM roles ro WHERE ro.tenant_id IS NULL AND ro.key = k.key);

-- 2) Permissions des sites hasRole() sans clé catalogue (convention
--    MODULE_EXECUTE_<MÉTHODE>, rôles = littéraux exacts des gardes).
INSERT INTO permissions (id, tenant_id, key, label, description, scope, category, system, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, p.key, p.label, p.label, 'TENANT', p.category, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('REPORTS_EXECUTE_CORRECTREPORT', 'Reports Execute Correctreport', 'REPORTS_EXECUTE'),
    ('EVENTS_EXECUTE_CREATETEMPLATE', 'Events Execute Createtemplate', 'EVENTS_EXECUTE'),
    ('EVENTS_EXECUTE_UPDATETEMPLATE', 'Events Execute Updatetemplate', 'EVENTS_EXECUTE'),
    ('EVENTS_EXECUTE_DELETETEMPLATE', 'Events Execute Deletetemplate', 'EVENTS_EXECUTE'),
    ('EVENTS_EXECUTE_TOGGLETEMPLATE', 'Events Execute Toggletemplate', 'EVENTS_EXECUTE'),
    ('EVENTS_EXECUTE_GENERATEWEEKPROGRAM', 'Events Execute Generateweekprogram', 'EVENTS_EXECUTE'),
    ('EVENTS_EXECUTE_GENERATEMONTHPROGRAM', 'Events Execute Generatemonthprogram', 'EVENTS_EXECUTE'),
    ('EVENTS_EXECUTE_GETEVENTSTATISTICS', 'Events Execute Geteventstatistics', 'EVENTS_EXECUTE'),
    ('EVENTS_EXECUTE_GETCONSOLIDATEDUPCOMING', 'Events Execute Getconsolidatedupcoming', 'EVENTS_EXECUTE'),
    ('EVENTS_EXECUTE_GETCONSOLIDATEDBYFAMILY', 'Events Execute Getconsolidatedbyfamily', 'EVENTS_EXECUTE'),
    ('WORKFLOW_EXECUTE_DELETEAUTOMATION', 'Workflow Execute Deleteautomation', 'WORKFLOW_EXECUTE'),
    ('IMPORTS_EXECUTE_VALIDATE', 'Imports Execute Validate', 'IMPORTS_EXECUTE'),
    ('IMPORTS_EXECUTE_IMPORTDATA', 'Imports Execute Importdata', 'IMPORTS_EXECUTE'),
    ('PEOPLE_EXECUTE_MERGEPERSONS', 'People Execute Mergepersons', 'PEOPLE_EXECUTE'),
    ('PEOPLE_EXECUTE_TRANSFERPASTOR', 'People Execute Transferpastor', 'PEOPLE_EXECUTE'),
    ('CONFIG_EXECUTE_CREATETEMPLATE', 'Config Execute Createtemplate', 'CONFIG_EXECUTE'),
    ('CONFIG_EXECUTE_UPDATETEMPLATE', 'Config Execute Updatetemplate', 'CONFIG_EXECUTE'),
    ('CONFIG_EXECUTE_DELETETEMPLATE', 'Config Execute Deletetemplate', 'CONFIG_EXECUTE'),
    ('FAMILIES_EXECUTE_COMPAREFAMILIES', 'Families Execute Comparefamilies', 'FAMILIES_EXECUTE'),
    ('EVALUATIONS_EXECUTE_GETALLEVALUATIONS', 'Evaluations Execute Getallevaluations', 'EVALUATIONS_EXECUTE'),
    ('USERS_EXECUTE_HARDDELETE', 'Users Execute Harddelete', 'USERS_EXECUTE')
) AS p(key, label, category)
ON CONFLICT (key) DO NOTHING;

-- 3) Matrice par défaut des clés de (2) : exactement les rôles du littéral.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('ADMIN', 'REPORTS_EXECUTE_CORRECTREPORT'),
    ('PASTEUR', 'EVENTS_EXECUTE_CREATETEMPLATE'),
    ('PASTEUR', 'EVENTS_EXECUTE_UPDATETEMPLATE'),
    ('PASTEUR', 'EVENTS_EXECUTE_DELETETEMPLATE'),
    ('PASTEUR', 'EVENTS_EXECUTE_TOGGLETEMPLATE'),
    ('PASTEUR', 'EVENTS_EXECUTE_GENERATEWEEKPROGRAM'),
    ('PASTEUR', 'EVENTS_EXECUTE_GENERATEMONTHPROGRAM'),
    ('PASTEUR', 'EVENTS_EXECUTE_GETEVENTSTATISTICS'),
    ('PASTEUR', 'EVENTS_EXECUTE_GETCONSOLIDATEDUPCOMING'),
    ('PASTEUR', 'EVENTS_EXECUTE_GETCONSOLIDATEDBYFAMILY'),
    ('ADMIN', 'WORKFLOW_EXECUTE_DELETEAUTOMATION'),
    ('ADMIN', 'IMPORTS_EXECUTE_VALIDATE'),
    ('ADMIN', 'IMPORTS_EXECUTE_IMPORTDATA'),
    ('ADMIN', 'PEOPLE_EXECUTE_MERGEPERSONS'),
    ('PASTOR_PRINCIPAL', 'PEOPLE_EXECUTE_TRANSFERPASTOR'),
    ('ADMIN', 'CONFIG_EXECUTE_CREATETEMPLATE'),
    ('ADMIN', 'CONFIG_EXECUTE_UPDATETEMPLATE'),
    ('ADMIN', 'CONFIG_EXECUTE_DELETETEMPLATE'),
    ('PASTEUR', 'FAMILIES_EXECUTE_COMPAREFAMILIES'),
    ('PASTEUR', 'EVALUATIONS_EXECUTE_GETALLEVALUATIONS'),
    ('ADMIN', 'USERS_EXECUTE_HARDDELETE')
) AS m(role_key, perm_key)
JOIN roles r ON r.key = m.role_key AND r.tenant_id IS NULL
JOIN permissions p ON p.key = m.perm_key
ON CONFLICT DO NOTHING;

-- 4) Réactivation des paires V258 citant les rôles de (1) : ces lignes
--    étaient insérables mais le JOIN les filtrait (aucune ligne globale).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('CAMPUS_ADMIN', 'PLATFORM_EXECUTE_UPDATENODE'),
    ('PASTOR_PRINCIPAL', 'FAMILIES_APPROVE'),
    ('PASTOR_PRINCIPAL', 'FAMILIES_EXECUTE'),
    ('PASTOR_PRINCIPAL', 'FAMILIES_EXECUTE_GETAPPOINTMENT'),
    ('PASTOR_PRINCIPAL', 'FAMILIES_EXECUTE_GETAPPOINTMENTS'),
    ('PASTOR_PRINCIPAL', 'FAMILIES_EXECUTE_GETPASTORHISTORY'),
    ('PASTOR_PRINCIPAL', 'FAMILIES_EXECUTE_GETTRANSFERS'),
    ('REGION_ADMIN', 'PLATFORM_EXECUTE_CREATENODE'),
    ('SUB_CHURCH_ADMIN', 'PLATFORM_EXECUTE_UPDATENODE')
) AS m(role_key, perm_key)
JOIN roles r ON r.key = m.role_key AND r.tenant_id IS NULL
JOIN permissions p ON p.key = m.perm_key
ON CONFLICT DO NOTHING;

-- 5) Héritage one-shot : tout rôle LOCAL homonyme d'un rôle système global
--    reçoit l'ensemble des capacités du jumeau global. Additif (ON CONFLICT
--    DO NOTHING) ; l'église conserve le pouvoir de RETIRER en local ensuite.
INSERT INTO role_permissions (role_id, permission_id)
SELECT local.id, rp.permission_id
FROM roles g
JOIN roles local ON local.key = g.key AND local.tenant_id IS NOT NULL
JOIN role_permissions rp ON rp.role_id = g.id
WHERE g.tenant_id IS NULL
ON CONFLICT DO NOTHING;
