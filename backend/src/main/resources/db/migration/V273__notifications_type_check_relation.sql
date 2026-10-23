-- ============================================================
-- notifications_type_check : miroir EXHAUSTIF de l'enum Java TypeNotification.
--
-- Constat (bug déclarant un supérieur → HTTP 500) : le couple de types apporté
-- par la hiérarchie/relations (V231) — RELATION_DECLAREE, RELATION_REVOQUEE — a
-- bien été ajouté à l'enum Java et au dictionnaire NOTIFICATION_TYPE (V232),
-- MAIS JAMAIS à la contrainte CHECK de `notifications.type`. Cette contrainte a
-- été figée pour la dernière fois en V207 à 25 valeurs.
--
-- Or `MemberRelationService.declare` émet une notification in-app
-- TypeNotification.RELATION_DECLAREE chez l'encadrant déclaré : l'INSERT échoue
-- sur `violates check constraint "notifications_type_check"`, la transaction
-- `@Transactional` est marquée rollback-only et la déclaration entière part en
-- 500 (« An error occurred »). Inversement, `revoke` (RELATION_REVOQUEE)
-- rencontre le même mur. Le self-service d'encadrement est donc TOTALEMENT
-- hors service, quel que soit le type déclaré.
--
-- Correction MONTEUSE et strictement additive : on redéfinit la contrainte
-- comme l'union de la liste V207 et des 2 valeurs manquantes (sur-ensemble
-- strict, aucune valeur existante retirée — même discipline qu'en V207). Les
-- lignes déjà présentes satisfont trivialement le nouvel ensemble.
--
-- Ce fichier est le miroir de TypeNotification ; y reporter TOUT nouveau type
-- d'enum, sous peine du même échec.
-- ============================================================

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (
    type IN (
        'RAPPORT_NON_SOUMIS',
        'ABSENCE_48H',
        'RAPPORT_FAMILLE_NON_SOUMIS',
        'ALERTE_ABSENCE',
        'INFORMATION',
        'PRIERE_EXAUCEE',
        'TRANSFERT_DEMANDE',
        'TRANSFERT_VALIDATION',
        'TRANSFERT_VALIDEE',
        'TRANSFERT_REFUSEE',
        'TRANSFERT_INFOS_DEMANDEES',
        'TRANSFERT_CORRECTION',
        'TRANSFERT_EXECUTEE',
        'TRANSFERT_ANNULEE',
        'TRANSFERT_DELAI_DEPASSE',
        'MEMBRE_AJOUTE',
        'MEMBRE_RETIRE',
        'TACHE_ASSIGNEE',
        'TACHE_EN_RETARD',
        'MEMBRE_AFFECTE',
        'EVENEMENT_RAPPEL',
        'DEMANDE_ACCES',
        'OFFLINE_CONFLIT',
        'SUIVI_RAPPEL',
        'PASTORAT_NOMINATION',
        'RELATION_DECLAREE',
        'RELATION_REVOQUEE'
    )
);
