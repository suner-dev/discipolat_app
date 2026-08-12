-- V42__platform_dictionaries.sql
-- ============================================================
-- DICTIONNAIRES DE LA PLATEFORME (référentiels configurables)
-- Remplace les listes codées en dur par des données en base :
-- l'administrateur peut renommer, réordonner, colorer, ajouter ou
-- désactiver chaque valeur — sans modifier le code.
-- ============================================================

CREATE TABLE IF NOT EXISTS dictionary_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dict_key VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    label VARCHAR(255) NOT NULL,
    description TEXT,
    color VARCHAR(50),
    ordre INTEGER NOT NULL DEFAULT 0,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dict_code UNIQUE (dict_key, code)
);

CREATE INDEX IF NOT EXISTS idx_dictionary_entries_key ON dictionary_entries(dict_key, ordre);

-- ============================================================
-- SEED DES DICTIONNAIRES PAR DÉFAUT
-- (is_default = TRUE : réinitialisation possible depuis l'admin)
-- ============================================================

-- Types d'événements
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('EVENT_TYPE', 'SORTIE', 'Sortie', '#22c55e', 1, TRUE),
    ('EVENT_TYPE', 'RETRAITE', 'Retraite', '#a855f7', 2, TRUE),
    ('EVENT_TYPE', 'EVANGELISATION', 'Évangélisation', '#f97316', 3, TRUE),
    ('EVENT_TYPE', 'REUNION', 'Réunion', '#3b82f6', 4, TRUE),
    ('EVENT_TYPE', 'VISITE', 'Visite', '#06b6d4', 5, TRUE),
    ('EVENT_TYPE', 'CONFERENCE', 'Conférence', '#6366f1', 6, TRUE),
    ('EVENT_TYPE', 'FORMATION', 'Formation', '#f59e0b', 7, TRUE),
    ('EVENT_TYPE', 'ANNIVERSAIRE', 'Anniversaire', '#ec4899', 8, TRUE),
    ('EVENT_TYPE', 'CULTE', 'Culte', '#22c55e', 9, TRUE),
    ('EVENT_TYPE', 'ETUDE_BIBLIQUE', 'Étude biblique', '#3b82f6', 10, TRUE),
    ('EVENT_TYPE', 'VEILLEE', 'Veillée', '#a855f7', 11, TRUE),
    ('EVENT_TYPE', 'PRIERE', 'Temps de prière', '#f59e0b', 12, TRUE),
    ('EVENT_TYPE', 'AUTRE', 'Autre', '#6b7280', 13, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Statuts d'événements
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('EVENT_STATUS', 'PLANIFIE', 'Planifié', '#3b82f6', 1, TRUE),
    ('EVENT_STATUS', 'EN_COURS', 'En cours', '#f59e0b', 2, TRUE),
    ('EVENT_STATUS', 'TERMINE', 'Terminé', '#22c55e', 3, TRUE),
    ('EVENT_STATUS', 'ANNULE', 'Annulé', '#ef4444', 4, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Types de disciples
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('SOUL_TYPE', 'NOUVEL_ARRIVANT', 'Nouvel arrivant', '#3b82f6', 1, TRUE),
    ('SOUL_TYPE', 'NOUVEAU_CONVERTI', 'Nouveau converti', '#22c55e', 2, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Statuts d'âme (miroir de l'enum backend StatutAme)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('SOUL_STATUS', 'NOUVEAU_CONVERTI', 'Nouveau converti', '#22c55e', 1, TRUE),
    ('SOUL_STATUS', 'NOUVEL_ARRIVANT', 'Nouvel arrivant', '#3b82f6', 2, TRUE),
    ('SOUL_STATUS', 'EN_INTEGRATION', 'En intégration', '#06b6d4', 3, TRUE),
    ('SOUL_STATUS', 'ACTIF', 'Actif', '#22c55e', 4, TRUE),
    ('SOUL_STATUS', 'EN_VEILLE', 'En veille', '#f59e0b', 5, TRUE),
    ('SOUL_STATUS', 'DECROCHE', 'Décroché', '#ef4444', 6, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Raisons d'absence
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('ABSENCE_RAISON', 'MALADIE', 'Maladie', '#ef4444', 1, TRUE),
    ('ABSENCE_RAISON', 'VOYAGE', 'Voyage', '#3b82f6', 2, TRUE),
    ('ABSENCE_RAISON', 'INDISPONIBILITE', 'Indisponibilité', '#f59e0b', 3, TRUE),
    ('ABSENCE_RAISON', 'INJOIGNABLE', 'Injoignable', '#6b7280', 4, TRUE),
    ('ABSENCE_RAISON', 'NON_RENSEIGNE', 'Non renseigné', '#94a3b8', 5, TRUE),
    ('ABSENCE_RAISON', 'AUTRE', 'Autre', '#6b7280', 6, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Motifs de sortie
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('EXIT_MOTIF', 'INTEGRE_AUTONOME', 'Intégré / autonome', '#22c55e', 1, TRUE),
    ('EXIT_MOTIF', 'TRANSFERT', 'Transfert', '#3b82f6', 2, TRUE),
    ('EXIT_MOTIF', 'ABANDON', 'Abandon', '#ef4444', 3, TRUE),
    ('EXIT_MOTIF', 'INJOIGNABLE_DURABLE', 'Injoignable durable', '#6b7280', 4, TRUE),
    ('EXIT_MOTIF', 'DECES', 'Décès', '#374151', 5, TRUE),
    ('EXIT_MOTIF', 'AUTRE', 'Autre', '#6b7280', 6, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Catégories de difficultés
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DIFFICULTE_CATEGORIE', 'SPIRITUEL', 'Spirituel', '#a855f7', 1, TRUE),
    ('DIFFICULTE_CATEGORIE', 'FAMILIAL', 'Familial', '#ec4899', 2, TRUE),
    ('DIFFICULTE_CATEGORIE', 'FINANCIER', 'Financier', '#f59e0b', 3, TRUE),
    ('DIFFICULTE_CATEGORIE', 'SANTE', 'Santé', '#ef4444', 4, TRUE),
    ('DIFFICULTE_CATEGORIE', 'AUTRE', 'Autre', '#6b7280', 5, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Situations familiales (champ libre côté backend : l'admin peut ajouter d'autres valeurs)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('SITUATION_FAMILIALE', 'CELIBATAIRE', 'Célibataire', '#3b82f6', 1, TRUE),
    ('SITUATION_FAMILIALE', 'MARIE', 'Marié(e)', '#22c55e', 2, TRUE),
    ('SITUATION_FAMILIALE', 'DIVORCE', 'Divorcé(e)', '#f59e0b', 3, TRUE),
    ('SITUATION_FAMILIALE', 'VEUF', 'Veuf / veuve', '#6b7280', 4, TRUE),
    ('SITUATION_FAMILIALE', 'PARENT_CELIBATAIRE', 'Parent célibataire', '#06b6d4', 5, TRUE),
    ('SITUATION_FAMILIALE', 'AUTRE', 'Autre', '#94a3b8', 6, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Catégories de prières
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('PRAYER_CATEGORIE', 'SANTE', 'Santé', '#ef4444', 1, TRUE),
    ('PRAYER_CATEGORIE', 'FAMILLE', 'Famille', '#22c55e', 2, TRUE),
    ('PRAYER_CATEGORIE', 'TRAVAIL', 'Travail', '#3b82f6', 3, TRUE),
    ('PRAYER_CATEGORIE', 'SPIRITUEL', 'Spirituel', '#a855f7', 4, TRUE),
    ('PRAYER_CATEGORIE', 'AUTRE', 'Autre', '#6b7280', 5, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Priorités de prières
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('PRAYER_PRIORITE', 'BASSE', 'Basse', '#94a3b8', 1, TRUE),
    ('PRAYER_PRIORITE', 'MOYENNE', 'Moyenne', '#f59e0b', 2, TRUE),
    ('PRAYER_PRIORITE', 'HAUTE', 'Haute', '#ef4444', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Catégories de documents
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DOCUMENT_CATEGORIE', 'COMPTE_RENDU', 'Compte rendu', '#3b82f6', 1, TRUE),
    ('DOCUMENT_CATEGORIE', 'RAPPORT', 'Rapport', '#22c55e', 2, TRUE),
    ('DOCUMENT_CATEGORIE', 'FORMATION', 'Formation', '#a855f7', 3, TRUE),
    ('DOCUMENT_CATEGORIE', 'ADMINISTRATIF', 'Administratif', '#f59e0b', 4, TRUE),
    ('DOCUMENT_CATEGORIE', 'AUTRE', 'Autre', '#6b7280', 5, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Raisons de suivi parallèle
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('FOLLOWUP_RAISON', 'DECROCHAGE', 'Décrochage', '#ef4444', 1, TRUE),
    ('FOLLOWUP_RAISON', 'ABSENCE_REPETEE', 'Absences répétées', '#f59e0b', 2, TRUE),
    ('FOLLOWUP_RAISON', 'DIFFICULTE_SPIRITUELLE', 'Difficulté spirituelle', '#a855f7', 3, TRUE),
    ('FOLLOWUP_RAISON', 'SITUATION_DIFFICILE', 'Situation difficile', '#ec4899', 4, TRUE),
    ('FOLLOWUP_RAISON', 'NOUVEAU_CONVERTI', 'Nouveau converti', '#22c55e', 5, TRUE),
    ('FOLLOWUP_RAISON', 'AUTRE', 'Autre', '#6b7280', 6, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Cultes / programmes de présence (alimente le rapport hebdomadaire)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('CULTE', 'DIMANCHE_MATIN', 'Dimanche Matin', '#22c55e', 1, TRUE),
    ('CULTE', 'MERCREDI_SOIR', 'Mercredi Soir', '#3b82f6', 2, TRUE),
    ('CULTE', 'VENDREDI_SOIR', 'Vendredi Soir', '#a855f7', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Types d'interactions
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('INTERACTION_TYPE', 'APPEL', 'Appel', '#3b82f6', 1, TRUE),
    ('INTERACTION_TYPE', 'VISITE', 'Visite', '#22c55e', 2, TRUE),
    ('INTERACTION_TYPE', 'MESSAGE', 'Message', '#06b6d4', 3, TRUE),
    ('INTERACTION_TYPE', 'RENCONTRE', 'Rencontre', '#f59e0b', 4, TRUE),
    ('INTERACTION_TYPE', 'AUTRE', 'Autre', '#6b7280', 5, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Motifs de rendez-vous (miroir de l'enum backend Appointment.Motif)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('APPOINTMENT_MOTIF', 'CONSEIL', 'Conseil', '#3b82f6', 1, TRUE),
    ('APPOINTMENT_MOTIF', 'CONFESSION', 'Confession', '#a855f7', 2, TRUE),
    ('APPOINTMENT_MOTIF', 'SUIVI', 'Suivi', '#22c55e', 3, TRUE),
    ('APPOINTMENT_MOTIF', 'FORMATION', 'Formation', '#f59e0b', 4, TRUE),
    ('APPOINTMENT_MOTIF', 'AUTRE', 'Autre', '#6b7280', 5, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Catégories d'actions de grâce (champ libre : l'admin peut ajouter les siennes)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('GRATITUDE_CATEGORIE', 'SANTE', 'Santé', '#ef4444', 1, TRUE),
    ('GRATITUDE_CATEGORIE', 'FAMILLE', 'Famille', '#22c55e', 2, TRUE),
    ('GRATITUDE_CATEGORIE', 'TRAVAIL', 'Travail', '#3b82f6', 3, TRUE),
    ('GRATITUDE_CATEGORIE', 'SPIRITUEL', 'Spirituel', '#a855f7', 4, TRUE),
    ('GRATITUDE_CATEGORIE', 'AUTRE', 'Autre', '#6b7280', 5, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Types de demandes membres (miroir de l'enum backend MemberRequest.Type)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('MEMBER_REQUEST_TYPE', 'SUGGESTION', 'Suggestion', '#3b82f6', 1, TRUE),
    ('MEMBER_REQUEST_TYPE', 'RENDEZ_VOUS', 'Rendez-vous', '#22c55e', 2, TRUE),
    ('MEMBER_REQUEST_TYPE', 'SIGNALEMENT', 'Signalement', '#f59e0b', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;
