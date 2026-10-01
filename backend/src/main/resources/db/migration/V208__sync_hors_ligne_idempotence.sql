-- ============================================================
-- PORT Develop1 -> main (lot « restauration des capacités absentes »).
-- Source: Develop1 V167__sync_offline_idempotency.sql, recopiee a l identique et renumerotee
-- (main occupe deja ces numeros avec d autres contenus).
-- ============================================================
-- G5.7 : mobile hors-ligne — idempotence du batch de sync + conflits LWW
-- (operations/sync_operation, sync_conflict). Rejeu apres reseau instable : aucun doublon.

-- G5.7 : mobile offline ciblé — idempotence du batch de sync + conflits LWW
-- (répertoire offline du mobile, rejeu après réseau instable : aucun doublon)

CREATE TABLE sync_operation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    actor_user_id UUID,
    client_uuid VARCHAR(64) NOT NULL,
    op_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    applied_at TIMESTAMPTZ,
    CONSTRAINT uq_sync_operation_client UNIQUE (tenant_id, client_uuid)
);

CREATE INDEX idx_sync_operation_tenant ON sync_operation (tenant_id, created_at DESC);

-- Conflits détectés à l'application (LWW appliquée, jamais de perte silencieuse) :
-- le responsable réconcilie manuellement via la liste /sync/conflicts.
CREATE TABLE sync_conflict (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    operation_id UUID REFERENCES sync_operation (id) ON DELETE SET NULL,
    client_uuid VARCHAR(64),
    entity_type VARCHAR(60) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    field_name VARCHAR(80),
    client_value JSONB,
    server_value JSONB,
    client_op_at TIMESTAMPTZ,
    server_updated_at TIMESTAMPTZ,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by UUID,
    resolved_at TIMESTAMPTZ,
    resolution_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_sync_conflict_tenant ON sync_conflict (tenant_id, resolved, created_at DESC);
