-- V194__restore_events_table_name.sql
-- ============================================================
-- DÉRIVE DE SCHÉMA H : LA TABLE PHYSIQUE « events » RÉTABLIE (famille des
-- correctifs H1-H5 de la branche fix/schema-drift-h1-h5 ; découverte le
-- 2026-09-29 par le replay de la recette verify-tenant-onboarding.sh sur
-- PostgreSQL 16 réel, TODO reprise §5.5).
--
-- Constat : V158 (G3.3, migration vers le Church OS Event Engine) a renommé
-- « events » → « legacy_events » et copié les lignes dans la nouvelle table
-- « event » (mappée par ChurchEvent), SANS JAMAIS publier de mappage pour
-- l'entité legacy Event (@Table « events », EventService, EventController
-- /api/v1/events, et l'étape FIRST_EVENT du wizard d'onboarding). Sur toute
-- base migrée, la relation « events » est donc absente : chaque appel legacy
-- répond 500 « ERROR: relation "events" does not exist » — vérifié en réel
-- (500 sur FIRST_EVENT au replay 2026-09-29). La suite de tests ne voyait
-- rien : profil H2 avec Flyway désactivé et ddl-auto créant la table depuis
-- l'entité. Le scan systématique du gate Flyway/Testcontainers (chaîne
-- complète sur PG réel puis existence de chaque table d'entité) bloque
-- désormais toute dérive de cette famille à l'entrée.
--
-- Résolution choisie : le contrat du code (entité, contrôleurs, seeds des
-- tests d'isolation multi-tenant) dit « events » ; seule la migration
-- faisait exception. C'est donc le schéma qui rejoint le code, par un
-- simple renommage inverse — sans perte de données (aucune écriture n'a pu
-- atteindre « events » puisque la table n'existait plus ; « legacy_events »
-- porte l'historique exact), et sans toucher à « event » (Church OS), qui
-- garde la copie effectuée par V158.
--
-- Fail-closed : si les DEUX tables coexistent (environnement having.run avec
-- profil docker ddl-auto:update où Hibernate aurait recréé « events » vide
-- depuis l'entité, avec des données divergentes des deux côtés), aucun
-- automerge n'est tenté — la migration échoue avec un message explicite et
-- la décision revient à un humain. Si aucune des deux n'existe, échec
-- également : l'environnement n'a pas la baseline V158 supposée.
-- ============================================================

DO $v194$
DECLARE
    has_legacy boolean;
    has_events boolean;
    r          record;
BEGIN
    has_legacy := to_regclass('public.legacy_events') IS NOT NULL;
    has_events := to_regclass('public.events') IS NOT NULL;

    IF has_legacy AND NOT has_events THEN
        ALTER TABLE public.legacy_events RENAME TO events;
        -- Rétablir les noms d'index renommés par V158 (cosmétique, only if free).
        FOR r IN
            SELECT * FROM (VALUES
                ('idx_legacy_events_organisateur', 'idx_events_organisateur'),
                ('idx_legacy_events_famille',      'idx_events_famille'),
                ('idx_legacy_events_type',         'idx_events_type'),
                ('idx_legacy_events_date',         'idx_events_date'),
                ('idx_legacy_events_statut',       'idx_events_statut'),
                ('idx_legacy_events_deleted',      'idx_events_deleted')
            ) AS t(oldname, newname)
        LOOP
            IF to_regclass('public.' || r.oldname) IS NOT NULL
               AND to_regclass('public.' || r.newname) IS NULL THEN
                EXECUTE 'ALTER INDEX public.' || quote_ident(r.oldname)
                     || ' RENAME TO ' || quote_ident(r.newname);
            END IF;
        END LOOP;
    ELSIF NOT has_legacy AND NOT has_events THEN
        RAISE EXCEPTION
            'V194 : ni « events » ni « legacy_events » — environnement sans la baseline V158 attendue ; vérifier l''historique Flyway avant de poursuivre.';
    ELSIF has_legacy AND has_events THEN
        RAISE EXCEPTION
            'V194 : coexistence « events » + « legacy_events » (signature d''un environnement ayant tourné avec ddl-auto:update après V158). Aucune fusion automatique : réconcilier manuellement les deux tables, puis re-exécuter le déploiement.';
    END IF;
    -- has_events AND NOT has_legacy : état cible déjà atteint (re-déploiement), RIEN.
END
$v194$;

COMMENT ON TABLE events IS
    'Événements legacy (Discipolat historique). V194 annule le renommage V158 qui cassait le contrat de l''entité Event ; la table Church OS « event » (V158, mappée par ChurchEvent) reste le modèle cible du chantier de réécriture en cours.';
