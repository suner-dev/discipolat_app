-- ============================================================
-- SPEC_ORGANISATION_DENOMINATION_V2 §5 — V223 : trace du transfert.
--
-- §4.4 / D4 / D5 : un membre qui passe de l'église A à l'église B
-- de la MÊME dénomination ne se réinscrit pas — il est TRANSFÉRÉ.
--
-- D5 : le transfert PRÉSERVE l'historique. L'appartenance source
--      n'est donc pas SUPPRIMÉE : elle passe en REVOKED et conserve
--      le moment, la cible et le motif. Supprimer une ligne ferait
--      perdre « qui était membre, depuis quand, dans quelle église »,
--      qui est à la fois une exigence d'audit et une réalité
--      pastorale (le pasteur qui change d'église garde son parcours).
--
-- Le statut `REVOKED` existe déjà dans l'enum MembershipStatus ; il
-- n'est pas étendu ici — ce sont les COLONNES de traçabilité qui
-- manquaient.
--
-- Rétrocompatibilité (D14) : migration MONTANTE uniquement.
-- ============================================================

ALTER TABLE tenant_memberships ADD COLUMN IF NOT EXISTS transferred_at TIMESTAMPTZ;
ALTER TABLE tenant_memberships ADD COLUMN IF NOT EXISTS transferred_to_tenant_id UUID;
ALTER TABLE tenant_memberships ADD COLUMN IF NOT EXISTS transferred_by_user_id UUID;
ALTER TABLE tenant_memberships ADD COLUMN IF NOT EXISTS transfer_reason VARCHAR(500);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_memberships_transferred_to_tenant'
    ) THEN
        ALTER TABLE tenant_memberships ADD CONSTRAINT fk_memberships_transferred_to_tenant
            FOREIGN KEY (transferred_to_tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;
    END IF;
END $$;

-- Historique des transferts : « qui a quitté quelle église, quand,
-- pour aller où ». Requête d'audit et support.
CREATE INDEX IF NOT EXISTS idx_memberships_transferred
    ON tenant_memberships(tenant_id, transferred_at DESC)
    WHERE transferred_at IS NOT NULL;

-- Retrouver, pour une organisation donnée, tous les membres partis.
CREATE INDEX IF NOT EXISTS idx_memberships_transferred_to
    ON tenant_memberships(transferred_to_tenant_id)
    WHERE transferred_to_tenant_id IS NOT NULL;

COMMENT ON COLUMN tenant_memberships.transferred_at IS
    'Instant du transfert de membre (SPEC ORGANISATION V2 §4.4, D4).';
COMMENT ON COLUMN tenant_memberships.transferred_to_tenant_id IS
    'Organisation d''accueil du transfert. NULL si l''appartenance n''a pas été transférée.';
COMMENT ON COLUMN tenant_memberships.transferred_by_user_id IS
    'Acteur ayant effectué le transfert (le membre lui-même, ou un admin plateforme).';
COMMENT ON COLUMN tenant_memberships.transfer_reason IS
    'Motif du transfert, conservé pour l''audit (D5).';
