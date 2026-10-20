-- V31__member_presences_soul_key.sql
-- Saisie des presences par le responsable (departement) :
-- Les membres d'un departement peuvent ne pas avoir de compte utilisateur lie.
-- On rend donc user_id NULLABLE et on permet de pointer une presence par soul_id + semaine.
-- La contrainte unique passe d'un index (user_id, semaine) a des index partiels
-- (user_id, semaine) et (soul_id, semaine).

ALTER TABLE member_presences ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE member_presences DROP CONSTRAINT IF EXISTS uq_member_presences_user_week;

-- Un membre ne peut avoir qu'une presence par semaine (si lie a un compte)
CREATE UNIQUE INDEX IF NOT EXISTS uq_member_presences_user_week
    ON member_presences(user_id, semaine) WHERE user_id IS NOT NULL;

-- Une âme ne peut avoir qu'une presence par semaine (saisie responsable)
CREATE UNIQUE INDEX IF NOT EXISTS uq_member_presences_soul_week
    ON member_presences(soul_id, semaine) WHERE soul_id IS NOT NULL;

COMMENT ON COLUMN member_presences.user_id IS 'Compte utilisateur lie (nullable : les membres sans compte sont pointes par soul_id).';
