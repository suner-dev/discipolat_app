-- V37__platform_roles_and_permission_catalog.sql
-- ============================================================
-- GESTION DES RÔLES ET CATALOGUE DE PERMISSIONS
-- Les rôles système (non supprimables) sont complétés par des rôles
-- personnalisés créés par l'administrateur. Chaque rôle dispose d'une
-- matrice de permissions (role_permissions, V6) éditable sans code.
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
    module VARCHAR(100) NOT NULL DEFAULT 'Général',
    description TEXT,
    ordre INTEGER NOT NULL DEFAULT 0
);

-- ============================================================
-- Rôles système (étiquettes françaises, non supprimables)
-- ============================================================
INSERT INTO platform_roles (key, label, description, system) VALUES
    ('ADMIN', 'Administrateur', 'Accès complet : configuration de la plateforme, rôles, permissions, audit.', TRUE),
    ('PASTEUR', 'Pasteur', 'Centre de commandement : discipolat, rapports, validation finale, utilisateurs.', TRUE),
    ('RESPONSABLE', 'Responsable de département', 'Gestion des départements, des membres, des présences et des rapports.', TRUE),
    ('CHEF_DE_FAMILLE', 'Chef de famille', 'Gestion de la famille de disciples, suivi et rapports de famille.', TRUE),
    ('FAISEUR', 'Faiseur de disciples', 'Suivi quotidien des disciples, rapports hebdomadaires et activités.', TRUE),
    ('MEMBRE', 'Membre', 'Espace personnel : profil, présences, formations, rendez-vous.', TRUE)
ON CONFLICT (key) DO NOTHING;

-- ============================================================
-- Catalogue des permissions connues (regroupées par module)
-- ============================================================
INSERT INTO permission_catalog (key, label, module, description, ordre) VALUES
    -- Utilisateurs
    ('USER_CREATE', 'Créer des utilisateurs', 'Utilisateurs', 'Créer des comptes utilisateurs.', 1),
    ('USER_READ', 'Lire les utilisateurs', 'Utilisateurs', 'Consulter les comptes utilisateurs.', 2),
    ('USER_UPDATE', 'Modifier les utilisateurs', 'Utilisateurs', 'Modifier les comptes (rôles, statut, informations).', 3),
    ('USER_DELETE', 'Supprimer les utilisateurs', 'Utilisateurs', 'Supprimer / désactiver des comptes.', 4),
    ('USER_EXPORT', 'Exporter les utilisateurs', 'Utilisateurs', 'Exporter la liste des utilisateurs.', 5),
    -- Âmes & disciples
    ('SOUL_CREATE', 'Créer des âmes', 'Discipolat', 'Créer des fiches de disciples.', 6),
    ('SOUL_READ', 'Lire les âmes', 'Discipolat', 'Consulter les fiches de disciples.', 7),
    ('SOUL_UPDATE', 'Modifier les âmes', 'Discipolat', 'Modifier les fiches de disciples.', 8),
    ('SOUL_DELETE', 'Supprimer des âmes', 'Discipolat', 'Supprimer des fiches de disciples.', 9),
    -- Familles
    ('FAMILY_CREATE', 'Créer des familles', 'Familles', 'Créer des familles de disciples.', 10),
    ('FAMILY_READ', 'Lire les familles', 'Familles', 'Consulter les familles.', 11),
    ('FAMILY_UPDATE', 'Modifier les familles', 'Familles', 'Modifier les familles et leur chef.', 12),
    ('FAMILY_DELETE', 'Supprimer des familles', 'Familles', 'Supprimer des familles.', 13),
    -- Départements
    ('DEPARTMENT_CREATE', 'Créer des départements', 'Départements', 'Créer des départements.', 14),
    ('DEPARTMENT_READ', 'Lire les départements', 'Départements', 'Consulter les départements.', 15),
    ('DEPARTMENT_UPDATE', 'Modifier les départements', 'Départements', 'Modifier les départements et leurs responsables.', 16),
    ('DEPARTMENT_DELETE', 'Supprimer des départements', 'Départements', 'Supprimer des départements.', 17),
    -- Rapports
    ('REPORT_CREATE', 'Créer des rapports', 'Rapports', 'Créer et soumettre des rapports hebdomadaires.', 18),
    ('REPORT_READ', 'Lire les rapports', 'Rapports', 'Consulter les rapports.', 19),
    ('REPORT_UPDATE', 'Modifier les rapports', 'Rapports', 'Modifier et corriger des rapports.', 20),
    ('REPORT_EXPORT', 'Exporter les rapports', 'Rapports', 'Exporter les rapports (CSV, PDF).', 21),
    ('REPORT_VALIDATE', 'Valider les rapports', 'Rapports', 'Valider les rapports aux niveaux de responsabilité.', 22),
    ('REPORT_CORRECT', 'Corriger les rapports', 'Rapports', 'Renvoi de correction aux auteurs de rapports.', 23),
    -- Transferts
    ('TRANSFER_CREATE', 'Initier des transferts', 'Transferts', 'Créer des demandes de transfert.', 24),
    ('TRANSFER_READ', 'Lire les transferts', 'Transferts', 'Consulter les demandes et leurs historiques.', 25),
    ('TRANSFER_VALIDATE', 'Valider des transferts', 'Transferts', 'Prendre une décision sur une demande.', 26),
    ('TRANSFER_EXECUTE', 'Exécuter des transferts', 'Transferts', 'Exécuter les transferts validés.', 27),
    ('TRANSFER_CONFIGURE', 'Configurer les workflows', 'Transferts', 'Configurer les circuits de validation.', 28),
    -- Vie de l'église
    ('PRAYER_CREATE', 'Créer des prières', 'Prières', 'Partager des sujets de prière.', 29),
    ('PRAYER_READ', 'Lire les prières', 'Prières', 'Consulter les sujets de prière.', 30),
    ('EVENT_CREATE', 'Créer des événements', 'Événements', 'Créer des événements et programmes.', 31),
    ('EVENT_READ', 'Lire les événements', 'Événements', 'Consulter le calendrier.', 32),
    ('EVENT_UPDATE', 'Modifier des événements', 'Événements', 'Modifier des événements.', 33),
    ('EVENT_DELETE', 'Supprimer des événements', 'Événements', 'Supprimer des événements.', 34),
    -- Engagement & outils
    ('DOCUMENT_UPLOAD', 'Téléverser des documents', 'Documents', 'Joindre des fichiers aux entités.', 35),
    ('DOCUMENT_READ', 'Lire les documents', 'Documents', 'Consulter les documents partagés.', 36),
    ('ALERT_MANAGE', 'Gérer les alertes', 'Alertes', 'Créer, traiter et résoudre les alertes.', 37),
    ('EVALUATION_READ', 'Lire les évaluations', 'Évaluations', 'Consulter les évaluations anonymes.', 38),
    ('EVALUATION_CREATE', 'Répondre aux évaluations', 'Évaluations', 'Participer aux évaluations.', 39),
    -- Administration
    ('AUDIT_READ', 'Lire le journal d''audit', 'Administration', 'Consulter l''historique complet des actions.', 40),
    ('PERMISSION_MANAGE', 'Gérer les permissions', 'Administration', 'Modifier la matrice des rôles.', 41),
    ('SETTINGS_MANAGE', 'Gérer l''identité & la marque', 'Administration', 'Configurer les paramètres de l''église.', 42),
    ('MODULE_MANAGE', 'Gérer les modules', 'Administration', 'Activer / désactiver les modules.', 43),
    ('MENU_MANAGE', 'Gérer les menus', 'Administration', 'Configurer la navigation.', 44),
    ('BULK_IMPORT', 'Importer des données', 'Administration', 'Imports en masse de données.', 45),
    ('DATA_EXPORT', 'Exporter les données', 'Administration', 'Exports avancés.', 46)
ON CONFLICT (key) DO NOTHING;

COMMENT ON TABLE platform_roles IS 'Rôles système (labels, description) + rôles personnalisés créés par l''administrateur (matrice de permissions éditables).';
COMMENT ON TABLE permission_catalog IS 'Catalogue des permissions connues, regroupées par module pour l''interface d''administration.';
