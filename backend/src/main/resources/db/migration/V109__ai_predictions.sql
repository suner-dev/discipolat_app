-- AiPrediction — moteur de predictions IA (Predictive Care).
-- Table creee par migration pour coller a ddl-auto=validate :
-- elle etait auparavant absente des migrations, ce qui cassait le
-- demarrage en dev (Hibernate validate) et toute requete en prod.

CREATE TABLE IF NOT EXISTS ai_predictions (
    id              BIGSERIAL PRIMARY KEY,
    prediction_type VARCHAR(50)  NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       BIGINT,
    metric_name     VARCHAR(100) NOT NULL,
    predicted_value DOUBLE PRECISION,
    confidence_score DOUBLE PRECISION,
    current_value   DOUBLE PRECISION,
    explanation     TEXT,
    risk_level      VARCHAR(20),
    tenant_id       UUID NOT NULL,
    generated_by    VARCHAR(50) DEFAULT 'AI_ENGINE_V1',
    created_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_predictions_tenant_type
    ON ai_predictions (tenant_id, prediction_type);
CREATE INDEX IF NOT EXISTS idx_ai_predictions_tenant_created
    ON ai_predictions (tenant_id, created_at DESC);