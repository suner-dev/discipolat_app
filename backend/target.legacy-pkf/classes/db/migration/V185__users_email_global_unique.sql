-- V185 : unicité GLOBALE et INSENSIBLE A LA CASSE de users.email (constat B4).
--
-- Problème corrigé : la contrainte historique `email VARCHAR(255) NOT NULL UNIQUE`
-- (V1) et l'index composite `uk_users_tenant_email` (V70, (tenant_id, email))
-- sont tous deux sensibles à la casse. Deux comptes actifs peuvent donc porter
-- `Pasteur@Eglise.com` et `pasteur@eglise.com`, ce qui rend le login ambigu et
-- permet la creation de doublons inter-eglises via les invitations.
--
-- Correctif : index unique partiel sur LOWER(email) restreint aux lignes actives.
--   - `LOWER(email)` : neutralise la casse.
--   - `WHERE deleted = false` : un compte archive ne bloque pas la reutilisation
--     de son adresse (contrainte d'index unique partial, standard PostgreSQL).
--
-- FAIL-CLOSED : si des doublons insensibles a la casse existent deja en base, on
-- leve une exception explicite plutot que de supprimer ou fusionner des donnees
-- automatiquement. Le dedoublonnage est une decision metier (humaine), hors
-- perimetre de cette migration : aucune donnee n'est perdue.
--
-- Le multi-mandat reste supported : un utilisateur appartient a plusieurs
-- eglises via `tenant_memberships`, pas via plusieurs lignes `users`.

DO $$
DECLARE dup_count int;
BEGIN
  SELECT COUNT(*) INTO dup_count FROM (
    SELECT LOWER(email) FROM users WHERE deleted = false GROUP BY LOWER(email) HAVING COUNT(*) > 1
  ) d;
  IF dup_count > 0 THEN
    RAISE EXCEPTION 'V185: % doublon(s) email insensibles a la casse - dedoublonnage manuel requis', dup_count;
  END IF;
END $$;

-- Note : `uk_users_tenant_email` (V70) est volontairement conserve (composite,
-- redondant mais inoffensif et utilise par des requetes de coherence).
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_lower ON users (LOWER(email)) WHERE deleted = false;
