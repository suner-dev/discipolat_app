-- V102 : demandes de démonstration (landing page publique).
-- Leads collectés sans authentification depuis la landing : niveau plateforme,
-- donc PAS de tenant_id (le TenantFilter ignore les chemins /api/v1/public).
CREATE TABLE IF NOT EXISTS demo_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    church_name VARCHAR(255) NOT NULL,
    role VARCHAR(100),
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'NOUVEAU',
    source VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_demo_requests_created_at ON demo_requests(created_at DESC);
CREATE INDEX idx_demo_requests_email ON demo_requests(email);
CREATE INDEX idx_demo_requests_status ON demo_requests(status);