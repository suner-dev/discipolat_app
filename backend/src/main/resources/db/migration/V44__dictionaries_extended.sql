-- V44__dictionaries_extended.sql
-- ============================================================
-- DICTIONNAIRES ÉTENDUS (référentiels configurables)
-- Complète V42 : rôles utilisateurs, feedback, transferts,
-- demandes membres, évaluations, discipline, visibilité des
-- prières, entités d'audit, état spirituel.
-- L'administrateur renomme, réordonne, colore, ajoute ou
-- désactive chaque valeur — sans modifier le code.
-- ============================================================

-- Rôles utilisateurs (libellés affichés dans toute l'application)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('USER_ROLE', 'ADMIN', 'Administrateur', '#3b82f6', 1, TRUE),
    ('USER_ROLE', 'PASTEUR', 'Pasteur', '#8b5cf6', 2, TRUE),
    ('USER_ROLE', 'RESPONSABLE', 'Responsable', '#f59e0b', 3, TRUE),
    ('USER_ROLE', 'CHEF_DE_FAMILLE', 'Chef de famille', '#f59e0b', 4, TRUE),
    ('USER_ROLE', 'FAISEUR', 'Faiseur de disciples', '#22c55e', 5, TRUE),
    ('USER_ROLE', 'MEMBRE', 'Membre', '#6b7280', 6, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Charge de travail des faiseurs
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('USER_CHARGE', 'LEGER', 'Léger', '#22c55e', 1, TRUE),
    ('USER_CHARGE', 'NORMAL', 'Normal', '#3b82f6', 2, TRUE),
    ('USER_CHARGE', 'SURCHARGE', 'Surchargé', '#ef4444', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Catégories de feedback testeurs
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('FEEDBACK_CATEGORIE', 'BUG', 'Bug', '#ef4444', 1, TRUE),
    ('FEEDBACK_CATEGORIE', 'UX', 'UX', '#06b6d4', 2, TRUE),
    ('FEEDBACK_CATEGORIE', 'SUGGESTION', 'Suggestion', '#3b82f6', 3, TRUE),
    ('FEEDBACK_CATEGORIE', 'FONCTIONNALITE_MANQUANTE', 'Fonctionnalité manquante', '#a855f7', 4, TRUE),
    ('FEEDBACK_CATEGORIE', 'PERFORMANCE', 'Performance', '#f59e0b', 5, TRUE),
    ('FEEDBACK_CATEGORIE', 'TRADUCTION', 'Traduction', '#ec4899', 6, TRUE),
    ('FEEDBACK_CATEGORIE', 'AFFICHAGE', 'Affichage', '#6366f1', 7, TRUE),
    ('FEEDBACK_CATEGORIE', 'AUTRE', 'Autre', '#6b7280', 8, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Statuts de feedback (miroir de l'enum backend Feedback.Status)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('FEEDBACK_STATUS', 'NOUVEAU', 'Nouveau', '#3b82f6', 1, TRUE),
    ('FEEDBACK_STATUS', 'EN_COURS', 'En cours', '#f59e0b', 2, TRUE),
    ('FEEDBACK_STATUS', 'RESOLU', 'Résolu', '#22c55e', 3, TRUE),
    ('FEEDBACK_STATUS', 'REJETE', 'Rejeté', '#6b7280', 4, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Types de transfert (miroir de l'enum backend TransferType)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('TRANSFER_TYPE', 'MEMBRE_DEPARTEMENT_TRANSFERT', 'Transfert de membre entre départements', '#3b82f6', 1, TRUE),
    ('TRANSFER_TYPE', 'MEMBRE_DEPARTEMENT_AJOUT', 'Ajout de membre dans un département', '#22c55e', 2, TRUE),
    ('TRANSFER_TYPE', 'MEMBRE_DEPARTEMENT_RETRAIT', 'Retrait de membre d''un département', '#ef4444', 3, TRUE),
    ('TRANSFER_TYPE', 'DISCIPLE_FAMILLE_TRANSFERT', 'Transfert de disciple entre familles', '#3b82f6', 4, TRUE),
    ('TRANSFER_TYPE', 'FAISEUR_FAMILLE_TRANSFERT', 'Transfert de faiseur entre familles', '#06b6d4', 5, TRUE),
    ('TRANSFER_TYPE', 'CHEF_FAMILLE_TRANSFERT', 'Transfert de chef de famille', '#f59e0b', 6, TRUE),
    ('TRANSFER_TYPE', 'FAISEUR_DISCIPLE_CHANGEMENT', 'Changement du faiseur d''un disciple', '#a855f7', 7, TRUE),
    ('TRANSFER_TYPE', 'RESPONSABLE_DEPARTEMENT_CHANGEMENT', 'Changement du responsable d''un département', '#ec4899', 8, TRUE),
    ('TRANSFER_TYPE', 'CHEF_ADJOINT_CHANGEMENT', 'Changement du chef adjoint d''une famille', '#6366f1', 9, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Statuts de transfert (miroir de l'enum backend TransferStatus)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('TRANSFER_STATUS', 'BROUILLON', 'Brouillon', '#6b7280', 1, TRUE),
    ('TRANSFER_STATUS', 'SOUMIS', 'Soumis', '#3b82f6', 2, TRUE),
    ('TRANSFER_STATUS', 'EN_ATTENTE_VALIDATION', 'En attente de validation', '#f59e0b', 3, TRUE),
    ('TRANSFER_STATUS', 'VALIDATION_PARTIELLE', 'Validation partielle', '#06b6d4', 4, TRUE),
    ('TRANSFER_STATUS', 'VALIDE', 'Validé', '#8b5cf6', 5, TRUE),
    ('TRANSFER_STATUS', 'REFUSE', 'Refusé', '#ef4444', 6, TRUE),
    ('TRANSFER_STATUS', 'ANNULE', 'Annulé', '#6b7280', 7, TRUE),
    ('TRANSFER_STATUS', 'EXECUTE', 'Exécuté', '#22c55e', 8, TRUE),
    ('TRANSFER_STATUS', 'ARCHIVE', 'Archivé', '#94a3b8', 9, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Décisions de validation (miroir de l'enum backend DecisionType)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('TRANSFER_DECISION', 'APPROBATION', 'Approbation', '#22c55e', 1, TRUE),
    ('TRANSFER_DECISION', 'REFUS', 'Refus', '#ef4444', 2, TRUE),
    ('TRANSFER_DECISION', 'DEMANDE_INFORMATIONS', 'Demande d''informations', '#f59e0b', 3, TRUE),
    ('TRANSFER_DECISION', 'RENVOI_CORRECTION', 'Renvoi pour correction', '#f59e0b', 4, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Priorités de transfert
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('TRANSFER_PRIORITE', 'BASSE', 'Basse', '#94a3b8', 1, TRUE),
    ('TRANSFER_PRIORITE', 'MOYENNE', 'Moyenne', '#3b82f6', 2, TRUE),
    ('TRANSFER_PRIORITE', 'HAUTE', 'Haute', '#f59e0b', 3, TRUE),
    ('TRANSFER_PRIORITE', 'URGENTE', 'Urgente', '#ef4444', 4, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Cibles des demandes membres (miroir de l'enum backend MemberRequest.Target)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('MEMBER_REQUEST_TARGET', 'PASTEUR', 'Pasteur', '#8b5cf6', 1, TRUE),
    ('MEMBER_REQUEST_TARGET', 'RESPONSABLE', 'Responsable', '#f59e0b', 2, TRUE),
    ('MEMBER_REQUEST_TARGET', 'CHEF_DE_FAMILLE', 'Chef de famille', '#22c55e', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Statuts des demandes membres (miroir de l'enum backend MemberRequest.Status)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('MEMBER_REQUEST_STATUS', 'OUVERT', 'Ouvert', '#f59e0b', 1, TRUE),
    ('MEMBER_REQUEST_STATUS', 'EN_COURS', 'En cours', '#3b82f6', 2, TRUE),
    ('MEMBER_REQUEST_STATUS', 'RESOLU', 'Résolu', '#22c55e', 3, TRUE),
    ('MEMBER_REQUEST_STATUS', 'REJETE', 'Rejeté', '#ef4444', 4, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Visibilités des prières (miroir de l'enum backend Prayer.Visibilite)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('PRAYER_VISIBILITE', 'GENERALE', 'Général', '#06b6d4', 1, TRUE),
    ('PRAYER_VISIBILITE', 'PASTEUR_RESPONSABLE', 'Pasteur + Resp.', '#a855f7', 2, TRUE),
    ('PRAYER_VISIBILITE', 'FAISEUR', 'Chefs + Faiseurs', '#f59e0b', 3, TRUE),
    ('PRAYER_VISIBILITE', 'PARTAGEE', 'Famille', '#3b82f6', 4, TRUE),
    ('PRAYER_VISIBILITE', 'PRIVEE', 'Privé', '#6b7280', 5, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Catégories d'évaluations (rôles évalués)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('EVALUATION_CATEGORIE', 'RESPONSABLE', 'Responsable', '#a855f7', 1, TRUE),
    ('EVALUATION_CATEGORIE', 'CHEF_FAMILLE', 'Chef de famille', '#f59e0b', 2, TRUE),
    ('EVALUATION_CATEGORIE', 'FAISEUR', 'Faiseur', '#22c55e', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- État spirituel (fiche disciple)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('SPIRITUAL_LEVEL', 'NOUVEAU_CONVERTI', 'Nouveau converti', '#22c55e', 1, TRUE),
    ('SPIRITUAL_LEVEL', 'EN_CROISSANCE', 'En croissance', '#06b6d4', 2, TRUE),
    ('SPIRITUAL_LEVEL', 'MATURE', 'Mature', '#3b82f6', 3, TRUE),
    ('SPIRITUAL_LEVEL', 'EN_DIFFICULTE', 'En difficulté', '#ef4444', 4, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Catégories de discipline (registre de discipline)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DISCIPLINE_CATEGORIE', 'COMPORTEMENT', 'Comportement', '#ef4444', 1, TRUE),
    ('DISCIPLINE_CATEGORIE', 'CONDUITE', 'Conduite', '#ef4444', 2, TRUE),
    ('DISCIPLINE_CATEGORIE', 'HABILLEMENT', 'Habillement', '#f59e0b', 3, TRUE),
    ('DISCIPLINE_CATEGORIE', 'VIE_SPIRITUELLE', 'Vie spirituelle', '#a855f7', 4, TRUE),
    ('DISCIPLINE_CATEGORIE', 'PONCTUALITE', 'Ponctualité', '#06b6d4', 5, TRUE),
    ('DISCIPLINE_CATEGORIE', 'PARTICIPATION', 'Participation', '#3b82f6', 6, TRUE),
    ('DISCIPLINE_CATEGORIE', 'FIDELITE', 'Fidélité', '#22c55e', 7, TRUE),
    ('DISCIPLINE_CATEGORIE', 'ENGAGEMENT', 'Engagement', '#22c55e', 8, TRUE),
    ('DISCIPLINE_CATEGORIE', 'REPROCHE', 'Reproche', '#f59e0b', 9, TRUE),
    ('DISCIPLINE_CATEGORIE', 'SANCTION', 'Sanction', '#ef4444', 10, TRUE),
    ('DISCIPLINE_CATEGORIE', 'LITIGE', 'Litige', '#f59e0b', 11, TRUE),
    ('DISCIPLINE_CATEGORIE', 'CONFLIT', 'Conflit', '#ef4444', 12, TRUE),
    ('DISCIPLINE_CATEGORIE', 'SCANDALE', 'Scandale', '#ef4444', 13, TRUE),
    ('DISCIPLINE_CATEGORIE', 'RELATION_PROBLEMATIQUE', 'Relation problématique', '#ec4899', 14, TRUE),
    ('DISCIPLINE_CATEGORIE', 'FLIRT_INAPPROPRIE', 'Flirt inapproprié', '#ec4899', 15, TRUE),
    ('DISCIPLINE_CATEGORIE', 'DEGAT_MATERIEL', 'Dégât matériel', '#6b7280', 16, TRUE),
    ('DISCIPLINE_CATEGORIE', 'DEGAT_RELATIONNEL', 'Dégât relationnel', '#6b7280', 17, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Entités du journal d'audit
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('AUDIT_ENTITY', 'USER', 'Utilisateur', '#3b82f6', 1, TRUE),
    ('AUDIT_ENTITY', 'FAMILY', 'Famille', '#22c55e', 2, TRUE),
    ('AUDIT_ENTITY', 'SOUL', 'Âme', '#06b6d4', 3, TRUE),
    ('AUDIT_ENTITY', 'REPORT', 'Rapport', '#f59e0b', 4, TRUE),
    ('AUDIT_ENTITY', 'DEPARTMENT', 'Département', '#a855f7', 5, TRUE),
    ('AUDIT_ENTITY', 'TRANSFER', 'Transfert', '#8b5cf6', 6, TRUE),
    ('AUDIT_ENTITY', 'EVENT', 'Événement', '#ec4899', 7, TRUE),
    ('AUDIT_ENTITY', 'MEMBER_REQUEST', 'Demande membre', '#6366f1', 8, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;
