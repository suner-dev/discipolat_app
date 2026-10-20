-- V167__missing_event_ai_location_tables.sql
-- ============================================================
-- Cree les tables attendues par les entites JPA du moteur evenementiel
-- Church OS (G3.3/G3.4), du module IA (AiUsage) et du transport d
-- evenements (ProcessedEvent). Ces tables n existaient pas encore dans
-- les migrations : le schema valide par Hibernate (ddl-auto: validate)
-- les exige au demarrage.
-- ============================================================

-- 1. AI USAGE
CREATE TABLE IF NOT EXISTS ai_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    request_type VARCHAR(50) NOT NULL,
    credits_consumed INTEGER NOT NULL DEFAULT 0,
    model_used VARCHAR(50),
    tokens_input INTEGER,
    tokens_output INTEGER,
    response_time_ms INTEGER,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    usage_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ai_usage_tenant_date ON ai_usage(tenant_id, usage_date);
CREATE INDEX IF NOT EXISTS idx_ai_usage_user ON ai_usage(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_type ON ai_usage(request_type);

-- 2. LOCATION
CREATE TABLE IF NOT EXISTS location (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address TEXT,
    capacity INTEGER,
    type VARCHAR(30),
    parent_location_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_location_tenant ON location(tenant_id);
CREATE INDEX IF NOT EXISTS idx_location_parent ON location(parent_location_id);

-- 3. EVENT TEAM
CREATE TABLE IF NOT EXISTS event_team (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    church_event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    space_id UUID,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    lead_person_id UUID,
    color VARCHAR(7),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_et_church_event ON event_team(church_event_id);
CREATE INDEX IF NOT EXISTS idx_et_space ON event_team(space_id);

-- 4. EVENT SCHEDULE
CREATE TABLE IF NOT EXISTS event_schedule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    church_event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    event_team_id UUID REFERENCES event_team(id) ON DELETE SET NULL,
    location_id UUID REFERENCES location(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ,
    order_index INTEGER NOT NULL DEFAULT 0,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_esched_church_event ON event_schedule(church_event_id);
CREATE INDEX IF NOT EXISTS idx_esched_time ON event_schedule(start_at);

-- 5. EVENT ATTENDANCE
CREATE TABLE IF NOT EXISTS event_attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    church_event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    person_id UUID NOT NULL,
    space_id UUID,
    status VARCHAR(30) NOT NULL DEFAULT 'PRESENT',
    check_in_at TIMESTAMPTZ,
    check_out_at TIMESTAMPTZ,
    check_in_method VARCHAR(30),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_attendance_church_event_person UNIQUE (church_event_id, person_id)
);
CREATE INDEX IF NOT EXISTS idx_evatt_church_event ON event_attendance(church_event_id);
CREATE INDEX IF NOT EXISTS idx_evatt_person ON event_attendance(person_id);
CREATE INDEX IF NOT EXISTS idx_evatt_status ON event_attendance(status);

-- 6. EVENT TASK
CREATE TABLE IF NOT EXISTS event_task (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    church_event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    space_id UUID,
    event_team_id UUID REFERENCES event_team(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'TODO',
    priority INTEGER NOT NULL DEFAULT 0,
    assignee_id UUID,
    due_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_etk_church_event ON event_task(church_event_id);
CREATE INDEX IF NOT EXISTS idx_etk_assignee ON event_task(assignee_id);
CREATE INDEX IF NOT EXISTS idx_etk_status ON event_task(status);
CREATE INDEX IF NOT EXISTS idx_etk_deleted ON event_task(deleted_at);

-- 7. EVENT SPACE
CREATE TABLE IF NOT EXISTS event_space (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    church_event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    space_id UUID NOT NULL,
    role VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_church_event_space UNIQUE (church_event_id, space_id)
);
CREATE INDEX IF NOT EXISTS idx_es_church_event ON event_space(church_event_id);
CREATE INDEX IF NOT EXISTS idx_es_space ON event_space(space_id);

-- 8. EVENT ASSET
CREATE TABLE IF NOT EXISTS event_asset (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    church_event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    asset_id UUID NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    condition_before VARCHAR(30),
    condition_after VARCHAR(30),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_evasset_church_event ON event_asset(church_event_id);
CREATE INDEX IF NOT EXISTS idx_evasset_asset ON event_asset(asset_id);

-- 9. EVENT EXPENSE
CREATE TABLE IF NOT EXISTS event_expense (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    church_event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    expense_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ee_church_event ON event_expense(church_event_id);
CREATE INDEX IF NOT EXISTS idx_ee_expense ON event_expense(expense_id);

-- 10. EVENT DOCUMENT
CREATE TABLE IF NOT EXISTS event_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    church_event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    document_id UUID NOT NULL,
    type VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ed_church_event ON event_document(church_event_id);

-- 11. PROCESSED EVENT (dedupe des consommateurs evenementiels)
CREATE TABLE IF NOT EXISTS processed_event (
    id BIGSERIAL PRIMARY KEY,
    consumer VARCHAR(100) NOT NULL,
    event_id BIGINT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_processed_consumer_event UNIQUE (consumer, event_id)
);
CREATE INDEX IF NOT EXISTS idx_processed_event_event_id ON processed_event(event_id);
CREATE INDEX IF NOT EXISTS idx_processed_consumer ON processed_event(consumer);