-- ============================================================================
-- perf_loadseed.sql — §G6.5 Générateur de charge multi-tenant RÉEL
-- ============================================================================
-- Cible de mesure (budgets §G6.5) :
--   tenant principal …0001 : 10 000 personnes · 108 espaces/nœuds organe
--                            300 événements · 60 000 pointages · 50 dress codes
--   + 100 tenants « églises » supplémentaires (20 personnes, 5 événements chacun)
--   ⇒ recherche globale cross-tenant, bootstrap d'espace, listes paginées
--     dans des conditions commerciales (100 tenants × ~250 lignes).
--
-- Ré-exécutable (ON CONFLICT DO NOTHING + UUIDs déterministes md5()).
-- N'EFFACE AUCUNE donnée existante.
--
-- Usage :
--   PGPASSWORD=discipolat_secret psql -h localhost -p 5433 \
--     -U discipolat -d discipolat -f scripts/perf_loadseed.sql
-- ============================================================================

\set ON_ERROR_STOP on
\set echo off

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. PERSONNES du tenant principal — 10 000 fiches scopées tenant
-- ---------------------------------------------------------------------------
INSERT INTO person (id, tenant_id, first_name, last_name, display_name, gender,
                    birth_date, phone_normalized, email_normalized, address,
                    status, visibility_scope, created_at, updated_at)
SELECT md5('perf-person-' || i)::uuid,
       '00000000-0000-0000-0000-000000000001'::uuid,
       'Prénom' || i,
       'Nom' || (i % 500),
       'Prénom' || i || ' Nom' || (i % 500),
       CASE WHEN i % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END,
       DATE '1970-01-01' + (i % 12000) * INTERVAL '1 day',
       '06' || lpad(i::text, 8, '0'),
       'perf' || i || '@discipolat.local',
       'Quartier ' || (i % 40) || ', Ville',
       'ACTIVE',
       CASE WHEN i % 10 = 0 THEN 'PRIVATE' ELSE 'CHURCH' END,
       NOW(), NOW()
FROM generate_series(1, 10000) AS i
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. NŒUDS ORGANISATION + ESPACES — 100 espaces de plus au tenant principal
-- ---------------------------------------------------------------------------
INSERT INTO organization_nodes (id, tenant_id, parent_id, type, name, code, status,
                                path, level, timezone, country, city,
                                metadata_json, config_source, resolved_config_json,
                                description, icon, color, sort_order, created_at, updated_at)
SELECT md5('perf-node-' || i)::uuid,
       '00000000-0000-0000-0000-000000000001'::uuid,
       'd1c3eaa4-6d55-460c-9ba0-0fdf8a7f6deb'::uuid,
       'DEPARTMENT',
       'Département perf ' || i,
       'PERF_DEPT_' || i,
       'ACTIVE',
       ('root_demo.perf_dept_' || i)::ltree,
       1,
       'Africa/Libreville', 'GA', 'Libreville',
       '{}'::jsonb, 'DEFAULT', '{}'::jsonb,
       'Espace de charge §G6.5 n°' || i,
       'business', '#6366f1', i, NOW(), NOW()
FROM generate_series(1, 100) AS i
ON CONFLICT (id) DO NOTHING;

INSERT INTO spaces (id, tenant_id, organization_unit_id, space_type, template_code,
                    name, code, icon, color, description, status,
                    visible_people_scope, configuration_json, created_at, updated_at)
SELECT md5('perf-space-' || i)::uuid,
       '00000000-0000-0000-0000-000000000001'::uuid,
       md5('perf-node-' || i)::uuid,
       'DEPARTMENT',
       'department-default',
       'Département perf ' || i,
       'PERF_SP_' || i,
       'business', '#6366f1',
       'Espace de charge §G6.5',
       'ACTIVE',
       'CHURCH',
       '{"modules": ["people", "events", "reports"]}'::jsonb,
       NOW(), NOW()
FROM generate_series(1, 100) AS i
ON CONFLICT (id) DO NOTHING;

-- Affiliations : ~10 membres par espace perf (1 000 lignes)
INSERT INTO space_membership (id, tenant_id, person_id, space_id, joined_at,
                              status, membership_type, created_at, updated_at)
SELECT md5('perf-sm-' || (s * 10 + m))::uuid,
       '00000000-0000-0000-0000-000000000001'::uuid,
       md5('perf-person-' || (((s * 10 + m) % 10000) + 1))::uuid,
       md5('perf-space-' || s)::uuid,
       NOW() - (s * INTERVAL '1 day'),
       'ACTIVE', 'MEMBER', NOW(), NOW()
FROM generate_series(1, 100) AS s, generate_series(1, 10) AS m
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. ÉVÉNEMENTS du tenant principal — 300 événements sur 18 mois
-- ---------------------------------------------------------------------------
INSERT INTO event (id, tenant_id, title, description, type, status, start_at, end_at,
                   timezone, is_recurring, visibility, created_at, updated_at)
SELECT md5('perf-event-' || i)::uuid,
       '00000000-0000-0000-0000-000000000001'::uuid,
       'Culte de chargement n°' || i,
       'Événement de performance §G6.5',
       'CULTE',
       CASE WHEN i % 7 = 0 THEN 'DRAFT' ELSE 'PUBLISHED' END,
       NOW() - INTERVAL '9 months' + i * INTERVAL '2 days',
       NOW() - INTERVAL '9 months' + i * INTERVAL '2 days' + INTERVAL '2 hours',
       'Africa/Libreville',
       false,
       'CHURCH',
       NOW(), NOW()
FROM generate_series(1, 300) AS i
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. POINTAGES — 200 présences × 300 événements = 60 000 lignes
--    (calendrier chargé + statistiques présence en charge)
-- ---------------------------------------------------------------------------
INSERT INTO event_attendance (id, tenant_id, church_event_id, person_id, space_id,
                              status, check_in_at, check_in_method, created_at, updated_at)
SELECT md5('perf-att-' || e || '-' || p)::uuid,
       '00000000-0000-0000-0000-000000000001'::uuid,
       md5('perf-event-' || e)::uuid,
       md5('perf-person-' || (((e * 200 + p) % 10000) + 1))::uuid,
       md5('perf-space-' || (((e * 200 + p) % 100) + 1))::uuid,
       'PRESENT',
       NOW() - INTERVAL '9 months' + e * INTERVAL '2 days',
       'MANUAL',
       NOW(), NOW()
FROM generate_series(1, 300) AS e, generate_series(1, 200) AS p
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5. DRESS CODES — 50 tenues à venir (agenda + notification §G3.4 en charge)
-- ---------------------------------------------------------------------------
INSERT INTO dress_code (id, tenant_id, space_id, event_id, service_name, title,
                        begins_at, ends_at, status, created_by, archived,
                        created_at, updated_at)
SELECT md5('perf-dc-' || i)::uuid,
       '00000000-0000-0000-0000-000000000001'::uuid,
       md5('perf-space-' || ((i % 100) + 1))::uuid,
       md5('perf-event-' || ((i % 280) + 1))::uuid,
       'Culte du ' || i,
       'Tenue de chargement n°' || i,
       NOW() + i * INTERVAL '3 days',
       NOW() + i * INTERVAL '3 days' + INTERVAL '2 hours',
       'PUBLISHED',
       '00000000-0000-0000-0000-000000000001'::uuid,
       false, NOW(), NOW()
FROM generate_series(1, 50) AS i
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. 100 TENANTS « églises » supplémentaires (pression cross-tenant)
--    Chaque église : racine organe + 20 personnes + 5 événements.
-- ---------------------------------------------------------------------------
INSERT INTO tenants (id, name, slug, status, plan, locale, timezone, country,
                     currency, created_at, updated_at)
SELECT md5('perf-tenant-' || t)::uuid,
       'Église perf ' || t,
       'perf-eglise-' || t,
       'ACTIVE',
       'STARTUP',
       'fr', 'Africa/Libreville', 'GA', 'XAF',
       NOW(), NOW()
FROM generate_series(1, 100) AS t
ON CONFLICT (id) DO NOTHING;

INSERT INTO organization_nodes (id, tenant_id, parent_id, type, name, code, status,
                                path, level, timezone, country, city,
                                metadata_json, config_source, resolved_config_json,
                                description, icon, color, sort_order, created_at, updated_at)
SELECT md5('perf-tnode-' || t)::uuid,
       md5('perf-tenant-' || t)::uuid,
       NULL,
       'ROOT_CHURCH',
       'Église perf ' || t,
       'PERF_ROOT_' || t,
       'ACTIVE',
       ('perf_root_' || t)::ltree,
       0,
       'Africa/Libreville', 'GA', 'Libreville',
       '{}'::jsonb, 'DEFAULT', '{}'::jsonb,
       'Racine de charge', 'church', '#10b981', t, NOW(), NOW()
FROM generate_series(1, 100) AS t
ON CONFLICT (id) DO NOTHING;

INSERT INTO person (id, tenant_id, first_name, last_name, display_name, gender,
                    birth_date, phone_normalized, email_normalized, address,
                    status, visibility_scope, created_at, updated_at)
SELECT md5('perf-tp-' || t || '-' || i)::uuid,
       md5('perf-tenant-' || t)::uuid,
       'Membre' || i,
       'Église' || t,
       'Membre' || i || ' Église' || t,
       CASE WHEN i % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END,
       DATE '1980-01-01' + i * INTERVAL '30 days',
       '07' || lpad(t::text, 3, '0') || lpad(i::text, 5, '0'),
       't' || t || 'p' || i || '@perf.local',
       'Ville ' || t,
       'ACTIVE', 'CHURCH', NOW(), NOW()
FROM generate_series(1, 100) AS t, generate_series(1, 20) AS i
ON CONFLICT (id) DO NOTHING;

INSERT INTO event (id, tenant_id, title, description, type, status, start_at, end_at,
                   timezone, is_recurring, visibility, created_at, updated_at)
SELECT md5('perf-tev-' || t || '-' || i)::uuid,
       md5('perf-tenant-' || t)::uuid,
       'Culte perf ' || t || '-' || i,
       'Événement de charge multi-tenant',
       'CULTE', 'PUBLISHED',
       NOW() + i * INTERVAL '7 days',
       NOW() + i * INTERVAL '7 days' + INTERVAL '2 hours',
       'Africa/Libreville', false, 'CHURCH', NOW(), NOW()
FROM generate_series(1, 100) AS t, generate_series(1, 5) AS i
ON CONFLICT (id) DO NOTHING;

COMMIT;

-- Statistiques fraîches pour des plans d'exécution réalistes
ANALYZE person; ANALYZE event; ANALYZE event_attendance;
ANALYZE spaces; ANALYZE organization_nodes; ANALYZE space_membership;
ANALYZE dress_code; ANALYZE tenants;

\echo 'PERF_LOADSEED_DONE'
