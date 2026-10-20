-- Dress Code Module (G3.4 — exigence utilisateur)
-- Each space can program dress codes by event/service and by group

CREATE TABLE dress_code (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    space_id UUID,
    event_id UUID,
    service_name VARCHAR(200),
    title VARCHAR(200) NOT NULL,
    begins_at TIMESTAMP WITH TIME ZONE,
    ends_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) DEFAULT 'DRAFT',
    created_by UUID,
    archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_dress_code_tenant ON dress_code(tenant_id);
CREATE INDEX idx_dress_code_space ON dress_code(space_id);
CREATE INDEX idx_dress_code_event ON dress_code(event_id);

CREATE TABLE dress_code_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dress_code_id UUID NOT NULL REFERENCES dress_code(id) ON DELETE CASCADE,
    group_name VARCHAR(100) NOT NULL,
    description TEXT,
    image_url VARCHAR(500)
);

CREATE INDEX idx_dress_code_rule_dress_code ON dress_code_rule(dress_code_id);

-- Comments
COMMENT ON TABLE dress_code IS 'Dress codes programmed for events/services per space';
COMMENT ON TABLE dress_code_rule IS 'Rules for specific groups within a dress code';
