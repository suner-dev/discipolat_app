-- ============================================================
-- SPEC_ORGANISATION_DENOMINATION_V2 §5 — V222 : modèle d'organisation.
--
-- D2 : un tenant n'est plus « une église », c'est une ENTITÉ dont la
--      nature est explicite et lisible dans les données.
-- D1 : une organisation peut contenir d'autres organisations
--      (modèle hybride : nœuds légers à l'intérieur, tenants fils
--      autonomes entre organisations).
-- D3 : `root_tenant_id` est le DÉTECTEUR du transfert de membre
--      (§4.4) : deux membres de la même racine partagent un
--      parcours, pas de réinscription.
--
-- Rétrocompatibilité (D14) : migration MONTANTE uniquement, aucune
-- modification de V≤221. Toutes les colonnes sont additives et
-- `root_tenant_id` est rempli pour l'existant avant de devenir
-- NOT NULL — sinon le upgrade casserait sur une base peuplée.
-- ============================================================

-- 1) Nature de l'organisation
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS kind VARCHAR(30) NOT NULL DEFAULT 'CHURCH';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'tenants_kind_check'
    ) THEN
        ALTER TABLE tenants ADD CONSTRAINT tenants_kind_check
            CHECK (kind IN ('CHURCH', 'DENOMINATION', 'ASSOCIATION', 'ORGANIZATION', 'MEGA_ASSOCIATION'));
    END IF;
END $$;

-- 2) Hiérarchie organisationnelle.
--    parent_tenant_id = rattachement direct (l'arborescence).
--    root_tenant_id   = la dénomination de référence, elle-même
--                        pour une racine. Propagé à toute la descendance.
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS parent_tenant_id UUID;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS root_tenant_id UUID;

-- ON DELETE RESTRICt : on ne supprime pas une dénomination qui a des
-- enfants. La suppression passe par le statut CANCELLED (§8.6), ce qui
-- préserve l'historique pastoral.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tenants_parent_tenant'
    ) THEN
        ALTER TABLE tenants ADD CONSTRAINT fk_tenants_parent_tenant
            FOREIGN KEY (parent_tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tenants_root_tenant'
    ) THEN
        ALTER TABLE tenants ADD CONSTRAINT fk_tenants_root_tenant
            FOREIGN KEY (root_tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;
    END IF;
END $$;

-- 3) Rétro-compatible : une organisation existante est sa propre racine.
--    Le backfill rend en outre `idx_tenants_root` réellement exploitable sur
--    l'existant, plutôt que de laisser l'index à moitié vide.
UPDATE tenants SET root_tenant_id = id WHERE root_tenant_id IS NULL;

-- 4) `root_tenant_id` reste NULLABLE — et c'est un choix délibéré.
--
--    La tentation est d'imposer `NOT NULL` pour que le détecteur du transfert
--    (§4.4) compare « toujours deux racines non nulles ». Cette contrainte
--    serait FAUSSE et casserait la production : une colonne qui doit égaler sa
--    propre clé primaire ne peut pas être renseignée par un trigger SQL
--    (le trigger s'exécute AVANT que l'UUID ne soit connu), ni de façon fiable
--    par un `@PrePersist` Java, qui dépend de l'ordre d'affectation de l'identifiant
--    par Hibernate. Un `NOT NULL` raté ici = `INSERT ... violates not-null
--    constraint` dès la première église créée après le déploiement, et rien
--    dans la suite locale ne l'aurait vu (profil `test` en `ddl-auto`, V222
--    jamais exécutée sans Docker).
--
--    La cohérence est donc portée là où elle est vérifiable :
--      * `Tenant.effectiveRootTenantId()` normalise — une organisation dont la
--        racine est nulle EST sa propre racine ;
--      * `TenantOrganizationService` affecte à toute enfant la racine
--        *résolue* de sa mère (`parent.effectiveRootTenantId()`), donc jamais
--        nulle pour un enfant ;
--      * le backfill ci-dessus aligne l'existant ;
--      * `TenantTransferService` compare via `effectiveRootTenantId()` des deux
--        côtés, donc ne rencontre jamais de « null » non traité.
--
--    Le backfill de l'étape 3 aligne l'existant ; il n'est pas rejoué ici.
--
-- 5) Une racine ne peut pas être sa propre enfant (garde anti-boucle
--    simple ; la détection des cycles longs est applicative).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'tenants_no_self_parent'
    ) THEN
        ALTER TABLE tenants ADD CONSTRAINT tenants_no_self_parent
            CHECK (parent_tenant_id IS NULL OR parent_tenant_id <> id);
    END IF;
END $$;

-- 6) Index : le réseau se parcourt par racine (descendance) et par
--    parent direct (arborescence d'un niveau).
CREATE INDEX IF NOT EXISTS idx_tenants_root      ON tenants(root_tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenants_parent    ON tenants(parent_tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenants_root_kind ON tenants(root_tenant_id, kind);
-- La console plateforme filtre par statut : l'index existant sur
-- `status` reste pertinent, on couvre le couple statut + racine.
CREATE INDEX IF NOT EXISTS idx_tenants_status_root ON tenants(status, root_tenant_id);

COMMENT ON COLUMN tenants.root_tenant_id IS
    'Racine du reseau (= la denomination). Detecteur du transfert de membre (SPEC ORGANISATION V2 §4.4, D3/D4). NULLABLE : NULL signifie « je suis ma propre racine » (organigramme clos). lire via Tenant.effectiveRootTenantId().';
COMMENT ON COLUMN tenants.parent_tenant_id IS
    'Rattachement direct dans l arborescence organisationnelle (§1.3 mode AUTONOME).';
COMMENT ON COLUMN tenants.kind IS
    'Nature de l''organisation : CHURCH | DENOMINATION | ASSOCIATION | ORGANIZATION | MEGA_ASSOCIATION (D2).';
