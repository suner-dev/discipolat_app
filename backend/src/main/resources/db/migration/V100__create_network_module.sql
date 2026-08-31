-- V100: Discipolat Network — module réseau inter-églises
-- Tables: network_resources, network_events, network_directory
-- Règle: aucune donnée privée exposée sans autorisation explicite

-- ======================== RESSOURCES PARTAGÉES ========================
CREATE TABLE IF NOT EXISTS network_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL DEFAULT 'BEST_PRACTICE',
    resource_type VARCHAR(50) NOT NULL DEFAULT 'GUIDE',
    file_url VARCHAR(1000),
    content TEXT,
    tags TEXT[],
    shared_with_public BOOLEAN NOT NULL DEFAULT FALSE,
    shared_by_user_id UUID,
    downloads INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_network_resources_tenant ON network_resources(tenant_id);
CREATE INDEX idx_network_resources_category ON network_resources(category);
CREATE INDEX idx_network_resources_active ON network_resources(is_active) WHERE is_active = TRUE;

-- ======================== ÉVÉNEMENTS INTER-ÉGLISES ========================
CREATE TABLE IF NOT EXISTS network_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    event_type VARCHAR(50) NOT NULL DEFAULT 'CONFERENCE',
    location VARCHAR(500),
    city VARCHAR(200),
    country VARCHAR(100),
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP,
    max_participants INT,
    current_participants INT NOT NULL DEFAULT 0,
    is_virtual BOOLEAN NOT NULL DEFAULT FALSE,
    virtual_link VARCHAR(1000),
    shared_with_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_network_events_tenant ON network_events(tenant_id);
CREATE INDEX idx_network_events_starts ON network_events(starts_at);
CREATE INDEX idx_network_events_active ON network_events(is_active) WHERE is_active = TRUE;

-- ======================== RÉPÉRTOIRE VOLONTAIRE DES ÉGLISES ========================
CREATE TABLE IF NOT EXISTS network_directory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE,
    church_name VARCHAR(300),
    city VARCHAR(200),
    country VARCHAR(100),
    denomination VARCHAR(200),
    pastor_name VARCHAR(300),
    contact_email VARCHAR(300),
    contact_phone VARCHAR(50),
    website VARCHAR(500),
    member_count INT,
    description TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    is_listed BOOLEAN NOT NULL DEFAULT FALSE,
    listed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_network_directory_listed ON network_directory(is_listed) WHERE is_listed = TRUE;
CREATE INDEX idx_network_directory_country ON network_directory(country);
