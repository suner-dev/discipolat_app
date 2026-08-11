-- ============================================================
-- seed-demo.sql — Jeu de données de DÉMONSTRATION (bêta-testing)
-- ============================================================
-- Restauré par POST /api/v1/admin/beta/reset (BetaResetService).
-- Données 100 % fictives, compatibles avec le schéma ACTUEL.
-- Les mots de passe (PLACEHOLDER) sont remplacés par 'password123'
-- par DataInitializer (activé uniquement sur le profil bêta).
-- Les comptes de démonstration principaux (admin, pasteur,
-- responsable, chef, faiseur, membre, paul) sont créés par
-- DataInitializer ; ce script fournit l'écosystème (responsables,
-- chefs, faiseurs, âmes, familles, départements, rapports, alertes).
--
-- ⚠️ N'EST PAS une migration Flyway : il reflète le schéma actuel
-- (V41). En cas d'évolution de schéma, mettre ce fichier à jour.
-- ============================================================

-- ============================================================
-- USERS (identifiants stables — référencés par DataInitializer)
-- ============================================================
INSERT INTO users (id, email, password_hash, first_name, last_name, role, est_chef_de_famille, statut, created_at, updated_at)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'pasteur@discipolat.com', 'PLACEHOLDER', 'Pierre', 'Apôtre', 'PASTEUR', false, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000002', 'responsable1@discipolat.com', 'PLACEHOLDER', 'Paul', 'Tarsien', 'RESPONSABLE', false, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000003', 'responsable2@discipolat.com', 'PLACEHOLDER', 'Jean', 'Baptiste', 'RESPONSABLE', false, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000004', 'chef1@discipolat.com', 'PLACEHOLDER', 'Timothée', 'Fils', 'FAISEUR', true, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000005', 'chef2@discipolat.com', 'PLACEHOLDER', 'Tite', 'Fidèle', 'FAISEUR', true, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000006', 'faiseur1@discipolat.com', 'PLACEHOLDER', 'Luc', 'Médecin', 'FAISEUR', false, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000007', 'faiseur2@discipolat.com', 'PLACEHOLDER', 'Marc', 'Évangéliste', 'FAISEUR', false, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000008', 'faiseur3@discipolat.com', 'PLACEHOLDER', 'Silas', 'Compagnon', 'FAISEUR', false, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000009', 'faiseur4@discipolat.com', 'PLACEHOLDER', 'Barnabas', 'Consolateur', 'FAISEUR', false, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- DEPARTMENTS (2 départements + 2 ministères)
-- ============================================================
INSERT INTO departments (id, nom, description, responsable_id, statut, created_at, updated_at)
VALUES
    ('b0000000-0000-0000-0000-000000000001', 'Département Jeunesse', 'Suivi des jeunes disciples de 15-25 ans', 'a0000000-0000-0000-0000-000000000002', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000002', 'Département Adultes', 'Suivi des disciples adultes', 'a0000000-0000-0000-0000-000000000003', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000003', 'Chorale', 'Ministère de louange et de musique', 'a0000000-0000-0000-0000-000000000002', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000004', 'Audiovisuel', 'Son, image et retransmission des cultes', 'a0000000-0000-0000-0000-000000000003', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- FAMILIES (chef + familles gérées)
-- ============================================================
INSERT INTO families (id, nom, chef_famille_id, date_creation, statut, created_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'Famille Timothée', 'a0000000-0000-0000-0000-000000000004', CURRENT_DATE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000002', 'Famille Tite', 'a0000000-0000-0000-0000-000000000005', CURRENT_DATE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000003', 'Famille Fidèle', 'a0000000-0000-0000-0000-000000000004', CURRENT_DATE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000004', 'Famille Espérance', 'a0000000-0000-0000-0000-000000000005', CURRENT_DATE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Chefs de famille → familles gérées
UPDATE users SET famille_geree_id = 'c0000000-0000-0000-0000-000000000001' WHERE id = 'a0000000-0000-0000-0000-000000000004';
UPDATE users SET famille_geree_id = 'c0000000-0000-0000-0000-000000000002' WHERE id = 'a0000000-0000-0000-0000-000000000005';

-- ============================================================
-- SOULS (âmes / disciples — données fictives)
-- ============================================================
INSERT INTO souls (id, nom, prenom, email, telephone, type_disciple, date_integration, statut, faiseur_id, famille_id, created_at, updated_at)
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'Dupont', 'Marie', 'marie.dupont@email.com', '0612345678', 'NOUVEAU_CONVERTI', '2026-01-15', 'ACTIF', 'a0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000002', 'Martin', 'Jean', 'jean.martin@email.com', '0623456789', 'NOUVEL_ARRIVANT', '2026-02-01', 'EN_INTEGRATION', 'a0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000003', 'Bernard', 'Sophie', 'sophie.bernard@email.com', '0634567890', 'NOUVEAU_CONVERTI', '2026-03-10', 'ACTIF', 'a0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000004', 'Petit', 'Pierre', 'pierre.petit@email.com', '0645678901', 'NOUVEL_ARRIVANT', '2026-03-20', 'EN_INTEGRATION', 'a0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000005', 'Robert', 'Anne', 'anne.robert@email.com', '0656789012', 'NOUVEAU_CONVERTI', '2026-04-05', 'ACTIF', 'a0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000006', 'Richard', 'Paul', 'paul.richard@email.com', '0667890123', 'NOUVEL_ARRIVANT', '2026-04-15', 'EN_INTEGRATION', 'a0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000007', 'Durand', 'Claire', 'claire.durand@email.com', '0678901234', 'NOUVEAU_CONVERTI', '2026-05-01', 'EN_VEILLE', 'a0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000008', 'Leroy', 'Thomas', 'thomas.leroy@email.com', '0689012345', 'NOUVEL_ARRIVANT', '2026-05-10', 'EN_INTEGRATION', 'a0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000009', 'Moreau', 'Sarah', 'sarah.moreau@email.com', '0690123456', 'NOUVEAU_CONVERTI', '2026-05-20', 'ACTIF', 'a0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000010', 'Simon', 'David', 'david.simon@email.com', '0601234567', 'NOUVEL_ARRIVANT', '2026-06-01', 'EN_INTEGRATION', 'a0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- MAKER REPORT (exemple soumis)
-- ============================================================
INSERT INTO maker_reports (id, faiseur_id, ame_id, semaine, presences_par_culte, soumis, date_soumission, created_at, updated_at)
VALUES
    ('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000001', CURRENT_DATE, '{"Dimanche Matin": true, "Mercredi Soir": true, "Vendredi Soir": false}'::jsonb, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- ALERTE (exemple actif)
-- ============================================================
INSERT INTO alerts (id, ame_id, faiseur_id, type_alerte, titre, message, date_declenchement, statut, cible, priorite, created_at, updated_at)
VALUES
    ('f0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000007', 'ABSENCE_48H', 'Absence prolongée', 'Claire Durand - Absence prolongée détectée (plus de 48h sans contact)', CURRENT_TIMESTAMP - INTERVAL '2 days', 'ACTIVE', 'PERSONNE', 'HAUTE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- FAMILY REPORT (exemple brouillon)
-- ============================================================
INSERT INTO family_reports (id, famille_id, chef_famille_id, semaine, presence_moyenne, total_presents, total_absents, total_sorties, total_maintenus, statut_validation, created_at, updated_at)
VALUES
    ('f0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004', CURRENT_DATE, 75.00, 6, 2, 0, 8, 'BROUILLON', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
