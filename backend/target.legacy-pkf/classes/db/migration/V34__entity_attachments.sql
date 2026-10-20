-- V34__entity_attachments.sql
-- ============================================================
-- PIECES JOINTES GENERIQUES (module fichiers)
-- Table de liaison reutilisable par tous les formulaires metier :
--   * MAKER_REPORT   : rapport hebdomadaire d'un faiseur (par âme)
--   * FAMILY_REPORT  : rapport hebdomadaire d'une famille
--   * MEMBER_REQUEST : demande membre (suggestion / rendez-vous / signalement)
--   * EVENT          : evenement
-- Meme pattern que transfer_attachments (V32) mais avec un type d'entite,
-- pour eviter de multiplier les tables de liaison par module.
-- ============================================================
CREATE TABLE IF NOT EXISTS entity_attachments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL
        CHECK (entity_type IN ('MAKER_REPORT', 'FAMILY_REPORT', 'MEMBER_REQUEST', 'EVENT')),
    entity_id UUID NOT NULL,
    file_id UUID NOT NULL REFERENCES files(id),
    uploaded_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_entity_attachments_entity ON entity_attachments(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_attachments_file ON entity_attachments(file_id);

COMMENT ON TABLE entity_attachments IS 'Pieces jointes generiques (module fichiers) liees a une entite metier : rapports faiseur/famille, demandes membres, evenements.';
