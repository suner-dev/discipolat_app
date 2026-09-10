-- GroupMessage — messages de groupe (départements, familles, équipes).
-- Table créée par migration pour coller à ddl-auto=validate.

CREATE TABLE IF NOT EXISTS group_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    group_id        UUID NOT NULL,
    group_type      VARCHAR(20) NOT NULL, -- DEPARTMENT, FAMILY, TEAM
    sender_id       UUID NOT NULL,
    content         TEXT,
    message_type    VARCHAR(20) DEFAULT 'TEXT', -- TEXT, IMAGE, FILE, VOICE, REACTION
    file_name       VARCHAR(500),
    reply_to_id     VARCHAR(500),
    reaction_count  INTEGER DEFAULT 0,
    is_deleted      BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_group_messages_tenant
    ON group_messages (tenant_id);
CREATE INDEX IF NOT EXISTS idx_group_messages_group
    ON group_messages (tenant_id, group_id);
CREATE INDEX IF NOT EXISTS idx_group_messages_created
    ON group_messages (tenant_id, group_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_group_messages_search
    ON group_messages (tenant_id, group_id, content);
