-- V12__add_soul_user_id.sql
-- Add optional user_id to souls table to link disciples to user accounts
-- Enables FAISEUR evaluation category (disciples can evaluate their maker)

ALTER TABLE souls ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id);

-- Also update the evaluations CHECK constraint to include FAISEUR
ALTER TABLE evaluations DROP CONSTRAINT IF EXISTS evaluations_categorie_check;
ALTER TABLE evaluations ADD CONSTRAINT evaluations_categorie_check
    CHECK (categorie IN ('RESPONSABLE', 'CHEF_FAMILLE', 'FAISEUR'));

CREATE INDEX IF NOT EXISTS idx_souls_user_id ON souls(user_id);
