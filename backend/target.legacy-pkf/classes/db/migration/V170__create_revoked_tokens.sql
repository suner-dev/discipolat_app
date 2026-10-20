CREATE TABLE revoked_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    token_type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NOT NULL,
    reason VARCHAR(120)
);

CREATE INDEX idx_revoked_tokens_expiry ON revoked_tokens(expires_at);
