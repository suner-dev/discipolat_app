CREATE TABLE tenant_registration_requests (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    organization_name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    plan VARCHAR(30) NOT NULL,
    country VARCHAR(2),
    currency VARCHAR(10),
    timezone VARCHAR(64),
    locale VARCHAR(10),
    status VARCHAR(30) NOT NULL,
    reviewer_id UUID,
    decision_reason VARCHAR(500),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_tenant_registration_status_created
    ON tenant_registration_requests(status, created_at DESC);
