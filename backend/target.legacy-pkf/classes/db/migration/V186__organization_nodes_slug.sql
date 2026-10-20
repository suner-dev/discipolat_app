-- V186 : alignement du schéma sur l'entité `OrganizationNode.slug` (constat H1).
--
-- ADDITIF STRICT : aucun DROP, aucune modification de migration existante.
--
-- `OrganizationNode` mappe `@Column(name = "slug", length = 100)` (champ ajouté
-- par le commit 69fea3b, présent sur `main`), et
-- `OrganizationManagementController` lit (`node.getSlug()`) comme écrit
-- (`node.setSlug(...)`) cette valeur. AUCUNE migration ne créait la colonne.
-- Conséquence : sur toute base construite par les migrations — donc sur tout
-- déploiement neuf — la simple lecture de la hiérarchie organizational lève
--
--     ERROR: column on1_0.slug does not exist
--
-- ce qui renvoyait un HTTP 500 sur le provisionnement atomique d'un tenant
-- (`POST /api/v1/platform/admin/provisioning`) et sur l'API organisation.
--
-- La correction se fait côté SCHÉMA et non côté entité : supprimer le champ
-- casserait l'API organisations et son client web. La colonne est nullable :
-- les lignes existantes restent valides et aucun backfill n'est nécessaire
-- (aucun code ne lit `slug` sans écrire : le seules lectures sont des
-- projections de réponse, tolérantes à l'absence de valeur).
--
-- La colonne est idempotente (`IF NOT EXISTS`) : rejouable sans risque sur une
-- base déjà corrigée.

ALTER TABLE organization_nodes ADD COLUMN IF NOT EXISTS slug VARCHAR(100);

-- La hiérarchie est lue très fréquemment par l'administration de tenant ; un
-- index sur le couple (tenant_id, slug) reste cohérent avec l'index de tenancy
-- existant et évite un scan lors des recherches par slug.
CREATE INDEX IF NOT EXISTS idx_organization_nodes_tenant_slug
    ON organization_nodes (tenant_id, slug);
