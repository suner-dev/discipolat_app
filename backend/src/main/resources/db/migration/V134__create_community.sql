-- Community - communautes en ligne (groupes de discussion, entraide, prieres).
-- Table creee par migration pour coller a ddl-auto=validate.

CREATE TABLE IF NOT EXISTS communities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(100), -- ENTRAIDE, PRIERE, ETUDE_BIBLIQUE, JEUNESSE, FEMMES, HOMMES
    visibility      VARCHAR(20) DEFAULT 'PUBLIC', -- PUBLIC, PRIVATE, TENANT_ONLY
    member_count    INTEGER DEFAULT 0,
    post_count      INTEGER DEFAULT 0,
    created_by      UUID NOT NULL,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_communities_tenant
    ON communities (tenant_id);
CREATE INDEX IF NOT EXISTS idx_communities_category
    ON communities (tenant_id, category);
CREATE INDEX IF NOT EXISTS idx_communities_active
    ON communities (tenant_id, is_active, created_at DESC);

-- CommunityPost - publications dans une communaute.
CREATE TABLE IF NOT EXISTS community_posts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    community_id    UUID NOT NULL,
    author_id       UUID NOT NULL,
    title           VARCHAR(500),
    content         TEXT NOT NULL,
    post_type       VARCHAR(20) DEFAULT 'TEXT', -- TEXT, PRAYER_REQUEST, TESTIMONY, ANNOUNCEMENT
    like_count      INTEGER DEFAULT 0,
    comment_count   INTEGER DEFAULT 0,
    is_pinned       BOOLEAN DEFAULT FALSE,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_community_posts_tenant
    ON community_posts (tenant_id);
CREATE INDEX IF NOT EXISTS idx_community_posts_community
    ON community_posts (tenant_id, community_id);
CREATE INDEX IF NOT EXISTS idx_community_posts_created
    ON community_posts (tenant_id, community_id, created_at DESC);
