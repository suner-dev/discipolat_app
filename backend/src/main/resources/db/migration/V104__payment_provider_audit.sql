-- P1 — Mobile Money : provider réel + traçabilité des webhooks opérateur signés.
-- Colonnes ajoutées à chaud (nullable) : aucune donnée existante n'est affectée.

ALTER TABLE payment_intents
    ADD COLUMN IF NOT EXISTS provider_name   VARCHAR(40),
    ADD COLUMN IF NOT EXISTS checkout_url    VARCHAR(2048),
    ADD COLUMN IF NOT EXISTS instructions    TEXT;

COMMENT ON COLUMN payment_intents.provider_name IS
    'Provider opérateur réellement sollicité (MTN MoMo / Orange Money / M-Pesa).';
COMMENT ON COLUMN payment_intents.checkout_url IS
    'URL de redirection/checkout opérateur retournée par l''API (pour les providers à redirection).';
COMMENT ON COLUMN payment_intents.instructions IS
    'Instructions d''affichage au client (ex. code OTP reçu par SMS).';

-- Journal d'audit des notifications de paiement reçues des opérateurs.
-- Chaque appel webhook est enregistré (même rejeté) pour non-répudiation.
CREATE TABLE IF NOT EXISTS payment_webhook_audit (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    operator        VARCHAR(40) NOT NULL,
    event_type      VARCHAR(60) NOT NULL,
    raw_payload     TEXT NOT NULL,
    provider_reference VARCHAR(255),
    signature_valid BOOLEAN NOT NULL,
    processed       BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at    TIMESTAMP,
    failure_reason  VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_operator ON payment_webhook_audit(operator);
CREATE INDEX IF NOT EXISTS idx_payment_webhook_ref ON payment_webhook_audit(provider_reference);
