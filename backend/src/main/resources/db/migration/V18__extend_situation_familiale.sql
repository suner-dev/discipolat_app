-- V18__extend_situation_familiale.sql
-- Espace Membre : ajout de 'PARENT_CELIBATAIRE' dans les valeurs autorisées
-- de situation_familiale (utilisateurs et âmes).

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_situation_familiale_check;
ALTER TABLE souls DROP CONSTRAINT IF EXISTS souls_situation_familiale_check;

ALTER TABLE users ADD CONSTRAINT users_situation_familiale_check
    CHECK (situation_familiale IN ('CELIBATAIRE', 'MARIE', 'DIVORCE', 'VEUF', 'PARENT_CELIBATAIRE', 'AUTRE'));

ALTER TABLE souls ADD CONSTRAINT souls_situation_familiale_check
    CHECK (situation_familiale IN ('CELIBATAIRE', 'MARIE', 'DIVORCE', 'VEUF', 'PARENT_CELIBATAIRE', 'AUTRE'));