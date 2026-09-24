ALTER TABLE invitations
    ADD COLUMN IF NOT EXISTS token_hash CHAR(64);

UPDATE invitations
SET token_hash = encode(sha256(convert_to(token, 'UTF8')), 'hex')
WHERE token_hash IS NULL
  AND token IS NOT NULL;

ALTER TABLE invitations
    ALTER COLUMN token DROP NOT NULL;

UPDATE invitations
SET token = NULL
WHERE token_hash IS NOT NULL;

ALTER TABLE invitations
    ALTER COLUMN token_hash SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_invitation_token_hash
    ON invitations(token_hash);
