-- Retire la colonne plaintext `token` des invitations.
--
-- Correctif 2026-09-24 : la version précédente faisait
-- `DROP INDEX IF EXISTS idx_invitation_token;` AVANT de supprimer la colonne.
-- Or, sur PostgreSQL, un indexrequis par une contrainte UNIQUE ne peut pas
-- être supprimé directement : selon l'historique de la base, l'objet peut
-- s'appeler `idx_invitation_token` (contrainte) ou `invitations_token_key`
-- (contrainte standard créée par `UNIQUE`). Le DROP INDEX échouait donc et
-- empêchait TOUTE l'application des migrations suivantes : l'application ne
-- démarrait plus. On supprime d'abord les contraintes candidates (chacune
-- emporte son index), puis la colonne.
--
-- Tout est idempotent (IF EXISTS) : sûr sur une base fraîche comme déjà migrée.

ALTER TABLE invitations
    DROP CONSTRAINT IF EXISTS idx_invitation_token;

ALTER TABLE invitations
    DROP CONSTRAINT IF EXISTS invitations_token_key;

-- Filet de sécurité : indexNU (non porté par une contrainte) portant ce nom.
DROP INDEX IF EXISTS idx_invitation_token;

ALTER TABLE invitations
    DROP COLUMN IF EXISTS token;
