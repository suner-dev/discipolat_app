-- V47__dictionaries_remaining.sql
-- ============================================================
-- DERNIERS DICTIONNAIRES : discipline, feedback, interactions
-- Types d'événements disciplinaires, gravité, priorités de
-- feedback et canaux d'interaction CRM — adaptables par
-- l'administrateur sans modifier le code.
-- ============================================================

-- Types d'événements disciplinaires
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DISCIPLINE_TYPE', 'REPROCHE', 'Reproche', '#f59e0b', 1, TRUE),
    ('DISCIPLINE_TYPE', 'SANCTION', 'Sanction', '#ef4444', 2, TRUE),
    ('DISCIPLINE_TYPE', 'LITIGE', 'Litige', '#f59e0b', 3, TRUE),
    ('DISCIPLINE_TYPE', 'CONFLIT', 'Conflit', '#ef4444', 4, TRUE),
    ('DISCIPLINE_TYPE', 'SCANDALE', 'Scandale', '#ef4444', 5, TRUE),
    ('DISCIPLINE_TYPE', 'OBSERVATION', 'Observation', '#3b82f6', 6, TRUE),
    ('DISCIPLINE_TYPE', 'TEMOIGNAGE', 'Témoignage', '#22c55e', 7, TRUE),
    ('DISCIPLINE_TYPE', 'ENTRETIEN', 'Entretien pastoral', '#06b6d4', 8, TRUE),
    ('DISCIPLINE_TYPE', 'RESOLUTION', 'Résolution', '#22c55e', 9, TRUE),
    ('DISCIPLINE_TYPE', 'AUTRE', 'Autre', '#6b7280', 10, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Gravité disciplinaire (miroir de l'enum backend GraviteDiscipline)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('DISCIPLINE_GRAVITE', 'FAIBLE', 'Faible', '#22c55e', 1, TRUE),
    ('DISCIPLINE_GRAVITE', 'MOYENNE', 'Moyenne', '#f59e0b', 2, TRUE),
    ('DISCIPLINE_GRAVITE', 'GRAVE', 'Grave', '#f97316', 3, TRUE),
    ('DISCIPLINE_GRAVITE', 'CRITIQUE', 'Critique', '#ef4444', 4, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Priorités de feedback (miroir de l'enum backend Feedback.Priority)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('FEEDBACK_PRIORITE', 'BASSE', 'Basse', '#94a3b8', 1, TRUE),
    ('FEEDBACK_PRIORITE', 'MOYENNE', 'Moyenne', '#3b82f6', 2, TRUE),
    ('FEEDBACK_PRIORITE', 'HAUTE', 'Haute', '#f59e0b', 3, TRUE),
    ('FEEDBACK_PRIORITE', 'CRITIQUE', 'Critique', '#ef4444', 4, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Canaux d'interaction CRM
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('INTERACTION_CANAL', 'TELEPHONE', 'Téléphone', '#3b82f6', 1, TRUE),
    ('INTERACTION_CANAL', 'WHATSAPP', 'WhatsApp', '#22c55e', 2, TRUE),
    ('INTERACTION_CANAL', 'SMS', 'SMS', '#06b6d4', 3, TRUE),
    ('INTERACTION_CANAL', 'EMAIL', 'E-mail', '#a855f7', 4, TRUE),
    ('INTERACTION_CANAL', 'VIDEO', 'Visioconférence', '#f59e0b', 5, TRUE),
    ('INTERACTION_CANAL', 'PRESENTIEL', 'En présentiel', '#22c55e', 6, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;
