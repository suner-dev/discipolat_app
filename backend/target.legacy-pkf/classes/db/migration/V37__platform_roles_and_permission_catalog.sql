-- V37__platform_roles_and_permission_catalog.sql
-- ============================================================
-- GESTION DES ROLES ET CATALOGUE DE PERMISSIONS
-- Les roles systeme (non supprimables) sont completes par des roles
-- personnalises crees par l'administrateur. Chaque role dispose d'une
-- matrice de permissions (role_permissions, V6) editable sans code.
-- ============================================================

CREATE TABLE IF NOT EXISTS platform_roles (
    key VARCHAR(50) PRIMARY KEY,
    label VARCHAR(255) NOT NULL,
    description TEXT,
    system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS permission_catalog (
    key VARCHAR(100) PRIMARY KEY,
    label VARCHAR(255) NOT NULL,
    module VARCHAR(100) NOT NULL DEFAULT 'General',
    description TEXT,
    ordre INTEGER NOT NULL DEFAULT 0
);

-- ============================================================
-- Roles systeme (etiquettes francaises, non supprimables)
-- ============================================================
INSERT INTO platform_roles (key, label, description, system) VALUES
    ('ADMIN', 'Administrateur', 'Acces complet : configuration de la plateforme, roles, permissions, audit.', TRUE),
    ('PASTEUR', 'Pasteur', 'Centre de commandement : discipolat, rapports, validation finale, utilisateurs.', TRUE),
    ('RESPONSABLE', 'Responsable de departement', 'Gestion des departements, des membres, des presences et des rapports.', TRUE),
    ('CHEF_DE_FAMILLE', 'Chef de famille', 'Gestion de la famille de disciples, suivi et rapports de famille.', TRUE),
    ('FAISEUR', 'Faiseur de disciples', 'Suivi quotidien des disciples, rapports hebdomadaires et activites.', TRUE),
    ('MEMBRE', 'Membre', 'Espace personnel : profil, presences, formations, rendez-vous.', TRUE)
ON CONFLICT (key) DO NOTHING;

-- ============================================================
-- Catalogue des permissions connues (regroupees par module)
-- ============================================================
INSERT INTO permission_catalog (key, label, module, description, ordre) VALUES
    -- Utilisateurs
    ('USER_CREATE', 'Creer des utilisateurs', 'Utilisateurs', 'Creer des comptes utilisateurs.', 1),
    ('USER_READ', 'Lire les utilisateurs', 'Utilisateurs', 'Consulter les comptes utilisateurs.', 2),
    ('USER_UPDATE', 'Modifier les utilisateurs', 'Utilisateurs', 'Modifier les comptes (roles, statut, informations).', 3),
    ('USER_DELETE', 'Supprimer les utilisateurs', 'Utilisateurs', 'Supprimer / desactiver des comptes.', 4),
    ('USER_EXPORT', 'Exporter les utilisateurs', 'Utilisateurs', 'Exporter la liste des utilisateurs.', 5),
    -- Âmes & disciples
    ('SOUL_CREATE', 'Creer des âmes', 'Discipolat', 'Creer des fiches de disciples.', 6),
    ('SOUL_READ', 'Lire les âmes', 'Discipolat', 'Consulter les fiches de disciples.', 7),
    ('SOUL_UPDATE', 'Modifier les âmes', 'Discipolat', 'Modifier les fiches de disciples.', 8),
    ('SOUL_DELETE', 'Supprimer des âmes', 'Discipolat', 'Supprimer des fiches de disciples.', 9),
    -- Familles
    ('FAMILY_CREATE', 'Creer des familles', 'Familles', 'Creer des familles de disciples.', 10),
    ('FAMILY_READ', 'Lire les familles', 'Familles', 'Consulter les familles.', 11),
    ('FAMILY_UPDATE', 'Modifier les familles', 'Familles', 'Modifier les familles et leur chef.', 12),
    ('FAMILY_DELETE', 'Supprimer des familles', 'Familles', 'Supprimer des familles.', 13),
    -- Departements
    ('DEPARTMENT_CREATE', 'Creer des departements', 'Departements', 'Creer des departements.', 14),
    ('DEPARTMENT_READ', 'Lire les departements', 'Departements', 'Consulter les departements.', 15),
    ('DEPARTMENT_UPDATE', 'Modifier les departements', 'Departements', 'Modifier les departements et leurs responsables.', 16),
    ('DEPARTMENT_DELETE', 'Supprimer des departements', 'Departements', 'Supprimer des departements.', 17),
    -- Rapports
    ('REPORT_CREATE', 'Creer des rapports', 'Rapports', 'Creer et soumettre des rapports hebdomadaires.', 18),
    ('REPORT_READ', 'Lire les rapports', 'Rapports', 'Consulter les rapports.', 19),
    ('REPORT_UPDATE', 'Modifier les rapports', 'Rapports', 'Modifier et corriger des rapports.', 20),
    ('REPORT_EXPORT', 'Exporter les rapports', 'Rapports', 'Exporter les rapports (CSV, PDF).', 21),
    ('REPORT_VALIDATE', 'Valider les rapports', 'Rapports', 'Valider les rapports aux niveaux de responsabilite.', 22),
    ('REPORT_CORRECT', 'Corriger les rapports', 'Rapports', 'Renvoi de correction aux auteurs de rapports.', 23),
    -- Transferts
    ('TRANSFER_CREATE', 'Initier des transferts', 'Transferts', 'Creer des demandes de transfert.', 24),
    ('TRANSFER_READ', 'Lire les transferts', 'Transferts', 'Consulter les demandes et leurs historiques.', 25),
    ('TRANSFER_VALIDATE', 'Valider des transferts', 'Transferts', 'Prendre une decision sur une demande.', 26),
    ('TRANSFER_EXECUTE', 'Executer des transferts', 'Transferts', 'Executer les transferts valides.', 27),
    ('TRANSFER_CONFIGURE', 'Configurer les workflows', 'Transferts', 'Configurer les circuits de validation.', 28),
    -- Vie de l'eglise
    ('PRAYER_CREATE', 'Creer des prieres', 'Prieres', 'Partager des sujets de priere.', 29),
    ('PRAYER_READ', 'Lire les prieres', 'Prieres', 'Consulter les sujets de priere.', 30),
    ('EVENT_CREATE', 'Creer des evenements', 'Evenements', 'Creer des evenements et programmes.', 31),
    ('EVENT_READ', 'Lire les evenements', 'Evenements', 'Consulter le calendrier.', 32),
    ('EVENT_UPDATE', 'Modifier des evenements', 'Evenements', 'Modifier des evenements.', 33),
    ('EVENT_DELETE', 'Supprimer des evenements', 'Evenements', 'Supprimer des evenements.', 34),
    -- Engagement & outils
    ('DOCUMENT_UPLOAD', 'Televerser des documents', 'Documents', 'Joindre des fichiers aux entites.', 35),
    ('DOCUMENT_READ', 'Lire les documents', 'Documents', 'Consulter les documents partages.', 36),
    ('ALERT_MANAGE', 'Gerer les alertes', 'Alertes', 'Creer, traiter et resoudre les alertes.', 37),
    ('EVALUATION_READ', 'Lire les evaluations', 'Evaluations', 'Consulter les evaluations anonymes.', 38),
    ('EVALUATION_CREATE', 'Repondre aux evaluations', 'Evaluations', 'Participer aux evaluations.', 39),
    -- Administration
    ('AUDIT_READ', 'Lire le journal d''audit', 'Administration', 'Consulter l''historique complet des actions.', 40),
    ('PERMISSION_MANAGE', 'Gerer les permissions', 'Administration', 'Modifier la matrice des roles.', 41),
    ('SETTINGS_MANAGE', 'Gerer l''identite & la marque', 'Administration', 'Configurer les parametres de l''eglise.', 42),
    ('MODULE_MANAGE', 'Gerer les modules', 'Administration', 'Activer / desactiver les modules.', 43),
    ('MENU_MANAGE', 'Gerer les menus', 'Administration', 'Configurer la navigation.', 44),
    ('BULK_IMPORT', 'Importer des donnees', 'Administration', 'Imports en masse de donnees.', 45),
    ('DATA_EXPORT', 'Exporter les donnees', 'Administration', 'Exports avances.', 46)
ON CONFLICT (key) DO NOTHING;

COMMENT ON TABLE platform_roles IS 'Roles systeme (labels, description) + roles personnalises crees par l''administrateur (matrice de permissions editables).';
COMMENT ON TABLE permission_catalog IS 'Catalogue des permissions connues, regroupees par module pour l''interface d''administration.';
