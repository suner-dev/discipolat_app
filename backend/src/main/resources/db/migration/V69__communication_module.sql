-- V69 : Outil metier COMMUNICATION (module activable)
-- ============================================================
-- Annonces et diffusion ciblees (TOUS / role / famille /
-- departement). La publication declenche des notifications IN_APP
-- vers les destinataires. Module activable (ModuleGateFilter :
-- /api/v1/communications → COMMUNICATION) + menu configurable.
-- ============================================================

CREATE TABLE IF NOT EXISTS communications (
    id               UUID PRIMARY KEY,
    titre            VARCHAR(200) NOT NULL,
    contenu          TEXT NOT NULL,
    cible            VARCHAR(20) NOT NULL CHECK (cible IN ('TOUS', 'ROLE', 'FAMILLE', 'DEPARTEMENT')),
    roles            JSONB,
    famille_id       UUID,
    department_id    UUID,
    statut           VARCHAR(20) NOT NULL DEFAULT 'BROUILLON'
                     CHECK (statut IN ('BROUILLON', 'PUBLIEE', 'ARCHIVEE')),
    date_publication TIMESTAMP,
    created_by       UUID,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP,
    deleted          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_communications_statut ON communications(statut);
CREATE INDEX IF NOT EXISTS idx_communications_date ON communications(date_publication);

COMMENT ON TABLE communications IS 'Annonces de l''eglise avec cible de diffusion (tous, role, famille, departement).';

-- Module activable + menu (tous les roles lisent les annonces publiees ;
-- la gestion est reservee ADMIN/PASTEUR cote page).
INSERT INTO platform_modules (key, label, description, icon, section, enabled, ordre)
VALUES ('COMMUNICATION', 'Communication',
        'Annonces et diffusion ciblees vers les membres de l''eglise',
        'Megaphone', 'Engagement & outils', TRUE, 23)
ON CONFLICT (key) DO NOTHING;

INSERT INTO menu_entries (key, label, href, icon, section, ordre, roles, module_key, enabled)
VALUES ('communications', 'Annonces', '/communications',
        'Megaphone', 'Engagement & outils', 10,
        '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR","MEMBRE"]'::jsonb,
        'COMMUNICATION', TRUE)
ON CONFLICT (key) DO NOTHING;
