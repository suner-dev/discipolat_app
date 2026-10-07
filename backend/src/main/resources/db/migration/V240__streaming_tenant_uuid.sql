-- V240__streaming_tenant_uuid.sql
-- ============================================================
-- STREAMING : fin de la dérive multi-tenant de live_streams.
--
-- Contexte (documenté en V200/V202) : V114/V133 ont créé
-- live_streams.tenant_id et live_streams.created_by en BIGINT, à
-- une époque où l'identifiant de tenant était numérique. Le modèle
-- courant est UUID partout (tenants.id, users.id) : ces deux
-- colonnes ne pouvaient donc référencer aucune FK et le contrôleur
-- LiveStreamController exigeait un tenantId fourni par le client
-- (faille IDOR). Cet écart est corrigé ici, module par module,
-- comme pour tasks (V234).
--
-- id RESTE BIGSERIAL : c'est le contrat structurel du module
-- (stream_chat_messages.stream_id BIGINT, mobile/web consomment un
-- int). Seules les colonnes d'association tenant/changeur passent
-- en UUID avec FK explicites.
--
-- Données : les valeurs BIGINT héritées (1, 2, …) ne sont PAS
-- cartographiables vers tenants.id UUID. Rebuild documenté :
-- purge des deux tables du module avant conversion. Les lignes
-- legacy n'étaient de toute façon lisibles par aucun tenant UUID
-- cohérent (TenantContext est UUID depuis V200).
-- ============================================================

-- 1) Purge legacy (dans l'ordre : messages avant streams).
DELETE FROM stream_chat_messages;
DELETE FROM live_streams;

-- 2) live_streams.tenant_id BIGINT -> UUID NOT NULL, FK tenants.
ALTER TABLE live_streams
    ALTER COLUMN tenant_id TYPE UUID USING (NULL::uuid);
ALTER TABLE live_streams
    ADD CONSTRAINT fk_live_streams_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

-- 3) live_streams.created_by BIGINT -> UUID. La colonne devient
--    nullable côté schéma (SET NULL si le compte est supprimé) ;
--    le service force sa valeur à la création depuis le JWT.
ALTER TABLE live_streams
    ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE live_streams
    ALTER COLUMN created_by TYPE UUID USING (NULL::uuid);
ALTER TABLE live_streams
    ADD CONSTRAINT fk_live_streams_created_by
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- 4) stream_chat_messages : rétablir l'intégrité référentielle
--    (tables vidées en 1, donc validation triviale).
ALTER TABLE stream_chat_messages
    ADD CONSTRAINT fk_stream_chat_stream
    FOREIGN KEY (stream_id) REFERENCES live_streams(id) ON DELETE CASCADE;
ALTER TABLE stream_chat_messages
    ADD CONSTRAINT fk_stream_chat_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
-- sender_id reste sans FK : l'émetteur peut être un identifiant
-- éphémère (websocket) et les messages ne doivent pas disparaître
-- avec le compte.

-- 5) Index de parcours alignés sur les requêtes du service
--    (findByTenantIdOrderByScheduledAtDesc, findByTenantIdAndStatus).
CREATE INDEX IF NOT EXISTS idx_live_streams_status
    ON live_streams (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_live_streams_scheduled
    ON live_streams (tenant_id, scheduled_at DESC);

COMMENT ON COLUMN live_streams.tenant_id IS 'UUID FK tenants (V240, était BIGINT non cartographiable).';
COMMENT ON COLUMN live_streams.created_by IS 'UUID FK users, forcé serveur depuis le JWT (V240).';
COMMENT ON TABLE live_streams IS 'Streams en direct. id BIGSERIAL = contrat mobile/web conservé.';
