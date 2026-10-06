-- V234__tasks_core.sql
-- ============================================================
-- TASKS (tâches, sous-tâches, commentaires, dépendances, kanban,
-- entrées de temps, templates, rapports)
-- Plan : docs/SPEC_BACKEND_SERVICES_MOBILES_V233.md
--
-- Le mobile (mobile/lib/features/tasks) appelle ces endpoints
-- depuis longtemps ; le backend n'existait pas. Cette migration crée
-- les tables additivement, sans toucher à l'existant.
--
-- Conventions alignées sur V222..V232 : TIMESTAMPTZ, now(), FK
-- explicites, CHECK sur les énumérations, ON DELETE explicite.
-- IDs : BIGSERIAL (le contrat mobile utilise des int, pas des UUID).
-- ============================================================

CREATE TABLE IF NOT EXISTS tasks (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title               VARCHAR(300) NOT NULL,
    description         TEXT,
    type                VARCHAR(20) NOT NULL DEFAULT 'TASK'
        CHECK (type IN ('TASK','SUBTASK','EPIC','STORY','BUG','FEATURE','CHORES','MEETING','CALL','REVIEW')),
    priority            VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT','CRITICAL')),
    status              VARCHAR(20) NOT NULL DEFAULT 'TODO'
        CHECK (status IN ('BACKLOG','TODO','IN_PROGRESS','IN_REVIEW','BLOCKED','DONE','CANCELLED')),
    project_id          BIGINT,
    assigned_to_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_by_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    department_id       BIGINT,
    due_date            TIMESTAMPTZ,
    start_date          TIMESTAMPTZ,
    completed_date      TIMESTAMPTZ,
    estimated_hours     INT,
    actual_hours        INT,
    tags                TEXT[],
    parent_task_id      BIGINT REFERENCES tasks(id) ON DELETE CASCADE,
    recurrence_rule_id  BIGINT,
    recurrence_pattern  VARCHAR(50),
    recurrence_end_date TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS task_attachments (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    task_id         BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    file_name       VARCHAR(255) NOT NULL,
    file_url        TEXT NOT NULL,
    mime_type       VARCHAR(100) NOT NULL,
    file_size       INT NOT NULL DEFAULT 0,
    uploaded_by_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS task_comments (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    task_id         BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    author_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content         TEXT NOT NULL,
    parent_comment_id BIGINT REFERENCES task_comments(id) ON DELETE CASCADE,
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS task_dependencies (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    task_id             BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    depends_on_task_id  BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    type                VARCHAR(20) NOT NULL DEFAULT 'BLOCKS'
        CHECK (type IN ('BLOCKS','IS_BLOCKED_BY','RELATES_TO','DUPLICATES','IS_DUPLICATED_BY'))
);

CREATE TABLE IF NOT EXISTS task_templates (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    type            VARCHAR(20) NOT NULL DEFAULT 'TASK'
        CHECK (type IN ('TASK','SUBTASK','EPIC','STORY','BUG','FEATURE','CHORES','MEETING','CALL','REVIEW')),
    priority        VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT','CRITICAL')),
    estimated_hours INT,
    default_tags    TEXT[],
    department_id   BIGINT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS task_template_subtasks (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    template_id     BIGINT NOT NULL REFERENCES task_templates(id) ON DELETE CASCADE,
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    priority        VARCHAR(20)
        CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT','CRITICAL')),
    estimated_hours INT,
    "order"         INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS kanban_columns (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    status          VARCHAR(20) NOT NULL
        CHECK (status IN ('BACKLOG','TODO','IN_PROGRESS','IN_REVIEW','BLOCKED','DONE','CANCELLED')),
    "order"         INT NOT NULL DEFAULT 0,
    wip_limit       INT,
    color           VARCHAR(20),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS task_time_entries (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    task_id         BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_time      TIMESTAMPTZ NOT NULL DEFAULT now(),
    end_time        TIMESTAMPTZ,
    duration_minutes INT,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tasks_tenant ON tasks (tenant_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned ON tasks (tenant_id, assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_tasks_parent ON tasks (tenant_id, parent_task_id);
CREATE INDEX IF NOT EXISTS idx_task_attach_task ON task_attachments (tenant_id, task_id);
CREATE INDEX IF NOT EXISTS idx_task_comments_task ON task_comments (tenant_id, task_id);
CREATE INDEX IF NOT EXISTS idx_task_deps_task ON task_dependencies (tenant_id, task_id);
CREATE INDEX IF NOT EXISTS idx_kanban_cols_tenant ON kanban_columns (tenant_id, "order");
CREATE INDEX IF NOT EXISTS idx_time_entries_task ON task_time_entries (tenant_id, task_id);

COMMENT ON TABLE tasks IS 'Tâches (V234).';
COMMENT ON TABLE task_attachments IS 'Pièces jointes (V234).';
COMMENT ON TABLE task_comments IS 'Commentaires (V234).';
COMMENT ON TABLE task_dependencies IS 'Dépendances entre tâches (V234).';
COMMENT ON TABLE task_templates IS 'Templates de tâches (V234).';
COMMENT ON TABLE kanban_columns IS 'Colonnes kanban (V234).';
COMMENT ON TABLE task_time_entries IS 'Entrées de temps (V234).';
