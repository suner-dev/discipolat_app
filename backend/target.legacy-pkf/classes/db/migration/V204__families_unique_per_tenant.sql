-- V204__families_unique_per_tenant.sql
-- ============================================================
-- ARBITRAGE D4 (orchestrateur, 2026-09-30) : « oui, corriger uk_families_nom ».
--
-- Constat Prouvé sur PostgreSQL 16 réel (§5.5, NEED-HELP D4) : V6 pose
-- UNIQUE (nom) MONDIAL sur families, à l'époque mono-tenant. V70 a rendu la
-- table possédée par tenant ; deux églises ne peuvent donc pas avoir une
-- famille du même nom — le second INSERT lève « duplicate key uk_families_nom ».
-- Exactement la classe du défaut dictionnaires corrigé par V193 (et masqué
-- jusqu'ici par la recette, qui utilise des noms à suffixe slug distincts).
--
-- Correction : même patron que V193 (et V70 uk_users_tenant_email) —
-- l'unicité devient (tenant_id, nom).
--
-- Sans perte de données : l'ancienne contrainte, plus stricte, garantissait
-- l'absence de doublons mondiaux — donc a fortiori l'absence de doublons
-- par tenant. Le re-scopage ne peut créer aucun conflit et ne nécessite
-- aucun nettoyage préalable.
--
-- Le nom du champ côté entité (Family.nom, unicité non déclarée en Java)
-- n'est pas affecté ; la suite H2 (ddl-auto) ne reproduisait déjà pas la
-- contrainte — c'est le gate Flyway/Testcontainers qui verrouille le
-- comportement, avec son test discriminant (deux tenants, même nom accepté ;
-- même tenant, même nom refusé).
-- ============================================================

ALTER TABLE families DROP CONSTRAINT IF EXISTS uk_families_nom;

CREATE UNIQUE INDEX IF NOT EXISTS uk_families_nom_per_tenant
    ON families (tenant_id, nom);

COMMENT ON INDEX uk_families_nom_per_tenant IS
    'Unicité par tenant : chaque église possède ses familles ; remplace uk_families_nom mondial de V6, faux depuis la multi-tenantisation V70 (V204, arbitrage D4).';
