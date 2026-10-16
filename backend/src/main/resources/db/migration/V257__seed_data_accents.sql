-- V257__seed_data_accents.sql
-- ============================================================
-- ACCENTS DES DONNEES DE DEMONSTRATION (seed initial V2 / V4 / V17)
-- Les migrations de jeu de donnees ont ete redigees sans accents
-- (contrainte d'encodage de l'epoque). Resultat a l'ecran du pasteur :
-- « Departement Adultes », « Reunion de famille », « Famille Timothee »,
-- « Sortie evangelistique », « Guerison pour Marie » — un francais mal
-- orthographie dans son propre outil. V256 avait aligne les dictionnaires
-- sur le seed Java ; on corrige ici les instances de donnees.
--
-- Chaque UPDATE cible le litteral d'origine : une valeur reprise ou
-- personnalisee par un utilisateur n'est jamais touchee, et la migration
-- est idempotente (le texte accentue ne re-matche plus).
-- ============================================================

-- Departements (V2 et V17)
UPDATE departments SET nom = 'Département Jeunesse', updated_at = CURRENT_TIMESTAMP WHERE nom = 'Departement Jeunesse';
UPDATE departments SET nom = 'Département Adultes',  updated_at = CURRENT_TIMESTAMP WHERE nom = 'Departement Adultes';
UPDATE departments SET description = 'Ministère de louange et de musique', updated_at = CURRENT_TIMESTAMP
 WHERE description = 'Ministere de louange et de musique';

-- Familles (V2)
UPDATE families SET nom = 'Famille Timothée',  updated_at = CURRENT_TIMESTAMP WHERE nom = 'Famille Timothee';
UPDATE families SET nom = 'Famille Fidèle',    updated_at = CURRENT_TIMESTAMP WHERE nom = 'Famille Fidele';
UPDATE families SET nom = 'Famille Espérance', updated_at = CURRENT_TIMESTAMP WHERE nom = 'Famille Esperance';

-- Comptes de demonstration (V2) : prenoms et noms bibliques
UPDATE users SET first_name = 'Timothée', updated_at = CURRENT_TIMESTAMP WHERE first_name = 'Timothee';
UPDATE users SET last_name = 'Apôtre',     updated_at = CURRENT_TIMESTAMP WHERE last_name = 'Apotre';
UPDATE users SET last_name = 'Fidèle',     updated_at = CURRENT_TIMESTAMP WHERE last_name = 'Fidele';
UPDATE users SET last_name = 'Médecin',    updated_at = CURRENT_TIMESTAMP WHERE last_name = 'Medecin';
UPDATE users SET last_name = 'Évangéliste', updated_at = CURRENT_TIMESTAMP WHERE last_name = 'Evangeliste';

-- Sujets de priere (V4)
UPDATE prayers SET titre = 'Guérison pour Marie Dupont', updated_at = CURRENT_TIMESTAMP
 WHERE titre = 'Guerison pour Marie Dupont';
UPDATE prayers SET titre = 'Exaucement : Guérison de Sophie', updated_at = CURRENT_TIMESTAMP
 WHERE titre = 'Exaucement : Guerison de Sophie';
UPDATE prayers SET description = 'Marie traverse une période difficile de maladie. Demandez la guérison complète.',
                  updated_at = CURRENT_TIMESTAMP
 WHERE description = 'Marie traverse une periode difficile de maladie. Demandez la guerison complete.';
UPDATE prayers SET description = 'Claire s''est éloignée depuis quelques semaines. Demandez son retour.',
                  updated_at = CURRENT_TIMESTAMP
 WHERE description = 'Claire s''est eloignee depuis quelques semaines. Demandez son retour.';
UPDATE prayers SET description = 'Sophie a été guérie après 3 semaines de prière. Merci Seigneur !',
                  updated_at = CURRENT_TIMESTAMP
 WHERE description = 'Sophie a ete guerie apres 3 semaines de priere. Merci Seigneur !';

-- Evenements : table historique `events` (V4) et table lue par l'app `event`
UPDATE events SET titre = 'Réunion de famille mensuelle', updated_at = CURRENT_TIMESTAMP
 WHERE titre = 'Reunion de famille mensuelle';
UPDATE events SET description = 'Réunion de partage et de prière pour toute la famille Timothée.',
                  updated_at = CURRENT_TIMESTAMP
 WHERE description = 'Reunion de partage et de priere pour toute la famille Timothee.';
UPDATE events SET description = 'Invitation au parc pour partager l''Évangile avec les voisins.',
                  updated_at = CURRENT_TIMESTAMP
 WHERE description = 'Invitation au parc pour partager l''Evangile avec les voisins.';
UPDATE events SET titre = 'Sortie d''évangélisation au parc', updated_at = CURRENT_TIMESTAMP
 WHERE titre IN ('Sortie evangelistique au parc', 'Sortie évangélistique au parc');
UPDATE events SET titre = 'Retraite spirituelle de fin d''année', updated_at = CURRENT_TIMESTAMP
 WHERE titre = 'Retraite spirituelle de fin d''annee';
UPDATE events SET lieu = 'Salle Évangile', updated_at = CURRENT_TIMESTAMP WHERE lieu = 'Salle Evangile';

UPDATE event SET title = 'Réunion de famille mensuelle', updated_at = CURRENT_TIMESTAMP
 WHERE title = 'Reunion de famille mensuelle';
UPDATE event SET description = 'Réunion de partage et de prière pour toute la famille Timothée.',
                 updated_at = CURRENT_TIMESTAMP
 WHERE description = 'Reunion de partage et de priere pour toute la famille Timothee.';
UPDATE event SET description = 'Invitation au parc pour partager l''Évangile avec les voisins.',
                 updated_at = CURRENT_TIMESTAMP
 WHERE description = 'Invitation au parc pour partager l''Evangile avec les voisins.';
UPDATE event SET title = 'Sortie d''évangélisation au parc', updated_at = CURRENT_TIMESTAMP
 WHERE title IN ('Sortie evangelistique au parc', 'Sortie évangélistique au parc');
UPDATE event SET title = 'Retraite spirituelle de fin d''année', updated_at = CURRENT_TIMESTAMP
 WHERE title = 'Retraite spirituelle de fin d''annee';
UPDATE event SET lieu = 'Salle Évangile', updated_at = CURRENT_TIMESTAMP WHERE lieu = 'Salle Evangile';

-- Accord fautif laissé par une saisie de test : « Premier rencontre ».
UPDATE event SET title = 'Première rencontre', updated_at = CURRENT_TIMESTAMP
 WHERE title = 'Premier rencontre';
UPDATE events SET titre = 'Première rencontre', updated_at = CURRENT_TIMESTAMP
 WHERE titre = 'Premier rencontre';
