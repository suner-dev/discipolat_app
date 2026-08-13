-- V49__department_management_dictionaries_permissions.sql
-- ============================================================
-- DEPARTMENT MANAGEMENT SYSTEM — configuration
-- Dictionnaires configurables (libellés des statuts de tâches,
-- priorités, types d'équipes, rôles d'affectation) + entrées du
-- catalogue de permissions pour les actions de gestion.
-- Miroirs des enums backend : l'administrateur personnalise les
-- libellés affichés sans modifier le code.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Statuts de tâches (enum backend DepartmentTask.TaskStatus)
-- ------------------------------------------------------------
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DEPARTMENT_TASK_STATUT', 'A_FAIRE', 'À faire', '#94a3b8', 1, TRUE),
    ('DEPARTMENT_TASK_STATUT', 'EN_COURS', 'En cours', '#3b82f6', 2, TRUE),
    ('DEPARTMENT_TASK_STATUT', 'BLOQUEE', 'Bloquée', '#ef4444', 3, TRUE),
    ('DEPARTMENT_TASK_STATUT', 'TERMINEE', 'Terminée', '#22c55e', 4, TRUE),
    ('DEPARTMENT_TASK_STATUT', 'VALIDEE', 'Validée', '#10b981', 5, TRUE),
    ('DEPARTMENT_TASK_STATUT', 'ANNULEE', 'Annulée', '#6b7280', 6, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- ------------------------------------------------------------
-- 2. Priorités de tâches (enum backend DepartmentTask.TaskPriority)
-- ------------------------------------------------------------
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DEPARTMENT_TASK_PRIORITE', 'BASSE', 'Basse', '#94a3b8', 1, TRUE),
    ('DEPARTMENT_TASK_PRIORITE', 'MOYENNE', 'Moyenne', '#3b82f6', 2, TRUE),
    ('DEPARTMENT_TASK_PRIORITE', 'HAUTE', 'Haute', '#f59e0b', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- ------------------------------------------------------------
-- 3. Types d'équipes / sous-départements (enum DepartmentTeam.TeamType)
-- ------------------------------------------------------------
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DEPARTMENT_TEAM_TYPE', 'SOUS_DEPARTEMENT', 'Sous-département', '#06b6d4', 1, TRUE),
    ('DEPARTMENT_TEAM_TYPE', 'EQUIPE_PERMANENTE', 'Équipe permanente', '#3b82f6', 2, TRUE),
    ('DEPARTMENT_TEAM_TYPE', 'EQUIPE_TEMPORAIRE', 'Équipe temporaire', '#a855f7', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- ------------------------------------------------------------
-- 4. Rôles d'affectation (enum DepartmentAssignment.AssignmentRole)
-- ------------------------------------------------------------
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DEPARTMENT_ASSIGNMENT_ROLE', 'CHEF', 'Chef', '#f59e0b', 1, TRUE),
    ('DEPARTMENT_ASSIGNMENT_ROLE', 'ADJOINT', 'Adjoint', '#3b82f6', 2, TRUE),
    ('DEPARTMENT_ASSIGNMENT_ROLE', 'MEMBRE', 'Membre', '#94a3b8', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- ------------------------------------------------------------
-- 5. Catalogue de permissions du Department Management System
-- (documentation + matrice admin ; l'exécution reste fondée sur
-- le rôle actif et le scoping par département)
-- ------------------------------------------------------------
INSERT INTO permission_catalog (key, label, module, description, ordre) VALUES
    ('DEPARTMENT_TEAMS_VIEW', 'Consulter les équipes / sous-départements', 'Départements', 'Consulter les équipes / sous-départements du département.', 18),
    ('DEPARTMENT_TEAMS_MANAGE', 'Gérer les équipes', 'Départements', 'Créer, modifier et archiver les équipes / sous-départements.', 19),
    ('DEPARTMENT_POSITIONS_VIEW', 'Consulter les postes', 'Départements', 'Consulter les postes du département.', 20),
    ('DEPARTMENT_POSITIONS_MANAGE', 'Gérer les postes', 'Départements', 'Créer, modifier et archiver les postes du département.', 21),
    ('DEPARTMENT_ASSIGNMENTS_VIEW', 'Consulter les affectations', 'Départements', 'Consulter les affectations des membres.', 22),
    ('DEPARTMENT_ASSIGNMENTS_MANAGE', 'Gérer les affectations', 'Départements', 'Affecter ou retirer les membres des équipes / postes.', 23),
    ('DEPARTMENT_TASKS_VIEW', 'Consulter les tâches', 'Départements', 'Consulter les tâches du département.', 24),
    ('DEPARTMENT_TASKS_MANAGE', 'Gérer les tâches', 'Départements', 'Créer, modifier et clôturer les tâches du département.', 25),
    ('DEPARTMENT_MEMBERS_MANAGE', 'Gérer les membres', 'Départements', 'Ajouter, créer ou retirer les membres du département.', 26),
    ('DEPARTMENT_ACTIVITY_VIEW', 'Consulter le journal d''activité', 'Départements', 'Consulter le journal d''activité du département.', 27)
ON CONFLICT (key) DO NOTHING;
