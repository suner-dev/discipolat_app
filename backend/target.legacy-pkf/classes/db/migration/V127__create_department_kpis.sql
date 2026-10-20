-- DepartmentKpi — KPIs par departement (taux remplissage, realisation, satisfaction).
-- Table creee par migration pour coller a ddl-auto=validate.

CREATE TABLE IF NOT EXISTS department_kpis (
    id              BIGSERIAL PRIMARY KEY,
    department_id   BIGINT NOT NULL,
    name            VARCHAR(255) NOT NULL,
     description     TEXT,
    target_value    DOUBLE PRECISION,
    current_value   DOUBLE PRECISION,
    unit            VARCHAR(50),
    period          VARCHAR(20) DEFAULT 'MONTHLY',
    tenant_id       BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_department_kpis_tenant
    ON department_kpis (tenant_id);
CREATE INDEX IF NOT EXISTS idx_department_kpis_department
    ON department_kpis (tenant_id, department_id);
CREATE INDEX IF NOT EXISTS idx_department_kpis_created
    ON department_kpis (tenant_id, created_at DESC);
