-- Registration flow creates users in PENDING_ACTIVATION until their activation
-- link is used (AuthService.register, UserService, BulkImportService). The
-- original baseline constraint only allowed ACTIVE/INACTIVE, making POST
-- /auth/register fail with a 500. Align the constraint with UserStatus enum.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_statut_check;
ALTER TABLE users ADD CONSTRAINT users_statut_check CHECK (statut IN ('ACTIVE', 'INACTIVE', 'PENDING_ACTIVATION'));