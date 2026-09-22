-- V4__seed_data_prayers_events.sql
-- Seed data for prayers, events, and enhanced fields

-- ============================================================
-- ENHANCE EXISTING USERS - Add profile fields
-- ============================================================
UPDATE users SET date_naissance = '1985-03-15', situation_familiale = 'MARIE', photo_url = NULL
WHERE id = 'a0000000-0000-0000-0000-000000000001';
UPDATE users SET date_naissance = '1990-07-22', situation_familiale = 'CELIBATAIRE', photo_url = NULL
WHERE id = 'a0000000-0000-0000-0000-000000000002';
UPDATE users SET date_naissance = '1988-11-10', situation_familiale = 'MARIE', photo_url = NULL
WHERE id = 'a0000000-0000-0000-0000-000000000003';
UPDATE users SET date_naissance = '1992-05-05', situation_familiale = 'MARIE', photo_url = NULL
WHERE id = 'a0000000-0000-0000-0000-000000000004';
UPDATE users SET date_naissance = '1995-01-18', situation_familiale = 'CELIBATAIRE', photo_url = NULL
WHERE id = 'a0000000-0000-0000-0000-000000000005';
UPDATE users SET date_naissance = '1993-09-28', situation_familiale = 'CELIBATAIRE', photo_url = NULL
WHERE id = 'a0000000-0000-0000-0000-000000000006';
UPDATE users SET date_naissance = '1991-12-03', situation_familiale = 'MARIE', photo_url = NULL
WHERE id = 'a0000000-0000-0000-0000-000000000007';
UPDATE users SET date_naissance = '1994-06-14', situation_familiale = 'CELIBATAIRE', photo_url = NULL
WHERE id = 'a0000000-0000-0000-0000-000000000008';
UPDATE users SET date_naissance = '1989-04-20', situation_familiale = 'MARIE', photo_url = NULL
WHERE id = 'a0000000-0000-0000-0000-000000000009';

-- ============================================================
-- ENHANCE EXISTING SOULS - Add spiritual fields
-- ============================================================
UPDATE souls SET etat_spirituel = 'EN_CROISSANCE', niveau_croissance = 4, situation_familiale = 'CELIBATAIRE'
WHERE id = 'd0000000-0000-0000-0000-000000000001';
UPDATE souls SET etat_spirituel = 'NOUVEAU_CONVERTI', niveau_croissance = 2, situation_familiale = 'CELIBATAIRE'
WHERE id = 'd0000000-0000-0000-0000-000000000002';
UPDATE souls SET etat_spirituel = 'MATURE', niveau_croissance = 5, situation_familiale = 'MARIE'
WHERE id = 'd0000000-0000-0000-0000-000000000003';
UPDATE souls SET etat_spirituel = 'NOUVEAU_CONVERTI', niveau_croissance = 1, situation_familiale = 'CELIBATAIRE'
WHERE id = 'd0000000-0000-0000-0000-000000000004';
UPDATE souls SET etat_spirituel = 'EN_CROISSANCE', niveau_croissance = 3, situation_familiale = 'MARIE'
WHERE id = 'd0000000-0000-0000-0000-000000000005';
UPDATE souls SET etat_spirituel = 'NOUVEAU_CONVERTI', niveau_croissance = 1, situation_familiale = 'CELIBATAIRE'
WHERE id = 'd0000000-0000-0000-0000-000000000006';
UPDATE souls SET etat_spirituel = 'EN_DIFFICULTE', niveau_croissance = 2, situation_familiale = 'MARIE'
WHERE id = 'd0000000-0000-0000-0000-000000000007';
UPDATE souls SET etat_spirituel = 'NOUVEAU_CONVERTI', niveau_croissance = 1, situation_familiale = 'CELIBATAIRE'
WHERE id = 'd0000000-0000-0000-0000-000000000008';
UPDATE souls SET etat_spirituel = 'EN_CROISSANCE', niveau_croissance = 3, situation_familiale = 'CELIBATAIRE'
WHERE id = 'd0000000-0000-0000-0000-000000000009';
UPDATE souls SET etat_spirituel = 'NOUVEAU_CONVERTI', niveau_croissance = 2, situation_familiale = 'CELIBATAIRE'
WHERE id = 'd0000000-0000-0000-0000-000000000010';

-- ============================================================
-- SAMPLE PRAYERS (Sujets de priere)
-- ============================================================
INSERT INTO prayers (id, auteur_id, famille_id, titre, description, categorie, priorite, statut, visibilite, created_at, updated_at)
VALUES
    ('aa000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001',
     'Guerison pour Marie Dupont', 'Marie traverse une periode difficile de maladie. Demandez la guerison complete.', 'SANTE', 'HAUTE', 'EN_COURS', 'PARTAGEE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('aa000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000001',
     'Emploi pour Jean Martin', 'Jean recherche un emploi depuis 2 mois. Demandez ouverture de portes.', 'TRAVAIL', 'HAUTE', 'EN_COURS', 'PARTAGEE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('aa000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002',
     'Retour de Claire Durand', 'Claire s''est eloignee depuis quelques semaines. Demandez son retour.', 'SPIRITUEL', 'MOYENNE', 'EN_COURS', 'PARTAGEE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('aa000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001',
     'Exaucement : Guerison de Sophie', 'Sophie a ete guerie apres 3 semaines de priere. Merci Seigneur !', 'SANTE', 'MOYENNE', 'EXAUCE', 'PARTAGEE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('aa000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000003',
     'Sagesse pour les examens', 'Sarah passe ses exams la semaine prochaine. Demandez la sagesse.', 'FAMILLE', 'BASSE', 'EN_COURS', 'PRIVEE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- SAMPLE EVENTS (Evenements)
-- ============================================================
INSERT INTO events (id, organisateur_id, famille_id, type_evenement, titre, description, lieu, date_debut, date_fin, limite_places, nb_inscrits, statut, created_at, updated_at)
VALUES
    ('bb000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001',
     'REUNION', 'Reunion de famille mensuelle', 'Reunion de partage et de priere pour toute la famille Timothee.',
     'Salle Evangile', CURRENT_TIMESTAMP + INTERVAL '7 days', CURRENT_TIMESTAMP + INTERVAL '7 days' + INTERVAL '2 hours', 30, 8, 'PLANIFIE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bb000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002',
     'SORTIE', 'Sortie evangelistique au parc', 'Invitation au parc pour partager l''Evangile avec les voisins.',
     'Parc Central', CURRENT_TIMESTAMP + INTERVAL '14 days', CURRENT_TIMESTAMP + INTERVAL '14 days' + INTERVAL '3 hours', 50, 12, 'PLANIFIE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bb000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003',
     'FORMATION', 'Formation des nouveaux faiseurs', 'Session de formation pour les faiseurs nouvellement promus.',
     'Salle Pierre', CURRENT_TIMESTAMP + INTERVAL '21 days', CURRENT_TIMESTAMP + INTERVAL '21 days' + INTERVAL '4 hours', 15, 5, 'PLANIFIE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bb000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000004',
     'RETRAITE', 'Retraite spirituelle de fin d''annee', '3 jours de retraite pour approfondir la foi.',
     'Centre Spirituel Montagne', CURRENT_TIMESTAMP + INTERVAL '60 days', CURRENT_TIMESTAMP + INTERVAL '62 days', 40, 18, 'PLANIFIE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- SAMPLE EVENT REGISTRATIONS
-- ============================================================
INSERT INTO event_registrations (id, event_id, utilisateur_id, statut_inscription, date_inscription)
VALUES
    ('cc000000-0000-0000-0000-000000000001', 'bb000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000006', 'INSCRIT', CURRENT_TIMESTAMP),
    ('cc000000-0000-0000-0000-000000000002', 'bb000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000007', 'INSCRIT', CURRENT_TIMESTAMP),
    ('cc000000-0000-0000-0000-000000000003', 'bb000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000008', 'INSCRIT', CURRENT_TIMESTAMP);

-- ============================================================
-- SAMPLE SOUL NOTES (Notes libres sur âme)
-- ============================================================
INSERT INTO soul_notes (id, ame_id, auteur_id, contenu, created_at, updated_at)
VALUES
    ('dd000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004',
     'Rencontre avec Marie aujourd''hui. Elle semble plus motivee apres notre discussion sur la priere.',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dd000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000007',
     'Claire a eu un moment difficile cette semaine. J''ai partage avec elle les Psaumes 23 et 91.',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dd000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000008',
     'Sarah a termine ses exams avec succes ! Louange a Dieu. Elle souhaite participer au groupe de louange.',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
