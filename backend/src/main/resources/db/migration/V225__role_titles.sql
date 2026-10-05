-- ============================================================
-- SPEC_ORGANISATION_MODULABLE_V3 §B — V225 : INTITULÉS par scope.
--
-- Sépare la CAPACITÉ (le rôle `Role`, jeu de permissions portable)
-- de l'INTITULÉ AFFICHÉ, redéfinissable par église/nœud : « Diacre »
-- ici, « Pasteur assistant » là. Une permission ne dépend JAMAIS du
-- label (V3-B / garde-fou §11.4).
--
-- D14 : migration MONTANTE uniquement (V>223). Rétrocompatible :
-- table additive, aucune colonne existante touchée.
--
-- Cible PostgreSQL (Flyway désactivé sous H2/tests — parité vérifiée
-- par le gate PG FlywayMigrationChainPostgreSqlTest).
-- ============================================================

CREATE TABLE IF NOT EXISTS role_titles (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    -- NULL = intitulé par défaut du tenant ; sinon intitulé pour CE nœud.
    node_id       UUID REFERENCES organization_nodes(id) ON DELETE CASCADE,
    label         VARCHAR(120) NOT NULL,
    label_plural  VARCHAR(120),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_role_title_lookup ON role_titles(role_id, node_id);
CREATE INDEX IF NOT EXISTS idx_role_title_tenant ON role_titles(tenant_id);

-- Un seul intitulé par (rôle, nœud) : contrainte standard.
ALTER TABLE role_titles
    ADD CONSTRAINT uk_role_title_tenant_role_node
    UNIQUE (tenant_id, role_id, node_id);

-- PG standard admet plusieurs NULL dans un UNIQUE → l'index partiel
-- garantit « UN SEUL défaut (node_id IS NULL) » par (tenant, rôle).
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_title_default
    ON role_titles(tenant_id, role_id) WHERE node_id IS NULL;
