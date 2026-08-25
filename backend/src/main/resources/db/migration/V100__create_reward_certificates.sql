-- V100 : table reward_certificates (module rewards / certificats)
-- L'entité Certificate (@Table reward_certificates) n'avait aucune migration :
-- en production (ddl-auto: none) la fonctionnalité échouait à l'exécution.
CREATE TABLE IF NOT EXISTS reward_certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    recipient_name VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    mention VARCHAR(50),
    description TEXT,
    reference VARCHAR(100) NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_reward_certificates_reference UNIQUE (tenant_id, reference)
);

CREATE INDEX idx_reward_certificates_tenant ON reward_certificates(tenant_id);
CREATE INDEX idx_reward_certificates_user ON reward_certificates(user_id);
