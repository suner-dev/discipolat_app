-- V264__permission_catalog_labels_accents.sql
-- ============================================================
-- ACCENTS DES LIBELLÉS DU CATALOGUE DE PERMISSIONS (+ rôles)
-- ------------------------------------------------------------
-- Les migrations fondatrices (V37, V49) ont été rédigées SANS accents
-- (contrainte d'encodage de l'époque). Résultat : la page « Rôles &
-- permissions » affiche « Creer des utilisateurs », « Gerer les
-- permissions », un module « Departements », etc. — un texte français
-- bancal qui donne une impression de « bricolé / codé en dur » au
-- premier regard d'un utilisateur réel (retour terrain pasteur 2026-10-17).
--
-- On aligne le catalogue sur un français correct, exactement dans
-- l'esprit de V256 (dictionary_labels_accents). Chaque REPLACE ne touche
-- que le jeton non accentué : un libellé déjà correct, ou personnalisé
-- par une église avec un libellé distinct, n'est pas cassé. Ordre
-- « pluriel puis singulier » pour éviter les remplacements partiels.
--
-- Table : permission_catalog (libellé + module + description) et
-- platform_roles (libellé + description). Cible PostgreSQL (Flyway).
-- ============================================================

-- Jetons « verbe » fréquents — CREATE/READ/UPDATE/DELETE des libellés.
UPDATE permission_catalog SET
  label       = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(label,
                   'Creer', 'Créer'), 'creer', 'créer'),
                   'Gerer', 'Gérer'), 'gerer', 'gérer'),
                   'Executer', 'Exécuter'), 'executer', 'exécuter');
UPDATE permission_catalog SET
  description = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(description,
                   'Creer', 'Créer'), 'creer', 'créer'),
                   'Gerer', 'Gérer'), 'gerer', 'gérer'),
                   'Executer', 'Exécuter'), 'executer', 'exécuter')
  WHERE description IS NOT NULL;

-- Téléverser / importer / exporter (documents, données).
UPDATE permission_catalog SET label = REPLACE(REPLACE(label, 'Televerser', 'Téléverser'), 'televerser', 'téléverser');
UPDATE permission_catalog SET description = REPLACE(REPLACE(description, 'Televerser', 'Téléverser'), 'televerser', 'téléverser') WHERE description IS NOT NULL;

-- Événement(s) — pluriel avant singulier.
UPDATE permission_catalog SET
  label = REPLACE(REPLACE(REPLACE(REPLACE(label,
            'Evenements', 'Événements'), 'evenements', 'événements'),
            'Evenement', 'Événement'), 'evenement', 'événement');
UPDATE permission_catalog SET
  description = REPLACE(REPLACE(REPLACE(REPLACE(description,
            'Evenements', 'Événements'), 'evenements', 'événements'),
            'Evenement', 'Événement'), 'evenement', 'événement') WHERE description IS NOT NULL;

-- Prière(s).
UPDATE permission_catalog SET
  label = REPLACE(REPLACE(REPLACE(REPLACE(label,
            'Prieres', 'Prières'), 'prieres', 'prières'),
            'Priere', 'Prière'), 'priere', 'prière');
UPDATE permission_catalog SET
  description = REPLACE(REPLACE(REPLACE(REPLACE(description,
            'Prieres', 'Prières'), 'prieres', 'prières'),
            'Priere', 'Prière'), 'priere', 'prière') WHERE description IS NOT NULL;

-- Évaluation(s).
UPDATE permission_catalog SET
  label = REPLACE(REPLACE(REPLACE(REPLACE(label,
            'Evaluations', 'Évaluations'), 'evaluations', 'évaluations'),
            'Evaluation', 'Évaluation'), 'evaluation', 'évaluation');
UPDATE permission_catalog SET
  description = REPLACE(REPLACE(REPLACE(REPLACE(description,
            'Evaluations', 'Évaluations'), 'evaluations', 'évaluations'),
            'Evaluation', 'Évaluation'), 'evaluation', 'évaluation') WHERE description IS NOT NULL;

-- Activité / activités.
UPDATE permission_catalog SET
  label = REPLACE(REPLACE(REPLACE(REPLACE(label,
            'activites', 'activités'), 'Activites', 'Activités'),
            'activite', 'activité'), 'Activite', 'Activité');
UPDATE permission_catalog SET
  description = REPLACE(REPLACE(REPLACE(REPLACE(description,
            'activites', 'activités'), 'Activites', 'Activités'),
            'activite', 'activité'), 'Activite', 'Activité') WHERE description IS NOT NULL;

-- Équipe(s), donnée(s), paramètre(s), identité, général, membre(s) divers.
UPDATE permission_catalog SET
  label = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(label,
            'equipes', 'équipes'), 'Equipes', 'Équipes'), 'equipe', 'équipe'), 'Equipe', 'Équipe'),
            'donnees', 'données'), 'Donnees', 'Données'),
            'parametres', 'paramètres'),
            'identite', 'identité'),
            'General', 'Général');
UPDATE permission_catalog SET
  description = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(description,
            'equipes', 'équipes'), 'Equipes', 'Équipes'), 'equipe', 'équipe'), 'Equipe', 'Équipe'),
            'donnees', 'données'), 'Donnees', 'Données'),
            'parametres', 'paramètres'),
            'identite', 'identité'),
            'General', 'Général') WHERE description IS NOT NULL;

-- Le module sert d'en-tête de section : on l'accentue au même titre.
UPDATE permission_catalog SET module = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(module,
            'Departements', 'Départements'), 'departements', 'départements'),
            'Evenements', 'Événements'), 'evenements', 'événements'),
            'Prieres', 'Prières');
UPDATE permission_catalog SET module = REPLACE(REPLACE(module, 'Evaluations', 'Évaluations'), 'General', 'Général');

-- Idem côté module : « Departements » (singulier éventuel) et « General ».
UPDATE permission_catalog SET module = REPLACE(REPLACE(module, 'Departement', 'Département'), 'departement', 'département');

-- Rôles système (platform_roles) — libellés/descriptions accentués.
UPDATE platform_roles SET
  label = REPLACE(REPLACE(REPLACE(label,
            'departement', 'département'), 'Departement', 'Département'),
            'Acces', 'Accès');
UPDATE platform_roles SET
  description = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(description,
            'Acces', 'Accès'), 'acces', 'accès'),
            'departement', 'département'), 'Departement', 'Département'),
            'discipolat', 'discipolat'),
            'roles', 'rôles'), 'Roles', 'Rôles')
  WHERE description IS NOT NULL;

-- ============================================================
-- COMPLÉMENT DE RIGUEUR — reliquats non accentués encore visibles.
-- ------------------------------------------------------------
-- Les blocs ci-dessus accentuaient « departement(s) » UNIQUEMENT dans la
-- colonne `module`, et omettaient plusieurs jetons présents dans des
-- LIBELLÉS (DEPARTMENT_* : « ... departements » ; EVALUATION_CREATE :
-- « Repondre ... ») et dans des DESCRIPTIONS (roles, responsabilite,
-- decision, resoudre, avances, partages, valides, entites, desactiver,
-- eglise) — ainsi que des descriptions de RÔLES (FAISEUR « activites »,
-- RESPONSABLE/MEMBRE « presences »). Un texte déjà correct, ou
-- personnalisé par une église, ressort inchangé (REPLACE est idempotent
-- sur un jeton absent). Ordre pluriel -> singulier ; on ne remplace
-- JAMAIS « Valider » (le jeton utilisé est « valides », pluriel exact —
-- « Valider » reste donc intact). Chaque jeton est explicite : aucune
-- règle générique, donc aucune correspondance partielle involontaire.
-- ============================================================

-- Libellés : DEPARTMENT_* et EVALUATION_CREATE.
UPDATE permission_catalog SET label = REPLACE(label, 'Departements', 'Départements');
UPDATE permission_catalog SET label = REPLACE(label, 'departements', 'départements');
UPDATE permission_catalog SET label = REPLACE(label, 'Departement', 'Département');
UPDATE permission_catalog SET label = REPLACE(label, 'departement', 'département');
UPDATE permission_catalog SET label = REPLACE(label, 'Repondre', 'Répondre');

-- Descriptions (info-bulles de la matrice des permissions).
UPDATE permission_catalog SET description = REPLACE(description, 'Departements', 'Départements') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'departements', 'départements') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'Departement', 'Département') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'departement', 'département') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'desactiver', 'désactiver') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'responsabilite', 'responsabilité') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'decision', 'décision') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'resoudre', 'résoudre') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'entites', 'entités') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'partages', 'partagés') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'valides', 'validés') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'avances', 'avancés') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'roles', 'rôles') WHERE description IS NOT NULL;
UPDATE permission_catalog SET description = REPLACE(description, 'eglise', 'église') WHERE description IS NOT NULL;

-- Descriptions des rôles système (libellés déjà couverts plus haut).
UPDATE platform_roles SET description = REPLACE(description, 'activites', 'activités') WHERE description IS NOT NULL;
UPDATE platform_roles SET description = REPLACE(description, 'presences', 'présences') WHERE description IS NOT NULL;

-- NB : les libellés du catalogue servent aussi de « humanisation » côté
-- front. Le front ne doit PLUS afficher la clé technique brute
-- (ex. « USER_CREATE ») ; voir frontend/src/pages/PermissionsPage.tsx.
