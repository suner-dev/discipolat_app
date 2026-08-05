-- ============================================================
-- V25 : Système de rendez-vous (prise de RDV, validation, rappels)
-- ============================================================

CREATE TABLE appointments (
    id               UUID PRIMARY KEY,
    demandeur_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recepteur_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    motif            VARCHAR(80) NOT NULL,        -- CONSEIL | CONFESSION | SUIVI | FORMATION | AUTRE
    objet            TEXT,
    date_prevue      TIMESTAMP NOT NULL,
    duree_minutes    INTEGER NOT NULL DEFAULT 30,
    statut           VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    reponse          TEXT,
    traite_par       UUID,
    date_traitement  TIMESTAMP,
    rappel_envoye    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_appointment_statut CHECK (statut IN ('EN_ATTENTE', 'CONFIRME', 'REFUSE', 'ANNULE', 'TERMINE')),
    CONSTRAINT ck_appointment_motif CHECK (motif IN ('CONSEIL', 'CONFESSION', 'SUIVI', 'FORMATION', 'AUTRE'))
);

CREATE INDEX idx_appointments_demandeur ON appointments (demandeur_id);
CREATE INDEX idx_appointments_recepteur ON appointments (recepteur_id);
CREATE INDEX idx_appointments_statut ON appointments (statut);
