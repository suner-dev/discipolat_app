-- V39__demo_seed_platform_config.sql
-- ============================================================
-- SEED DE DEMONSTRATION POUR LA PLATEFORME CONFIGURABLE
-- Peuple les nouvelles tables avec des donnees d'exemple :
--   1. Champs personnalises par defaut (definitions) pour les
--      entites SOUL / USER / FAMILY / DEPARTMENT ;
--   2. Valeurs de demonstration sur les donnees seedees (V2) —
--      âmes, famille, departement — pour illustrer le rendu ;
--   3. Roles personnalises exemples (non systeme) avec leur
--      matrice de permissions (role_permissions).
-- Idempotent : ON CONFLICT DO NOTHING partout.
-- ============================================================

-- ============================================================
-- 1. DEFINITIONS DE CHAMPS PERSONNALISES PAR DEFAUT
--    (roles_lecture/roles_ecriture vides = visible/editable par tous)
-- ============================================================
INSERT INTO custom_field_definitions
    (id, entite_type, code, label, type, obligatoire, ordre, options, placeholder, default_value, roles_lecture, roles_ecriture, actif)
VALUES
    -- Âme (SOUL)
    ('90000000-0000-0000-0000-000000000001', 'SOUL', 'LANGUE', 'Langue parlee', 'TEXTE', FALSE, 1, NULL, 'Ex : Francais, Lingala…', NULL, '[]'::jsonb, '[]'::jsonb, TRUE),
    ('90000000-0000-0000-0000-000000000002', 'SOUL', 'PROFESSION', 'Profession', 'TEXTE', FALSE, 2, NULL, 'Ex : Infirmiere, etudiant…', NULL, '[]'::jsonb, '[]'::jsonb, TRUE),
    ('90000000-0000-0000-0000-000000000003', 'SOUL', 'DATE_NAISSANCE', 'Date de naissance', 'DATE', FALSE, 3, NULL, 'JJ/MM/AAAA', NULL, '[]'::jsonb, '[]'::jsonb, TRUE),
    ('90000000-0000-0000-0000-000000000004', 'SOUL', 'NIVEAU_ETUDE', 'Niveau d''etudes', 'SELECTION', FALSE, 4, '["Primaire","Secondaire","Universite","Formation professionnelle","Aucun"]'::jsonb, 'Choisir…', NULL, '[]'::jsonb, '[]'::jsonb, TRUE),
    ('90000000-0000-0000-0000-000000000005', 'SOUL', 'SITUATION_FAMILIALE', 'Situation familiale', 'SELECTION', FALSE, 5, '["Celibataire","Marie(e)","Divorce(e)","Veuf(ve)"]'::jsonb, 'Choisir…', NULL, '[]'::jsonb, '[]'::jsonb, TRUE),
    ('90000000-0000-0000-0000-000000000006', 'SOUL', 'TALENT', 'Talent / don', 'TEXTE', FALSE, 6, NULL, 'Ex : Chant, enseignement…', NULL, '[]'::jsonb, '[]'::jsonb, TRUE),
    ('90000000-0000-0000-0000-000000000007', 'SOUL', 'OBSERVATIONS', 'Observations', 'TEXTAREA', FALSE, 7, NULL, 'Suivi pastoral, points d''attention…', NULL, '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, TRUE),
    -- Utilisateur (USER)
    ('90000000-0000-0000-0000-000000000008', 'USER', 'TELEPHONE_SECONDAIRE', 'Telephone secondaire', 'TELEPHONE', FALSE, 1, NULL, 'Numero de secours', NULL, '[]'::jsonb, '[]'::jsonb, TRUE),
    ('90000000-0000-0000-0000-000000000009', 'USER', 'PROFESSION', 'Profession', 'TEXTE', FALSE, 2, NULL, 'Ex : Enseignant, commercant…', NULL, '[]'::jsonb, '[]'::jsonb, TRUE),
    -- Famille (FAMILY)
    ('90000000-0000-0000-0000-000000000010', 'FAMILY', 'QUARTIER', 'Quartier', 'TEXTE', FALSE, 1, NULL, 'Quartier de la famille', NULL, '[]'::jsonb, '[]'::jsonb, TRUE),
    -- Departement (DEPARTMENT)
    ('90000000-0000-0000-0000-000000000011', 'DEPARTMENT', 'OBJECTIF_ANNEE', 'Objectif de l''annee', 'TEXTAREA', FALSE, 1, NULL, 'Objectif pastoral de l''annee', NULL, '[]'::jsonb, '[]'::jsonb, TRUE)
ON CONFLICT (entite_type, code) DO NOTHING;

-- ============================================================
-- 2. VALEURS DE DEMONSTRATION
--    Reference les entites seedees par V2 (âmes d0000000-…,
--    famille c0000000-…-001, departement b0000000-…-001).
-- ============================================================
INSERT INTO custom_field_values (entite_type, entite_id, field_id, value)
VALUES
    -- Marie Dupont (d…-001)
    ('SOUL', 'd0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001', 'Francais'),
    ('SOUL', 'd0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000002', 'Infirmiere'),
    ('SOUL', 'd0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000003', '1998-04-12'),
    ('SOUL', 'd0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000004', 'Universite'),
    ('SOUL', 'd0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000005', 'Celibataire'),
    ('SOUL', 'd0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000006', 'Chant'),
    ('SOUL', 'd0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000007', 'Nouvelle convertie — suivi hebdomadaire regulier.'),

    -- Jean Martin (d…-002)
    ('SOUL', 'd0000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000001', 'Francais'),
    ('SOUL', 'd0000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000004', 'Secondaire'),
    ('SOUL', 'd0000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000005', 'Celibataire'),

    -- Sophie Bernard (d…-003)
    ('SOUL', 'd0000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000001', 'Francais'),
    ('SOUL', 'd0000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000002', 'Enseignante'),
    ('SOUL', 'd0000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000005', 'Marie(e)'),

    -- Anne Robert (d…-005)
    ('SOUL', 'd0000000-0000-0000-0000-000000000005', '90000000-0000-0000-0000-000000000002', 'Commercante'),
    ('SOUL', 'd0000000-0000-0000-0000-000000000005', '90000000-0000-0000-0000-000000000006', 'Enseignement'),

    -- Claire Durand (d…-007)
    ('SOUL', 'd0000000-0000-0000-0000-000000000007', '90000000-0000-0000-0000-000000000007', 'Absente prolongee detectee (48h sans contact) — en veille.'),

    -- Famille Timothee (c…-001)
    ('FAMILY', 'c0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000010', 'Quartier Kintambo'),

    -- Departement Jeunesse (b…-001)
    ('DEPARTMENT', 'b0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000011', 'Accompagner 20 nouveaux convertis vers l''integration complete.'),

    -- Utilisateurs de demo (a…-001 pasteur, a…-002/-003 responsables, a…-004 chef)
    ('USER', 'a0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000008', '+243 812 345 678'),
    ('USER', 'a0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000009', 'Pasteur principal'),
    ('USER', 'a0000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000008', '+243 823 456 789'),
    ('USER', 'a0000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000009', 'Comptable'),
    ('USER', 'a0000000-0000-0000-0000-000000000004', '90000000-0000-0000-0000-000000000009', 'Etudiant')
ON CONFLICT (entite_type, entite_id, field_id) DO NOTHING;

-- ============================================================
-- 3. ROLES PERSONNALISES EXEMPLES (non systeme)
--    Visibles dans la gestion des roles (PermissionsPage) avec
--    leur matrice de permissions prete a l'emploi.
-- ============================================================
INSERT INTO platform_roles (key, label, description, system) VALUES
    ('SECRETAIRE', 'Secretaire', 'Role personnalise d''exemple : gestion administrative, lecture des rapports et du registre.', FALSE),
    ('TRESORIER', 'Tresorier', 'Role personnalise d''exemple : acces aux rapports, export des donnees, lecture du registre.', FALSE),
    ('RESPONSABLE_COMMUNICATION', 'Responsable Communication', 'Role personnalise d''exemple : evenements, prieres, documents et communication.', FALSE),
    ('INTERCESSEUR', 'Intercesseur', 'Role personnalise d''exemple : sujets de priere et evaluations.', FALSE)
ON CONFLICT (key) DO NOTHING;

INSERT INTO role_permissions (role, permission, enabled) VALUES
    -- Secretaire : lecture large + gestion documentaire + rapport export
    ('SECRETAIRE', 'USER_READ', TRUE),
    ('SECRETAIRE', 'SOUL_READ', TRUE),
    ('SECRETAIRE', 'SOUL_CREATE', TRUE),
    ('SECRETAIRE', 'SOUL_UPDATE', TRUE),
    ('SECRETAIRE', 'FAMILY_READ', TRUE),
    ('SECRETAIRE', 'DEPARTMENT_READ', TRUE),
    ('SECRETAIRE', 'REPORT_READ', TRUE),
    ('SECRETAIRE', 'REPORT_EXPORT', TRUE),
    ('SECRETAIRE', 'EVENT_READ', TRUE),
    ('SECRETAIRE', 'DOCUMENT_UPLOAD', TRUE),
    ('SECRETAIRE', 'DOCUMENT_READ', TRUE),
    ('SECRETAIRE', 'EVALUATION_READ', TRUE),
    -- Tresorier : lecture registre + rapports + exports
    ('TRESORIER', 'USER_READ', TRUE),
    ('TRESORIER', 'SOUL_READ', TRUE),
    ('TRESORIER', 'FAMILY_READ', TRUE),
    ('TRESORIER', 'DEPARTMENT_READ', TRUE),
    ('TRESORIER', 'REPORT_READ', TRUE),
    ('TRESORIER', 'REPORT_EXPORT', TRUE),
    ('TRESORIER', 'EVENT_READ', TRUE),
    ('TRESORIER', 'DATA_EXPORT', TRUE),
    ('TRESORIER', 'DOCUMENT_READ', TRUE),
    -- Responsable Communication : evenements + prieres + documents
    ('RESPONSABLE_COMMUNICATION', 'EVENT_CREATE', TRUE),
    ('RESPONSABLE_COMMUNICATION', 'EVENT_READ', TRUE),
    ('RESPONSABLE_COMMUNICATION', 'EVENT_UPDATE', TRUE),
    ('RESPONSABLE_COMMUNICATION', 'PRAYER_CREATE', TRUE),
    ('RESPONSABLE_COMMUNICATION', 'PRAYER_READ', TRUE),
    ('RESPONSABLE_COMMUNICATION', 'DOCUMENT_UPLOAD', TRUE),
    ('RESPONSABLE_COMMUNICATION', 'DOCUMENT_READ', TRUE),
    ('RESPONSABLE_COMMUNICATION', 'USER_READ', TRUE),
    -- Intercesseur : prieres + evaluations
    ('INTERCESSEUR', 'PRAYER_CREATE', TRUE),
    ('INTERCESSEUR', 'PRAYER_READ', TRUE),
    ('INTERCESSEUR', 'EVALUATION_CREATE', TRUE),
    ('INTERCESSEUR', 'EVALUATION_READ', TRUE),
    ('INTERCESSEUR', 'SOUL_READ', TRUE)
ON CONFLICT (role, permission) DO NOTHING;
