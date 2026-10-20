-- V5__add_admin_role_and_account_activation.sql
-- US-02: Add ADMIN role, account activation tokens, and other enhancements

-- Update users CHECK constraint to include ADMIN
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR'));

-- ============================================================
-- ACCOUNT ACTIVATION TOKENS - US-02
-- ============================================================
CREATE TABLE IF NOT EXISTS activation_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_activation_tokens_token ON activation_tokens(token);
CREATE INDEX IF NOT EXISTS idx_activation_tokens_user ON activation_tokens(user_id);

-- ============================================================
-- PASSWORD RESET TOKENS - US-03 (persistent storage)
-- ============================================================
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user ON password_reset_tokens(user_id);

-- ============================================================
-- 2FA SETTINGS - US-04
-- ============================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_secret VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_backup_codes TEXT;

-- ============================================================
-- FAMILY HISTORY - US-07 (track chief changes)
-- ============================================================
CREATE TABLE IF NOT EXISTS family_chief_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    famille_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    ancien_chef_id UUID REFERENCES users(id),
    nouveau_chef_id UUID NOT NULL REFERENCES users(id),
    changed_by UUID REFERENCES users(id),
    raison VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_family_chief_history_famille ON family_chief_history(famille_id);

-- ============================================================
-- FAISEUR TRANSFERS - US-13
-- ============================================================
CREATE TABLE IF NOT EXISTS faiseur_transfers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    faiseur_id UUID NOT NULL REFERENCES users(id),
    ancienne_famille_id UUID REFERENCES families(id),
    nouvelle_famille_id UUID NOT NULL REFERENCES families(id),
    transferer_ames BOOLEAN NOT NULL DEFAULT FALSE,
    transferred_by UUID REFERENCES users(id),
    raison TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_faiseur_transfers_faiseur ON faiseur_transfers(faiseur_id);

-- ============================================================
-- SOUL EXIT TRACKING - US-22 (proper exit tracking)
-- ============================================================
CREATE TABLE IF NOT EXISTS soul_exits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ame_id UUID NOT NULL REFERENCES souls(id),
    faiseur_id UUID NOT NULL REFERENCES users(id),
    motif VARCHAR(50) NOT NULL,
    motif_detail TEXT,
    peut_reintegrer BOOLEAN NOT NULL DEFAULT TRUE,
    date_sortie DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_soul_exits_ame ON soul_exits(ame_id);

-- ============================================================
-- REPORT CORRECTIONS - US-34
-- ============================================================
CREATE TABLE IF NOT EXISTS report_corrections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id UUID NOT NULL REFERENCES maker_reports(id),
    corrected_by UUID NOT NULL REFERENCES users(id),
    ancienne_valeur JSONB NOT NULL,
    nouvelle_valeur JSONB NOT NULL,
    raison TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_report_corrections_report ON report_corrections(report_id);
