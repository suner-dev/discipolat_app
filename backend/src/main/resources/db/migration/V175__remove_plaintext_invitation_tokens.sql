DROP INDEX IF EXISTS idx_invitation_token;

ALTER TABLE invitations
    DROP COLUMN IF EXISTS token;
