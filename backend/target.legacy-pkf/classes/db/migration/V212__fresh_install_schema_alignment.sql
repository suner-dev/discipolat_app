-- V212__fresh_install_schema_alignment.sql (PORTÉ de Develop1 V170, ré-encodé après V194/V205)
-- ============================================================
-- §G6.9 — ALIGNEMENT SCHÉMA INSTALLATION VIERGE (production)
-- Constat (audit E2E G6.4) : les bases de développement portaient des objets
-- créés hors migrations par un ancien « ddl-auto: update » (tables event engine,
-- outbox/processed events, colonnes saas_plans double-marché, etc.). La chaîne
-- migratoire ne les créait jamais → un déploiement neuf était INOPÉRABLE.
-- Méthode : clonage de la base vierge migrée + boot en ddl-auto=update + dump
-- exact du delta. Tout est défensif (IF NOT EXISTS / DO blocks) : no-op complet
-- sur les bases existantes (dev/beta/Render).
-- ============================================================

-- 1. Colonnes manquantes sur tables existantes
ALTER TABLE conversation_messages ADD COLUMN IF NOT EXISTS group_id uuid;
ALTER TABLE conversation_messages ADD COLUMN IF NOT EXISTS is_deleted boolean;
ALTER TABLE conversation_messages ADD COLUMN IF NOT EXISTS media_duration integer;
ALTER TABLE conversation_messages ADD COLUMN IF NOT EXISTS media_url character varying(255);
ALTER TABLE conversation_messages ADD COLUMN IF NOT EXISTS message_type character varying(255);
ALTER TABLE conversation_messages ADD COLUMN IF NOT EXISTS reply_to_content character varying(255);
ALTER TABLE conversation_messages ADD COLUMN IF NOT EXISTS reply_to_id uuid;
ALTER TABLE conversation_messages ADD COLUMN IF NOT EXISTS reply_to_sender_name character varying(255);
ALTER TABLE files ADD COLUMN IF NOT EXISTS evenement_id uuid;
ALTER TABLE invitations ADD COLUMN IF NOT EXISTS organization_node_id uuid;
ALTER TABLE organization_nodes ADD COLUMN IF NOT EXISTS slug character varying(100);
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS billing_period character varying(20);
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS modules_included_json jsonb;
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS price_eur bigint;
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS price_usd bigint;
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS price_xaf bigint;
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS regions_json jsonb;

-- 2. Tables manquantes (moteurs Event OS, IA, lieu, outbox/idempotence)
CREATE TABLE IF NOT EXISTS public.ai_usage (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    credits_consumed integer NOT NULL,
    error_message text,
    model_used character varying(50),
    request_type character varying(50) NOT NULL,
    response_time_ms integer,
    success boolean,
    tenant_id uuid NOT NULL,
    tokens_input integer,
    tokens_output integer,
    usage_date date NOT NULL,
    user_id uuid NOT NULL
);
CREATE TABLE IF NOT EXISTS public.event_asset (
    id uuid NOT NULL,
    asset_id uuid NOT NULL,
    church_event_id uuid NOT NULL,
    condition_after character varying(30),
    condition_before character varying(30),
    created_at timestamp(6) with time zone NOT NULL,
    notes text,
    quantity integer NOT NULL,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE IF NOT EXISTS public.event_attendance (
    id uuid NOT NULL,
    check_in_at timestamp(6) with time zone,
    check_in_method character varying(30),
    check_out_at timestamp(6) with time zone,
    church_event_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    notes text,
    person_id uuid NOT NULL,
    space_id uuid,
    status character varying(30) NOT NULL,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE IF NOT EXISTS public.event_document (
    id uuid NOT NULL,
    church_event_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    document_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    type character varying(30)
);
CREATE TABLE IF NOT EXISTS public.event_expense (
    id uuid NOT NULL,
    church_event_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expense_id uuid NOT NULL,
    tenant_id uuid NOT NULL
);
CREATE TABLE IF NOT EXISTS public.event_schedule (
    id uuid NOT NULL,
    church_event_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    description text,
    end_at timestamp(6) with time zone,
    event_team_id uuid,
    is_public boolean NOT NULL,
    location_id uuid,
    order_index integer NOT NULL,
    start_at timestamp(6) with time zone NOT NULL,
    tenant_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE IF NOT EXISTS public.event_space (
    id uuid NOT NULL,
    church_event_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    role character varying(30),
    space_id uuid NOT NULL,
    tenant_id uuid NOT NULL
);
CREATE TABLE IF NOT EXISTS public.event_task (
    id uuid NOT NULL,
    assignee_id uuid,
    church_event_id uuid NOT NULL,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    deleted_at timestamp(6) with time zone,
    description text,
    due_at timestamp(6) with time zone,
    event_team_id uuid,
    priority integer,
    space_id uuid,
    status character varying(30) NOT NULL,
    tenant_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE IF NOT EXISTS public.event_team (
    id uuid NOT NULL,
    church_event_id uuid NOT NULL,
    color character varying(7),
    created_at timestamp(6) with time zone NOT NULL,
    description text,
    lead_person_id uuid,
    name character varying(200) NOT NULL,
    space_id uuid,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE IF NOT EXISTS public.events (
    id uuid NOT NULL,
    compte_rendu character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    date_debut timestamp(6) without time zone NOT NULL,
    date_fin timestamp(6) without time zone,
    deleted boolean NOT NULL,
    department_id uuid,
    description character varying(255),
    famille_id uuid,
    lieu character varying(255),
    limite_places integer,
    nb_inscrits integer NOT NULL,
    organisateur_id uuid NOT NULL,
    organization_unit_id uuid,
    resource_scope character varying(20) NOT NULL,
    statut character varying(255) NOT NULL,
    tenant_id uuid NOT NULL,
    titre character varying(255) NOT NULL,
    type_evenement character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT events_resource_scope_check CHECK (((resource_scope)::text = ANY ((ARRAY['TENANT_GLOBAL'::character varying, 'ORGANIZATION_LOCAL'::character varying, 'UNIT_LOCAL'::character varying])::text[])))
);
CREATE TABLE IF NOT EXISTS public.location (
    id uuid NOT NULL,
    address text,
    capacity integer,
    created_at timestamp(6) with time zone NOT NULL,
    deleted_at timestamp(6) with time zone,
    description text,
    name character varying(255) NOT NULL,
    parent_location_id uuid,
    tenant_id uuid NOT NULL,
    type character varying(30),
    updated_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE IF NOT EXISTS public.outbox_event (
    id bigint NOT NULL,
    aggregate_id uuid NOT NULL,
    aggregate_type character varying(100) NOT NULL,
    attempts integer NOT NULL,
    available_at timestamp(6) with time zone NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    event_type character varying(100) NOT NULL,
    payload_json jsonb NOT NULL,
    published_at timestamp(6) with time zone,
    status character varying(20) NOT NULL,
    tenant_id uuid
);
CREATE TABLE IF NOT EXISTS public.processed_event (
    id bigint NOT NULL,
    consumer character varying(100) NOT NULL,
    event_id bigint NOT NULL,
    processed_at timestamp(6) with time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ai_usage_tenant_date ON public.ai_usage USING btree (tenant_id, usage_date);
CREATE INDEX IF NOT EXISTS idx_ai_usage_type ON public.ai_usage USING btree (request_type);
CREATE INDEX IF NOT EXISTS idx_ai_usage_user ON public.ai_usage USING btree (user_id);
CREATE INDEX IF NOT EXISTS idx_ed_church_event ON public.event_document USING btree (church_event_id);
CREATE INDEX IF NOT EXISTS idx_ee_church_event ON public.event_expense USING btree (church_event_id);
CREATE INDEX IF NOT EXISTS idx_ee_expense ON public.event_expense USING btree (expense_id);
CREATE INDEX IF NOT EXISTS idx_es_church_event ON public.event_space USING btree (church_event_id);
CREATE INDEX IF NOT EXISTS idx_es_space ON public.event_space USING btree (space_id);
CREATE INDEX IF NOT EXISTS idx_esched_church_event ON public.event_schedule USING btree (church_event_id);
CREATE INDEX IF NOT EXISTS idx_esched_time ON public.event_schedule USING btree (start_at);
CREATE INDEX IF NOT EXISTS idx_et_church_event ON public.event_team USING btree (church_event_id);
CREATE INDEX IF NOT EXISTS idx_et_space ON public.event_team USING btree (space_id);
CREATE INDEX IF NOT EXISTS idx_etk_assignee ON public.event_task USING btree (assignee_id);
CREATE INDEX IF NOT EXISTS idx_etk_church_event ON public.event_task USING btree (church_event_id);
CREATE INDEX IF NOT EXISTS idx_etk_deleted ON public.event_task USING btree (deleted_at);
CREATE INDEX IF NOT EXISTS idx_etk_status ON public.event_task USING btree (status);
CREATE INDEX IF NOT EXISTS idx_evasset_asset ON public.event_asset USING btree (asset_id);
CREATE INDEX IF NOT EXISTS idx_evasset_church_event ON public.event_asset USING btree (church_event_id);
CREATE INDEX IF NOT EXISTS idx_evatt_church_event ON public.event_attendance USING btree (church_event_id);
CREATE INDEX IF NOT EXISTS idx_evatt_person ON public.event_attendance USING btree (person_id);
CREATE INDEX IF NOT EXISTS idx_evatt_status ON public.event_attendance USING btree (status);
CREATE INDEX IF NOT EXISTS idx_location_parent ON public.location USING btree (parent_location_id);
CREATE INDEX IF NOT EXISTS idx_location_tenant ON public.location USING btree (tenant_id);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON public.outbox_event USING btree (aggregate_type, aggregate_id);
CREATE INDEX IF NOT EXISTS idx_outbox_status_available ON public.outbox_event USING btree (status, available_at);
CREATE INDEX IF NOT EXISTS idx_outbox_tenant ON public.outbox_event USING btree (tenant_id);
CREATE INDEX IF NOT EXISTS idx_processed_consumer ON public.processed_event USING btree (consumer);
CREATE INDEX IF NOT EXISTS idx_processed_event_event_id ON public.processed_event USING btree (event_id);