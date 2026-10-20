-- ============================================================
-- PORT Develop1 -> main (lot « restauration des capacités absentes »).
-- Source: Develop1 V164__legacy_migration_engine.sql, recopiee a l identique et renumerotee
-- (main occupe deja ces numeros avec d autres contenus).
-- ============================================================
-- G4.6 : moteur de migration legacy (dry-run / migrate / rollback idempotent)
-- tables migration_job, migration_audit, migration_snapshot. DISTINCT de
-- data_migration_jobs (V114/V164 de main), moteur canonique de reprise.

-- ============================================================
-- G4.6 — Legacy data migration engine (dry-run, replay idempotent, rollback)
-- Piloté par le toggle tenant `legacy_migration_enabled` (§G1.2)
-- Table canonique d'audit : migration_audit (une ligne par source migrée)
-- Job de session : migration_job (dry-run | migrate | rollback)
-- Snapshot rollback : migration_snapshot (payload cible avant rollback)
-- ============================================================

CREATE TABLE IF NOT EXISTS migration_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    module_code VARCHAR(50) NOT NULL,
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('DRY_RUN', 'MIGRATE', 'ROLLBACK')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    rows_seen INTEGER NOT NULL DEFAULT 0,
    rows_migrated INTEGER NOT NULL DEFAULT 0,
    rows_merged INTEGER NOT NULL DEFAULT 0,
    rows_skipped INTEGER NOT NULL DEFAULT 0,
    rows_conflicts INTEGER NOT NULL DEFAULT 0,
    report_json JSONB,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_migration_job_tenant ON migration_job(tenant_id, module_code, created_at);

COMMENT ON TABLE migration_job IS 'G4.6 : job de migration legacy (dry-run/migrate/rollback) par tenant et module';

CREATE TABLE IF NOT EXISTS migration_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    job_id UUID REFERENCES migration_job(id) ON DELETE SET NULL,
    module_code VARCHAR(50) NOT NULL,
    source_table VARCHAR(100) NOT NULL,
    source_id VARCHAR(100) NOT NULL,
    target_table VARCHAR(100),
    target_id VARCHAR(100),
    status VARCHAR(20) NOT NULL CHECK (status IN ('MIGRATED', 'MERGED', 'SKIPPED', 'CONFLICT')),
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Replay idempotent : une seule ligne d'audit (MIGRATED/MERGED) par (tenant, source_table, source_id)
CREATE UNIQUE INDEX IF NOT EXISTS uk_migration_audit_source
    ON migration_audit(tenant_id, source_table, source_id)
    WHERE status IN ('MIGRATED', 'MERGED');
CREATE INDEX IF NOT EXISTS idx_migration_audit_job ON migration_audit(job_id);
CREATE INDEX IF NOT EXISTS idx_migration_audit_tenant ON migration_audit(tenant_id, module_code);

COMMENT ON TABLE migration_audit IS 'G4.6 : traçabilité ligne par ligne de la migration legacy (source -> cible), jamais destructrice';

CREATE TABLE IF NOT EXISTS migration_snapshot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    job_id UUID NOT NULL REFERENCES migration_job(id) ON DELETE CASCADE,
    audit_id UUID REFERENCES migration_audit(id) ON DELETE CASCADE,
    target_table VARCHAR(100) NOT NULL,
    target_id VARCHAR(100) NOT NULL,
    payload_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_migration_snapshot_job ON migration_snapshot(job_id);
CREATE INDEX IF NOT EXISTS idx_migration_snapshot_tenant ON migration_snapshot(tenant_id);

COMMENT ON TABLE migration_snapshot IS 'G4.6 : snapshot des lignes CRÉÉES par un job, pour rollback ≤ 30 jours';
