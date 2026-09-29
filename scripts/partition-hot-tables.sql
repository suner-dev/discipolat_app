-- ============================================================================
-- A3 (M7) — PROCÉDURE DOCUMENTÉE : conversion RANGE d'une table chaude
-- ----------------------------------------------------------------------------
-- POURQUOI UN SCRIPT ET PAS UNE MIGRATION FLYWAY ?
-- Le prompt A3 l'exige explicitement : « sur une table peuplée, crée la table
-- partitionnée en parallèle et un script de migration documenté — NE SUPPRIME
-- PAS la table existante ». Convertir une table en table partitionnée impose
-- PostgreSQL à reconstruire la table (CREATE TABLE ... PARTITION OF + copie) :
-- sur une base de production peuplée, c'est des minutes/heures de verrouillage
-- si c'est mal fait. Une migration automatique qui fait ça au démarrage de
-- l'application est dangereux. Cette procédure est donc un runbook EXÉCUTÉ
-- MANUELLEMENT pendant une fenêtre de maintenance, étape par étape, avec
-- vérification à chaque phase.
--
-- QUELLE TABLE ? — Mesure honnête obligatoire avant d'agir (jamais « au feeling ») :
--   SELECT relname, reltuples::bigint AS est_rows
--     FROM pg_class WHERE relkind='r' ORDER BY reltuples DESC LIMIT 10;
-- Sur la base de référence du produit (schéma frais), AUCUNE table n'atteint
-- l'échelle où le partitionnement paie (seuil pratique : ~50-100 M de rows).
-- Les candidates désignées par la croissance prévue : notifications (1 ligne
-- par événement notifié), audit/event_publication, finance_transactions.
-- L'exemple ci-dessous porte sur `notifications`, adapter nom/colonnes sinon.
--
-- PRÉREQUIS : fenêtre de maintenance, réplication testée, dump pg_dump frais,
-- place disque ≥ 2× la table, et shard-count documenté dans docs/SCALING.md.
-- ============================================================================

-- ÉTAPE 0 — Sauvegarde de sécurité (à ne JAMAIS sauter)
\echo '!! pg_dump -Fc -t notifications > /backup/notifications_pre_partition.dump avant de continuer !!'

-- ÉTAPE 1 — Table partitionnée EN PARALLÈLE (la table existante n'est pas touchée)
-- Partitionnement RANGE sur tenant_id : chaque partition couvre un anneau de
-- valeurs d'UUID (quart de l'espace des clés ci-dessous).
-- Alternative plus simple si N est petit : LIST partition par tenant_id explicite
-- pour les gros tenants + DEFAULT pour les autres.

BEGIN;
CREATE TABLE notifications_partitioned (
    LIKE notifications INCLUDING ALL
) PARTITION BY RANGE (tenant_id);

-- Exemple de découpage : anneaux par préfixe hexadécimal (tenant_id est un UUID ;
-- le RANGE découpe l'espace des clés en parts égales). Ce découpage est LOCAL au
-- cluster (pruning de partition) ; le routage multi-cluster de ShardRouting est
-- l'étape Niveau 3 de docs/SCALING.md, orthogonale à celle-ci.
CREATE TABLE notifications_p0 PARTITION OF notifications_partitioned FOR VALUES FROM (MINVALUE) TO ('40000000-0000-0000-0000-000000000000');
CREATE TABLE notifications_p1 PARTITION OF notifications_partitioned FOR VALUES FROM ('40000000-0000-0000-0000-000000000000') TO ('80000000-0000-0000-0000-000000000000');
CREATE TABLE notifications_p2 PARTITION OF notifications_partitioned FOR VALUES FROM ('80000000-0000-0000-0000-000000000000') TO ('c0000000-0000-0000-0000-000000000000');
CREATE TABLE notifications_p3 PARTITION OF notifications_partitioned FOR VALUES FROM ('c0000000-0000-0000-0000-000000000000') TO (MAXVALUE);
COMMIT;

-- ÉTAPE 2 — Recopie incrémentale testée (hors maintenance, aucune lock)
INSERT INTO notifications_partitioned
SELECT * FROM notifications;

-- ÉTAPE 3 — Vérification d'exhaustivité AVANT bascule (bloquante)
-- Les deux compteurs DOIVENT être égaux ; sinon, ne PAS basculer, debugger.
SELECT (SELECT count(*) FROM notifications)          AS ancien,
       (SELECT count(*) FROM notifications_partitioned) AS nouveau;

-- ÉTAPE 4 — Pendant la fenêtre de gel des écritures : stopper le backend, rattraper
-- le delta s'il y en a, puis bascule par RENAME (atomique, < 1 s). L'ancienne
-- table est CONSERVÉE (retour arrière possible).
-- BEGIN;
-- LOCK TABLE notifications IN ACCESS EXCLUSIVE MODE;
-- SELECT count(*) FROM notifications WHERE id NOT IN (SELECT id FROM notifications_partitioned);
-- ALTER TABLE notifications RENAME TO notifications_pre_partition_backup;
-- ALTER TABLE notifications_partitioned RENAME TO notifications;
-- COMMIT;
-- (décommenter et exécuter pendant la fenêtre, après validation en préproduction)

-- ÉTAPE 5 — Validation applicative puis nettoyage différé (≥ 30 jours de garde)
-- DROP TABLE notifications_pre_partition_backup; -- SEULEMENT après période de garde
