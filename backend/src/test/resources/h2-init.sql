-- H2 : compatibilité avec les colonnes jsonb des entités (production = PostgreSQL).
-- Le type jsonb n'existe pas nativement dans H2 ; on le déclare comme domaine JSON
-- afin que Hibernate puisse créer les tables (soul_history, audit_log, rapports…).
CREATE DOMAIN IF NOT EXISTS jsonb AS JSON;
