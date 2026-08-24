-- V97__create_whatsapp_reminders.sql
-- P0 #1 — Pont WhatsApp : rappels automatiques

CREATE TABLE IF NOT EXISTS whatsapp_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    reference_type VARCHAR(32) NOT NULL,
    reference_id UUID NOT NULL,
    phone_number VARCHAR(32) NOT NULL,
    message TEXT NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_whatsapp_reminders_tenant_status ON whatsapp_reminders(tenant_id, status, scheduled_at);
