-- LiveStream - diffusion en direct (cultes, evenements, conferences).
-- Table creee par migration pour coller a ddl-auto=validate.

CREATE TABLE IF NOT EXISTS live_streams (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    stream_url      VARCHAR(1000),
    thumbnail_url   VARCHAR(1000),
    status          VARCHAR(20) DEFAULT 'SCHEDULED', -- SCHEDULED, LIVE, ENDED, CANCELLED
    tenant_id       BIGINT NOT NULL,
    created_by      BIGINT NOT NULL,
    scheduled_at    TIMESTAMP,
    started_at      TIMESTAMP,
    ended_at        TIMESTAMP,
    viewer_count    INTEGER DEFAULT 0,
    recording_url   VARCHAR(1000),
    created_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_live_streams_tenant
    ON live_streams (tenant_id);
CREATE INDEX IF NOT EXISTS idx_live_streams_status
    ON live_streams (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_live_streams_scheduled
    ON live_streams (tenant_id, scheduled_at DESC);

-- StreamChatMessage - chat en direct pendant les streams.
CREATE TABLE IF NOT EXISTS stream_chat_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stream_id       BIGINT NOT NULL,
    tenant_id       UUID NOT NULL,
    sender_id       UUID NOT NULL,
    sender_name     VARCHAR(255) NOT NULL,
    content         TEXT NOT NULL,
    message_type    VARCHAR(20) DEFAULT 'TEXT', -- TEXT, REACTION
    emoji           VARCHAR(50),
    created_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_stream_chat_tenant
    ON stream_chat_messages (tenant_id);
CREATE INDEX IF NOT EXISTS idx_stream_chat_stream
    ON stream_chat_messages (stream_id, created_at ASC);
