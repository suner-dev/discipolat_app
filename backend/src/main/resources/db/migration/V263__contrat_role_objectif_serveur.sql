-- V263__contrat_role_objectif_serveur.sql
-- P7a — Contrat de rôle des objectifs élargi au catalogue de rôles servi.
--
-- Historique : V22 figeait objectives.role par un CHECK en dur sur quatre
-- noms de l'enum socle UserRole ('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE',
-- 'FAISEUR'), et l'entité JPA la stockait en @Enumerated — impossible pour
-- une église de cibler un objectif sur un rôle custom du catalogue servi
-- (GET /admin/roles/overview), et crash 500 (Enum.valueOf) sur un rôle
-- actif custom dans la progression. Le contrat devient une clé de rôle en
-- texte : nom du rôle socle OU clé du catalogue du tenant, validé côté
-- service contre l'union enum ∪ catalogue (union additive uniquement,
-- aucune restriction nouvelle — les 8 objectifs par défaut de V22 restent
-- conformes au nouveau motif).

-- 1. Largeur alignée sur roles.key VARCHAR(50).
ALTER TABLE objectives ALTER COLUMN role TYPE VARCHAR(50);

-- 2. CHECK sémantique (liste fermée) → CHECK syntaxique (motif d'identifiant
--    de clé de rôle, compatible avec les clés du catalogue et les noms de
--    l'enum socle).
ALTER TABLE objectives DROP CONSTRAINT IF EXISTS ck_objective_role;
ALTER TABLE objectives ADD CONSTRAINT ck_objective_role CHECK (role ~ '^[A-Z][A-Z0-9_]*$');
