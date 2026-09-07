-- AiChatConversation — persistance de la messagerie IA contextuelle (copilot).
-- Table ajoutée par migration : indispensable au démarrage en dev
-- (Hibernate ddl-auto=validate) et aux requêtes en production (ddl-auto=none).

CREATE TABLE IF NOT EXISTS ai_chat_conversations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    user_id     UUID NOT NULL,
    session_id  UUID NOT NULL,
    role        VARCHAR(20) NOT NULL,
    content     TEXT NOT NULL,
    sources_json TEXT,
    created_at  TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_conversations_tenant
    ON ai_chat_conversations (tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_chat_conversations_session
    ON ai_chat_conversations (tenant_id, session_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_chat_conversations_user
    ON ai_chat_conversations (tenant_id, user_id);
