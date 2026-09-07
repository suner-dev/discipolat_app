-- USSD Sessions — persistence des sessions USSD Africa's Talking
-- Chaque interaction USSD entrante crée une session pour la continuité multi-écrans

CREATE TABLE IF NOT EXISTS ussd_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      VARCHAR(100) NOT NULL,
    phone_number    VARCHAR(20) NOT NULL,
    tenant_id       VARCHAR(36) NOT NULL,
    current_menu    VARCHAR(50) DEFAULT 'main',
    context_data    VARCHAR(500),
    step            INTEGER DEFAULT 0,
    ended           BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    last_activity   TIMESTAMP
);

-- Index pour recherche par session_id (unique tant que non terminée)
CREATE INDEX IF NOT EXISTS idx_ussd_session_id ON ussd_sessions (session_id);
CREATE INDEX IF NOT EXISTS idx_ussd_phone ON ussd_sessions (phone_number);
CREATE INDEX IF NOT EXISTS idx_ussd_tenant ON ussd_sessions (tenant_id);
CREATE INDEX IF NOT EXISTS idx_ussd_ended ON ussd_sessions (ended);
CREATE INDEX IF NOT EXISTS idx_ussd_last_activity ON ussd_sessions (last_activity);
