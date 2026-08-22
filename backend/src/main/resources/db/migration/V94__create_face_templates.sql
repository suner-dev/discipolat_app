-- V94 : gabarits biométriques faciaux (pointage par reconnaissance faciale)
-- Aucune image stockée — uniquement l'empreinte perceptuelle non réversible.
CREATE TABLE IF NOT EXISTS face_templates (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    user_id         UUID,
    soul_id         UUID,
    display_name    VARCHAR(255) NOT NULL,
    descriptor_hash VARCHAR(64)  NOT NULL,
    quality_score   DOUBLE PRECISION NOT NULL DEFAULT 0,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_face_user UNIQUE (tenant_id, user_id),
    CONSTRAINT fk_face_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT fk_face_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_face_templates_tenant_active
    ON face_templates (tenant_id, active);
