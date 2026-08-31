-- Bible Reading Plans and Entries
-- P1 #49/#60 — Plan de lecture biblique partagé

CREATE TABLE IF NOT EXISTS bible_reading_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    type_plan VARCHAR(50) NOT NULL DEFAULT 'PERSONNALISE',
    statut VARCHAR(50) NOT NULL DEFAULT 'ACTIF',
    createur_id UUID,
    partage_famille BOOLEAN NOT NULL DEFAULT false,
    jours_total INT NOT NULL DEFAULT 365,
    jours_completes INT NOT NULL DEFAULT 0,
    date_debut DATE NOT NULL DEFAULT CURRENT_DATE,
    cree_le TIMESTAMP NOT NULL DEFAULT now(),
    modifie_le TIMESTAMP
);

CREATE INDEX idx_bible_plans_tenant ON bible_reading_plans(tenant_id);
CREATE INDEX idx_bible_plans_createur ON bible_reading_plans(createur_id);

CREATE TABLE IF NOT EXISTS bible_reading_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plan_id UUID NOT NULL REFERENCES bible_reading_plans(id) ON DELETE CASCADE,
    utilisateur_id UUID NOT NULL,
    reference_verset VARCHAR(255) NOT NULL,
    categorie VARCHAR(100),
    theme VARCHAR(255),
    lu BOOLEAN NOT NULL DEFAULT false,
    date_lecture DATE NOT NULL DEFAULT CURRENT_DATE,
    note TEXT,
    cree_le TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_bible_entries_tenant ON bible_reading_entries(tenant_id);
CREATE INDEX idx_bible_entries_plan ON bible_reading_entries(plan_id);
CREATE INDEX idx_bible_entries_user ON bible_reading_entries(utilisateur_id);
