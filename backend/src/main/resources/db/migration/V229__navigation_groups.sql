-- V229__navigation_groups.sql
--
-- LOT 2 « TOUT PARAMÉTRABLE » §GR — navigation groupée.
--
-- Problème : la barre latérale empile ~175 entrées en sections plates ; à
-- chaque usage l'utilisateur doit faire défiler une longue liste. On introduit
-- un niveau d'INDIRECTION : un GROUPE d'onglets qui, lorsqu'on le clique, révèle
-- la liste de ses sous-onglets.
--
-- Choix de conception (à lire avant de modifier) :
--
--  * `tenant_id` NULLABLE. NULL = groupe GLOBAL livré par défaut, visible par
--    toutes les églises ; renseigné = groupe d'UNE église qui OVERRIDE le
--    global de même `key`. On reproduit ainsi la sémantique d'héritage déjà en
--    place (ConfigurationResolver : DEFAULT / INHERITED / OVERRIDDEN) sans
--    dépendre du filtre Hibernate `tenantFilter` — la résolution est EXPLICITE
--    (tenant + globaux), donc testable et sans surprise cross-tenant (H4).
--
--  * AUCUNE suppression. `menu_entries.section` reste la source de vérité
--    existante ; un groupe sans affectation explicite capte les entrées dont la
--    `section` correspond (voir NavigationGroupResolver côté application).
--    Un church admin peut donc réorganiser le menu SANS renseigner une ligne.
--
--  * Les affectations (`navigation_group_items`) référencent une entrée par son
--    `href` — dénormalisé mais volontaire : la même table sert à regrouper les
--    entrées `menu_entries` (backend) ET les ~175 entrées du menu statique
--    frontend, dont aucune ligne n'existe en base.

CREATE TABLE IF NOT EXISTS navigation_groups (
    id                   UUID PRIMARY KEY,
    -- NULL = groupe global (livré par défaut, commun à toutes les églises).
    tenant_id            UUID REFERENCES tenants(id) ON DELETE CASCADE,
    key                  VARCHAR(60) NOT NULL,
    label                VARCHAR(120) NOT NULL,
    description          VARCHAR(255),
    icon                 VARCHAR(50),
    -- Imbrication : un groupe peut contenir des sous-groupes.
    parent_group_id      UUID REFERENCES navigation_groups(id) ON DELETE CASCADE,
    display_order        INTEGER NOT NULL DEFAULT 0,
    -- Rôles autorisés à VOIR le groupe ([] = tous les rôles).
    roles                JSONB NOT NULL DEFAULT '[]'::jsonb,
    -- Le groupe n'apparaît que si ce module est actif.
    module_key           VARCHAR(50),
    enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    -- Ouvert au chargement de la page, ou replié (défaut : replié = moins de bruit).
    collapsed_by_default BOOLEAN NOT NULL DEFAULT TRUE,
    -- Affiche le nombre d'entités du groupe sur son en-tête.
    show_count           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ
);

-- Unicité par église pour les groupes d'une église…
CREATE UNIQUE INDEX IF NOT EXISTS uk_navigation_groups_tenant_key
    ON navigation_groups(tenant_id, key) WHERE tenant_id IS NOT NULL;
-- …et par clé pour les groupes globaux (un `UNIQUE (tenant_id, key)` seul
-- admetrait plusieurs NULL en PostgreSQL standard).
CREATE UNIQUE INDEX IF NOT EXISTS uk_navigation_groups_global_key
    ON navigation_groups(key) WHERE tenant_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_navigation_groups_tenant ON navigation_groups(tenant_id);
CREATE INDEX IF NOT EXISTS idx_navigation_groups_parent ON navigation_groups(parent_group_id);
CREATE INDEX IF NOT EXISTS idx_navigation_groups_module ON navigation_groups(module_key);

COMMENT ON TABLE navigation_groups IS
    'Groupes de navigation (GRP-onglets) hiérarchiques, paramétrables par église. tenant_id NULL = global.';
COMMENT ON COLUMN navigation_groups.tenant_id IS
    'NULL = groupe global livré par défaut ; renseigné = groupe propre à une église (override du global de même key).';
COMMENT ON COLUMN navigation_groups.collapsed_by_default IS
    'true = sous-onglets repliés jusqu''au clic sur le groupe.';


-- Affectation d'une entrée de menu (menu_entries OU entrée du menu statique
-- frontend) à un groupe. `href` est la clé de jointure logique : aucune table
-- FK ne peut référencer « la nav statique », qui n'existe qu'en TypeScript.
CREATE TABLE IF NOT EXISTS navigation_group_items (
    id            UUID PRIMARY KEY,
    group_id      UUID NOT NULL REFERENCES navigation_groups(id) ON DELETE CASCADE,
    tenant_id     UUID REFERENCES tenants(id) ON DELETE CASCADE,
    -- Clé de l'entrée quand elle existe en base (menu_entries.key) ; sinon
    -- l'identifiant interne de l'entrée de nav (ex. 'dashboard.membre').
    item_key      VARCHAR(60),
    href          VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_navigation_group_item UNIQUE (group_id, href)
);

CREATE INDEX IF NOT EXISTS idx_navigation_group_items_group ON navigation_group_items(group_id, display_order);
CREATE INDEX IF NOT EXISTS idx_navigation_group_items_href ON navigation_group_items(href);