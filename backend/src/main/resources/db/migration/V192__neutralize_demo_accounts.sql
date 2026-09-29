-- V192 : neutralisation des 9 comptes de demonstration seeds (securite P0)
--
-- Constat verifie le 2026-09-29 (passe-plat Agent B,
-- reports/plan-2agents/HANDOVER-VERS-AGENT-A.md, section P0) :
--   * V2__seed_data.sql insere 9 utilisateurs 'ACTIVE' avec password_hash = 'PLACEHOLDER'
--     (pasteur@, responsable1/2@, chef1/2@, faiseur1..4@discipolat.com) ;
--   * V8__fix_password_hashes.sql remplace 'PLACEHOLDER' par le BCrypt (cout 10) de
--     « password123 », mot de passe ecrit en clair dans l'en-tete de ce fichier ;
--   * V70__add_multitenancy.sql rattache les utilisateurs sans tenant au tenant par defaut
--     (00000000-0000-0000-0000-000000000001), statut ACTIVE.
--   Aucune migration ulterieure ne neutralise ces comptes : sur toute base construite par
--   les migrations, AuthService.login accepte « password123 » et delivre un JWT real avec
--   un role PASTEUR / RESPONSABLE / FAISEUR. C'est un trou de securite, pas une demo.
--
-- Le runtime de demonstration, lui, est deja correctement garde (DataInitializer.run :
-- !isProductionEnvironment() && seedDemoAccounts, defaut DEMO_SEED_ENABLED:false) et
-- n'est PAS touche par la presente migration.
--
-- Regle 1 (« NE SUPPRIMER RIEN ») : aucune donnee n'est supprimee. Les comptes passent en
-- statut INACTIVE + deleted = true. AuthService rejette INACTIVE avant meme le controle de
-- statut du tenant (AuthService.java:140-141, BadCredentialsException). Les donnees metier
-- rattachees (âmes, familles, departments, historiques) restent intactes et reattribuables.
--
-- Le UPDATE est filtre sur le hash connu : si un compte portait un mot de passe change
-- (donnee reelle qui aurait reuse cet email), il n'est PAS touche — c'est delibere.

UPDATE users
   SET statut = 'INACTIVE',
       deleted = true,
       updated_at = CURRENT_TIMESTAMP
 WHERE email IN ('pasteur@discipolat.com', 'responsable1@discipolat.com',
                 'responsable2@discipolat.com', 'chef1@discipolat.com',
                 'chef2@discipolat.com', 'faiseur1@discipolat.com',
                 'faiseur2@discipolat.com', 'faiseur3@discipolat.com',
                 'faiseur4@discipolat.com')
   AND password_hash = '$2a$10$xf6qwOh4g8AidlGwgyD8S.Vbl7FVv3dNkO5GI7.iE/dgrveA5/j..';

-- Filet de securite fail-closed (deliberement bloquant) : si un compte @discipolat.com est
-- encore ACTIVE et non supprime apres neutralisation, quelqu'un a soit change son hash,
-- soit cree un vrai compte sur ce domaine. Dans les deux cas la decision est HUMAINE :
-- la migration refuse d'echouer en silence et refuse de desactiver une donnee reelle.
DO $$
DECLARE remaining int;
BEGIN
    SELECT count(*) INTO remaining FROM users
     WHERE email LIKE '%@discipolat.com' AND statut = 'ACTIVE' AND deleted = false;
    IF remaining > 0 THEN
        RAISE EXCEPTION 'V192: % compte(s) @discipolat.com encore actif(s) — dedoublonnage/decision manuelle requise (ne pas desactiver une donnee reelle en silence)', remaining;
    END IF;
END $$;
