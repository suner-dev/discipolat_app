-- ExecutiveInsight — insights exécutifs générés par IA pour le dashboard pastoral.
-- Table créée par migration pour coller à ddl-auto=validate.

CREATE TABLE IF NOT EXISTS executive_insights (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    severity            VARCHAR(20) DEFAULT 'INFO', -- INFO, WARNING, CRITICAL, OPPORTUNITY
    category            VARCHAR(20), -- GROWTH, RETENTION, ENGAGEMENT, FINANCE, OPERATIONS, SPIRITUAL
    recommended_action  TEXT,
    metric_value        VARCHAR(100),
    metric_change       VARCHAR(100),
    is_read             BOOLEAN DEFAULT FALSE,
    is_dismissed        BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_executive_insights_tenant
    ON executive_insights (tenant_id);
CREATE INDEX IF NOT EXISTS idx_executive_insights_active
    ON executive_insights (tenant_id, is_dismissed, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_executive_insights_severity
    ON executive_insights (tenant_id, severity);
