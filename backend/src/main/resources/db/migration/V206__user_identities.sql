-- V206 : identités externes (Google / Microsoft) rattachées à un compte.
--
-- ADDITIF STRICT : aucun DROP, aucune modification de migration existante.
--
-- POURQUOI UNE TABLE DÉDIÉE (et non une colonne sur `users`) :
--
--  1) Un compte peut cumuler PLUSIEURS identités (Google + Microsoft) : une
--     colonne unique sur `users` obligerait à choisir, donc à perdre l'accès
--     quand un utilisateur change de fournisseur.
--
--  2) L'identifiant fiable est le `subject` (claim `sub`) du fournisseur, PAS
--     l'email. Apple ne restitue l'email qu'à la PREMIÈRE autorisation : au
--     deuxième login, aucun email n'est renvoyé. Une table clé-email casserait
--     silencieusement le reconnectement Apple. `(provider, subject)` est donc
--     la clé d'unicité métier.
--
--  3) AUCUNE colonne `tenant_id`, et AUCUN filtre Hibernate `tenantFilter`.
--     L'identité appartient au COMPTE, pas à l'église. Un compte Discipolat peut
--     être membre de plusieurs églises (table `tenant_membership`) : si cette
--     table portait `tenant_id`, une identité créée depuis l'église A serait
--     invisible depuis l'église B et le reconnectement social échouerait.
--     Requêtes : toujours par `user_id`, obtained d'un compte déjà résolu —
--     donc aucune fuite possible entre tenants.
--
--  4) AUCUN secret stocké : uniquement l'identifiant opaque du fournisseur et
--     l'email observé AU MOMENT du linkage (traçabilité RGPD, jamais utilisé
--     comme clé d'authentification).

CREATE TABLE IF NOT EXISTS user_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    email_at_link VARCHAR(255),
    picture_url VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ,
    CONSTRAINT ck_user_identities_provider
        CHECK (provider IN ('GOOGLE', 'MICROSOFT')),
    CONSTRAINT uk_user_identities_provider_subject
        UNIQUE (provider, subject)
);

-- Requêtes applicatives : lister les identités d'un compte, et résoudre
-- (provider, subject) → compte lors d'un login social.
CREATE INDEX IF NOT EXISTS idx_user_identities_user ON user_identities (user_id);

COMMENT ON TABLE user_identities IS
    'Identités de connexion externes (Google, Microsoft) rattachées à un compte Discipolat. Clé métier : (provider, subject). Sans tenant_id : l''identité est globale au compte.';
COMMENT ON COLUMN user_identities.subject IS
    'Claim `sub` du fournisseur : identifiant opaque et stable, SEULE clé d''authentification (l''email provider est renvoyé une seule fois par Apple).';
COMMENT ON COLUMN user_identities.email_at_link IS
    'Email vérifié par le fournisseur au moment du linkage. Traçabilité RGPD uniquement — jamais utilisé comme clé de connexion.';
