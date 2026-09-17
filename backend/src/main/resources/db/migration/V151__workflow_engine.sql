-- V151__workflow_engine.sql
-- ============================================================
-- G2.5 — Workflow Engine configurable (contrat Annexe A §A.3)
-- workflow_definition / workflow_step / workflow_transition /
-- workflow_instance / workflow_task
-- ============================================================

CREATE TABLE IF NOT EXISTS workflow_definition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity_type VARCHAR(60) NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(200) NOT NULL,
    space_id UUID REFERENCES spaces(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_wf_def_tenant ON workflow_definition(tenant_id);
CREATE INDEX IF NOT EXISTS idx_wf_def_tenant_code ON workflow_definition(tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_wf_def_space ON workflow_definition(space_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_wf_def_scope
    ON workflow_definition (tenant_id, COALESCE(space_id, '00000000-0000-0000-0000-000000000000'::uuid), code, version)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS workflow_step (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflow_definition(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    step_type VARCHAR(30) NOT NULL CHECK (step_type IN
        ('APPROVAL', 'AUTO_ACTION', 'NOTIFY', 'EXPENSE', 'ASSET_STATUS', 'FORM')),
    name VARCHAR(200) NOT NULL,
    conditions_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    assignee_role VARCHAR(80),
    assignee_scope VARCHAR(40),
    timeout_hours INTEGER,
    escalation_role VARCHAR(80),
    auto_action_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_wf_step_workflow ON workflow_step(workflow_id);
CREATE INDEX IF NOT EXISTS idx_wf_step_order ON workflow_step(workflow_id, step_order);

CREATE TABLE IF NOT EXISTS workflow_transition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflow_definition(id) ON DELETE CASCADE,
    from_step_id UUID NOT NULL REFERENCES workflow_step(id) ON DELETE CASCADE,
    to_step_id UUID NOT NULL REFERENCES workflow_step(id) ON DELETE CASCADE,
    on_event VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_wf_transition UNIQUE (from_step_id, to_step_id, on_event)
);
CREATE INDEX IF NOT EXISTS idx_wf_trans_from ON workflow_transition(from_step_id);
CREATE INDEX IF NOT EXISTS idx_wf_trans_workflow ON workflow_transition(workflow_id);

CREATE TABLE IF NOT EXISTS workflow_instance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    workflow_id UUID NOT NULL REFERENCES workflow_definition(id) ON DELETE CASCADE,
    entity_id UUID,
    space_id UUID REFERENCES spaces(id) ON DELETE SET NULL,
    current_step_id UUID REFERENCES workflow_step(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RUNNING' CHECK (status IN
        ('RUNNING', 'APPROVED', 'REJECTED', 'ESCALATED', 'CANCELLED', 'COMPLETED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_wf_inst_tenant ON workflow_instance(tenant_id);
CREATE INDEX IF NOT EXISTS idx_wf_inst_workflow ON workflow_instance(workflow_id);
CREATE INDEX IF NOT EXISTS idx_wf_inst_entity ON workflow_instance(entity_id);
CREATE INDEX IF NOT EXISTS idx_wf_inst_status ON workflow_instance(status);

CREATE TABLE IF NOT EXISTS workflow_task (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    instance_id UUID NOT NULL REFERENCES workflow_instance(id) ON DELETE CASCADE,
    step_id UUID NOT NULL REFERENCES workflow_step(id) ON DELETE CASCADE,
    assignee_id UUID,
    assignee_role VARCHAR(80),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN
        ('PENDING', 'APPROVED', 'REJECTED', 'ESCALATED', 'CANCELLED')),
    due_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    comment TEXT,
    escalated_from_task_id UUID REFERENCES workflow_task(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_wf_task_tenant ON workflow_task(tenant_id);
CREATE INDEX IF NOT EXISTS idx_wf_task_instance ON workflow_task(instance_id);
CREATE INDEX IF NOT EXISTS idx_wf_task_assignee ON workflow_task(assignee_id);
CREATE INDEX IF NOT EXISTS idx_wf_task_status_due ON workflow_task(status, due_at);

COMMENT ON TABLE workflow_definition IS 'G2.5 : processus modélisé par un espace, aucun workflow codé en dur';
COMMENT ON COLUMN workflow_definition.space_id IS 'Workflow propre à un espace ; NULL = workflow du tenant (héritage G1.7)';
COMMENT ON TABLE workflow_task IS 'G2.5 : tâche d''approbation assignée à un acteur, avec délai (due_at) et escalade';
COMMENT ON COLUMN workflow_task.escalated_from_task_id IS 'Traçabilité : tâche d''origine lors d''une escalade par timeout';