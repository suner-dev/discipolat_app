-- V230__ui_labels_and_page_features.sql
--
-- LOT 2 §LB « TOUT PARAMÉTRABLE » — noms, boutons et fonctionnalités.
--
-- Constat qui motive cette migration : l'application traduit ses libellés
-- avec une table statique de 3 300 entrées × 6 langues, et chaque bouton est
-- écrit en dur dans la page. Conséquence produit : une église qui veut appeler
-- ses « anciens » « Responsables spirituels », ou retirer le bouton « Exporter »
-- de l'écran « Âmes », n'a aucun moyen de le faire sans écrire de code.
--
-- Cette migration ajoute le VOLANT, sans toucher au système de traduction :
--
--   * `ui_label_overrides` — surcharge d'AUTRE libellé, par clé et par langue.
--     Résolution : nœud → église → global, et langue exacte avant « toutes ».
--     Point clé : le frontend interroge ce store AVANT le dictionnaire i18n, ce
--     qui rend chaque chaîne déjà traduite renommable sans toucher une page.
--
--   * `ui_page_features` — activation/désactivation d'une fonctionnalité ou
--     d'un bouton sur un écran donné, avec libellé propre. C'est le « on peut
--     choisir les fonctionnalités à ajouter sur telle page ou en enlever ».
--
-- `tenant_id` NULLABLE partout : NULL = réglage GLOBAL livré par défaut,
-- renseigné = réglage d'UNE église qui l'écrase. Même sémantique d'héritage que
-- `navigation_groups` (V229) et `ConfigurationResolver`, résolue explicitement
-- côté service (pas de filtre Hibernate) pour rester testable et sans fuite
-- cross-tenant (H4).

CREATE TABLE IF NOT EXISTS ui_label_overrides (
    id          UUID PRIMARY KEY,
    -- NULL = surcharge globale (livrée par défaut, commune à toutes les églises).
    tenant_id   UUID REFERENCES tenants(id) ON DELETE CASCADE,
    node_id     UUID REFERENCES organization_nodes(id) ON DELETE CASCADE,
    -- Clé du dictionnaire i18n (ex. 'souls.title') ou libellé source (ex. 'Retour').
    label_key   VARCHAR(150) NOT NULL,
    -- Code langue ISO ('fr', 'en'…) ou '*' = toutes les langues.
    locale      VARCHAR(10) NOT NULL DEFAULT '*',
    value       VARCHAR(400) NOT NULL,
    description VARCHAR(255),
    -- Une surcharge désactivée est conservée mais ignorée : l'admin peut
    -- « suspendre » un renommage sans perdre son texte.
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ui_label_global
    ON ui_label_overrides(label_key, locale) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_ui_label_tenant
    ON ui_label_overrides(tenant_id, label_key, locale) WHERE tenant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ui_label_tenant ON ui_label_overrides(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ui_label_key ON ui_label_overrides(label_key);

COMMENT ON TABLE ui_label_overrides IS
    'Surcharge d''un libellé par l''église (LOT 2 §LB). Résolution : nœud → église → global.';
COMMENT ON COLUMN ui_label_overrides.node_id IS
    'NULL = réglage d''église ; renseigné = réglage propre à un nœud du réseau.';


CREATE TABLE IF NOT EXISTS ui_page_features (
    id             UUID PRIMARY KEY,
    -- NULL = réglage GLOBAL livré par défaut.
    tenant_id      UUID REFERENCES tenants(id) ON DELETE CASCADE,
    node_id        UUID REFERENCES organization_nodes(id) ON DELETE CASCADE,
    -- Écran : clé de route ('/souls') ou identifiant d'écran ('soul-detail').
    page_key       VARCHAR(100) NOT NULL,
    -- Fonctionnalité ou bouton : 'export', 'delete', 'import'…
    feature_key    VARCHAR(100) NOT NULL,
    -- Renomme ce bouton précisément, sans toucher le dictionnaire global.
    label_override VARCHAR(200),
    -- false = l'admin RETIRE la fonctionnalité de cet écran.
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    display_order  INTEGER NOT NULL DEFAULT 0,
    module_key     VARCHAR(50),
    description    VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ui_page_feature_global
    ON ui_page_features(page_key, feature_key) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_ui_page_feature_tenant
    ON ui_page_features(tenant_id, page_key, feature_key) WHERE tenant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ui_page_feature_tenant ON ui_page_features(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ui_page_feature_page ON ui_page_features(page_key);

COMMENT ON TABLE ui_page_features IS
    'Fonctionnalités et boutons activables/retirables par écran (LOT 2 §LB).';
COMMENT ON COLUMN ui_page_features.enabled IS
    'false = fonctionnalité retirée de cet écran par l''administration de l''église.';