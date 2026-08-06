-- ============================================================
-- SEED DATA ENRICHI — Discipolat (final corrected version)
-- ============================================================

-- ============================================================
-- 1. VISITES
-- ============================================================
INSERT INTO visits (id, soul_id, visiteur_id, date_prevue, date_realisee, statut, motif, objectif, compte_rendu, present, created_at, updated_at) VALUES
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000006', '2026-07-15', '2026-07-15', 'TERMINEE', 'PASTORALE', 'Suivi spirituel', 'Bonne séance.', true, NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000006', '2026-07-20', '2026-07-20', 'TERMINEE', 'FORMATION', 'Formation biblique', 'Discussion Romains.', true, NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000007', '2026-07-22', '2026-07-22', 'TERMINEE', 'PASTORALE', 'Visite bienvenue', 'Nouveau disciple.', true, NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000007', '2026-07-25', NULL, 'PLANIFIEE', 'EVANGELISATION', 'Premier contact', NULL, NULL, NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000008', '2026-07-10', '2026-07-10', 'TERMINEE', 'SUIVI', 'Post-baptême', 'Suivi rassurant.', true, NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000008', '2026-08-01', NULL, 'PLANIFIEE', 'PASTORALE', 'Encouragement', NULL, NULL, NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000006', '2026-07-05', '2026-07-05', 'TERMINEE', 'FORMATION', 'Cours leadership', 'Leader naturel.', true, NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000009', '2026-07-28', NULL, 'PLANIFIEE', 'EVANGELISATION', 'Invitation culte', NULL, NULL, NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000009', '2026-07-18', '2026-07-18', 'TERMINEE', 'PASTORALE', 'Réconciliation', 'Conflit résolu.', true, NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000006', '2026-08-03', NULL, 'PLANIFIEE', 'FORMATION', 'Préparation service', NULL, NULL, NOW(), NOW());

-- ============================================================
-- 2. CONVERSATIONS & MESSAGES
-- ============================================================
INSERT INTO conversations (id, user_a_id, user_b_id, last_message_at, last_message, last_message_sender_id, created_at) VALUES
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000006', NOW() - INTERVAL '1 hour', 'Merci pour le rapport', 'a0000000-0000-0000-0000-000000000006', NOW() - INTERVAL '2 hours'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000006', NOW() - INTERVAL '3 hours', 'Présences à jour', 'a0000000-0000-0000-0000-000000000002', NOW() - INTERVAL '5 hours'),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'a0000000-0000-0000-0000-000000000006', NOW() - INTERVAL '2 hours', 'Absent dimanche', '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', NOW() - INTERVAL '4 hours'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', NOW() - INTERVAL '6 hours', 'Rapport trimestriel', 'a0000000-0000-0000-0000-000000000002', NOW() - INTERVAL '1 day'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000007', NOW() - INTERVAL '12 hours', 'Aide groupe prière', 'a0000000-0000-0000-0000-000000000006', NOW() - INTERVAL '1 day');

-- ============================================================
-- 3. PRIÈRES (10 prières)
-- ============================================================
INSERT INTO prayers (id, auteur_id, titre, description, categorie, priorite, statut, visibilite, created_at, updated_at) VALUES
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'Guérison mère', 'Mère hospitalisée.', 'SANTE', 'HAUTE', 'EN_COURS', 'PARTAGEE', NOW() - INTERVAL '5 days', NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000006', 'Sagesse décisions', 'Besoin sagesse.', 'SPIRITUEL', 'MOYENNE', 'EN_COURS', 'PARTAGEE', NOW() - INTERVAL '3 days', NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000007', 'Protection voyageurs', 'Membres voyagent.', 'FAMILLE', 'HAUTE', 'EN_COURS', 'PARTAGEE', NOW() - INTERVAL '2 days', NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000008', 'Réveil spirituel', 'Réveil église.', 'SPIRITUEL', 'HAUTE', 'EN_COURS', 'PARTAGEE', NOW() - INTERVAL '7 days', NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000009', 'Emploi Paul', 'Paul cherche travail.', 'TRAVAIL', 'HAUTE', 'EN_COURS', 'PARTAGEE', NOW() - INTERVAL '10 days', NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000006', 'Études jeunes', 'Jeunes aux examens.', 'AUTRE', 'MOYENNE', 'EXAUCE', 'PARTAGEE', NOW() - INTERVAL '15 days', NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000007', 'Réconciliation', 'Conflit familial.', 'FAMILLE', 'HAUTE', 'EN_COURS', 'PRIVEE', NOW() - INTERVAL '4 days', NOW()),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'Paix intérieure', 'Besoin paix Dieu.', 'SPIRITUEL', 'MOYENNE', 'EN_COURS', 'PARTAGEE', NOW() - INTERVAL '1 day', NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000008', 'Projet communautaire', 'Aide sans-abris.', 'AUTRE', 'MOYENNE', 'EN_COURS', 'PARTAGEE', NOW() - INTERVAL '6 days', NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000009', 'Délivrance', 'Personne opprimée.', 'SPIRITUEL', 'HAUTE', 'EXAUCE', 'PARTAGEE', NOW() - INTERVAL '20 days', NOW());

-- ============================================================
-- 4. ÉVÉNEMENTS (8 événements)
-- ============================================================
INSERT INTO events (id, organisateur_id, type_evenement, titre, description, lieu, date_debut, date_fin, limite_places, nb_inscrits, statut, created_at, updated_at) VALUES
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'REUNION', 'Culte de rentrée', 'Premier culte.', 'Église', '2026-09-07 09:00:00', '2026-09-07 12:00:00', 200, 85, 'PLANIFIE', NOW(), NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000002', 'REUNION', 'Réunion responsables', 'Point mensuel.', 'Salle A', '2026-08-10 18:00:00', '2026-08-10 20:00:00', 20, 12, 'PLANIFIE', NOW(), NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'FORMATION', 'Séminaire', 'Formation leaders.', 'Centre', '2026-08-15 08:00:00', '2026-08-17 17:00:00', 50, 32, 'PLANIFIE', NOW(), NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000006', 'VISITE', 'Visite malades', 'Hôpital.', 'Hôpital', '2026-08-05 14:00:00', '2026-08-05 17:00:00', 15, 8, 'TERMINE', NOW(), NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'EVANGELISATION', 'Campagne', '3 jours évangélisation.', 'Marché', '2026-08-20 08:00:00', '2026-08-22 18:00:00', 100, 45, 'PLANIFIE', NOW(), NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000007', 'ANNIVERSAIRE', 'Sport', 'Football familles.', 'Stade', '2026-08-25 08:00:00', '2026-08-25 18:00:00', 60, 28, 'PLANIFIE', NOW(), NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'ANNIVERSAIRE', 'Anniversaire église', '15 ans.', 'Église', '2026-09-15 09:00:00', '2026-09-15 13:00:00', 300, 120, 'PLANIFIE', NOW(), NOW()),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000008', 'FORMATION', 'Atelier prière', 'Prier en groupe.', 'Salle prière', '2026-08-08 15:00:00', '2026-08-08 17:00:00', 25, 18, 'TERMINE', NOW(), NOW());

-- ============================================================
-- 5. PIPELINE ÉVANGÉLISATION (6 pipelines) — schema: soul_id, etape, date_etape, cree_par
-- ============================================================
INSERT INTO evangelism_track (id, soul_id, etape, date_etape, note, cree_par, cree_le, maj_le) VALUES
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', 'DISCIPOLAT', '2026-06-20', 'Disciple confirmé.', 'a0000000-0000-0000-0000-000000000006', NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000003', 'SUIVI', '2026-05-15', 'En suivi.', 'a0000000-0000-0000-0000-000000000007', NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000005', 'LEADER', '2026-07-01', 'Promu leader.', 'a0000000-0000-0000-0000-000000000008', NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000008', 'VISITE', '2026-07-10', 'Première visite.', 'a0000000-0000-0000-0000-000000000009', NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000010', 'INVITATION', '2026-06-01', 'Invité au culte.', 'a0000000-0000-0000-0000-000000000006', NOW(), NOW()),
(uuid_generate_v4(), '5a3cdd65-619d-4c3b-8cf7-4725a6e59fd4', 'CONTACT', '2026-07-20', 'Premier contact.', 'a0000000-0000-0000-0000-000000000006', NOW(), NOW());

-- ============================================================
-- 6. SCORES SPIRITUELS (12 entrées)
-- ============================================================
INSERT INTO spiritual_score_history (id, soul_id, semaine, score_global, sante, fidelite, engagement, participation, created_at) VALUES
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', '2026-01-05', 75, 80, 70, 75, 75, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', '2026-02-02', 82, 85, 78, 83, 82, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', '2026-03-02', 88, 90, 85, 90, 88, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', '2026-04-06', 85, 82, 88, 86, 85, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', '2026-05-04', 90, 92, 88, 91, 90, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000002', '2026-01-05', 60, 55, 50, 65, 70, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000002', '2026-02-02', 68, 70, 65, 70, 68, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000002', '2026-03-02', 72, 75, 70, 72, 72, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000003', '2026-04-06', 45, 40, 50, 45, 45, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000003', '2026-05-04', 55, 60, 55, 50, 55, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000005', '2026-05-04', 92, 95, 90, 92, 92, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000007', '2026-06-01', 88, 90, 85, 88, 88, NOW());

-- ============================================================
-- 7. NOTES D'ÂMES (8 notes)
-- ============================================================
INSERT INTO soul_notes (id, ame_id, auteur_id, contenu, created_at, updated_at) VALUES
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000006', 'Excellente progression prière.', NOW() - INTERVAL '2 days', NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000006', 'Besoin accompagnement renforcé.', NOW() - INTERVAL '5 days', NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000007', 'Très impliqué Accueil.', NOW() - INTERVAL '1 day', NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000008', 'Leader naturel, à former.', NOW() - INTERVAL '3 days', NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000006', 'Terminé formation leadership.', NOW() - INTERVAL '7 days', NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000009', 'Nouveau, patience.', NOW() - INTERVAL '4 days', NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000009', 'Épreuve, consoler.', NOW() - INTERVAL '6 days', NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000006', 'Progresse bien, suivre.', NOW() - INTERVAL '2 days', NOW());

-- ============================================================
-- 8. USER BADGES (10 badges) — skip duplicates
-- ============================================================
INSERT INTO user_badges (id, user_id, badge_id, earned_at)
SELECT uuid_generate_v4(), u.id, b.id, NOW() - (random() * interval '60 days')
FROM (VALUES
  ('a0000000-0000-0000-0000-000000000006'),
  ('a0000000-0000-0000-0000-000000000007'),
  ('a0000000-0000-0000-0000-000000000008'),
  ('a0000000-0000-0000-0000-000000000009'),
  ('2cce3c59-629d-4a05-80ec-3857c0a3f3ab')
) AS u(id)
CROSS JOIN (SELECT id FROM badges ORDER BY random() LIMIT 1) AS b
WHERE NOT EXISTS (SELECT 1 FROM user_badges WHERE user_id = u.id AND badge_id = b.id);

-- ============================================================
-- 9. RENDEZ-VOUS (5 RDV)
-- ============================================================
INSERT INTO appointments (id, demandeur_id, recepteur_id, motif, objet, date_prevue, duree_minutes, statut, reponse, date_traitement, rappel_envoye, created_at) VALUES
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'a0000000-0000-0000-0000-000000000001', 'SUIVI', 'Suivi spirituel', '2026-08-10 10:00:00', 45, 'CONFIRME', 'Bienvenu.', '2026-08-01 14:00:00', false, NOW() - INTERVAL '5 days'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000002', 'FORMATION', 'Formation leaders', '2026-08-12 14:00:00', 60, 'CONFIRME', 'Salle B.', '2026-08-02 09:00:00', false, NOW() - INTERVAL '4 days'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000001', 'CONSEIL', 'Conseil projet', '2026-08-15 09:00:00', 30, 'EN_ATTENTE', NULL, NULL, false, NOW() - INTERVAL '2 days'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000002', 'SUIVI', 'Point Jeunesse', '2026-08-08 16:00:00', 30, 'TERMINE', 'Excellent.', '2026-08-08 16:30:00', true, NOW() - INTERVAL '10 days'),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'a0000000-0000-0000-0000-000000000006', 'CONSEIL', 'Orientation', '2026-08-20 11:00:00', 45, 'EN_ATTENTE', NULL, NULL, false, NOW() - INTERVAL '1 day');

-- ============================================================
-- 10. NOTIFICATIONS (15 notifications)
-- ============================================================
INSERT INTO notifications (id, destinataire_id, type, canal, titre, message, lu, entite_reference_id, entite_reference_type, created_at) VALUES
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000006', 'RAPPORT_NON_SOUMIS', 'EMAIL', 'Rapport', 'Envoyez rapport.', false, NULL, 'RAPPORT', NOW() - INTERVAL '1 hour'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'ALERTE_ABSENCE', 'PUSH', 'Demande', 'Nouvelle demande.', false, NULL, 'MEMBER_REQUEST', NOW() - INTERVAL '2 hours'),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'INFORMATION', 'EMAIL', 'Message', 'Message faiseur.', false, NULL, 'MESSAGE', NOW() - INTERVAL '3 hours'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000002', 'INFORMATION', 'PUSH', 'Réunion', 'Réunion demain 18h.', false, NULL, 'EVENT', NOW() - INTERVAL '5 hours'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000006', 'RAPPORT_NON_SOUMIS', 'EMAIL', 'Visite', 'Visite le 25.', false, NULL, 'VISIT', NOW() - INTERVAL '6 hours'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'ALERTE_ABSENCE', 'PUSH', 'Absence', 'Disciple absent 3 cultes.', false, NULL, 'ALERT', NOW() - INTERVAL '8 hours'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000007', 'INFORMATION', 'EMAIL', 'Formation', 'Séminaire 15 août.', false, NULL, 'EVENT', NOW() - INTERVAL '10 hours'),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'INFORMATION', 'PUSH', 'Événement', 'Culte rentrée créé.', false, NULL, 'EVENT', NOW() - INTERVAL '12 hours'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000008', 'INFORMATION', 'PUSH', 'Badge', 'Badge ASSIDU.', false, NULL, 'BADGE', NOW() - INTERVAL '1 day'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000006', 'INFORMATION', 'EMAIL', 'Réponse', 'Réponse pasteur.', false, NULL, 'MESSAGE', NOW() - INTERVAL '1 day'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000009', 'RAPPORT_FAMILLE_NON_SOUMIS', 'EMAIL', 'Objectif', 'Objectif en retard.', false, NULL, 'OBJECTIVE', NOW() - INTERVAL '2 days'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'ALERTE_ABSENCE', 'PUSH', 'Rapport', 'Cas exceptionnel.', false, NULL, 'MAKER_REPORT', NOW() - INTERVAL '2 days'),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'INFORMATION', 'EMAIL', 'RDV', 'RDV confirmé.', false, NULL, 'APPOINTMENT', NOW() - INTERVAL '3 days'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000002', 'INFORMATION', 'PUSH', 'Programme', 'Programme modifié.', false, NULL, 'EVENT', NOW() - INTERVAL '4 days'),
(uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000006', 'INFORMATION', 'PUSH', 'Disciple', 'Nouveau disciple.', false, NULL, 'SOUL', NOW() - INTERVAL '5 days');

-- ============================================================
-- 11. ÉVALUATIONS (6 évaluations) — categories: RESPONSABLE, CHEF_FAMILLE, FAISEUR
-- ============================================================
INSERT INTO evaluations (id, evalue_id, evaluateur_id, categorie, note, commentaire, created_at, updated_at) VALUES
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000006', 'FAISEUR', 4, 'Excellente assiduité.', NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000006', 'CHEF_FAMILLE', 4, 'Bonne croissance.', NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000007', 'FAISEUR', 3, 'Retards fréquents.', NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000008', 'RESPONSABLE', 5, 'Leader exemplaire.', NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000006', 'FAISEUR', 3, 'En progrès.', NOW(), NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000009', 'CHEF_FAMILLE', 2, 'Nouveau, irrégulier.', NOW(), NOW());

-- ============================================================
-- 12. SUIVIS PARALLÈLES (4 suivis) — raison: TRANSFERT_EN_COURS, RENFORT, VISITE, REPRISE_CONTACT, AUTRE
-- ============================================================
INSERT INTO parallel_followups (id, ame_id, initiateur_id, raison, raison_detail, statut, date_debut, date_fin, created_at) VALUES
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000008', 'REPRISE_CONTACT', 'Post-baptême.', 'EN_COURS', '2026-07-01', NULL, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000009', 'RENFORT', 'Réconciliation.', 'EN_COURS', '2026-06-15', NULL, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000006', 'VISITE', 'Formation leadership.', 'EN_COURS', '2026-05-01', NULL, NOW()),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000006', 'AUTRE', 'Retour foi.', 'CLOTURE', '2026-03-01', '2026-06-01', NOW());

-- ============================================================
-- 13. DEMANDES MEMBRES (4 demandes)
-- ============================================================
INSERT INTO member_requests (id, user_id, type, cible, message, statut, department_id, family_id, created_at) VALUES
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'SUGGESTION', 'PASTEUR', 'Retraite spirituelle annuelle.', 'OUVERT', NULL, NULL, NOW() - INTERVAL '3 days'),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'RENDEZ_VOUS', 'PASTEUR', 'Rencontre engagement.', 'RESOLU', NULL, NULL, NOW() - INTERVAL '7 days'),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'SIGNALEMENT', 'RESPONSABLE', 'Difficulté financière.', 'EN_COURS', 'b0000000-0000-0000-0000-000000000002', NULL, NOW() - INTERVAL '5 days'),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'SUGGESTION', 'CHEF_DE_FAMILLE', 'Activité août.', 'OUVERT', NULL, 'c0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '1 day');

-- ============================================================
-- 14. PRÉSENCES MEMBRES (6 présences)
-- ============================================================
INSERT INTO member_presences (id, user_id, soul_id, semaine, presences, notes, created_at) VALUES
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'd0000000-0000-0000-0000-000000000002', '2026-06-30', '{"Lundi":true,"Mercredi":true,"Vendredi":false,"Dimanche":true}'::jsonb, 'Absent vendredi.', NOW()),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'd0000000-0000-0000-0000-000000000002', '2026-07-07', '{"Lundi":true,"Mercredi":true,"Vendredi":true,"Dimanche":true}'::jsonb, 'Parfaite !', NOW()),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'd0000000-0000-0000-0000-000000000002', '2026-07-14', '{"Lundi":true,"Mercredi":false,"Vendredi":true,"Dimanche":true}'::jsonb, 'Absent merredi.', NOW()),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'd0000000-0000-0000-0000-000000000002', '2026-07-21', '{"Lundi":true,"Mercredi":true,"Vendredi":true,"Dimanche":false}'::jsonb, 'Absent dimanche.', NOW()),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'd0000000-0000-0000-0000-000000000002', '2026-07-28', '{"Lundi":true,"Mercredi":true,"Vendredi":true,"Dimanche":true}'::jsonb, 'Bonne reprise.', NOW()),
(uuid_generate_v4(), '2cce3c59-629d-4a05-80ec-3857c0a3f3ab', 'd0000000-0000-0000-0000-000000000002', '2026-08-04', '{"Lundi":true,"Mercredi":true,"Vendredi":false,"Dimanche":true}'::jsonb, NULL, NOW());

-- ============================================================
-- 15. HISTORIQUE D'ÂMES (8 entrées)
-- ============================================================
INSERT INTO soul_history (id, ame_id, type_evenement, description, ancien_statut, nouveau_statut, utilisateur_id, created_at) VALUES
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', 'CREATION', 'Création âme', NULL, NULL, 'a0000000-0000-0000-0000-000000000006', NOW() - INTERVAL '365 days'),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', 'BAPTEME', 'Baptême', NULL, NULL, 'a0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '300 days'),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', 'AFFECTATION', 'Chorale', NULL, 'Chorale', 'a0000000-0000-0000-0000-000000000002', NOW() - INTERVAL '280 days'),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', 'AFFECTATION', 'Famille Timothée', NULL, 'Famille Timothée', 'a0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '250 days'),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000002', 'CREATION', 'Martin créé', NULL, NULL, 'a0000000-0000-0000-0000-000000000006', NOW() - INTERVAL '200 days'),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000002', 'TRANSFERT', 'Faiseur2', 'Faiseur1', 'Faiseur2', 'a0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '150 days'),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000005', 'BAPTEME', 'Baptême Petit', NULL, NULL, 'a0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '100 days'),
(uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000005', 'PROMOTION', 'Promu Faiseur', 'Disciple', 'Faiseur', 'a0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '50 days');
