-- Asset Engine Enhancement (G3.5)
-- Adds checkout/return workflow, maintenance tracking, and TCO support

CREATE TABLE asset_checkout (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    item_id UUID NOT NULL,
    member_id UUID NOT NULL,
    space_id UUID,
    event_id UUID,
    checked_out_at TIMESTAMP NOT NULL DEFAULT NOW(),
    due_back_at TIMESTAMP,
    returned_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'CHECKED_OUT',
    condition_on_checkout VARCHAR(50),
    condition_on_return VARCHAR(50),
    checked_out_by UUID,
    returned_by UUID,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_checkout_item ON asset_checkout(item_id);
CREATE INDEX idx_checkout_tenant ON asset_checkout(tenant_id);
CREATE INDEX idx_checkout_member ON asset_checkout(member_id);
CREATE INDEX idx_checkout_status ON asset_checkout(tenant_id, status);

CREATE TABLE asset_maintenance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    item_id UUID NOT NULL,
    maintenance_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    title VARCHAR(200) NOT NULL,
    description TEXT,
    performed_by UUID,
    vendor_name VARCHAR(200),
    vendor_contact VARCHAR(200),
    cost DOUBLE PRECISION,
    currency VARCHAR(3) DEFAULT 'XAF',
    scheduled_for TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    next_maintenance_due TIMESTAMP,
    parts_replaced TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_maintenance_item ON asset_maintenance(item_id);
CREATE INDEX idx_maintenance_tenant ON asset_maintenance(tenant_id);
CREATE INDEX idx_maintenance_status ON asset_maintenance(tenant_id, status);
CREATE INDEX idx_maintenance_scheduled ON asset_maintenance(tenant_id, scheduled_for);

-- Add new columns to inventory_items for TCO tracking
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS total_maintenance_cost DOUBLE PRECISION DEFAULT 0;
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS total_checkout_count INTEGER DEFAULT 0;
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS purchase_price DOUBLE PRECISION;
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS expected_lifespan_months INTEGER;

COMMENT ON TABLE asset_checkout IS 'Tracks checkout and return history of inventory items';
COMMENT ON TABLE asset_maintenance IS 'Tracks maintenance history and scheduling for inventory items';
