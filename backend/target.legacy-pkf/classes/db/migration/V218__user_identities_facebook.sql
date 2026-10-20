-- V218 (renuméroté depuis V207 — V207 occupé par le port Develop1 sur main) : ouverture de `user_identities` au fournisseur Facebook.
--
-- ADDITIF STRICT : aucun DROP de données, aucune modification de migration
-- existante. V206 limitait la contrainte à ('GOOGLE', 'MICROSOFT') ; un
-- rattachement Facebook serait donc REJETÉ par la base — la contrainte est
-- élargie, pas contournée.
--
-- Pourquoi une nouvelle migration plutôt que modifier V206 : Flyway n'écrit
-- jamais une migration déjà appliquée (le checksum est figé). Modifier V206
-- ferait échouer le démarrage de toute base existante.
--
-- Contrainte replacée avec la nouvelle valeur : PostgreSQL ne permet pas
-- d'élargir un CHECK existant, il faut le DROP puis le recréer. La table
-- elle-même n'est jamais touchée.

ALTER TABLE user_identities
    DROP CONSTRAINT IF EXISTS ck_user_identities_provider;

ALTER TABLE user_identities
    ADD CONSTRAINT ck_user_identities_provider
    CHECK (provider IN ('GOOGLE', 'MICROSOFT', 'FACEBOOK'));

COMMENT ON TABLE user_identities IS
    'Identités de connexion externes (Google, Microsoft, Facebook) rattachées à un compte Discipolat. Clé métier : (provider, subject). Sans tenant_id : l''identité est globale au compte.';
