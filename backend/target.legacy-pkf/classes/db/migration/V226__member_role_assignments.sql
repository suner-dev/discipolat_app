-- ============================================================
-- SPEC_ORGANISATION_MODULABLE_V3 §C — V226 : AFFILIATION multi-nœuds.
--
-- « X porte le rôle R sur le nœud N » (0..n), DÉCOUPLÉ de
-- l'appartenance (`TenantMembership`). Gère « je nomme X ancien et je
-- lui associe ces 3 campus ». Prérequis : T-B-fix-F17 (multi-lines).
--
-- D14 : migration MONTANTE. Table additive, rétrocompatible.
-- Cible PostgreSQL (parité validée par le gate PG).
-- ============================================================

CREATE TABLE IF NOT EXISTS member_role_assignments (
    id             UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id        UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    -- NULL = portée tenant ; sinon portée du nœud (et de sa descendance).
    node_id        UUID REFERENCES organization_nodes(id) ON DELETE CASCADE,
    assigned_by    UUID REFERENCES users(id),
    assigned_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE','SUSPENDED','ENDED')),
    ended_at       TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_mra_user ON member_role_assignments(user_id, status);
CREATE INDEX IF NOT EXISTS idx_mra_node ON member_role_assignments(node_id, role_id);
CREATE INDEX IF NOT EXISTS idx_mra_tenant ON member_role_assignments(tenant_id);

-- Empêche le doublon actif (user, role, node). node_id NULL → plusieurs
-- NULL admis en PG standard ; le service impose l'unicité de la ligne
-- ACTIVE par (user, role, node) en applicatif (upsert).
ALTER TABLE member_role_assignments
    ADD CONSTRAINT uk_mra_active UNIQUE (user_id, role_id, node_id, status);
