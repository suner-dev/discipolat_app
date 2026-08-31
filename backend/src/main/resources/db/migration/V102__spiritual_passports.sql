-- V102: Passeport spirituel portable et vérifiable (PHASE 2 du prompt maître)
-- Tables: spiritual_passports, spiritual_passport_entries, passport_verifications
-- Sécurité: signature RSA (clé app), QR de vérification publique, traçabilité des vérifications.

CREATE TABLE IF NOT EXISTS spiritual_passports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    member_id UUID NOT NULL,
    passport_code VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR(500),
    payload_hash VARCHAR(64),
    signature TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_passport_tenant_member UNIQUE (tenant_id, member_id)
);

CREATE INDEX idx_passports_tenant ON spiritual_passports(tenant_id);
CREATE INDEX idx_passports_member ON spiritual_passports(member_id);
CREATE INDEX idx_passports_status ON spiritual_passports(status);

CREATE TABLE IF NOT EXISTS spiritual_passport_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    passport_id UUID NOT NULL REFERENCES spiritual_passports(id) ON DELETE CASCADE,
    entry_type VARCHAR(40) NOT NULL DEFAULT 'DISCIPLESHIP_STEP',
    title VARCHAR(300) NOT NULL,
    description TEXT,
    occurred_at DATE,
    issuing_organization VARCHAR(300),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_passport_entries_passport ON spiritual_passport_entries(passport_id);
CREATE INDEX idx_passport_entries_tenant ON spiritual_passport_entries(tenant_id);

-- Journal de traçabilité des vérifications publiques (QR) — non filtré par tenant :
-- la page de vérification est publique et doit tracer toute tentative.
CREATE TABLE IF NOT EXISTS passport_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    passport_code VARCHAR(40) NOT NULL,
    result VARCHAR(20) NOT NULL,
    remote_addr VARCHAR(60),
    user_agent VARCHAR(300),
    verified_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_passport_verifications_code ON passport_verifications(passport_code);
