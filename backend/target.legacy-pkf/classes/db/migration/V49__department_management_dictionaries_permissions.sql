-- V49__department_management_dictionaries_permissions.sql
-- ============================================================
-- DEPARTMENT MANAGEMENT SYSTEM — configuration
-- Dictionnaires configurables (libelles des statuts de tâches,
-- priorites, types d'equipes, roles d'affectation) + entrees du
-- catalogue de permissions pour les actions de gestion.
-- Miroirs des enums backend : l'administrateur personnalise les
-- libelles affiches sans modifier le code.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Statuts de tâches (enum backend DepartmentTask.TaskStatus)
-- ------------------------------------------------------------
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DEPARTMENT_TASK_STATUT', 'A_FAIRE', 'A faire', '#94a3b8', 1, TRUE),
    ('DEPARTMENT_TASK_STATUT', 'EN_COURS', 'En cours', '#3b82f6', 2, TRUE),
    ('DEPARTMENT_TASK_STATUT', 'BLOQUEE', 'Bloquee', '#ef4444', 3, TRUE),
    ('DEPARTMENT_TASK_STATUT', 'TERMINEE', 'Terminee', '#22c55e', 4, TRUE),
    ('DEPARTMENT_TASK_STATUT', 'VALIDEE', 'Validee', '#10b981', 5, TRUE),
    ('DEPARTMENT_TASK_STATUT', 'ANNULEE', 'Annulee', '#6b7280', 6, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- ------------------------------------------------------------
-- 2. Priorites de tâches (enum backend DepartmentTask.TaskPriority)
-- ------------------------------------------------------------
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DEPARTMENT_TASK_PRIORITE', 'BASSE', 'Basse', '#94a3b8', 1, TRUE),
    ('DEPARTMENT_TASK_PRIORITE', 'MOYENNE', 'Moyenne', '#3b82f6', 2, TRUE),
    ('DEPARTMENT_TASK_PRIORITE', 'HAUTE', 'Haute', '#f59e0b', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- ------------------------------------------------------------
-- 3. Types d'equipes / sous-departements (enum DepartmentTeam.TeamType)
-- ------------------------------------------------------------
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DEPARTMENT_TEAM_TYPE', 'SOUS_DEPARTEMENT', 'Sous-departement', '#06b6d4', 1, TRUE),
    ('DEPARTMENT_TEAM_TYPE', 'EQUIPE_PERMANENTE', 'Equipe permanente', '#3b82f6', 2, TRUE),
    ('DEPARTMENT_TEAM_TYPE', 'EQUIPE_TEMPORAIRE', 'Equipe temporaire', '#a855f7', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- ------------------------------------------------------------
-- 4. Roles d'affectation (enum DepartmentAssignment.AssignmentRole)
-- ------------------------------------------------------------
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DEPARTMENT_ASSIGNMENT_ROLE', 'CHEF', 'Chef', '#f59e0b', 1, TRUE),
    ('DEPARTMENT_ASSIGNMENT_ROLE', 'ADJOINT', 'Adjoint', '#3b82f6', 2, TRUE),
    ('DEPARTMENT_ASSIGNMENT_ROLE', 'MEMBRE', 'Membre', '#94a3b8', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- ------------------------------------------------------------
-- 5. Catalogue de permissions du Department Management System
-- (documentation + matrice admin ; l'execution reste fondee sur
-- le role actif et le scoping par departement)
-- ------------------------------------------------------------
INSERT INTO permission_catalog (key, label, module, description, ordre) VALUES
    ('DEPARTMENT_TEAMS_VIEW', 'Consulter les equipes / sous-departements', 'Departements', 'Consulter les equipes / sous-departements du departement.', 18),
    ('DEPARTMENT_TEAMS_MANAGE', 'Gerer les equipes', 'Departements', 'Creer, modifier et archiver les equipes / sous-departements.', 19),
    ('DEPARTMENT_POSITIONS_VIEW', 'Consulter les postes', 'Departements', 'Consulter les postes du departement.', 20),
    ('DEPARTMENT_POSITIONS_MANAGE', 'Gerer les postes', 'Departements', 'Creer, modifier et archiver les postes du departement.', 21),
    ('DEPARTMENT_ASSIGNMENTS_VIEW', 'Consulter les affectations', 'Departements', 'Consulter les affectations des membres.', 22),
    ('DEPARTMENT_ASSIGNMENTS_MANAGE', 'Gerer les affectations', 'Departements', 'Affecter ou retirer les membres des equipes / postes.', 23),
    ('DEPARTMENT_TASKS_VIEW', 'Consulter les tâches', 'Departements', 'Consulter les tâches du departement.', 24),
    ('DEPARTMENT_TASKS_MANAGE', 'Gerer les tâches', 'Departements', 'Creer, modifier et cloturer les tâches du departement.', 25),
    ('DEPARTMENT_MEMBERS_MANAGE', 'Gerer les membres', 'Departements', 'Ajouter, creer ou retirer les membres du departement.', 26),
    ('DEPARTMENT_ACTIVITY_VIEW', 'Consulter le journal d''activite', 'Departements', 'Consulter le journal d''activite du departement.', 27)
ON CONFLICT (key) DO NOTHING;
