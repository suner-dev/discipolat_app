-- MarketplaceListing — marketplace de biens/services/templates entre églises.
-- Table créée par migration pour coller à ddl-auto=validate.

CREATE TABLE IF NOT EXISTS marketplace_listings (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    listing_type    VARCHAR(20) DEFAULT 'OFFER', -- OFFER, REQUEST, SERVICE, FREE
    category        VARCHAR(100),
    price_cents     BIGINT,
    image_url       VARCHAR(1000),
    seller_id       BIGINT NOT NULL,
    tenant_id       BIGINT NOT NULL,
    is_active       BOOLEAN DEFAULT TRUE,
    contact_info    VARCHAR(500),
    created_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_marketplace_tenant
    ON marketplace_listings (tenant_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_category
    ON marketplace_listings (tenant_id, category);
CREATE INDEX IF NOT EXISTS idx_marketplace_active
    ON marketplace_listings (tenant_id, is_active, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_marketplace_type
    ON marketplace_listings (tenant_id, listing_type);
