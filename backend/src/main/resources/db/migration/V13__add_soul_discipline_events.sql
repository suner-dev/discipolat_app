-- V13: Ajout de la table soul_discipline_events pour le suivi comportemental (Phase 7)
-- Permet de tracker : comportement, conduite, litiges, conflits, témoignages, etc.

CREATE TABLE soul_discipline_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ame_id UUID NOT NULL REFERENCES souls(id) ON DELETE CASCADE,
    auteur_id UUID NOT NULL REFERENCES users(id),
    categorie VARCHAR(50) NOT NULL,
    type_evenement VARCHAR(50) NOT NULL,
    gravite VARCHAR(20),
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    date_evenement DATE NOT NULL DEFAULT CURRENT_DATE,
    resolu BOOLEAN DEFAULT FALSE,
    date_resolution DATE,
    resolu_par UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- Index pour les recherches par âme
CREATE INDEX idx_soul_discipline_events_ame_id ON soul_discipline_events(ame_id);
CREATE INDEX idx_soul_discipline_events_categorie ON soul_discipline_events(categorie);
CREATE INDEX idx_soul_discipline_events_resolu ON soul_discipline_events(resolu);
CREATE INDEX idx_soul_discipline_events_date ON soul_discipline_events(date_evenement);
