-- V239__tasks_sort_order.sql
-- ============================================================
-- Rang d'affichage des tâches dans le Kanban.
--
-- CONSTAT
--   Le mobile envoie `{status, order}` sur `POST /tasks/{id}/reorder`
--   (`reorderTasks` de tasks_service.dart). Le serveur déléguait à
--   `updateTask`, qui ne connaît pas la clé `order` : l'appel renvoyait 200
--   SANS rien persister. Le glisser-déposer du tableau Kanban était donc inopérant.
--
-- CORRECTION
--   Colonne `sort_order` nullable et sans contrainte : elle n'affecte que
--   l'ordre d'affichage, n'est pas une donnée métier, et son absence doit rester
--   possible (tâches créées avant cette migration).
--
-- Règle R10 : migration additive et idempotente (`IF NOT EXISTS`), et surtout
-- pas de réécriture de V234.
-- ============================================================

ALTER TABLE IF EXISTS tasks
    ADD COLUMN IF NOT EXISTS sort_order INT;

CREATE INDEX IF NOT EXISTS idx_tasks_kanban ON tasks (tenant_id, status, sort_order);

COMMENT ON COLUMN tasks.sort_order IS 'Rang d''affichage dans la colonne Kanban (V239).';
