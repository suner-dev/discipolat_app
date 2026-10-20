-- ============================================================
-- SPEC_ORGANISATION_MODULABLE_V3 §D+§E — V228 : THÈME par nœud +
-- SNAPSHOT des agrégats recalculés.
--
-- D : thème complet héritable/override par nœud (subset de
--     TenantSettings). V3-D → null = indépendant (pas d'héritage forcé).
-- E : compteurs REMONTÉS du sous-arbre (fidèles, églises, leaders,
--     sermons, prières) + progression (série de snapshots). Lecture
--     seule côté client ; recalcul serveur (events + job).
--
-- D14 : migration MONTANTE. Additif : `theme_json` NULLABLE (aucun nœud
-- existant cassé). Cible PostgreSQL (parité validée par le gate PG).
-- ============================================================

-- 1) Override de thème local (jsonb, null = indépendant)
ALTER TABLE organization_nodes ADD COLUMN IF NOT EXISTS theme_json JSONB;

-- 2) Snapshot des agrégats par nœud (recalculé, jamais dérivé nominatif)
CREATE TABLE IF NOT EXISTS node_aggregate_snapshots (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    node_id            UUID NOT NULL REFERENCES organization_nodes(id) ON DELETE CASCADE,
    snapshot_at        TIMESTAMPTZ NOT NULL,
    member_count       BIGINT NOT NULL DEFAULT 0,   -- fidèles rattachés au sous-arbre
    church_count       BIGINT NOT NULL DEFAULT 0,   -- nœuds « église/campus » descendants
    leader_count       BIGINT NOT NULL DEFAULT 0,   -- porteurs d'un rôle de direction
    sermon_count       BIGINT NOT NULL DEFAULT 0,
    prayer_topic_count BIGINT NOT NULL DEFAULT 0,
    metrics_json       JSONB NOT NULL DEFAULT '{}'::jsonb,  -- extension (progression, attendance…)
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_node_agg UNIQUE (node_id, snapshot_at)
);
CREATE INDEX IF NOT EXISTS idx_node_agg_lookup ON node_aggregate_snapshots(node_id, snapshot_at DESC);
CREATE INDEX IF NOT EXISTS idx_node_agg_tenant ON node_aggregate_snapshots(tenant_id);
