-- ============================================================
-- PORT Develop1 -> main (lot « restauration des capacités absentes »).
-- Source: Develop1 V175__lowband_interaction.sql, recopiee a l identique et renumerotee
-- (main occupe deja ces numeros avec d autres contenus).
-- ============================================================
-- G5.9 : portail basse connexion — journal d interactions WhatsApp/USSD
-- table lowband_interaction (canal, numero, action, detail).

-- V175 : §G5.9 — Portail basse connexion : journal d'interactions WhatsApp/USSD.
-- Traçabilité complète (canal, numéro, action, détail) sans données sensibles :
-- uniquement le strict nécessaire membre (tenue, planning, présence, don, prière).
CREATE TABLE IF NOT EXISTS lowband_interaction (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    channel VARCHAR(16) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL,
    detail TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_lowband_tenant_created ON lowband_interaction(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_lowband_phone ON lowband_interaction(tenant_id, phone);
