-- V34__entity_attachments.sql
-- ============================================================
-- PIÈCES JOINTES GÉNÉRIQUES (module fichiers)
-- Table de liaison réutilisable par tous les formulaires métier :
--   * MAKER_REPORT   : rapport hebdomadaire d'un faiseur (par âme)
--   * FAMILY_REPORT  : rapport hebdomadaire d'une famille
--   * MEMBER_REQUEST : demande membre (suggestion / rendez-vous / signalement)
--   * EVENT          : événement
-- Même pattern que transfer_attachments (V32) mais avec un type d'entité,
-- pour éviter de multiplier les tables de liaison par module.
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

COMMENT ON TABLE entity_attachments IS 'Pièces jointes génériques (module fichiers) liées à une entité métier : rapports faiseur/famille, demandes membres, événements.';
