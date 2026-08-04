-- V19__add_member_presences_and_requests.sql
-- Espace Membre — Phase 2 : présences hebdomadaires + suggestions / rendez-vous / signalements
-- Les données saisies par le membre sont visibles par son chef de famille,
-- son responsable de département et le pasteur (scoping côté service).

-- ============================================================
-- MEMBER PRESENCES — saisie de présence hebdomadaire par le membre
-- ============================================================
CREATE TABLE IF NOT EXISTS member_presences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    soul_id UUID REFERENCES souls(id) ON DELETE CASCADE,
    semaine DATE NOT NULL,
    presences JSONB NOT NULL DEFAULT '{}'::jsonb,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_member_presences_user_week UNIQUE (user_id, semaine)
);

CREATE INDEX IF NOT EXISTS idx_member_presences_user ON member_presences(user_id);
CREATE INDEX IF NOT EXISTS idx_member_presences_soul ON member_presences(soul_id);
CREATE INDEX IF NOT EXISTS idx_member_presences_semaine ON member_presences(semaine);

-- ============================================================
-- MEMBER REQUESTS — suggestions, rendez-vous, signalements
-- cible : à qui la demande est adressée (visible par)
-- department_id / family_id : portée de la demande (département / famille du membre)
-- ============================================================
CREATE TABLE IF NOT EXISTS member_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL CHECK (type IN ('SUGGESTION', 'RENDEZ_VOUS', 'SIGNALEMENT')),
    cible VARCHAR(30) NOT NULL CHECK (cible IN ('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')),
    message TEXT NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'OUVERT' CHECK (statut IN ('OUVERT', 'EN_COURS', 'RESOLU', 'REJETE')),
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    family_id UUID REFERENCES families(id) ON DELETE SET NULL,
    reponse TEXT,
    traite_par UUID REFERENCES users(id),
    date_traitement TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_member_requests_user ON member_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_member_requests_cible ON member_requests(cible);
CREATE INDEX IF NOT EXISTS idx_member_requests_statut ON member_requests(statut);
CREATE INDEX IF NOT EXISTS idx_member_requests_department ON member_requests(department_id);
CREATE INDEX IF NOT EXISTS idx_member_requests_family ON member_requests(family_id);
