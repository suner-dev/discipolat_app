-- V180 : Preuve de consentement RGPD — colonnes additionnelles (aucune suppression)
-- 1) consent_logs : version du document accepté + trace technique (art. 7 RGPD)
-- 2) tenant_registration_requests : consentement capturé à la souscription,
--    matérialisé dans consent_logs après approbation Super Admin.

ALTER TABLE consent_logs
    ADD COLUMN IF NOT EXISTS policy_version varchar(20),
    ADD COLUMN IF NOT EXISTS ip_address     varchar(64),
    ADD COLUMN IF NOT EXISTS user_agent     varchar(255);

CREATE INDEX IF NOT EXISTS idx_consent_logs_user ON consent_logs (utilisateur_id, type_consentement);

ALTER TABLE tenant_registration_requests
    ADD COLUMN IF NOT EXISTS consent_terms_version varchar(20),
    ADD COLUMN IF NOT EXISTS consent_cgu           boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS consent_privacy       boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS consent_art9          boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS consent_ip            varchar(64),
    ADD COLUMN IF NOT EXISTS consent_given_at      timestamptz;
