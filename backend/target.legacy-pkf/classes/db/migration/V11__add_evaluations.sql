-- V11__add_evaluations.sql
-- Phase 8: Anonymous evaluations system
-- Responsables evaluated by department members
-- Chefs de famille evaluated by their faiseurs
-- Faiseurs evaluated by their disciples

CREATE TABLE IF NOT EXISTS evaluations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    evalue_id UUID NOT NULL REFERENCES users(id),
    evaluateur_id UUID NOT NULL REFERENCES users(id),
    categorie VARCHAR(30) NOT NULL CHECK (categorie IN ('RESPONSABLE', 'CHEF_FAMILLE')),
    note INTEGER NOT NULL CHECK (note >= 1 AND note <= 5),
    commentaire TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(evaluateur_id, evalue_id, categorie)
);

CREATE INDEX IF NOT EXISTS idx_evaluations_evalue ON evaluations(evalue_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_categorie ON evaluations(categorie);
CREATE INDEX IF NOT EXISTS idx_evaluations_evaluateur ON evaluations(evaluateur_id);
