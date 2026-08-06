-- V29__family_risk_level.sql
-- Système "Famille à risque" :
-- Le pasteur peut définir un niveau : NORMAL / SOUS_SURVEILLANCE / A_RISQUE
-- Le système calcule automatiquement un indice de risque (taux de présence,
-- âmes perdues, croissance, stagnation, absences, litiges, retards).
-- Un historique complet des changements de niveau est conservé.

-- ============================================================
-- 1. FAMILIES : Ajouter la colonne niveau_risque
-- ============================================================

ALTER TABLE families ADD COLUMN IF NOT EXISTS niveau_risque VARCHAR(30) NOT NULL DEFAULT 'NORMAL';
CREATE INDEX IF NOT EXISTS idx_families_niveau_risque ON families(niveau_risque);

-- ============================================================
-- 2. FAMILY_RISK_HISTORY : Historique des changements de niveau
-- ============================================================

CREATE TABLE IF NOT EXISTS family_risk_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    family_id UUID NOT NULL,
    ancien_niveau VARCHAR(30),
    nouveau_niveau VARCHAR(30) NOT NULL,
    score_risque INTEGER,
    changed_by UUID,
    raison TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_family_risk_history_family ON family_risk_history(family_id);
CREATE INDEX IF NOT EXISTS idx_family_risk_history_changed ON family_risk_history(changed_by);

COMMENT ON TABLE family_risk_history IS 'Historique des changements de niveau de risque des familles (NORMAL / SOUS_SURVEILLANCE / A_RISQUE).';
COMMENT ON COLUMN families.niveau_risque IS 'Niveau de risque actuel de la famille : NORMAL, SOUS_SURVEILLANCE ou A_RISQUE.';
