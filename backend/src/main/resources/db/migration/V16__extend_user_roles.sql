-- V16: Extend the users.role CHECK constraint to cover all supported roles.
-- The V5 migration only allowed ADMIN/PASTEUR/RESPONSABLE/FAISEUR, which
-- prevented creating users with the CHEF_DE_FAMILLE or MEMBRE roles.

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'));
