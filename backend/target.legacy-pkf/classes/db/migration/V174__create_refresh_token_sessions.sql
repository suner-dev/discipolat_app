CREATE TABLE refresh_token_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    used_at TIMESTAMP,
    revoked_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_token_sessions_user ON refresh_token_sessions(user_id);
CREATE INDEX idx_refresh_token_sessions_family ON refresh_token_sessions(family_id);
CREATE INDEX idx_refresh_token_sessions_expiry ON refresh_token_sessions(expires_at);
