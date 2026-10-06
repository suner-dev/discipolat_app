-- V232__notification_type_dictionary_backfill.sql
-- ============================================================
-- Backfill du dictionnaire NOTIFICATION_TYPE.
--
-- Constat : l'enum TypeNotification porte 24 valeurs alors que le
-- dictionnaire NOTIFICATION_TYPE n'en portait que 15 (V45). Les 9
-- premiers types ajoutés par la roadmap G4.x n'ont jamais reçu de
-- ligne de dictionnaire. Conséquences visibles par les utilisateurs :
--   - libellé non personnalisable par l'église ;
--   - PAS DE COULEUR de badge (dictionaries.color() renvoie undefined,
--     l'interface retombe sur un badge gris) ;
--   - sur les écrans non francophones, repli sur le code brut.
--
-- V231 en ajoute 2 de plus (RELATION_DECLAREE, RELATION_REVOQUEE),
-- ce qui porte l'écart à 11 lignes manquantes — toutes insérées ici.
-- Migration MONTANTE et strictement additive : garde NOT EXISTS sur
-- (tenant_id, dict_key, code) → ré-exécution sans danger, et aucune
-- ligne existante n'est écrasée (une église qui a personnalisé son
-- libellé ou sa couleur conserve sa valeur).
--
-- Les nouveaux tenants les reçoivent déjà via
-- DictionaryService.defaultSeedData() (miroir de ce fichier).
-- ============================================================

INSERT INTO dictionary_entries (id, tenant_id, dict_key, code, label, color, ordre, actif, is_default, created_at, updated_at)
SELECT uuid_generate_v4(), t.id, 'NOTIFICATION_TYPE', seed.code, seed.label, seed.color, seed.ordre, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
CROSS JOIN (VALUES
    ('MEMBRE_AJOUTE',             'Membre ajouté',             '#22c55e', 20),
    ('MEMBRE_RETIRE',             'Membre retiré',             '#6b7280', 21),
    ('MEMBRE_AFFECTE',            'Membre affecté',            '#3b82f6', 22),
    ('TACHE_ASSIGNEE',            'Tâche assignée',            '#3b82f6', 23),
    ('TACHE_EN_RETARD',           'Tâche en retard',           '#f59e0b', 24),
    ('EVENEMENT_RAPPEL',          'Rappel d''événement',       '#8b5cf6', 25),
    ('DEMANDE_ACCES',             'Demande d''accès',           '#f59e0b', 26),
    ('OFFLINE_CONFLIT',           'Conflit hors-ligne',        '#ef4444', 27),
    ('SUIVI_RAPPEL',              'Rappel de suivi',           '#f59e0b', 28),
    ('PASTORAT_NOMINATION',       'Nomination pastorale',      '#8b5cf6', 29),
    ('RELATION_DECLAREE',         'Nouveau rattachement',      '#ec4899', 30),
    ('RELATION_REVOQUEE',         'Rattachement retiré',      '#64748b', 31)
) AS seed(code, label, color, ordre)
WHERE NOT EXISTS (
    SELECT 1 FROM dictionary_entries d
    WHERE d.tenant_id = t.id AND d.dict_key = 'NOTIFICATION_TYPE' AND d.code = seed.code
);
