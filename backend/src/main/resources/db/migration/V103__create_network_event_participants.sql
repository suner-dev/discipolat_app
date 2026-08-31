-- V103: Table de participation aux événements inter-églises (RSVP)
-- Utilisée par NetworkService pour le suivi idempotent des inscriptions.

CREATE TABLE IF NOT EXISTS network_event_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES network_events(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
);

CREATE UNIQUE INDEX idx_network_event_participants_unique
    ON network_event_participants(event_id, user_id);

CREATE INDEX idx_network_event_participants_user
    ON network_event_participants(user_id);
