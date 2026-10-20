-- V188 : tokens d'appareil pour les notifications push (constat M1, tâche A1).
--
-- `PushTokenController` enregistrait les tokens mais NE LES PERSISTAIT PAS
-- (une seule ligne de log) : aucun envoi n'était donc possible, et le mobile
-- (firebase_messaging) ne recevait jamais rien. C'est un mensonge fonctionnel.
--
-- ADDITIF STRICT : aucun DROP, aucune modification de migration existante.
--
-- Le token est UNIQUE GLOBAL, et non par tenant : un même appareil ne doit pas
-- être compté deux fois si l'utilisateur change d'église, sinon la même
-- notification partirait en double. C'est cohérent avec l'unicité email globale
-- déjà posée par V185.

CREATE TABLE IF NOT EXISTS push_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID        NOT NULL REFERENCES tenants (id),
    user_id     UUID        NOT NULL,
    token       VARCHAR(400) NOT NULL,
    platform    VARCHAR(20),
    app_version VARCHAR(40),
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    CONSTRAINT uk_push_tokens_token UNIQUE (token)
);

-- La lecture par utilisateur est le chemin chaud de chaque envoi.
CREATE INDEX IF NOT EXISTS idx_push_tokens_user   ON push_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_push_tokens_tenant ON push_tokens (tenant_id);

COMMENT ON TABLE push_tokens IS
    'Tokens FCM par appareil (constat M1). Unique global : un appareil reste un '
    'seul device, meme si son utilisateur appartient a plusieurs eglises.';
