-- Webhook delivery logs: enregistre tous les callbacks recus des operateurs
CREATE TABLE IF NOT EXISTS webhook_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    provider        VARCHAR(30) NOT NULL,          -- M_PESA, MTN_MOMO, ORANGE_MONEY, GENERIC
    endpoint        VARCHAR(100) NOT NULL,         -- /webhooks/mpesa, /webhooks/orange, etc.
    source_ip       VARCHAR(45),                   -- IPv4 ou IPv6
    status_code     INT,                           -- HTTP status retourne (200, 403, 500…)
    status_label    VARCHAR(20) NOT NULL DEFAULT 'RECEIVED', -- RECEIVED, VERIFIED, REJECTED, PROCESSED, ERROR
    reference       VARCHAR(100),                  -- providerReference ou pay_token
    payment_id      UUID,                          -- lien vers payment_intents.id
    signature_valid BOOLEAN,                       -- true si HMAC verification passed
    request_headers JSONB,                         -- headers pertinents (sanitises)
    request_body    TEXT,                          -- body brut (peut etre tronque en prod)
    response_body   TEXT,                          -- reponse envoyee
    error_message   TEXT,                          -- message d'erreur si echec
    duration_ms     INT,                           -- temps de traitement en ms
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhook_logs_tenant_id ON webhook_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_provider ON webhook_logs(provider);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_created ON webhook_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_reference ON webhook_logs(reference);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_status ON webhook_logs(status_label);
