-- V193__dictionary_entries_unique_per_tenant.sql
-- ============================================================
-- UNICITÉ DES DICTIONNAIRES RE-SCOPÉE PAR TENANT (correctif de dérive
-- multi-tenant, découvert le 2026-09-29 par le replay de la recette
-- verify-tenant-onboarding.sh sur PostgreSQL 16 réel — TODO reprise §5.5).
--
-- Constat : V42 crée dictionary_entries avec UNIQUE (dict_key, code) GLOBAL,
-- à l'époque où la table était unique pour toute la plateforme. V70 a rendu la
-- table possédée par tenant (tenant_id NOT NULL, rattachement au tenant par
-- défaut), et le seed de copie par tenant (A3 item 10,
-- DictionaryService.seedForTenant appelé par TenantService.create) insère DONC
-- les mêmes (dict_key, code) pour chaque nouveau tenant : la création d'un
-- SECOND tenant échouait en 500 « duplicate key uq_dict_code
-- (EVENT_TYPE, SORTIE) » dès l'instant où un premier tenant avait seedé ses
-- entrées. La suite de tests ne le voyait pas : profil H2 avec
-- spring.flyway.enabled:false et ddl-auto (create-drop), donc la contrainte
-- réelle n'y est jamais appliquée — c'est précisément la classe de bug que le
-- gate Flyway/Testcontainers (V1..V192 sur PG réel) et la recette PG existent
-- pour attraper.
--
-- Correction : même patron que V70 pour users (uk_users_tenant_email),
-- précédents V115/V158 pour les DROP de contraintes devenues fausses.
-- Sans perte de données : l'ancienne contrainte, plus stricte, garantissait
-- l'absence de doublons mondiaux — donc a fortiori l'absence de doublons
-- (tenant_id, dict_key, code) ; le passage à la portée par tenant ne peut
-- créer aucun conflit et ne nécessite aucun nettoyage préalable.
-- ============================================================

ALTER TABLE dictionary_entries DROP CONSTRAINT IF EXISTS uq_dict_code;

CREATE UNIQUE INDEX IF NOT EXISTS uq_dict_code_per_tenant
    ON dictionary_entries (tenant_id, dict_key, code);

COMMENT ON INDEX uq_dict_code_per_tenant IS
    'Unicité par tenant : chaque église possède sa copie éditable des dictionnaires (V70 + A3 item 10) ; remplace uq_dict_code global de V42, faux depuis la multi-tenantisation (V193).';
