-- ============================================================
-- V213 (PORTÉ de Develop1 V171)
-- §G6.4 — E2E CP2 : le CHECK de V154 sur membership.source refusait
-- les valeurs réellement écrites par le code :
--   'MANUEL'      → répertoire web (PeopleDirectoryPage, source=MANUEL)
--   'SELF_SIGNUP' → auto-inscription (AuthService.register → registerPerson)
-- Sur les bases de développement, la contrainte datait d'avant V154 et
-- n'avait jamais été re-vérifiée ; une installation vierge plantait en 500.
-- PeopleService normalise désormais la source contre cette liste (défaut
-- 'INSCRIPTION'), donc le CHECK reste une garantie de qualité de données.
-- ============================================================

ALTER TABLE membership DROP CONSTRAINT IF EXISTS membership_source_check;
ALTER TABLE membership ADD CONSTRAINT membership_source_check
    CHECK (source IN ('INSCRIPTION', 'INVITATION', 'IMPORT', 'EVANGELISATION', 'TRANSFERT', 'MANUEL', 'SELF_SIGNUP'));
