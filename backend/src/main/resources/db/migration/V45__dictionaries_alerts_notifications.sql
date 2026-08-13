-- V45__dictionaries_alerts_notifications.sql
-- ============================================================
-- DICTIONNAIRES : STATUTS DE RAPPORTS, ALERTES ET NOTIFICATIONS
-- Complète V42/V44 : statuts de validation des rapports de
-- famille, types/cibles/priorités/statuts d'alertes, types et
-- canaux de notifications.
-- L'administrateur renomme, réordonne, colore, ajoute ou
-- désactive chaque valeur — sans modifier le code.
-- ============================================================

-- Statuts de validation des rapports (miroir de l'enum StatutValidation)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('REPORT_STATUS', 'BROUILLON', 'Brouillon', '#f59e0b', 1, TRUE),
    ('REPORT_STATUS', 'SOUMIS', 'Soumis', '#22c55e', 2, TRUE),
    ('REPORT_STATUS', 'VU_PAR_RESPONSABLE', 'Vu responsable', '#3b82f6', 3, TRUE),
    ('REPORT_STATUS', 'VU_PAR_PASTEUR', 'Vu pasteur', '#8b5cf6', 4, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Types d'alertes (miroir des valeurs émises par le backend)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('ALERT_TYPE', 'ABSENCE_48H', 'Absence 48h', '#ef4444', 1, TRUE),
    ('ALERT_TYPE', 'ABSENCE_3_SEMAINES', 'Décrochage 3 semaines', '#ef4444', 2, TRUE),
    ('ALERT_TYPE', 'RAPPORT_NON_SOUMIS', 'Rapport non soumis', '#f59e0b', 3, TRUE),
    ('ALERT_TYPE', 'RAPPORT_FAMILLE_NON_SOUMIS', 'Rapport famille non soumis', '#f59e0b', 4, TRUE),
    ('ALERT_TYPE', 'ALERTE_ABSENCE', 'Alerte absence', '#ef4444', 5, TRUE),
    ('ALERT_TYPE', 'MANUEL', 'Manuelle', '#8b5cf6', 6, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Cibles des alertes (miroir de l'enum CibleAlerte)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('ALERT_CIBLE', 'PERSONNE', 'Personne', '#3b82f6', 1, TRUE),
    ('ALERT_CIBLE', 'DEPARTEMENT', 'Département', '#a855f7', 2, TRUE),
    ('ALERT_CIBLE', 'FAMILLE', 'Famille', '#22c55e', 3, TRUE),
    ('ALERT_CIBLE', 'GROUPE', 'Groupe', '#f59e0b', 4, TRUE),
    ('ALERT_CIBLE', 'EGLISE', 'Église', '#06b6d4', 5, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Priorités des alertes (miroir de l'enum PrioriteAlerte)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('ALERT_PRIORITE', 'BASSE', 'Basse', '#94a3b8', 1, TRUE),
    ('ALERT_PRIORITE', 'MOYENNE', 'Moyenne', '#3b82f6', 2, TRUE),
    ('ALERT_PRIORITE', 'HAUTE', 'Haute', '#f59e0b', 3, TRUE),
    ('ALERT_PRIORITE', 'URGENTE', 'Urgente', '#ef4444', 4, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Statuts des alertes (miroir de l'enum StatutAlerte)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('ALERT_STATUS', 'ACTIVE', 'Active', '#ef4444', 1, TRUE),
    ('ALERT_STATUS', 'TRAITEE', 'Traitée', '#f59e0b', 2, TRUE),
    ('ALERT_STATUS', 'RESOLUE', 'Résolue', '#22c55e', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Types de notifications (miroir de l'enum TypeNotification)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('NOTIFICATION_TYPE', 'RAPPORT_NON_SOUMIS', 'Rapport non soumis', '#f59e0b', 1, TRUE),
    ('NOTIFICATION_TYPE', 'ABSENCE_48H', 'Absence 48h', '#ef4444', 2, TRUE),
    ('NOTIFICATION_TYPE', 'RAPPORT_FAMILLE_NON_SOUMIS', 'Rapport famille non soumis', '#f59e0b', 3, TRUE),
    ('NOTIFICATION_TYPE', 'ALERTE_ABSENCE', 'Alerte absence', '#ef4444', 4, TRUE),
    ('NOTIFICATION_TYPE', 'INFORMATION', 'Information', '#3b82f6', 5, TRUE),
    ('NOTIFICATION_TYPE', 'PRIERE_EXAUCEE', 'Prières exaucées', '#22c55e', 6, TRUE),
    ('NOTIFICATION_TYPE', 'TRANSFERT_DEMANDE', 'Demande de transfert', '#3b82f6', 7, TRUE),
    ('NOTIFICATION_TYPE', 'TRANSFERT_VALIDATION', 'Validation de transfert', '#8b5cf6', 8, TRUE),
    ('NOTIFICATION_TYPE', 'TRANSFERT_VALIDEE', 'Transfert validé', '#22c55e', 9, TRUE),
    ('NOTIFICATION_TYPE', 'TRANSFERT_REFUSEE', 'Transfert refusé', '#ef4444', 10, TRUE),
    ('NOTIFICATION_TYPE', 'TRANSFERT_INFOS_DEMANDEES', 'Informations demandées', '#f59e0b', 11, TRUE),
    ('NOTIFICATION_TYPE', 'TRANSFERT_CORRECTION', 'Correction demandée', '#f59e0b', 12, TRUE),
    ('NOTIFICATION_TYPE', 'TRANSFERT_EXECUTEE', 'Transfert exécuté', '#22c55e', 13, TRUE),
    ('NOTIFICATION_TYPE', 'TRANSFERT_ANNULEE', 'Transfert annulé', '#6b7280', 14, TRUE),
    ('NOTIFICATION_TYPE', 'TRANSFERT_DELAI_DEPASSE', 'Délai de traitement dépassé', '#ef4444', 15, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;

-- Canaux de notification (miroir de l'enum CanalNotification)
INSERT INTO dictionary_entries (dict_key, code, label, color, ordre, is_default) VALUES
    ('NOTIFICATION_CANAL', 'IN_APP', 'Dans l''application', '#3b82f6', 1, TRUE),
    ('NOTIFICATION_CANAL', 'EMAIL', 'E-mail', '#8b5cf6', 2, TRUE),
    ('NOTIFICATION_CANAL', 'PUSH', 'Notification push', '#06b6d4', 3, TRUE)
ON CONFLICT (dict_key, code) DO NOTHING;
