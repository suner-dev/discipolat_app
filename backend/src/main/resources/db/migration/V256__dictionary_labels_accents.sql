-- V256__dictionary_labels_accents.sql
-- ============================================================
-- ACCENTS DES LIBELLES DE DICTIONNAIRES
-- La migration V42 (redigee sans accents pour des raisons
-- d'encodage) a seede des libelles du type « Planifie »,
-- « Reunion », « Sante »... affichés tels quels dans les badges
-- d'evenements et de prieres. Le seed Java (DictionaryService
-- .defaultSeedData, miroir attendus des admins) contient deja les
-- libelles accentues : on aligne la base dessus.
-- Chaque UPDATE ne touche que les lignes encore au libelle
-- d'origine : un libelle personnalise par un admin est preserve.
-- ============================================================

-- Types d'evenements
UPDATE dictionary_entries SET label = 'Évangélisation', updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'EVENT_TYPE'      AND code = 'EVANGELISATION' AND label = 'Evangelisation';
UPDATE dictionary_entries SET label = 'Réunion',        updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'EVENT_TYPE'      AND code = 'REUNION'          AND label = 'Reunion';
UPDATE dictionary_entries SET label = 'Conférence',     updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'EVENT_TYPE'      AND code = 'CONFERENCE'       AND label = 'Conference';
UPDATE dictionary_entries SET label = 'Étude biblique', updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'EVENT_TYPE'      AND code = 'ETUDE_BIBLIQUE'   AND label = 'Etude biblique';
UPDATE dictionary_entries SET label = 'Veillée',        updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'EVENT_TYPE'      AND code = 'VEILLEE'          AND label = 'Veillee';
UPDATE dictionary_entries SET label = 'Temps de prière',updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'EVENT_TYPE'      AND code = 'PRIERE'           AND label = 'Temps de priere';

-- Statuts d'evenements
UPDATE dictionary_entries SET label = 'Planifié', updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'EVENT_STATUS' AND code = 'PLANIFIE' AND label = 'Planifie';
UPDATE dictionary_entries SET label = 'Terminé',  updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'EVENT_STATUS' AND code = 'TERMINE'  AND label = 'Termine';
UPDATE dictionary_entries SET label = 'Annulé',   updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'EVENT_STATUS' AND code = 'ANNULE'   AND label = 'Annule';

-- Statuts d'ame
UPDATE dictionary_entries SET label = 'En intégration', updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'SOUL_STATUS' AND code = 'EN_INTEGRATION' AND label = 'En integration';
UPDATE dictionary_entries SET label = 'Décroché',       updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'SOUL_STATUS' AND code = 'DECROCHE'       AND label = 'Decroche';

-- Raisons d'absence
UPDATE dictionary_entries SET label = 'Indisponibilité', updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'ABSENCE_RAISON' AND code = 'INDISPONIBILITE' AND label = 'Indisponibilite';
UPDATE dictionary_entries SET label = 'Non renseigné',   updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'ABSENCE_RAISON' AND code = 'NON_RENSEIGNE'   AND label = 'Non renseigne';

-- Motifs de sortie
UPDATE dictionary_entries SET label = 'Intégré / autonome', updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'EXIT_MOTIF' AND code = 'INTEGRE_AUTONOME' AND label = 'Integre / autonome';
UPDATE dictionary_entries SET label = 'Décès',              updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'EXIT_MOTIF' AND code = 'DECES'            AND label = 'Deces';

-- Categories de difficultes
UPDATE dictionary_entries SET label = 'Santé', updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'DIFFICULTE_CATEGORIE' AND code = 'SANTE' AND label = 'Sante';

-- Situations familiales
UPDATE dictionary_entries SET label = 'Célibataire',      updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'SITUATION_FAMILIALE' AND code = 'CELIBATAIRE'        AND label = 'Celibataire';
UPDATE dictionary_entries SET label = 'Marié(e)',         updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'SITUATION_FAMILIALE' AND code = 'MARIE'              AND label = 'Marie(e)';
UPDATE dictionary_entries SET label = 'Divorcé(e)',       updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'SITUATION_FAMILIALE' AND code = 'DIVORCE'            AND label = 'Divorce(e)';
UPDATE dictionary_entries SET label = 'Parent célibataire',updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'SITUATION_FAMILIALE' AND code = 'PARENT_CELIBATAIRE' AND label = 'Parent celibataire';

-- Categories de prières
UPDATE dictionary_entries SET label = 'Santé', updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'PRAYER_CATEGORIE' AND code = 'SANTE' AND label = 'Sante';

-- Raisons de suivi parallele
UPDATE dictionary_entries SET label = 'Décrochage',           updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'FOLLOWUP_RAISON' AND code = 'DECROCHAGE'            AND label = 'Decrochage';
UPDATE dictionary_entries SET label = 'Absences répétées',    updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'FOLLOWUP_RAISON' AND code = 'ABSENCE_REPETEE'       AND label = 'Absences repetees';
UPDATE dictionary_entries SET label = 'Difficulté spirituelle',updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'FOLLOWUP_RAISON' AND code = 'DIFFICULTE_SPIRITUELLE' AND label = 'Difficulte spirituelle';

-- Categories d'actions de grâce
UPDATE dictionary_entries SET label = 'Santé', updated_at = CURRENT_TIMESTAMP WHERE dict_key = 'GRATITUDE_CATEGORIE' AND code = 'SANTE' AND label = 'Sante';
