-- V21: Premium features — CRM interactions, spiritual score history
-- ============================================================
-- INTERACTIONS CRM : chaque contact (appel, SMS, WhatsApp, email,
-- visite, réunion, conseil, prière, suivi) est historisé par âme.
-- ============================================================
CREATE TABLE IF NOT EXISTS soul_interactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    soul_id UUID NOT NULL REFERENCES souls(id) ON DELETE CASCADE,
    auteur_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL
        CHECK (type IN ('APPEL', 'SMS', 'WHATSAPP', 'EMAIL', 'VISITE', 'REUNION', 'PRIERE', 'CONSEIL', 'SUIVI', 'PROGRAMME')),
    canal VARCHAR(30),
    objet VARCHAR(200),
    contenu TEXT,
    date_interaction TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    a_faire_par UUID REFERENCES users(id) ON DELETE SET NULL,
    rappel_le TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_soul_interactions_soul ON soul_interactions(soul_id, date_interaction DESC);
CREATE INDEX IF NOT EXISTS idx_soul_interactions_auteur ON soul_interactions(auteur_id, date_interaction DESC);

-- ============================================================
-- HISTORIQUE DU SCORE SPIRITUEL : échantillonnage hebdomadaire
-- du score global par âme (pour les courbes d'évolution).
-- ============================================================
CREATE TABLE IF NOT EXISTS spiritual_score_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    soul_id UUID NOT NULL REFERENCES souls(id) ON DELETE CASCADE,
    semaine DATE NOT NULL,
    score_global INT NOT NULL,
    sante INT NOT NULL,
    fidelite INT NOT NULL,
    engagement INT NOT NULL,
    participation INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_spiritual_score_week UNIQUE (soul_id, semaine)
);

CREATE INDEX IF NOT EXISTS idx_spiritual_score_soul ON spiritual_score_history(soul_id, semaine);

ANALYZE;
