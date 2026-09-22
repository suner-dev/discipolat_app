-- ============================================================
-- V65 : Page Builder — pages personnalisees.
-- L'administrateur cree des pages metier (blocs KPI / tableau /
-- liste / texte / liens / recherche / images) publiees pour des
-- roles donnes. Le contenu des blocs est resolu cote serveur sur
-- des donnees reelles (aucune statistique fictive). Chaque page
-- est versionnee dans config_revisions (entity_type CUSTOM_PAGE).
-- ============================================================
CREATE TABLE custom_pages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key         VARCHAR(50)  NOT NULL UNIQUE,       -- cle technique unique
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    slug        VARCHAR(255) NOT NULL UNIQUE,       -- URL publique /pages/:slug
    layout      VARCHAR(20)  NOT NULL DEFAULT 'STACK', -- STACK / GRID_2 / GRID_3
    blocks      JSONB        NOT NULL DEFAULT '[]'::jsonb,
    roles       JSONB        NOT NULL DEFAULT '[]'::jsonb, -- [] = tous les roles
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    published   BOOLEAN      NOT NULL DEFAULT FALSE,
    version     INTEGER      NOT NULL DEFAULT 1,
    created_by  UUID REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_custom_pages_slug ON custom_pages (slug);
CREATE INDEX idx_custom_pages_enabled ON custom_pages (enabled, published);

-- Menu d'administration du Page Builder
INSERT INTO menu_entries (key, label, href, icon, section, ordre, roles, module_key)
VALUES ('pages', 'Pages personnalisees', '/admin/pages', 'LayoutTemplate', 'Administration', 7,
        '["ADMIN"]'::jsonb, 'PERMISSIONS')
ON CONFLICT (key) DO NOTHING;

-- Page d'exemple immediatement exploitable (supprimable / modifiable par l'admin).
-- Tous les blocs sont resolus sur des donnees reelles par le serveur.
INSERT INTO custom_pages (key, title, description, slug, layout, blocks, roles, enabled, published, version)
VALUES (
    'apercu-eglise',
    'Vue d''ensemble de l''eglise',
    'Synthese en temps reel : effectifs, alertes et evenements a venir.',
    'apercu-eglise',
    'GRID_2',
    '[
      {"type":"KPI","config":{"label":"Âmes suivies","source":"SOULS_TOTAL","icon":"Heart","color":"primary"}},
      {"type":"KPI","config":{"label":"Âmes actives","source":"SOULS_ACTIFS","icon":"Activity","color":"emerald"}},
      {"type":"KPI","config":{"label":"Familles","source":"FAMILIES_TOTAL","icon":"Users","color":"amber"}},
      {"type":"KPI","config":{"label":"Departements","source":"DEPARTMENTS_TOTAL","icon":"Building2","color":"violet"}},
      {"type":"KPI","config":{"label":"Alertes ouvertes","source":"ALERTS_OPEN","icon":"Bell","color":"rose"}},
      {"type":"KPI","config":{"label":"Transferts en attente","source":"TRANSFERS_PENDING","icon":"ArrowLeftRight","color":"sky"}},
      {"type":"TABLEAU","config":{"title":"Dernieres âmes","source":"RECENT_SOULS"}},
      {"type":"TABLEAU","config":{"title":"Evenements a venir","source":"UPCOMING_EVENTS"}},
      {"type":"LISTE","config":{"title":"Alertes recentes","source":"RECENT_ALERTS"}},
      {"type":"LIENS","config":{"title":"Acces rapides","items":[
          {"label":"Recherche globale","href":"/search","icon":"Search"},
          {"label":"Âmes & disciples","href":"/souls","icon":"Heart"},
          {"label":"Familles","href":"/families","icon":"Users"},
          {"label":"Rapports","href":"/reports","icon":"FileText"}
      ]}}
    ]'::jsonb,
    '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb,
    TRUE, TRUE, 1
)
ON CONFLICT (key) DO NOTHING;

COMMENT ON TABLE custom_pages IS 'Pages personnalisees du Page Builder : blocs configures par l''administrateur, donnees resolues cote serveur sur les entites reelles, versionnees (config_revisions).';
