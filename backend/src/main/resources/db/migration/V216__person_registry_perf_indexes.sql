-- ============================================================================
-- V216 (PORTÉ de Develop1 V176) — §G6.5 Performance : index de recherche/tri sur le registre canonique
-- ============================================================================
-- Constat mesuré en charge (scripts/perf_loadseed.sql : 10 000 personnes,
-- 101 tenants, 812 événements, 60 000 pointages) via scripts/perf_bench.py :
--
--   /api/v1/people?search=…      → Seq Scan on person (Rows Removed: 11 698)
--                                  Execution Time 7,4 ms pour 12 k lignes
--   /api/v1/people?page=250      → Seq Scan + quicksort de 10 018 lignes
--                                  (Memory: 2924 kB) Execution Time 16,9 ms
--
-- Le registre `person` (people engine §G3.1, 10 000+ lignes/tenant) était le
-- SEUL gros tableau dépourvu d'index trigramme : `users` et `souls` en avaient,
-- `person` non. À 100 000 personnes (cible §71), ces plans deviennent des
-- parcours complets à chaque frappe au clavier → dépassement du budget
-- p95 < 500 ms.
--
-- Corrections :
--   1. GIN pg_trgm sur lower(first/last_name), display_name, email_normalized,
--      phone_normalized → matche exactement le prédicat
--      LOWER(col) LIKE LOWER('%q%') émis par PersonRepository#search.
--   2. B-tree (tenant_id, last_name, first_name) → évite tri + parcours complet
--      des listes paginées (ORDER BY lastName, firstName scoping tenant).
--   3. B-tree (tenant_id, status), (tenant_id, created_at DESC) → filtres
--      d'état et « derniers ajoutés ».
--   4. space_membership (person_id, status) → le compteur « sans espace »
--      (§G3.1/G6.4) devient un index-only scan au lieu d'un COUNT par ligne.
--   5. event_attendance (tenant_id, church_event_id) → statistiques de
--      présence et calendrier chargé (§71 « calendrier chargé »).
--
-- Ces index ne suppriment aucune donnée et sont idempotents (IF NOT EXISTS).
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 1. Recherche (PersonRepository#search : LOWER(col) LIKE LOWER('%q%'))
CREATE INDEX IF NOT EXISTS idx_person_first_name_trgm
    ON person USING gin (lower(first_name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_person_last_name_trgm
    ON person USING gin (lower(last_name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_person_display_name_trgm
    ON person USING gin (lower(display_name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_person_email_trgm
    ON person USING gin (lower(email_normalized) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_person_phone_trgm
    ON person USING gin (lower(phone_normalized) gin_trgm_ops);

-- 2. Listes paginées triées par nom (budget < 500 ms même en pagination profonde)
CREATE INDEX IF NOT EXISTS idx_person_tenant_name
    ON person (tenant_id, last_name, first_name);

-- 3. Filtres d'état et tri par ancienneté
CREATE INDEX IF NOT EXISTS idx_person_tenant_status
    ON person (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_person_tenant_created
    ON person (tenant_id, created_at DESC);

-- 4. Appartenance aux espaces (fichier « sans espace », compteurs par personne)
CREATE INDEX IF NOT EXISTS idx_sm_person_status
    ON space_membership (person_id, status);
CREATE INDEX IF NOT EXISTS idx_sm_tenant_space
    ON space_membership (tenant_id, space_id);

-- 5. Présences : calendrier chargé + rapports de présence par tenant
CREATE INDEX IF NOT EXISTS idx_evatt_tenant_event
    ON event_attendance (tenant_id, church_event_id);
CREATE INDEX IF NOT EXISTS idx_evatt_tenant_person
    ON event_attendance (tenant_id, person_id);

ANALYZE person;
ANALYZE space_membership;
ANALYZE event_attendance;
