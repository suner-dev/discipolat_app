-- V238__tasks_tags_text_column.sql
-- ============================================================
-- Correction du type des colonnes de tags de la V234.
--
-- CONSTAT
--   La migration V234 déclarait `tags TEXT[]` et `default_tags TEXT[]`
--   (tableau PostgreSQL), MAIS l'entité `Task` sérialise ces listes via
--   `StringListConverter`, un `AttributeConverter<List<String>, String>` qui
--   écrit une CHAÎNE (liste jointe par des virgules).
--
--   Les deux déclarations étaient donc incompatibles :
--     * sur le profil de test (H2), `TEXT[]` est un type invalide -> la
--       création du schéma échouait et AUCUNE table tasks/task_templates
--       n'était créée ;
--     * sur PostgreSQL, toute écriture aurait été rejetée (chaîne dans une
--       colonne tableau).
--
--   Ce défaut n'était pas visible à la compilation : il ne se révélait qu'au
--   démarrage du contexte Spring.
--
-- CORRECTION
--   Migration additive et idempotente : on convertit les colonnes en TEXT.
--   `USING` rend la conversion compatible avec les lignes déjà présentes
--   (un tableau PostgreSQL est aplati en sa représentation textuelle) ; sur une
--   base neuve, la colonne est simplement créée en TEXT.
--
-- RÈGLE : ne jamais réécrire une migration déjà appliquée (R10) — d'où V238
-- plutôt qu'une édition de V234.
-- ============================================================

ALTER TABLE IF EXISTS tasks
    ALTER COLUMN tags TYPE TEXT
    USING CASE WHEN tags IS NULL THEN NULL ELSE array_to_string(tags, ',') END;

ALTER TABLE IF EXISTS task_templates
    ALTER COLUMN default_tags TYPE TEXT
    USING CASE WHEN default_tags IS NULL THEN NULL ELSE array_to_string(default_tags, ',') END;

COMMENT ON COLUMN tasks.tags IS 'Tags séparés par des virgules (sérialisés par StringListConverter) — V238.';
COMMENT ON COLUMN task_templates.default_tags IS 'Tags par défaut séparés par des virgules — V238.';
