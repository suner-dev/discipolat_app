-- V182 : Journal d'idempotence des événements webhook Stripe
-- Chaque event.id Stripe est tracé pour garantir un traitement exactly-once.

CREATE TABLE IF NOT EXISTS stripe_webhook_events (
    event_id     varchar(80)  PRIMARY KEY,
    type         varchar(80)  NOT NULL,
    received_at  timestamptz  NOT NULL DEFAULT now(),
    processed_at timestamptz,
    status       varchar(20)  NOT NULL DEFAULT 'RECEIVED',
    error        text
);

CREATE INDEX IF NOT EXISTS idx_stripe_webhook_events_status ON stripe_webhook_events (status, received_at);
