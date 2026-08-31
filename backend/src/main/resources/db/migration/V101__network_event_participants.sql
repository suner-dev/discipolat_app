-- V101: Discipolat Network — traçabilité RSVP par utilisateur
-- Corrige le compteur d'événements : un utilisateur ne peut s'inscrire qu'une
-- seule fois (idempotence) et l'annulation reflète un état réel.
CREATE TABLE IF NOT EXISTS network_event_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES network_events(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_network_event_participant UNIQUE (event_id, user_id)
);

CREATE INDEX idx_network_event_participants_user ON network_event_participants(user_id);
CREATE INDEX idx_network_event_participants_event ON network_event_participants(event_id);
