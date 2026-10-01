-- V214__fresh_install_identity_and_pk_fix.sql (PORTÉ de Develop1 V172 ; suit V212)
-- ============================================================
-- §G6.4 — E2E CP2 a révélé un défaut d'installation vierge : V212 (ex-V170) a recréé
-- les tables « delta ddl-auto » en dumpant les COLONNES mais ni les séquences
-- IDENTITY (bigint) ni les clés primaires. Résultat sur base neuve : toute
-- écriture dans outbox_event / processed_event partait en 500 (id null) et
-- onze tables moteur vivaient sans PK.
-- Correctif défensif : chaque opération est no-op sur les bases déjà saines
-- (dev/beta créées par ddl-auto, Render). En détail :
--   1. séquences + valeurs par défaut des deux tables du transactional outbox
--      (OutboxEvent/ProcessedEvent = GenerationType.IDENTITY côté entité) ;
--   2. PRIMARY KEY (id) restaurée si absente ;
--   3. unicité (consumer, event_id) de processed_event — c'est la garantie
--      d'idempotence des consommateurs outbox, pas un simple index.
-- ============================================================

-- 1. IDENTITY : séquences ownées par la colonne (identique à bigserial)
CREATE SEQUENCE IF NOT EXISTS public.outbox_event_id_seq;
ALTER TABLE public.outbox_event ALTER COLUMN id SET DEFAULT nextval('public.outbox_event_id_seq');
ALTER SEQUENCE public.outbox_event_id_seq OWNED BY public.outbox_event.id;
SELECT setval('public.outbox_event_id_seq', COALESCE((SELECT MAX(id) FROM public.outbox_event), 0) + 1, false);

CREATE SEQUENCE IF NOT EXISTS public.processed_event_id_seq;
ALTER TABLE public.processed_event ALTER COLUMN id SET DEFAULT nextval('public.processed_event_id_seq');
ALTER SEQUENCE public.processed_event_id_seq OWNED BY public.processed_event.id;
SELECT setval('public.processed_event_id_seq', COALESCE((SELECT MAX(id) FROM public.processed_event), 0) + 1, false);

-- 2. Clés primaires manquantes (liste = inventaire réel d'une base vierge migrée)
DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'outbox_event', 'processed_event', 'ai_usage', 'events',
        'event_asset', 'event_attendance', 'event_document', 'event_expense',
        'event_schedule', 'event_space', 'event_task', 'event_team', 'location'
    ]
    LOOP
        IF EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = 'public' AND table_name = t)
          AND NOT EXISTS (SELECT 1 FROM pg_constraint
                          WHERE contype = 'p' AND conrelid = ('public.' || quote_ident(t))::regclass)
        THEN
            EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT pk_%s PRIMARY KEY (id)', t, t);
        END IF;
    END LOOP;
END $$;

-- 3. Idempotence consommateurs : un même événement n'est traité qu'une fois
--    par consommateur (uk_processed_consumer_event de l'entité).
CREATE UNIQUE INDEX IF NOT EXISTS uk_processed_consumer_event
    ON public.processed_event (consumer, event_id);
