ALTER TABLE invitations
    ADD COLUMN IF NOT EXISTS organization_node_id UUID REFERENCES organization_nodes(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_invitation_organization_node
    ON invitations(organization_node_id);
