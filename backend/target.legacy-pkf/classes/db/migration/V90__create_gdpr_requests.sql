CREATE TABLE gdpr_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    requester_user_id UUID NOT NULL,
    request_type    VARCHAR(50) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    requested_at    TIMESTAMP NOT NULL DEFAULT now(),
    processed_at    TIMESTAMP,
    processed_by    UUID,
    notes           TEXT,
    export_data     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_gdpr_requests_tenant_id ON gdpr_requests (tenant_id);
CREATE INDEX idx_gdpr_requests_requester_user_id ON gdpr_requests (requester_user_id);
CREATE INDEX idx_gdpr_requests_status ON gdpr_requests (tenant_id, status);
